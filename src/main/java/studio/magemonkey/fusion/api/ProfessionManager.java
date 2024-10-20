package studio.magemonkey.fusion.api;

import org.bukkit.entity.Player;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.data.recipes.CraftingTable;
import studio.magemonkey.fusion.gui.ProfessionGuiRegistry;

public class ProfessionManager {

    /**
     * Get a profession by name
     * The profession instances are mainly know as CraftinTable objects that contains recipes and other data
     *
     * @param profession The name of the profession
     * @return The profession
     */
    public CraftingTable getProfession(String profession) {
        CraftingTable table = ProfessionsCfg.getTable(profession);
        if(table == null) {
            FusionAPI.getInstance().getLogger().warning("Failed to get profession " + profession + " because it does not exist.");
            return null;
        }
        return table;
    }

    /**
     * Get a profession registry gui by name
     * The profession gui instances are mainly know as ProfessionGuiRegistry objects that contains the gui data
     *
     * @param profession The name of the profession
     * @return The profession gui
     */
    public ProfessionGuiRegistry getProfessionGui(String profession) {
        ProfessionGuiRegistry gui = ProfessionsCfg.getGUI(profession);
        if(gui == null) {
            FusionAPI.getInstance().getLogger().warning("Failed to get profession gui for " + profession + " because it does not exist.");
            return null;
        }
        return gui;
    }

    /**
     * Open a professions gui for a player and get a professions registry object
     * This object handles all players guis individually by caching them in a map.
     *
     * @param profession The name of the profession
     * @param player The Player object
     * @return The profession gui object
     */
    public ProfessionGuiRegistry openProfessionGui(String profession, Player player) {
        ProfessionGuiRegistry gui = getProfessionGui(profession);
        if(gui == null) {
            FusionAPI.getInstance().getLogger().warning("Failed to open profession gui for " + profession + "(" + player.getUniqueId() + ") because it does not exist.");
            return null;
        }
        gui.open(player);
        return gui;
    }


    /**
     * Close the professions gui for all players
     * This will close all guis contained in the profession registry and for every player
     * In case you want to close a gui for a specific player, you can just use Player#closeInventory()
     *
     * @param professionName The name of the profession
     */
    public void closeProfessionGui(String professionName) {
        ProfessionGuiRegistry gui = ProfessionsCfg.getGUI(professionName);
        if(gui == null) {
            FusionAPI.getInstance().getLogger().warning("Failed to close profession gui for " + professionName + " because it does not exist.");
            return;
        }
        gui.closeAll();
    }
}
