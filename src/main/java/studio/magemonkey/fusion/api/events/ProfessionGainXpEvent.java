package studio.magemonkey.fusion.api.events;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;

@Setter
@Getter
public class ProfessionGainXpEvent extends FusionEvent {

    private double gainedExp;

    public ProfessionGainXpEvent(String professionName, Player player, double gainedExp) {
        super(professionName, ProfessionsCfg.getTable(professionName), player);
        this.gainedExp = gainedExp;
    }
}
