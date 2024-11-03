package studio.magemonkey.fusion.api.events;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;

@Getter
public class ProfessionLevelUpEvent extends FusionEvent {

    /**
     * The previous level of the profession
     */
    private final int previousLevel;

    /**
     * The new level of the profession
     */
    @Setter
    private int newLevel;

    /**
     * Constructor for the ProfessionLevelUpEvent
     *
     * @param professionName The name of the profession
     * @param player The player that leveled up
     * @param previousLevel The previous level of the profession
     * @param newLevel The new level of the profession
     */
    public ProfessionLevelUpEvent(String professionName, Player player, int previousLevel, int newLevel) {
        super(professionName, ProfessionsCfg.getTable(professionName), player);
        this.previousLevel = previousLevel;
        this.newLevel = newLevel;
    }

    /**
     * Get the level difference
     *
     * @return The level difference
     */
    public int getLevelDifference() {
        return newLevel - previousLevel;
    }
}
