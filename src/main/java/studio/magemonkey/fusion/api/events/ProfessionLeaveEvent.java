package studio.magemonkey.fusion.api.events;

import org.bukkit.entity.Player;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.data.recipes.CraftingTable;

public class ProfessionLeaveEvent extends FusionEvent {

    /**
     * Constructor for the ProfessionLeaveEvent
     *
     * @param professionName The name of the profession
     * @param player The player that left the profession
     */
    public ProfessionLeaveEvent(String professionName, Player player) {
        super(professionName, ProfessionsCfg.getTable(professionName), player);
    }

    /**
     * Get the left profession
     *
     * @return The left profession
     */
    public CraftingTable getLeftProfession() {
        return getCraftingTable();
    }
}
