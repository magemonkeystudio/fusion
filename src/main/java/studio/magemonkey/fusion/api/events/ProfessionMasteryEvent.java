package studio.magemonkey.fusion.api.events;

import lombok.Getter;
import org.bukkit.entity.Player;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;

@Getter
public class ProfessionMasteryEvent extends FusionEvent {

    private final boolean hasMastered;

    public ProfessionMasteryEvent(String professionName, Player player, boolean hasMastered) {
        super(professionName, ProfessionsCfg.getTable(professionName), player);
        this.hasMastered = hasMastered;
    }
}
