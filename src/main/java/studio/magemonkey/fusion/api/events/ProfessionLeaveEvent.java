package studio.magemonkey.fusion.api.events;

import org.bukkit.entity.Player;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.data.recipes.CraftingTable;

public class ProfessionLeaveEvent extends FusionEvent {

    public ProfessionLeaveEvent(String professionName, Player player) {
        super(professionName, ProfessionsCfg.getTable(professionName), player);
    }

    public CraftingTable getLeftProfession() {
        return getCraftingTable();
    }
}
