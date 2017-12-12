package com.gotofinal.darkrise.crafting.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import com.gotofinal.darkrise.crafting.CalculatedRecipe;
import com.gotofinal.darkrise.crafting.Category;
import com.gotofinal.darkrise.crafting.CraftingTable;
import com.gotofinal.darkrise.crafting.DarkRiseCrafting;
import com.gotofinal.darkrise.crafting.LevelFunction;
import com.gotofinal.darkrise.crafting.MasteryManager;
import com.gotofinal.darkrise.crafting.Recipe;
import com.gotofinal.darkrise.crafting.RecipeItem;
import com.gotofinal.darkrise.crafting.Utils;
import com.gotofinal.darkrise.crafting.cfg.Cfg;
import com.gotofinal.darkrise.crafting.gui.slot.Slot;
import com.gotofinal.darkrise.economy.DarkRiseEconomy;
import com.gotofinal.darkrise.economy.DarkRiseItems;
import com.gotofinal.darkrise.spigot.core.Vault;
import com.gotofinal.darkrise.spigot.core.utils.ExperienceManager;
import com.gotofinal.darkrise.spigot.core.utils.ItemUtils;

import com.gotofinal.darkrise.spigot.core.utils.cmds.DelayedCommand;
import com.gotofinal.darkrise.spigot.core.utils.cmds.R;
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
import org.bukkit.inventory.meta.ItemMeta;

public class PlayerCustomGUI implements Listener
{
    private final CustomGUI gui;
    private final Player    player;
    private final Inventory inventory;
    private final Category  category;

    private       int page    = 0;
    private final int maxPage = 0;
    private final Int2ObjectMap<CalculatedRecipe> recipes;

    public PlayerCustomGUI(CustomGUI gui, Player player, Inventory inventory, Category category)
    {
        this.gui = gui;
        this.player = player;
        this.inventory = inventory;
        this.recipes = new Int2ObjectOpenHashMap<>(20);
        this.category = category;
    }

    public void reloadRecipesTask()
    {
        DarkRiseCrafting.getInstance().runSync(this::reloadRecipes);
    }

    public void reloadRecipes()
    {
        try
        {
            CraftingTable table = Cfg.getTable(this.gui.name);
            Collection<Recipe> allRecipes = new HashSet<>(table.getRecipes().values());
            allRecipes.removeIf(r -> r.getNeededLevels() > LevelFunction.getLevel(player) + 5);
            allRecipes.removeIf(r -> !MasteryManager.hasMastery(player, gui.name));
            allRecipes.removeIf(r -> !Utils.hasCraftingPermission(player, r.getName()));
            allRecipes.removeIf(r -> !category.getRecipes().contains(r));
            int pageSize = this.gui.resultSlots.size();
            int allRecipeCount = allRecipes.size();
            int i = 0;
            int page = this.page;

            int fullPages = allRecipeCount / pageSize;
            int rest = allRecipeCount % pageSize;
            int pages = (rest == 0) ? fullPages : (fullPages + 1);
            if (page > pages)
            {
                this.page = pages - 1;
                this.reloadRecipes();
                return;
            }

            Collection<ItemStack> playerItems = getPlayerItems(this.player);
            CalculatedRecipe[] calculatedRecipes = new CalculatedRecipe[(page < pages) ? pageSize : ((rest == 0) ? pageSize : rest)];
            Recipe[] allRecipesArray = allRecipes.toArray(new Recipe[allRecipeCount]);

            int[] slots = this.gui.resultSlots.toIntArray();
            for (int slot : slots)
            {
                this.inventory.setItem(slot, null);
            }
            for (int k = (page * pageSize), e = Math.min(slots.length, calculatedRecipes.length); (k < allRecipesArray.length) && (i < e); k++, i++)
            {
                Recipe recipe = allRecipesArray[k];
                int slot = slots[i];
                CalculatedRecipe calculatedRecipe = CalculatedRecipe.create(recipe, playerItems, this.player);
                this.recipes.put(slot, calculatedRecipes[i] = calculatedRecipe);
                this.inventory.setItem(slot, calculatedRecipe.getIcon().clone());
            }
        }
        catch (Exception e)
        {
            this.inventory.clear();
            this.player.closeInventory();
            throw new RuntimeException("Exception was thrown when reloading recipes for: " + this.player.getName(), e);
        }
    }

    public static Collection<ItemStack> getPlayerItems(InventoryHolder player)
    {
        DarkRiseItems itemsRegistry = DarkRiseEconomy.getItemsRegistry();
        ItemStack[] contents = ItemUtils.compact(false, player.getInventory().getContents());
        List<ItemStack> result = new ArrayList<>(contents.length);
        for (ItemStack content : contents)
        {
            if (content == null)
            {
                continue;
            }
//            DarkRiseItem item = itemsRegistry.getItemByStack(content);
//            if (item == null)
//            {
//                continue;
//            }
            result.add(content);
        }
        return result;
    }

