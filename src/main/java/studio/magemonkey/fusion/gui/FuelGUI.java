package studio.magemonkey.fusion.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.Cfg;
import studio.magemonkey.fusion.cfg.FuelItem;
import studio.magemonkey.fusion.cfg.FuelManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * GUI for the server fuel tank.
 *
 * Layout (9 slots per row):
 *   [fuel items — one slot each]   [status item — always last slot]
 *
 * Players click a fuel item slot to consume one from their inventory and add fuel.
 * The last slot always shows current/max fuel.
 */
public class FuelGUI implements Listener {

    private static final HashMap<UUID, FuelGUI> openGuis = new HashMap<>();

    private final Player      player;
    private final Inventory   inventory;
    /** Maps inventory slot index → FuelItem for click handling */
    private final HashMap<Integer, FuelItem> slotToFuelItem = new HashMap<>();

    private FuelGUI(Player player) {
        this.player    = player;
        int itemCount  = Cfg.fuelItems.size();
        // Choose size: smallest multiple of 9 that fits all items + 1 status slot
        int size       = Math.max(9, (int) Math.ceil((itemCount + 1) / 9.0) * 9);
        this.inventory = Bukkit.createInventory(null, size, ChatColor.GOLD + "Fuel");
        populate();
        player.openInventory(inventory);
        openGuis.put(player.getUniqueId(), this);
    }

    // ──────────────────────────────────────────────
    // Open / close
    // ──────────────────────────────────────────────

    public static void open(Player player) {
        // Close existing instance if open
        FuelGUI existing = openGuis.get(player.getUniqueId());
        if (existing != null) {
            openGuis.remove(player.getUniqueId());
        }
        FuelGUI gui = new FuelGUI(player);
        Fusion.registerListener(gui);
    }

    public static void closeAll() {
        for (FuelGUI gui : new ArrayList<>(openGuis.values())) {
            gui.player.closeInventory();
        }
        openGuis.clear();
    }

    // ──────────────────────────────────────────────
    // Rendering
    // ──────────────────────────────────────────────

    private void populate() {
        inventory.clear();
        slotToFuelItem.clear();

        int size = inventory.getSize();

        // Place fuel items in consecutive slots (all but the last)
        List<FuelItem> items = Cfg.fuelItems;
        for (int i = 0; i < items.size() && i < size - 1; i++) {
            FuelItem fi = items.get(i);
            inventory.setItem(i, fi.createDisplayItem());
            slotToFuelItem.put(i, fi);
        }

        // Fill remaining slots (between items and status) with glass pane
        for (int i = items.size(); i < size - 1; i++) {
            inventory.setItem(i, makeFiller());
        }

        // Last slot — status item
        inventory.setItem(size - 1, makeStatusItem());
    }

    private ItemStack makeStatusItem() {
        int current = FuelManager.getFuel();
        int max     = FuelManager.getMaxFuel();

        // Choose material based on fill level
        Material mat;
        if (current == 0)                      mat = Material.BARRIER;
        else if (current < max / 3)            mat = Material.RED_STAINED_GLASS;
        else if (current < (max * 2) / 3)     mat = Material.YELLOW_STAINED_GLASS;
        else                                   mat = Material.GREEN_STAINED_GLASS;

        ItemStack item = new ItemStack(mat);
        ItemMeta  meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "⛽ Fuel");
            meta.setLore(List.of(
                    ChatColor.YELLOW + "Current: " + ChatColor.WHITE + current + " / " + max,
                    buildBar(current, max),
                    "",
                    ChatColor.GRAY + "Click fuel items on the left to add fuel"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack makeFiller() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta  meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RESET + "");
            pane.setItemMeta(meta);
        }
        return pane;
    }

    /** Renders a simple text bar: ██████░░░░ */
    private static String buildBar(int current, int max) {
        int total  = 20;
        int filled = (max > 0) ? (int) Math.round((double) current / max * total) : 0;
        StringBuilder sb = new StringBuilder(ChatColor.YELLOW + "[");
        for (int i = 0; i < total; i++) {
            sb.append(i < filled ? ChatColor.GREEN + "█" : ChatColor.DARK_GRAY + "░");
        }
        sb.append(ChatColor.YELLOW + "]");
        return sb.toString();
    }

    // ──────────────────────────────────────────────
    // Events
    // ──────────────────────────────────────────────

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;
        if (!p.getUniqueId().equals(player.getUniqueId())) return;
        if (!event.getInventory().equals(inventory)) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        // Ignore clicks in the player's own inventory section
        if (slot < 0 || slot >= inventory.getSize()) return;

        FuelItem fi = slotToFuelItem.get(slot);
        if (fi == null) return; // clicked filler or status item

        FuelItem used = FuelManager.addFuelFromInventory(player, fi);
        if (used == null) {
            player.sendMessage(ChatColor.RED + "You don't have any " + fi.getMaterial().name() + " in your inventory.");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.8f, 1f);
        } else {
            player.sendMessage(ChatColor.GREEN + "Added " + ChatColor.WHITE + used.getFuelAmount()
                    + ChatColor.GREEN + " fuel. Tank: " + ChatColor.WHITE
                    + FuelManager.getFuel() + "/" + FuelManager.getMaxFuel());
            player.playSound(player.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 1f, 1.2f);
            // Refresh status slot
            inventory.setItem(inventory.getSize() - 1, makeStatusItem());
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player p)) return;
        if (!p.getUniqueId().equals(player.getUniqueId())) return;
        if (!event.getInventory().equals(inventory)) return;
        openGuis.remove(player.getUniqueId());
    }
}
