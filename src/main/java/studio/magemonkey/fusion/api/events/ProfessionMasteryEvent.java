package studio.magemonkey.fusion.api.events;

import lombok.Getter;
import org.bukkit.entity.Player;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;

@Getter
public class ProfessionMasteryEvent extends FusionEvent {

    /**
     * Whether the player has mastered the profession
     */
    private final boolean hasMastered;

    /**
     * Constructor for the ProfessionMasteryEvent
     *
     * @param professionName The name of the profession
     * @param player The player that has mastered the profession
     * @param hasMastered Whether the player has mastered the profession
     */
    public ProfessionMasteryEvent(String professionName, Player player, boolean hasMastered) {
        super(professionName, ProfessionsCfg.getTable(professionName), player);
        this.hasMastered = hasMastered;
    }
}
