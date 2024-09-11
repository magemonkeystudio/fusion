package studio.magemonkey.fusion.api;

import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;
import studio.magemonkey.fusion.Fusion;

@Getter
public class FusionAPI {

    @Getter
    private static final JavaPlugin instance = Fusion.getInstance();
    @Getter
    private static ProfessionManager professionManager;
    @Getter
    private static PlayerManager playerManager;
    @Getter
    private static EventManager eventManager;

    public static void init() {
        professionManager = new ProfessionManager();
        playerManager = new PlayerManager();
        eventManager = new EventManager();
        FusionAPI.getInstance().getLogger().info("FusionAPI has been initialized.");
    }

}
