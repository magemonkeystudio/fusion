package studio.magemonkey.fusion.api.events;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;

@Setter
@Getter
public class ProfessionGainXpEvent extends FusionEvent {

    /**
     * The amount of experience gained
     */
    private long gainedExp;

    /**
     * Constructor for the ProfessionGainXpEvent
     *
     * @param professionName The name of the profession
     * @param player The player that gained the experience
     * @param gainedExp The amount of experience gained
     */
    public ProfessionGainXpEvent(String professionName, Player player, long gainedExp) {
        super(professionName, ProfessionsCfg.getTable(professionName), player);
        this.gainedExp = gainedExp;
    }
}
