package studio.magemonkey.fusion.cfg;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.Profession;
import studio.magemonkey.fusion.cfg.sql.SQLManager;
import studio.magemonkey.fusion.queue.QueueItem;

import java.io.File;
import java.util.*;

public class PConfigManager {

    private static final HashMap<UUID, PlayerConfig> pConfig = new HashMap<>();

    public static PlayerConfig getPlayerConfig(Player player) {
        if (!pConfig.containsKey(player.getUniqueId()))
            pConfig.put(player.getUniqueId(), new PlayerConfig(player));

        return pConfig.get(player.getUniqueId());
    }

    public static void clearPConfigCache() {
        pConfig.clear();
    }

    public static void migrateIntoSQL() {
        File parentFolder = new File(Fusion.getInstance().getDataFolder() + File.separator + "players");
        if (!parentFolder.exists()) return;
        for(File file : parentFolder.listFiles()) {
            if(file.getName().endsWith(".yml")) {
                FileConfiguration cfg = new YamlConfiguration();
                try {
                    cfg.load(file);
                    UUID uuid = UUID.fromString(file.getName().replace(".yml", ""));

                    // Logic for players table
                    boolean autoCraft = cfg.getBoolean("autoCraft", false);
                    SQLManager.players().setAutoCrafting(uuid, autoCraft);

                    // Logic for professions table
                    Map<String, Boolean> professionData = new HashMap<>();
                    for(String profession : cfg.getStringList("professions")) {
                        professionData.put(profession, false);
                    }
                    for(String profession : cfg.getConfigurationSection("guis").getKeys(false)) {
                        professionData.put(profession, cfg.getBoolean("guis." + profession));
                    }


                    List<Profession> professions = SQLManager.professions().getProfessions(uuid);
                    for(Profession profession : professions) {
                        if(professionData.containsKey(profession.getName())) {
                            profession.setMastered(professionData.get(profession.getName()));
                            profession.setJoined(cfg.contains("professions." + profession.getName()));
                            SQLManager.professions().setProfession(uuid, profession);
                        }
                    }

                    // Logic for queues table
                    List<QueueItem> entries = new ArrayList<>();
                    for(String profession : cfg.getConfigurationSection("queues").getKeys(false)) {
                        for(String category : cfg.getConfigurationSection("queues." + profession).getKeys(false)) {
                            for(String recipe : cfg.getConfigurationSection("queues." + profession + "." + category).getKeys(false)) {
                                List<Long> timestamps = cfg.getLongList("queues." + profession + "." + category + "." + recipe + ".timestamps");
                                for(long timestamp : timestamps)
                                    entries.add(new QueueItem(-1, profession, ProfessionsCfg.getTable(profession).getCategory(category), ProfessionsCfg.getTable(profession).getCategory(category).getRecipe(recipe), timestamp, 0));
                            }
                        }
                    }
                    entries.forEach(e -> SQLManager.queues().setQueueItem(uuid, e));

                    // Deleting the file when done
                    file.delete();
                    Fusion.getInstance().getLogger().info("Migrated player data for " + uuid + " into SQL.");
                } catch (Exception e) {
                    Fusion.getInstance().getLogger().warning("Can't load player data file: " + file);
                    e.printStackTrace();
                    return;
                }
            }
        }
        if(parentFolder.listFiles().length == 0) {
            parentFolder.delete();
            Fusion.getInstance().getLogger().info("Players folder is not required anymore and will be deleted.");
        }
    }
}
