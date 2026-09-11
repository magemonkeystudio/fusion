package studio.magemonkey.fusion.data.player;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.sql.SQLManager;
import studio.magemonkey.fusion.data.professions.Profession;
import studio.magemonkey.fusion.data.professions.pattern.Category;
import studio.magemonkey.fusion.data.queue.CraftingQueue;
import studio.magemonkey.fusion.data.queue.QueueItem;
import studio.magemonkey.fusion.data.recipes.CraftingTable;
import studio.magemonkey.fusion.data.recipes.Recipe;
import studio.magemonkey.fusion.util.RecipeAmounts;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class FusionPlayer {

    private final UUID uuid;

    private       Map<String, CraftingQueue>     cachedQueues       = new TreeMap<>();

    public boolean isAutoCrafting() {
        return SQLManager.players().isAutoCrafting(uuid);
    }

    public void setAutoCrafting(boolean autoCrafting) {
        SQLManager.players().setAutoCrafting(uuid, autoCrafting);

    }

    // Track whether this player is currently locked for saving (in-memory mirror of DB lock)
    @Getter
    @Setter
    private volatile boolean locked;

    public FusionPlayer(UUID uuid) {
        this.uuid = uuid;
        // initialize locked state from DB to reflect current status
        this.locked = SQLManager.players().isLocked(uuid);

        cachedQueues = SQLManager.queues().getCraftingQueues(getPlayer());
    }

    private Map<String, Profession> professions() {
        Map<String, Profession> current = new TreeMap<>();
        for (Profession profession : SQLManager.professions().getProfessions(uuid)) {
            current.put(profession.getName(), profession);
        }
        return current;
    }
    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public CraftingQueue getQueue(String profession, Category category) {
        if (!cachedQueues.containsKey(profession + "." + category.getName())) {
            cachedQueues.put(profession + "." + category.getName(), new CraftingQueue(getPlayer(), profession, category));
            Bukkit.getConsoleSender().sendMessage("Created new crafting queue for profession " + profession + " and category " + category.getName() + " for player " + getPlayer().getName());
        }
        return cachedQueues.get(profession + "." + category.getName());
    }

    public PlayerRecipeLimit getRecipeLimit(Recipe recipe) {
        return getRecipeLimit(recipe.getRecipePath());
    }

    public PlayerRecipeLimit getRecipeLimit(String recipePath) {
        return SQLManager.recipeLimits().getRecipeLimit(uuid, recipePath);
    }

    public void incrementLimit(Recipe recipe) {
        SQLManager.recipeLimits().incrementLimit(uuid, recipe.getRecipePath(), 1,
                recipe.getCraftingLimitCooldown());
    }

    public boolean hasRecipeLimitReached(Recipe recipe) {
        if (recipe.getCraftingLimit() <= 0) return false;
        return getRecipeLimit(recipe.getRecipePath()).getLimit() >= recipe.getCraftingLimit();
    }

    public long getExperience(String profession) {
        long experience = 0;
        Profession current = getProfession(profession);
        if (current != null) {
            experience = current.getExp();
        }
        return experience;
    }

    public long getExperience(Profession profession) {
        return getExperience(profession.getName());
    }

    public long getExperience(CraftingTable table) {
        return getExperience(table.getName());
    }

    public int getLevel(String profession) {
        int level = 0;
        Profession current = getProfession(profession);
        if (current != null) {
            level = current.getLevel();
        }
        return level;
    }

    public int getLevel(Profession profession) {
        return getLevel(profession.getName());
    }

    public int getLevel(CraftingTable table) {
        return getLevel(table.getName());
    }

    @Nullable
    public Profession getProfession(String profession) {
        return SQLManager.professions().getProfession(uuid, profession);
    }

    @Nullable
    public Profession getProfession(CraftingTable table) {
        return getProfession(table.getName());
    }

    public void removeProfession(String profession) {
        SQLManager.professions().removeProfession(uuid.toString(), profession);

    }

    public void removeProfession(Profession profession) {
        removeProfession(profession.getName());
    }

    public void removeProfession(CraftingTable table) {
        removeProfession(table.getName());
    }

    public void addProfession(Profession profession) {
        SQLManager.professions().setProfession(uuid, profession);

    }

    public boolean hasProfession(String profession) {
        return getProfession(profession) != null;
    }

    public boolean hasProfession(Profession profession) {
        return hasProfession(profession.getName());
    }

    public boolean hasProfession(CraftingTable table) {
        return hasProfession(table.getName());
    }

    public Collection<Profession> getJoinedProfessions() {
        return professions().values().stream().filter(Profession::isJoined).collect(Collectors.toList());
    }

    public Collection<Profession> getMasteredProfessions() {
        return professions().values().stream().filter(Profession::isMastered).collect(Collectors.toList());
    }

    public Collection<Profession> getProfessions() {
        return professions().values();
    }

    public boolean hasMastered(String profession) {
        Profession current = getProfession(profession);
        return current != null && current.isMastered();
    }

    public boolean hasMastered(Profession profession) {
        return hasMastered(profession.getName());
    }

    public boolean hasMastered(CraftingTable table) {
        return hasMastered(table.getName());
    }

    public boolean hasJoined(String profession) {
        Profession current = getProfession(profession);
        return current != null && current.isJoined();
    }

    public boolean hasJoined(Profession profession) {
        return hasJoined(profession.getName());
    }

    public boolean hasJoined(CraftingTable table) {
        return hasJoined(table.getName());
    }

    public void setMastered(String profession, boolean mastered) {
        Profession current = getProfession(profession);
        if (current != null) {
            current.setMastered(mastered);
        }
    }

    public void setMastered(Profession profession, boolean mastered) {
        setMastered(profession.getName(), mastered);
    }

    public void setMastered(CraftingTable table, boolean mastered) {
        setMastered(table.getName(), mastered);
    }

    public void setJoined(String profession, boolean joined) {
        Profession current = getProfession(profession);
        if (current != null) {
            current.setJoined(joined);
        }
    }

    public void setJoined(Profession profession, boolean joined) {
        setJoined(profession.getName(), joined);
    }

    public void setJoined(CraftingTable table, boolean joined) {
        setJoined(table.getName(), joined);
    }

    public void addExperience(String profession, long experience) {
        Profession current = getProfession(profession);
        if (current != null) {
            current.addExp(experience);
        }
    }

    public void addExp(Profession profession, long experience) {
        addExperience(profession.getName(), experience);
    }

    public void addExp(CraftingTable table, long experience) {
        addExperience(table.getName(), experience);
    }

    public void setExperience(String profession, long experience) {
        Profession current = getProfession(profession);
        if (current != null) {
            current.setExp(experience);
        }
    }

    public void setExp(Profession profession, long experience) {
        setExperience(profession.getName(), experience);
    }

    public void setExp(CraftingTable table, long experience) {
        setExperience(table.getName(), experience);
    }

    public void removeExperience(String profession, long experience) {
        Profession current = getProfession(profession);
        if (current != null) {
            current.removeExp(experience);
        }
    }

    public void removeExp(Profession profession, long experience) {
        removeExperience(profession.getName(), experience);
    }

    public void removeExp(CraftingTable table, long experience) {
        removeExperience(table.getName(), experience);
    }

    public void resetExperience(String profession) {
        Profession current = getProfession(profession);
        if (current != null) {
            current.resetExp();
        }
    }

    public void resetExp(Profession profession) {
        resetExperience(profession.getName());
    }

    public void resetExp(CraftingTable table) {
        resetExperience(table.getName());
    }

    public void resetMastered(String profession) {
        Profession current = getProfession(profession);
        if (current != null) {
            current.resetMastered();
        }
    }

    public void resetMastered(Profession profession) {
        resetMastered(profession.getName());
    }

    public void resetMastered(CraftingTable table) {
        resetMastered(table.getName());
    }

    public void resetJoined(String profession) {
        Profession current = getProfession(profession);
        if (current != null) {
            current.resetJoined();
        }
    }

    public void resetJoined(Profession profession) {
        resetJoined(profession.getName());
    }

    public void resetJoined(CraftingTable table) {
        resetJoined(table.getName());
    }

    public void reset(String profession) {
        Profession current = getProfession(profession);
        if (current != null) {
            current.reset();
        }
    }

    public void reset(Profession profession) {
        reset(profession.getName());
    }

    public void reset(CraftingTable table) {
        reset(table.getName());
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
    public int[] getQueueSizes(String profession, Category category) {
        int[]  limits = new int[]{0, 0, 0};
        String path   = profession + "." + category.getName();
        limits[0] = cachedQueues.containsKey(path) ? cachedQueues.get(path).getQueue().size() : 0;
        for (Map.Entry<String, CraftingQueue> queue : cachedQueues.entrySet()) {
            if (queue.getKey().contains(profession + ".")) {
                limits[1] += queue.getValue().getQueue().size();
            }
            limits[2] += queue.getValue().getQueue().size();
        }
        return limits;
    }

    public int getFinishedSize() {
        int size = 0;
        for (CraftingQueue queue : cachedQueues.values()) {
            for (QueueItem item : queue.getQueue()) {
                if (item.isDone()) {
                    size++;
                }
            }
        }
        return size;
    }

    public int getFinishedOutputAmount() {
        int amount = 0;
        for (CraftingQueue queue : cachedQueues.values()) {
            for (QueueItem item : queue.getQueue()) {
                if (item.isDone()) {
                    amount += RecipeAmounts.outputAmount(item.getRecipe());
                }
            }
        }
        return amount;
    }

    public void save() {
        save(false);
    }

    public void save(boolean clearCaches) {
        // Finish before unloading: a new session must never race an old async snapshot.
        this.locked = true;
        try {
            for (CraftingQueue queue : cachedQueues.values()) {
                if (clearCaches) queue.cancelTask();
                long now = System.currentTimeMillis();
                queue.getQueue().forEach(item -> item.setTimestamp(now));
                SQLManager.queues().saveCraftingQueue(queue);
            }
            if (clearCaches) cachedQueues.clear();
        } finally {
            SQLManager.players().setLocked(uuid, false);
            this.locked = false;
        }
    }
}
