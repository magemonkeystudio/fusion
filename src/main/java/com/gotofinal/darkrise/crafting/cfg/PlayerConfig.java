package com.gotofinal.darkrise.crafting.cfg;

import com.gotofinal.darkrise.crafting.DarkRiseCrafting;
import com.gotofinal.darkrise.crafting.gui.CustomGUI;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PlayerConfig {

    private Player player;
    private FileConfiguration config;
    private HashMap<String, Boolean> mastery = new HashMap<>();
    private boolean autoCraft = false;
    private File file;

    public PlayerConfig(Player player) {
        this.player = player;

        try {
            file = new File(DarkRiseCrafting.getInstance().getDataFolder() + File.separator + "players", player.getUniqueId().toString() + ".yml");
            file.getParentFile().mkdirs();
            if (!file.exists()) {
                file.createNewFile();
            }

            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

            config = yaml;

            for (Map.Entry<String, CustomGUI> entry : Cfg.getGuiMap().entrySet()) {
                if (config.contains("guis." + entry.getKey()))
                    mastery.put(entry.getKey(), config.getBoolean("guis." + entry.getKey()));
            }

            if (config.contains("autoCraft"))
                autoCraft = config.getBoolean("autoCraft");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean hasMastery(String gui) {
        return mastery.containsKey(gui) && mastery.get(gui);
    }

    public void setHasMastery(String gui, boolean hasMastery) {
        mastery.put(gui, hasMastery);
        config.set("guis", mastery);
        saveConfig();
    }

    public boolean isAutoCraft() {
        return autoCraft;
    }

    public void setAutoCraft(boolean auto) {
        autoCraft = auto;
        config.set("autoCraft", auto);
        saveConfig();
    }

    public void saveConfig() {
        config.set("guis", mastery);
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
