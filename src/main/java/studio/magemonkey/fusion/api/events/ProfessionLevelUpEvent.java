package studio.magemonkey.fusion.api.events;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;

@Getter
public class ProfessionLevelUpEvent extends FusionEvent {

    private final int previousLevel;
    @Setter
    private int newLevel;

    public ProfessionLevelUpEvent(String professionName, Player player, int previousLevel, int newLevel) {
        super(professionName, ProfessionsCfg.getTable(professionName), player);
        this.previousLevel = previousLevel;
        this.newLevel = newLevel;
    }

    public int getLevelDifference() {
        return newLevel - previousLevel;
    }
}
