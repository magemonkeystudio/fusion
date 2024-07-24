package studio.magemonkey.fusion;

import lombok.Getter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.cfg.professions.Profession;
import studio.magemonkey.fusion.cfg.sql.SQLManager;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ExperienceManager {

    public static class PlayerData {
        @Getter
        private final UUID                        uuid;
        private final Map<CraftingTable, Integer> data = new HashMap<>();

        public PlayerData(Map<String, Object> map) {
            uuid = UUID.fromString((String) map.get("uuid"));
            //noinspection unchecked
            ((Map<String, Object>) map.get("data")).forEach((key, value) -> data.put(ProfessionsCfg.getTable(key),
                    (Integer) value));
        }

        public Integer getExperience(CraftingTable craftingTable) {
            return data.getOrDefault(craftingTable, 0);
        }

        public void remove(String craftingTable) {
            data.remove(ProfessionsCfg.getTable(craftingTable));
        }
    }

    private static final File file = new File(Fusion.getInstance().getDataFolder(), "data.yml");

    // TODO Migration into sql
    public static void migrateIntoSQL() {
        if (!file.exists()) return;

        YamlConfiguration cfg = new YamlConfiguration();
        try {
            cfg.load(file);
        } catch (Exception e) {
            Fusion.getInstance().getLogger().warning("Can't load exp data file: " + file);
            e.printStackTrace();
            return;
        }

        List<Map<?, ?>> typesSection = cfg.getMapList("data");
        for (Map<?, ?> typeData : typesSection) {
            //noinspection unchecked
            PlayerData playerData = new PlayerData((Map<String, Object>) typeData);
            for (Map.Entry<CraftingTable, Integer> table : playerData.data.entrySet()) {
                SQLManager.professions()
                        .setProfession(playerData.uuid,
                                new Profession(-1,
                                        playerData.getUuid(),
                                        table.getKey().getName(),
                                        playerData.getExperience(table.getKey()),
                                        false,
                                        false));
                //playerData.remove(table.getKey().getName());
                Fusion.getInstance()
                        .getLogger()
                        .info("Migrated " + playerData.getUuid() + " with " + table.getKey().getName());
            }
            if (playerData.data.isEmpty()) {
                typesSection.remove(typeData);
            }
        }
        if (typesSection.isEmpty()) {
            backup();
            if (file.delete()) {
                Fusion.getInstance().getLogger().info("Deleted data.yml");
            }
        } else {
            cfg.set("data", typesSection);
        }
    }

    public static void backup() {
        if (!file.exists()) return;

        YamlConfiguration cfg = new YamlConfiguration();
        try {
            cfg.load(file);
            File backupFile = new File(Fusion.getInstance().getDataFolder(), "backup/data.yml");
            cfg.save(backupFile);
            Fusion.getInstance().getLogger().info("Backup saved to " + backupFile);
        } catch (Exception e) {
            Fusion.getInstance().getLogger().warning("Can't load exp data file: " + file);
            e.printStackTrace();
        }
    }

    // Stuff that can remain yet
    @Deprecated
    public static int getTotalExperience(Player player) {
        int experience;
        int level = player.getLevel();
        if ((level >= 0) && (level <= 15)) {
            experience = (int) Math.ceil(Math.pow(level, 2) + (6 * level));
            int    requiredExperience = (2 * level) + 7;
            double currentExp         = Double.parseDouble(Float.toString(player.getExp()));
            experience += (int) Math.ceil(currentExp * requiredExperience);
            return experience;
        } else if ((level > 15) && (level <= 30)) {
            experience = (int) Math.ceil((((2.5 * Math.pow(level, 2)) - (40.5 * level)) + 360));
            int    requiredExperience = (5 * level) - 38;
            double currentExp         = Double.parseDouble(Float.toString(player.getExp()));
            experience += (int) Math.ceil(currentExp * requiredExperience);
            return experience;
        } else {
            experience = (int) Math.ceil(((((4.5 * Math.pow(level, 2)) - (162.5 * level)) + 2220)));
            int    requiredExperience = (9 * level) - 158;
            double currentExp         = Double.parseDouble(Float.toString(player.getExp()));
            experience += (int) Math.ceil(currentExp * requiredExperience);
            return experience;
        }
    }

    @Deprecated
    public static void setTotalExperience(Player player, int xp) {
        //Levels 0 through 15
        if ((xp >= 0) && (xp < 351)) {
            //Calculate Everything
            int   a                = 1;
            int   b                = 6;
            int   c                = -xp;
            int   level            = (int) (-b + Math.sqrt(Math.pow(b, 2) - (4 * a * c))) / (2 * a);
            int   xpForLevel       = (int) (Math.pow(level, 2) + (6 * level));
            int   remainder        = xp - xpForLevel;
            int   experienceNeeded = (2 * level) + 7;
            float experience       = (float) remainder / (float) experienceNeeded;
            experience = round(experience, 2);
//            System.out.println("xpForLevel: " + xpForLevel);
//            System.out.println(experience);

            //Set Everything
            player.setLevel(level);
            player.setExp(experience);
            //Levels 16 through 30
        } else if ((xp >= 352) && (xp < 1507)) {
            //Calculate Everything
            double a                = 2.5;
            double b                = -40.5;
            int    c                = -xp + 360;
            double dLevel           = (-b + Math.sqrt(Math.pow(b, 2) - (4 * a * c))) / (2 * a);
            int    level            = (int) Math.floor(dLevel);
            int    xpForLevel       = (int) (((2.5 * Math.pow(level, 2)) - (40.5 * level)) + 360);
            int    remainder        = xp - xpForLevel;
            int    experienceNeeded = (5 * level) - 38;
            float  experience       = (float) remainder / (float) experienceNeeded;
            experience = round(experience, 2);
//            System.out.println("xpForLevel: " + xpForLevel);
//            System.out.println(experience);

            //Set Everything
            player.setLevel(level);
            player.setExp(experience);
            //Level 31 and greater
        } else {
            //Calculate Everything
            double a                = 4.5;
            double b                = -162.5;
            int    c                = -xp + 2220;
            double dLevel           = (-b + Math.sqrt(Math.pow(b, 2) - (4 * a * c))) / (2 * a);
            int    level            = (int) Math.floor(dLevel);
            int    xpForLevel       = (int) (((4.5 * Math.pow(level, 2)) - (162.5 * level)) + 2220);
            int    remainder        = xp - xpForLevel;
            int    experienceNeeded = (9 * level) - 158;
            float  experience       = (float) remainder / (float) experienceNeeded;
            experience = round(experience, 2);
//            System.out.println("xpForLevel: " + xpForLevel);
//            System.out.println(experience);

            //Set Everything
            player.setLevel(level);
            player.setExp(experience);
        }
    }

    private static float round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, RoundingMode.HALF_DOWN);
        return bd.floatValue();
    }
}