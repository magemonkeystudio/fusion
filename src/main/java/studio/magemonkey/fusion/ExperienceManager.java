package studio.magemonkey.fusion;

import studio.magemonkey.codex.util.messages.MessageData;
import studio.magemonkey.codex.util.messages.MessageUtil;
import studio.magemonkey.fusion.cfg.Cfg;
import org.apache.commons.lang3.Validate;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public final class ExperienceManager {
    public static class PlayerData implements ConfigurationSerializable {
        private final UUID                        uuid;
        private final Map<CraftingTable, Integer> data = new HashMap<>();

        /**
         * The constructor
         *
         * @param map serialized data
         */
        public PlayerData(Map<String, Object> map) {
            uuid = UUID.fromString((String) map.get("uuid"));
            //noinspection unchecked
            ((Map<String, Object>) map.get("data")).forEach((key, value) -> data.put(ProfessionsCfg.getTable(key),
                    (Integer) value));
        }

        /**
         * The constructor
         *
         * @param uuid player uuid
         */
        public PlayerData(UUID uuid) {
            this.uuid = uuid;
        }

        /**
         * Gets player uuid
         *
         * @return uuid
         */
        public UUID getUuid() {
            return uuid;
        }

        /**
         * Gets data map
         *
         * @return data map
         */
        public Map<CraftingTable, Integer> getMap() {
            return data;
        }

        /**
         * Gets experience
         *
         * @param craftingTable table
         * @return experience
         */
        public Integer getExperience(CraftingTable craftingTable) {
            return data.getOrDefault(craftingTable, 0);
        }

        /**
         * Adds experience
         *
         * @param craftingTable table
         * @param experience    experience
         */
        public void add(CraftingTable craftingTable, Integer experience) {
            int previousXp    = data.getOrDefault(craftingTable, 0);
            int previousLevel = LevelFunction.getLevel(previousXp);
            data.put(craftingTable, previousXp + experience);
            int newLevel = LevelFunction.getLevel(previousXp + experience);
            if (newLevel > previousLevel)
                MessageUtil.sendMessage("fusion.levelup",
                        Bukkit.getPlayer(uuid),
                        new MessageData("level", newLevel),
                        new MessageData("experience", previousXp + experience));
        }

        public void remove(String craftingTable) {
            data.remove(ProfessionsCfg.getTable(craftingTable));

            try {
                Fusion.getExperienceManager().save();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public Map<String, Object> serialize() {
            final Map<String, Object> map = new HashMap<>();

            map.put("uuid", uuid.toString());
            Map<String, Object> dataMap = new HashMap<>();
            data.forEach((key, value) -> dataMap.put(key.getName(), value));
            map.put("data", dataMap);

            return map;
        }
    }

    private final Collection<PlayerData> playerDataSet = new HashSet<>();
    private final File                   file          = new File(Fusion.getInstance().getDataFolder(), "data.yml");

    /**
     * Loads data
     */
    public void load() {
        try {
            if (!file.exists() && !file.createNewFile()) {
                Fusion.getInstance().getLogger().severe("Failed to create exp data file");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        YamlConfiguration configuration = new YamlConfiguration();

        try {
            configuration.load(file);
        } catch (Exception e) {
            Fusion.getInstance().getLogger().warning("Can't load exp data file: " + file);
            e.printStackTrace();
            return;
        }

        List<Map<?, ?>> typesSection = configuration.getMapList("data");
        for (Map<?, ?> typeData : typesSection) {
            //noinspection unchecked
            PlayerData playerData = new PlayerData((Map<String, Object>) typeData);
            if (playerDataSet.stream().filter(data -> data.getUuid().equals(playerData.getUuid())).count() == 0)
                this.playerDataSet.add(playerData);
        }
    }

    /**
     * Saves all the data
     *
     * @throws IOException when something goes wrong
     */
    public void save() throws IOException {
        YamlConfiguration         configuration = new YamlConfiguration();
        List<Map<String, Object>> list          = new ArrayList<>();

        this.playerDataSet.forEach(playerData -> list.add(playerData.serialize()));

        configuration.set("data", list);
        configuration.save(file);
    }

    /**
     * Gets player experience
     *
     * @param player        player
     * @param craftingTable table
     * @return experience as int
     */
    public int getExperience(Player player, CraftingTable craftingTable) {
        Validate.notNull(player);
        Validate.notNull(craftingTable);
        PlayerData playerData = getPlayerData(player);
        Validate.notNull(playerData);
        return playerData.getExperience(craftingTable);
    }

    /**
     * Gets player data
     *
     * @param player player
     * @return player data object
     */
    public PlayerData getPlayerData(Player player) {
        for (PlayerData data : playerDataSet) {
            if (data.getUuid().equals(player.getUniqueId())) {
                return data;
            }
        }

        return createPlayerData(player);
    }

    /**
     * Creates player data
     *
     * @param player player
     * @return player data object
     */
    public PlayerData createPlayerData(Player player) {
        PlayerData playerData = new PlayerData(player.getUniqueId());
        playerDataSet.add(playerData);
        return playerData;
    }

    @Deprecated
    public static int getTotalExperience(Player player) {
        int experience;
        int level = player.getLevel();
        if ((level >= 0) && (level <= 15)) {
            experience = (int) Math.ceil(Math.pow(level, 2) + (6 * level));
            int    requiredExperience = (2 * level) + 7;
            double currentExp         = Double.parseDouble(Float.toString(player.getExp()));
            experience += Math.ceil(currentExp * requiredExperience);
            return experience;
        } else if ((level > 15) && (level <= 30)) {
            experience = (int) Math.ceil((((2.5 * Math.pow(level, 2)) - (40.5 * level)) + 360));
            int    requiredExperience = (5 * level) - 38;
            double currentExp         = Double.parseDouble(Float.toString(player.getExp()));
            experience += Math.ceil(currentExp * requiredExperience);
            return experience;
        } else {
            experience = (int) Math.ceil(((((4.5 * Math.pow(level, 2)) - (162.5 * level)) + 2220)));
            int    requiredExperience = (9 * level) - 158;
            double currentExp         = Double.parseDouble(Float.toString(player.getExp()));
            experience += Math.ceil(currentExp * requiredExperience);
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