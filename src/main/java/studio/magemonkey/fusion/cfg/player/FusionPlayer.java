package studio.magemonkey.fusion.cfg.player;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import studio.magemonkey.fusion.Category;
import studio.magemonkey.fusion.CraftingTable;
import studio.magemonkey.fusion.Profession;
import studio.magemonkey.fusion.cfg.sql.SQLManager;
import studio.magemonkey.fusion.queue.CraftingQueue;
import studio.magemonkey.fusion.queue.QueueItem;

import java.util.*;

public class FusionPlayer {

    private final UUID uuid;

    private final Map<String, Profession> professions = new TreeMap<>();
    private final Map<String, CraftingQueue> cachedQeues = new TreeMap<>();

    public FusionPlayer(UUID uuid) {
        this.uuid = uuid;
        for(Profession profession : SQLManager.professions().getProfessions(uuid)) {
            professions.put(profession.getName(), profession);
        }
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public CraftingQueue getQueue(String profession, Category category) {
        if (!cachedQeues.containsKey(profession)) {
            cachedQeues.put(profession, new CraftingQueue(getPlayer(), profession, category));
        }
        return cachedQeues.get(profession);
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

    public int getExperience(String profession) {
        int experience = 0;
        if(professions.containsKey(profession)) {
            experience = (int) professions.get(profession).getExp();
        }
        return experience;
    }

    public int getExperience(Profession profession) {
        return getExperience(profession.getName());
    }

    public int getExperience(CraftingTable table) {
        return getExperience(table.getName());
    }

    public Profession getProfession(String profession) {
        return professions.get(profession);
    }

    public Profession getProfession(CraftingTable table) {
        return getProfession(table.getName());
    }

    public int[] getQueueSizes(String profession, Category category) {
        int[] limits = new int[]{0, 0, 0};
        limits[0] = cachedQeues.get(profession).getQueue().size();
        for(CraftingQueue queue : cachedQeues.values()) {
            if(queue.getProfession().equals(profession)) {
                limits[1] += queue.getQueue().size();
            }
            limits[2] += queue.getQueue().size();
        }
        return limits;
    }

    public int getFinishedSize() {
        int size = 0;
        for(CraftingQueue queue : cachedQeues.values()) {
            for(QueueItem item : queue.getQueue()) {
                if(item.isDone()) {
                    size++;
                }
            }
        }
        return size;
    }

    public void save() {
        for(Profession profession : professions.values()) {
            SQLManager.professions().setProfession(uuid, profession);
        }
        for(CraftingQueue queue : cachedQeues.values()) {
            SQLManager.queues().saveCraftingQueue(queue);
        }
    }
}
