package com.gotofinal.darkrise.crafting.gui;

import com.gotofinal.darkrise.crafting.Category;
import com.gotofinal.darkrise.crafting.CraftingTable;
import com.gotofinal.darkrise.crafting.cfg.Cfg;
import it.unimi.dsi.fastutil.chars.Char2ObjectMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class PlayerInitialGUI extends PlayerCustomGUI {
    private final CustomGUI gui;
    private final Map<Integer, Category> slotMap = new HashMap<>();

    private PlayerInitialGUI(CustomGUI gui, Player player, Inventory inventory) {
        super(gui, player, inventory, null);
        this.gui = gui;
    }

    public static PlayerInitialGUI open(CustomGUI gui, Player player)
    {
        InventoryView iv = player.getOpenInventory();
        if ((iv != null) && (iv.getTopInventory() != null))
        {
            gui.map.remove(player);
            player.closeInventory();
        }

        Inventory inv = null;
        try
        {
            inv = Bukkit.createInventory(player, gui.slots.length, ChatColor.translateAlternateColorCodes('&', gui.inventoryName));
            int k = - 1;
            Char2ObjectMap<ItemStack> items = gui.pattern.getItems();
            PlayerInitialGUI playerCustomGUI = new PlayerInitialGUI(gui, player, inv);
            CraftingTable table = Cfg.getTable(gui.name);
            Iterator<Category> categoryIterator = table.getCategories().values().iterator();

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

                    //Slots
                    if (c == 'o' && categoryIterator.hasNext())
                    {
                        Category category = categoryIterator.next();
                        inv.setItem(k, category.getIconItem().getItem());
                        playerCustomGUI.slotMap.put(k, category);
                    }
                }
            }

            gui.open(player, playerCustomGUI);
            player.openInventory(inv);
            gui.map.put(player, playerCustomGUI);
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

    @Override
    public void onClick(InventoryClickEvent e) {
        Category category = slotMap.get(e.getSlot());

        if (category != null)
        {
            e.getWhoClicked().closeInventory();
            PlayerCustomGUI.open(gui, (Player) e.getWhoClicked(), category);
            e.setCancelled(true);
        }
    }
}