    public static PlayerCustomGUI open(CustomGUI gui, Player player, Category category)
    {
        Inventory inv = null;
        try
        {
            inv = Bukkit.createInventory(player, gui.slots.length, ChatColor.translateAlternateColorCodes('&', gui.inventoryName));
            int k = - 1;
            Char2ObjectMap<ItemStack> items = gui.pattern.getItems();
            for (String row : gui.pattern.getPattern())
            {
                for (char c : row.toCharArray())
                {
                    k++;
                    ItemStack item = items.get(c);
                    if (item != null)
                    {
                        inv.setItem(k, item.clone());
                    }
                }
            }
            PlayerCustomGUI playerCustomGUI = new PlayerCustomGUI(gui, player, inv, category);
            gui.open(player, playerCustomGUI);
            player.openInventory(inv);
            playerCustomGUI.reloadRecipesTask();
            return playerCustomGUI;
        }
        catch (Exception e)
        {
            if (inv != null)
            {
                inv.clear();
            }
            player.closeInventory();
            throw new RuntimeException("Exception was thrown on gui open for: " + player.getName(), e);
        }
    }

    public void onClick(InventoryClickEvent e)
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

        Character c = gui.getPattern().getSlot(e.getRawSlot());
        Collection<DelayedCommand> patternCommands = gui.getPattern().getCommands(c);
        if (patternCommands != null && ! patternCommands.isEmpty())
        {
            DelayedCommand.invoke(DarkRiseCrafting.getInstance(), e.getWhoClicked(), patternCommands,
                    R.r("{crafting}", this.gui.getName()),
                    R.r("{inventoryName}", this.gui.getInventoryName()));
        }
//        System.out.println("CLICK(" + e.getRawSlot() + ")..." + this.slots[e.getRawSlot()] + ", " + e.getAction() + ", Crafts: " + Arrays.toString(this
// .craftingSlots.toArray()) + ", Results: " + Arrays.toString(this.resultSlots.toArray()) + ", Blockeds: " + Arrays.toString(this.blockedSlots.toArray()));
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
            DarkRiseCrafting.getInstance().runSync(() ->
                                                   {
                                                       this.reloadRecipes();
                                                       this.craft(e.getRawSlot(), true);
                                                       this.reloadRecipesTask();
                                                   });
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
        CraftingTable table = Cfg.getTable(this.gui.name);
        Collection<Recipe> allRecipes = table.getRecipes().values();
        int pageSize = this.gui.resultSlots.size();
        int allRecipeCount = allRecipes.size();
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
            PlayerInitialGUI.open(gui, player);
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

    private boolean craft(int slot, boolean addToCursor)
    {
        CalculatedRecipe calculatedRecipe = this.recipes.get(slot);
        if ((calculatedRecipe == null) || ! calculatedRecipe.isCanCraft())
        {
            return false;
        }
//        this.reloadRecipes();
        if (! Objects.equals(this.recipes.get(slot), calculatedRecipe))
        {
            return false;
        }
        Recipe recipe = calculatedRecipe.getRecipe();
        if (LevelFunction.getLevel(player) < recipe.getNeededLevels())
        {
            return false;
        }
        if (ExperienceManager.getTotalExperience(this.player) < recipe.getNeededXp())
        {
            return false;
        }
        if (! Vault.canPay(this.player, recipe.getPrice()))
        {
            return false;
        }
        if (! MasteryManager.hasMastery(player, gui.name))
        {
            return false;
        }

        RecipeItem recipeResult = recipe.getResult();
        ItemStack resultItem = recipeResult.getItemStack();

        //Add "Crafted by"
        if (player.hasPermission("crafting.craftedby." + recipe.getName()))
        {
            ItemMeta meta = resultItem.getItemMeta();
            List<String> lore = meta.getLore();
            lore.add(ChatColor.WHITE + " - " + ChatColor.YELLOW + "Crafted by: " + ChatColor.WHITE + player.getName());
            meta.setLore(lore);
            resultItem.setItemMeta(meta);
        }

        ItemStack cursor = this.player.getItemOnCursor();
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

        Collection<ItemStack> itemsToTake = recipe.getItemsToTake();
        Collection<ItemStack> taken = new ArrayList<>(itemsToTake.size());
        PlayerInventory inventory = this.player.getInventory();

        for (Iterator<ItemStack> iterator = itemsToTake.iterator(); iterator.hasNext(); )
        {
            ItemStack toTake = iterator.next();
            HashMap<Integer, ItemStack> notRemoved = inventory.removeItem(toTake);
            if (notRemoved.isEmpty())
            {
                taken.add(toTake);
                iterator.remove();
                continue;
            }
            for (ItemStack itemStack : taken)
            {
                HashMap<Integer, ItemStack> notAdded = inventory.addItem(itemStack);
                if (notAdded.isEmpty())
                {
                    break;
                }
                for (ItemStack stack : notAdded.values())
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
                this.player.setItemOnCursor(resultItem);
            }
        }
        else
        {
            HashMap<Integer, ItemStack> notAdded = inventory.addItem(resultItem);
            if (notAdded.isEmpty())
            {
                return true;
            }
            for (ItemStack stack : notAdded.values())
            {
                this.player.getWorld().dropItemNaturally(this.player.getLocation(), stack);
            }
        }

        //Commands
        DelayedCommand.invoke(DarkRiseCrafting.getInstance(), player, recipe.getCommands());

        return true;
    }
}
