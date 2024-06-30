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

        CraftingTable table = ProfessionsCfg.getTable(profession);
        ExperienceManager.PlayerData dat = Fusion.getExperienceManager().getPlayerData(player);
        int exp = dat.getExperience(table);
        int dock = (int) (exp * Cfg.forgetPenalty);
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

    public void setAutoCraft(boolean auto) {
        autoCraft = auto;
        saveConfig();
    }

    public void addQueueItem(QueueItem item) {
        String path = "queue." + item.getProfession() + "." + item.getCategory().getName() + "." + item.getRecipe().getName();
        config.set(path + ".cooldown", item.getRecipe().getCooldown());
        List<Long> timestamps = config.isSet(path + ".timestamps") ? config.getLongList(path + ".timestamps") : new ArrayList<>();
        timestamps.add(System.currentTimeMillis());
        config.set(path + ".timestamps", timestamps);
        saveConfig();
    }

    public void removeQueueItem(QueueItem item) {
        String path = "queue." + item.getProfession() + "." + item.getCategory().getName() + "." + item.getRecipe().getName();
        List<Long> timestamps = config.isSet(path + ".timestamps") ? config.getLongList(path + ".timestamps") : new ArrayList<>();
        timestamps.remove(item.getTimestamp());
        if (timestamps.isEmpty()) {
            // If no Items of the recipe are queue, remove the recipe from the queue
            config.set(path, null);
        } else {
            // Else, update the remaining timestamps
            config.set(path + ".timestamps", timestamps);
        }

        // If no Items of the category are queue, remove the category from the queue
        String categoryPath = "queue." + item.getProfession() + "." + item.getCategory().getName();
        if (config.getConfigurationSection(categoryPath).getValues(false).keySet().isEmpty()) {
            config.set(categoryPath, null);
        }

        // If no Items of the profession are queue, remove the profession from the queue
        String professionPath = "queue." + item.getProfession();
        if (config.getConfigurationSection(professionPath).getValues(false).keySet().isEmpty()) {
            config.set(professionPath, null);
        }
        saveConfig();
    }

    public List<QueueItem> getQueueItems(String profession, Category category) {
        List<QueueItem> items = new ArrayList<>();
        String path = "queue." + profession + "." + category.getName();
        if (!config.isSet(path)) return items;
        for (String key : config.getConfigurationSection(path).getValues(true).keySet()) {
            Recipe recipe = category.getRecipe(key);
            if (recipe == null) continue;
            List<Long> timestamps = config.getLongList(path + "." + key + ".timestamps");
            for (long timestamp : timestamps) {
                items.add(new QueueItem(profession, category, recipe, timestamp));
            }
        }
        return items;
    }

    /*
     * Returns the amount of items in the queue for the given profession and category
     * @param profession The profession to check
     * @param category The category to check
     * @return The amount of items in the queue for the given profession and category

     * sizes[0] = amount of items in the queue for the given profession and category
     * sizes[1] = amount of items in the queue for the given profession
     * sizes[2] = amount of items in the queue
     */
    public int[] getQueueSizes(String profession, String category) {
        int[] sizes = new int[]{0, 0, 0};
        if (!config.isSet("queue")) return sizes;
        for (String prof : config.getConfigurationSection("queue").getKeys(false)) {
            for (String cat : config.getConfigurationSection("queue." + prof).getKeys(false)) {
                for (String recipe : config.getConfigurationSection("queue." + prof + "." + cat).getKeys(false)) {
                    int recipeSize = config.getLongList("queue." + prof + "." + cat + "." + recipe + ".timestamps").size();
                    if (prof.equals(profession) && cat.equals(category)) {
                        sizes[0] += recipeSize;
                    }
                    if (profession.equals(prof)) {
                        sizes[1] += recipeSize;
                    }
                    sizes[2] += recipeSize;
                }
            }
        }
        return sizes;
    }

    public void clearQueue(String profession, Category category, boolean onlyUnfinished) {
        for (QueueItem item : getQueueItems(profession, category)) {
            if (onlyUnfinished || !item.isDone()) {
                removeQueueItem(item);
            } else {
                removeQueueItem(item);
            }
        }
    }

    public int getFinishedQueueAmount() {
        List<Long> entries = new ArrayList<>();
        for (String profession : config.getConfigurationSection("queue").getKeys(false)) {
            for (String category : config.getConfigurationSection("queue." + profession).getKeys(false)) {
                for (String recipe : config.getConfigurationSection("queue." + profession + "." + category).getKeys(false)) {
                    List<Long> timestamps = config.getLongList("queue." + profession + "." + category + "." + recipe + ".timestamps");
                    int cooldown = config.getInt("queue." + profession + "." + category + "." + recipe + ".cooldown");
                    timestamps.removeIf(timestamp -> (System.currentTimeMillis() - timestamp) / 1000 < cooldown);
                    entries.addAll(timestamps);
                }
            }
        }
        return entries.size();
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
