package com.gotofinal.darkrise.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.caversia.plugins.economy.model.CustomItem;
import com.caversia.plugins.economy.persistence.ItemsRepository;
import com.gotofinal.darkrise.CalculatedRecipe;
import com.gotofinal.darkrise.CraftingTable;
import com.gotofinal.darkrise.DarkRiseCrafting;
import com.gotofinal.darkrise.Recipe;
import com.gotofinal.darkrise.RecipeItem;
import com.gotofinal.darkrise.cfg.Cfg;
import com.gotofinal.darkrise.core.Vault;
import com.gotofinal.darkrise.core.utils.ItemUtils;
import com.gotofinal.darkrise.gui.slot.Slot;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import it.unimi.dsi.fastutil.chars.Char2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class PlayerCustomGUI implements Listener
{
    private final CustomGUI gui;
    private final Player    player;
    private final Inventory inventory;

    private       int page    = 0;
    private final int maxPage = 0;
    private final Int2ObjectMap<CalculatedRecipe> recipes;

    private PlayerCustomGUI(final CustomGUI gui, final Player player, final Inventory inventory)
    {
        this.gui = gui;
        this.player = player;
        this.inventory = inventory;
        this.recipes = new Int2ObjectOpenHashMap<>(20);
    }

    public void reloadRecipesTask()
    {
        DarkRiseCrafting.getInstance().runSync(this::reloadRecipes);
    }

    public void reloadRecipes()
    {
        try
        {
            final CraftingTable table = Cfg.getTable(this.gui.name);
            final Collection<Recipe> allRecipes = table.getRecipes().values();
            final int pageSize = this.gui.resultSlots.size();
            final int allRecipeCount = allRecipes.size();
            int i = 0;
            int page = this.page;

            int fullPages = allRecipeCount / pageSize;
            int rest = allRecipeCount % pageSize;
            int pages = (rest == 0) ? fullPages : (fullPages + 1);
            if (page >= pages)
            {
                this.page = pages - 1;
                this.reloadRecipes();
                return;
            }

            final Collection<ItemStack> playerItems = getPlayerItems(this.player);
            final CalculatedRecipe[] calculatedRecipes = new CalculatedRecipe[(page < pages) ? pageSize : ((rest == 0) ? pageSize : rest)];
            final Recipe[] allRecipesArray = allRecipes.toArray(new Recipe[allRecipeCount]);

            final int[] slots = this.gui.resultSlots.toIntArray();
            for (final int slot : slots)
            {
                this.inventory.setItem(slot, null);
            }
            for (int k = (page * pageSize), e = Math.min(slots.length, calculatedRecipes.length); (k < allRecipesArray.length) && (i < e); k++, i++)
            {
                Recipe recipe = allRecipesArray[k];
                final int slot = slots[i];
                final CalculatedRecipe calculatedRecipe = CalculatedRecipe.create(recipe, playerItems, this.player);
                this.recipes.put(slot, calculatedRecipes[i] = calculatedRecipe);
                this.inventory.setItem(slot, calculatedRecipe.getIcon().clone());
            }
        } catch (Exception e)
        {
            this.inventory.clear();
            this.player.closeInventory();
            throw new RuntimeException("Exception was thrown when reloading recipes for: " + this.player.getName(), e);
        }
    }

    public static Collection<ItemStack> getPlayerItems(InventoryHolder player)
    {
        final ItemsRepository ir = ItemsRepository.INSTANCE;
        final ItemStack[] contents = ItemUtils.compact(false, player.getInventory().getContents());
        final List<ItemStack> result = new ArrayList<>(contents.length);
        for (final ItemStack content : contents)
        {
            if (content == null)
            {
                continue;
            }
            final Optional<CustomItem> item = ir.getItem(content);
            if (! item.isPresent())
            {
                continue;
            }
            result.add(content);
        }
        return result;
    }

    public static PlayerCustomGUI open(final CustomGUI gui, final Player player)
    {
        Inventory inv = null;
        try
        {
            inv = Bukkit.createInventory(player, gui.slots.length, ChatColor.translateAlternateColorCodes('&', gui.inventoryName));
            int k = - 1;
            final Char2ObjectMap<ItemStack> items = gui.pattern.getItems();
            for (final String row : gui.pattern.getPattern())
            {
                for (final char c : row.toCharArray())
                {
                    k++;
                    final ItemStack item = items.get(c);
                    if (item != null)
                    {
                        inv.setItem(k, item.clone());
                    }
                }
            }
            player.openInventory(inv);
            final PlayerCustomGUI playerCustomGUI = new PlayerCustomGUI(gui, player, inv);
            playerCustomGUI.reloadRecipesTask();
            return playerCustomGUI;
        } catch (Exception e)
        {
            if (inv != null)
            {
                inv.clear();
            }
            player.closeInventory();
            throw new RuntimeException("Exception was thrown on gui open for: " + player.getName(), e);
        }
    }

    public void onClick(final InventoryClickEvent e)
    {
        if ((e.getRawSlot() >= this.gui.slots.length))
        {
            return;
        }
        if (e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY)
        {
            e.setResult(Result.DENY);
            return;
        }
//        System.out.println("CLICK(" + e.getRawSlot() + ")..." + this.slots[e.getRawSlot()] + ", " + e.getAction() + ", Crafts: " + Arrays.toString(this.craftingSlots.toArray()) + ", Results: " + Arrays.toString(this.resultSlots.toArray()) + ", Blockeds: " + Arrays.toString(this.blockedSlots.toArray()));
        if (this.gui.slots[e.getRawSlot()].equals(Slot.BLOCKED_SLOT))
        {
//            System.out.println("CLICK_DENY-(" + e.getRawSlot() + "): " + this.slots[e.getRawSlot()]);
//e.setCancelled(true);
            e.setResult(Result.DENY);
            if ((this.gui.nextPage != - 1) && (e.getSlot() == this.gui.nextPage))
            {
                this.nextPage();
                return;
            }
            if ((this.gui.prevPage != - 1) && (e.getSlot() == this.gui.prevPage))
            {
                this.prevPage();
                return;
            }
            return;
        }
        if (this.gui.slots[e.getRawSlot()].equals(Slot.BASE_RESULT_SLOT))
        {
//            switch (e.getAction())
//            {
//                case NOTHING:
//                case PLACE_SOME:
//                case PLACE_ONE:
//                case SWAP_WITH_CURSOR:
//                case DROP_ALL_CURSOR:
//                case DROP_ONE_CURSOR:
//                case DROP_ALL_SLOT:
//                case DROP_ONE_SLOT:
//                case HOTBAR_MOVE_AND_READD:
//                case HOTBAR_SWAP:
//                case CLONE_STACK:
//                case UNKNOWN:
//                case MOVE_TO_OTHER_INVENTORY:
//                case COLLECT_TO_CURSOR:
//                default:
//                    e.setCancelled(true);
//                    e.setResult(Result.DENY);
//                case PICKUP_ALL:
//                case PICKUP_SOME:
//                case PICKUP_HALF:
//                case PICKUP_ONE:
//                    break;
//                case PLACE_ALL:
//                    e.setCurrentItem(null);
//                    e.setCancelled(true);
//                    e.setResult(Result.DENY);
//                    DarkRiseCrafting.getInstance().runSync(() -> this.craft(e.getRawSlot(), true));
//                    break;
//            }
            e.setCancelled(true);
            e.setResult(Result.DENY);
            DarkRiseCrafting.getInstance().runSync(() -> this.craft(e.getRawSlot(), true));
            return;
        }
        if (e.getCursor().getType() != Material.AIR)
        {

//            System.out.println(e.getCursor() + ", " + e.getCurrentItem());
            if (Slot.SPECIAL_CRAFTING_SLOT.canHoldItem(e.getCursor()) == null)
            {
//e.setCancelled(true);
                e.setResult(Result.DENY);
                return;
            }
        }
        this.reloadRecipesTask();
    }

    private boolean validatePageCount()
    {
        if (this.page <= 0)
        {
            this.reloadRecipesTask();
            return false;
        }
        final CraftingTable table = Cfg.getTable(this.gui.name);
        final Collection<Recipe> allRecipes = table.getRecipes().values();
        final int pageSize = this.gui.resultSlots.size();
        final int allRecipeCount = allRecipes.size();
        int i = 0;
        int page = this.page;

        int fullPages = allRecipeCount / pageSize;
        int rest = allRecipeCount % pageSize;
        int pages = (rest == 0) ? fullPages : (fullPages + 1);
        if (page > pages)
        {
            this.page = pages;
            this.reloadRecipesTask();
            return false;
        }
        return true;
    }

    private void prevPage()
    {
        if (this.page <= 0)
        {
            return;
        }
        this.page--;
        if (this.validatePageCount())
        {
            this.reloadRecipesTask();
        }
    }

    private void nextPage()
    {
        this.page++;
        if (this.validatePageCount())
        {
            this.reloadRecipesTask();
        }
    }

    private boolean craft(final int slot, final boolean addToCursor)
    {
        final CalculatedRecipe calculatedRecipe = this.recipes.get(slot);
        if ((calculatedRecipe == null) || ! calculatedRecipe.isCanCraft())
        {
            return false;
        }
        this.reloadRecipes();
        if (! Objects.equals(this.recipes.get(slot), calculatedRecipe))
        {
            return false;
        }
        final Recipe recipe = calculatedRecipe.getRecipe();
        if (! Vault.canPay(this.player, recipe.getPrice()))
        {
            return false;
        }

        final RecipeItem recipeResult = recipe.getResult();
        final ItemStack resultItem = recipeResult.getItemStack();
        final ItemStack cursor = this.player.getItemOnCursor();
        if (addToCursor)
        {
            if (resultItem.isSimilar(cursor))
            {
                if ((resultItem.getAmount() + cursor.getAmount()) > resultItem.getMaxStackSize())
                {
                    return false;
                }
            }
            else if ((cursor != null) && (cursor.getType() != Material.AIR))
            {
                return false;
            }
        }

        final Collection<ItemStack> itemsToTake = recipe.getItemsToTake();
        final Collection<ItemStack> taken = new ArrayList<>(itemsToTake.size());
        final PlayerInventory inventory = this.player.getInventory();

        for (Iterator<ItemStack> iterator = itemsToTake.iterator(); iterator.hasNext(); )
        {
            final ItemStack toTake = iterator.next();
            final HashMap<Integer, ItemStack> notRemoved = inventory.removeItem(toTake);
            if (notRemoved.isEmpty())
            {
                taken.add(toTake);
                iterator.remove();
                continue;
            }
            for (final ItemStack itemStack : taken)
            {
                final HashMap<Integer, ItemStack> notAdded = inventory.addItem(itemStack);
                if (notAdded.isEmpty())
                {
                    break;
                }
                for (final ItemStack stack : notAdded.values())
                {
                    this.player.getWorld().dropItemNaturally(this.player.getLocation(), stack);
                }
            }
            break;
        }
        if (! itemsToTake.isEmpty())
        {
            return false;
        }

        Vault.pay(this.player, recipe.getPrice());
        if (addToCursor)
        {
            if ((cursor != null) && (cursor.getType() != Material.AIR))
            {
                cursor.setAmount(cursor.getAmount() + recipe.getResult().getAmount());
                this.player.setItemOnCursor(cursor);
            }
            else
            {
                this.player.setItemOnCursor(recipe.getResult().getItemStack());
            }
        }
        else
        {
            final HashMap<Integer, ItemStack> notAdded = inventory.addItem(resultItem);
            if (notAdded.isEmpty())
            {
                return true;
            }
            for (final ItemStack stack : notAdded.values())
            {
                this.player.getWorld().dropItemNaturally(this.player.getLocation(), stack);
            }
        }
        return true;
    }
}
