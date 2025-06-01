package studio.magemonkey.fusion.api;

import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.api.events.services.EventServices;

@Getter
public class FusionAPI {

    @Getter
    private static final JavaPlugin        instance = Fusion.getInstance();
    private static       ProfessionManager professionManager;
    private static       PlayerManager     playerManager;
    private static       EventServices     eventServices;

    public static void init() {
        professionManager = new ProfessionManager();
        playerManager = new PlayerManager();
        eventServices = new EventServices();
        FusionAPI.getInstance().getLogger().info("FusionAPI has been initialized.");
    }

    public static ProfessionManager getProfessionManager() {
        return professionManager != null ? professionManager : new ProfessionManager();
    }

    public static PlayerManager getPlayerManager() {
        return playerManager != null ? playerManager : new PlayerManager();
    }

    public static EventServices getEventServices() {
        return eventServices != null ? eventServices : new EventServices();
    }
}
