package studio.magemonkey.fusion.api.events;

import org.bukkit.entity.Player;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.data.recipes.CraftingTable;

public class ProfessionJoinEvent extends FusionEvent {

    /**
     * Constructor for the ProfessionJoinEvent
     *
     * @param professionName The name of the profession
     * @param player The player that joined the profession
     */
    public ProfessionJoinEvent(String professionName, Player player) {
        super(professionName, ProfessionsCfg.getTable(professionName), player);
    }

    /**
     * Get the joined profession
     *
     * @return The joined profession
     */
    public CraftingTable getJoinedProfession() {
        return getCraftingTable();
    }
}
