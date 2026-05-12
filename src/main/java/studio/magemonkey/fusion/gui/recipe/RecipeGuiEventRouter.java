package studio.magemonkey.fusion.gui.recipe;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.data.player.FusionPlayer;
import studio.magemonkey.fusion.data.player.PlayerLoader;
import studio.magemonkey.fusion.data.recipes.CraftingTable;
import studio.magemonkey.fusion.gui.ProfessionGuiRegistry;
import studio.magemonkey.fusion.gui.RecipeGui;

/**
 * Centralized listener for all RecipeGui‐related events.
 * For each incoming event, we look up the player’s FusionPlayer and its cachedGuis.
 * If an event’s Inventory matches one of the cached RecipeGui inventories, we forward
 * to that RecipeGui’s click/drag/close/drop logic.
 */
public class RecipeGuiEventRouter implements Listener {

    /**
     * Look up, for a given Player, which RecipeGui (if any) has this exact Inventory open.
     * We fetch that player’s FusionPlayer via PlayerLoader.getPlayer(Player).
     */
    private RecipeGui findGuiFor(Player player, Inventory inv) {
        if (!ProfessionGuiRegistry.getLatestRecipeGui().containsKey(player.getUniqueId()))
            return null;
        RecipeGui gui = ProfessionGuiRegistry.getLatestRecipeGui().get(player.getUniqueId());
        if (gui.getInventory().equals(inv)) {
            return gui;
        }
        return null;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;
        Inventory inv = event.getClickedInventory();
        RecipeGui gui = findGuiFor(p, inv);
        if (gui == null || inv == null) return;

        // Only forward if the clicked inventory is *exactly* the GUI’s inventory
        if (!inv.equals(gui.getInventory())) return;
        if (event.getRawSlot() < 0) return;

        // Delegate to RecipeGui.click(...)
        gui.click(event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;
        Inventory inv = event.getInventory();
        RecipeGui gui = findGuiFor(p, inv);
        if (gui == null) return;

        // Only route if the drag is happening inside this GUI’s inventory
        if (!inv.equals(gui.getInventory())) return;

        // Delegate to RecipeGui’s drag logic
        gui.onDrag(event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player p)) return;
        Inventory inv = event.getInventory();
        RecipeGui gui = findGuiFor(p, inv);
        if (gui == null) return;

        gui.close(p, inv);

        if (event.getReason() == InventoryCloseEvent.Reason.PLAYER) {
            CraftingTable table = gui.getTable();
            if (table.getUseCategories() && !table.getCategories().isEmpty()) {
                Bukkit.getScheduler().runTaskLater(Fusion.getInstance(),
                        () -> ProfessionsCfg.getGUI(gui.getName()).open(p), 1L);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDrop(PlayerDropItemEvent event) {
        Player    p   = event.getPlayer();
        RecipeGui gui = findGuiFor(p, p.getOpenInventory().getTopInventory());
        if (gui == null) return;

        // If the player drops an item while their RecipeGui is open, route to its drop logic
        gui.drop(event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        RecipeGui gui = findGuiFor(p, p.getOpenInventory().getTopInventory());
        if (gui == null) return;

        // If the player picks up anything while the GUI is open, trigger a reload
        gui.reloadRecipesTask();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player       p  = event.getPlayer();
        FusionPlayer fp = PlayerLoader.getPlayer(p);
        if (fp == null) return;

        // On quit, close and remove *all* open RecipeGuis for that player
        RecipeGui gui = ProfessionGuiRegistry.getLatestRecipeGui().get(p.getUniqueId());
        if (gui == null) return;
        gui.close(p, gui.getInventory());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player p = event.getPlayer();
        RecipeGui gui = ProfessionGuiRegistry.getLatestRecipeGui().get(p.getUniqueId());
        if (gui == null) return;
        gui.close(p, gui.getInventory());
    }
}
