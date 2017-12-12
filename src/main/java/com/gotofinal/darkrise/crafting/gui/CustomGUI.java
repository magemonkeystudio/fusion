package com.gotofinal.darkrise.crafting.gui;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.gotofinal.darkrise.crafting.DarkRiseCrafting;
import com.gotofinal.darkrise.crafting.InventoryPattern;
import com.gotofinal.darkrise.crafting.gui.slot.Slot;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

public class CustomGUI implements Listener
{
    protected final String name;
    protected final String inventoryName;
    protected final           Slot[]  slots        = new Slot[9 * 6];
    protected final transient IntList resultSlots  = new IntArrayList(20);
    protected final transient IntList blockedSlots = new IntArrayList(20);
    protected final transient int              nextPage;
    protected final transient int              prevPage;
    protected final           InventoryPattern pattern;

    protected final Map<Player, PlayerCustomGUI> map = new ConcurrentHashMap<>(20);

    public CustomGUI(String name, String inventoryName, InventoryPattern pattern)
    {
        this.name = name;
        this.inventoryName = inventoryName;
        this.pattern = pattern;
        int k = - 1;
        int prevPage = - 1, nextPage = - 1;
        for (String row : this.pattern.getPattern())
        {
            for (char c : row.toCharArray())
            {
                k++;
                if (c == '=' || c == 'o')
                {
                    this.slots[k] = Slot.BASE_RESULT_SLOT;
                    this.resultSlots.add(k);
                }
                else if (c == '>')
                {
                    this.slots[k] = Slot.BLOCKED_SLOT;
                    nextPage = k;
                }
                else if (c == '<')
                {
                    this.slots[k] = Slot.BLOCKED_SLOT;
                    prevPage = k;
                }
                else
                {
                    this.slots[k] = Slot.BLOCKED_SLOT;
                    this.blockedSlots.add(k);
                }
            }
        }
        this.nextPage = nextPage;
        this.prevPage = prevPage;
        Bukkit.getPluginManager().registerEvents(this, DarkRiseCrafting.getInstance());
    }

    public String getName()
    {
        return this.name;
    }

    public String getInventoryName()
    {
        return this.inventoryName;
    }

    public InventoryPattern getPattern() {
        return pattern;
    }

    public void setSlot(int i, Slot slot)
    {
        this.slots[i] = slot;
    }

    public Slot getSlot(int i)
    {
        return this.slots[i];
    }

    public PlayerCustomGUI open(Player p, PlayerCustomGUI playerCustomGUI)
    {
        InventoryView iv = p.getOpenInventory();
        if ((iv != null) && (iv.getTopInventory() != null))
        {
            this.map.remove(p);
            p.closeInventory();
        }

        if (playerCustomGUI != null)
        {
            this.map.put(p, playerCustomGUI);
        }
        return playerCustomGUI;
    }

    private boolean isThis(Inventory inv)
    {
        return inv.getTitle().equals(ChatColor.translateAlternateColorCodes('&', this.inventoryName));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent e)
    {
        Inventory inv = e.getView().getTopInventory();
        if (e.getRawSlot() < 0)
        {
            return;
        }
//        System.out.println("CLICK(" + e.getRawSlot() + ")..." + (e.getRawSlot() >= this.slots.length ? "NOPE" : this.slots[e.getRawSlot()]) + ", " + e.getAction() + ", " + inv + ", " + this.isThis(inv));
        if ((inv == null) || (! (e.getWhoClicked() instanceof Player)) || ! this.isThis(inv))
        {
//            System.out.println("Ugh, fail!");
            return;
        }
        Player p = (Player) e.getWhoClicked();
        PlayerCustomGUI playerCustomGUI = this.map.get(p);
        if (playerCustomGUI == null)
        {
            return;
        }
        playerCustomGUI.onClick(e);
    }

    private void reloadRecipeTask(Player p)
    {
        DarkRiseCrafting.getInstance().runSync(() -> this.reloadRecipe(p));
    }

    private void reloadRecipe(Player p)
    {
        PlayerCustomGUI playerCustomGUI = this.map.get(p);
        if (playerCustomGUI == null)
        {
            return;
        }
        playerCustomGUI.reloadRecipes();
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent e)
    {
        Inventory inv = e.getView().getTopInventory();
        if ((inv == null) || ! (e.getWhoClicked() instanceof Player))
        {
            return;
        }
        if (this.isThis(inv))
        {
            if (e.getRawSlots().stream().anyMatch(i -> (i < this.slots.length) && (! Objects.equals(this.slots[i], Slot.SPECIAL_CRAFTING_SLOT))))
            {
                e.setResult(Result.DENY);
                return;
            }

            if (e.getNewItems().values().stream().anyMatch(i -> Slot.SPECIAL_CRAFTING_SLOT.canHoldItem(i) == null))
            {
                e.setResult(Result.DENY);
            }
            this.reloadRecipeTask((Player) e.getWhoClicked());
        }
    }

    private void onClose(Player p)
    {
        InventoryView v = p.getOpenInventory();
        if (v != null)
        {
            this.onClose(p, v.getTopInventory());
        }
    }

    private void onClose(Player p, Inventory inv)
    {
        if (inv == null)
        {
            return;
        }
        Inventory pInventory = p.getInventory();
        if (this.isThis(inv))
        {
            for (int i = 0; i < this.slots.length; i++)
            {
                if (this.slots[i].equals(Slot.BLOCKED_SLOT) || this.slots[i].equals(Slot.BASE_RESULT_SLOT))
                {
                    continue;
                }
                ItemStack it = inv.getItem(i);
                if (it != null)
                {
                    pInventory.addItem(it)
                            .values()
                            .stream()
                            .filter(Objects::nonNull)
                            .forEach(itemStack -> p.getWorld().dropItem(p.getLocation(), itemStack));
                }
            }
            this.map.remove(p);
            inv.clear();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onClose(PlayerPickupItemEvent e)
    {
        PlayerCustomGUI customGUI = this.map.get(e.getPlayer());
        if (customGUI == null)
        {
            return;
        }
        customGUI.reloadRecipesTask();
    }

    @EventHandler(ignoreCancelled = true)
    public void onClose(InventoryCloseEvent e)
    {
        if (e.getPlayer() instanceof Player)
        {
            this.onClose((Player) e.getPlayer(), e.getInventory());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onExit(PlayerQuitEvent e)
    {
        this.onClose(e.getPlayer());
    }

    public void closeAll()
    {
        new ArrayList<>(this.map.keySet()).forEach(h -> {
            this.onClose(h);
            h.closeInventory();
        });
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .appendSuper(super.toString())
                .append("name", this.name)
                .append("inventoryName", this.inventoryName)
                .append("slots", this.slots)
                .append("pattern", this.pattern)
                .toString();
    }
}