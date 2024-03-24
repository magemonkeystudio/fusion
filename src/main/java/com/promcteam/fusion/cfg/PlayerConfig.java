package com.promcteam.fusion.cfg;

import com.promcteam.fusion.CraftingTable;
import com.promcteam.fusion.Fusion;
import com.promcteam.fusion.ExperienceManager;
import com.promcteam.fusion.gui.CustomGUI;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PlayerConfig {

    private final Player                   player;
    private       FileConfiguration        config;
    private final HashMap<String, Boolean> mastery     = new HashMap<>();
    private       List<String>             professions = new ArrayList<>();
    private       boolean                  autoCraft   = false;
    private       File                     file;

    public PlayerConfig(Player player) {
        this.player = player;

        try {
            file = new File(Fusion.getInstance().getDataFolder() + File.separator + "players",
                    player.getUniqueId() + ".yml");
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

            if (config.contains("professions"))
                professions = config.getStringList("professions");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean hasProfession(String profession) {
        return professions.contains(profession);
    }

    public List<String> getUnlockedProfessions() {
        if (!config.contains("professions"))
            return Collections.emptyList();

        if (professions != null && !professions.isEmpty())
            return professions;

        List<String> profess = config.getStringList("professions");
        professions = profess;
        return profess;
    }

    public void unlockProfession(String profession) {
        professions.add(profession);
        saveConfig();
    }

    public void removeProfession(String profession) {
        removeProfession(profession, true);
    }

    public void removeProfession(String profession, boolean penalty) {
        while (professions.contains(profession))
            professions.remove(profession);

        CraftingTable                table = Cfg.getTable(profession);
        ExperienceManager.PlayerData dat   = Fusion.getExperienceManager().getPlayerData(player);
        int                          exp   = dat.getExperience(table);
        int                          dock  = (int) (exp * Cfg.forgetPenalty);
        dat.add(table, -dock);

        saveConfig();
    }

    public boolean hasMastery(String gui) {
        return mastery.containsKey(gui) && mastery.get(gui);
    }

    public void setHasMastery(String gui, boolean hasMastery) {
        mastery.put(gui, hasMastery);
        saveConfig();
    }

    public boolean isAutoCraft() {
        return autoCraft;
    }

    public void setAutoCraft(boolean auto) {
        autoCraft = auto;
        saveConfig();
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
