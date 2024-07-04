package studio.magemonkey.fusion.cfg;

import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import studio.magemonkey.fusion.*;
import studio.magemonkey.fusion.gui.CustomGUI;
import studio.magemonkey.fusion.queue.QueueItem;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PlayerConfig {

    private final Player player;
    private FileConfiguration config;
    private final HashMap<String, Boolean> mastery = new HashMap<>();
    private List<String> professions = new ArrayList<>();
    @Getter
    private boolean autoCraft = false;
    private File file;

    public PlayerConfig(Player player) {
        this.player = player;

        try {
            file = new File(Fusion.getInstance().getDataFolder() + File.separator + "players",
                    player.getUniqueId() + ".yml");
            file.getParentFile().mkdirs();
            if (!file.exists()) {
                file.createNewFile();
            }
            config = YamlConfiguration.loadConfiguration(file);
            for (Map.Entry<String, CustomGUI> entry : ProfessionsCfg.getGuiMap().entrySet()) {
                if (config.contains("guis." + entry.getKey()))
                    mastery.put(entry.getKey(), config.getBoolean("guis." + entry.getKey()));
            }

            if (config.contains("autoCraft"))
                autoCraft = config.getBoolean("autoCraft");

            if (config.contains("professions"))
                professions = config.getStringList("professions");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveConfig() {
        config.set("guis", mastery);
        config.set("autoCraft", autoCraft);
        config.set("professions", professions);
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
