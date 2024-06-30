package studio.magemonkey.fusion;

import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import studio.magemonkey.codex.config.legacy.LegacyConfigManager;
import studio.magemonkey.codex.items.ItemType;
import studio.magemonkey.codex.legacy.RisePlugin;
import studio.magemonkey.codex.legacy.placeholder.PlaceholderRegistry;
import studio.magemonkey.codex.legacy.placeholder.PlaceholderType;
import studio.magemonkey.codex.util.messages.MessageData;
import studio.magemonkey.codex.util.messages.MessageUtil;
import studio.magemonkey.fusion.cfg.*;
import studio.magemonkey.fusion.gui.BrowseGUI;
import studio.magemonkey.fusion.gui.CustomGUI;
import studio.magemonkey.sapphire.Sapphire;
import studio.magemonkey.sapphire.SapphireItemProvider;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

/*
 * © 2024 MageMonkeyStudio
 */
public class Fusion extends RisePlugin implements Listener {
    public static final PlaceholderType<RecipeItem>       RECIPE_ITEM        =
            PlaceholderType.create("recipeItem", RecipeItem.class);
    public static final PlaceholderType<Recipe>           RECIPE             =
            PlaceholderType.create("recipe", Recipe.class);
    public static final PlaceholderType<CraftingTable>    CRAFTING_TABLE     =
            PlaceholderType.create("craftingTable", CraftingTable.class);
    public static final PlaceholderType<CalculatedRecipe> CALCULATED_RECIPE  =
            PlaceholderType.create("calculatedRecipe", CalculatedRecipe.class);
    public static final PlaceholderType<CustomGUI>        CRAFTING_INVENTORY =
            PlaceholderType.create("craftingInventory", CustomGUI.class);

    @Getter
    private static Fusion            instance;
    private static ExperienceManager experienceManager;

    private BukkitTask saveTask;

    {
        instance = this;
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        FileConfiguration lang =
                LegacyConfigManager.loadConfigFile(new File(getDataFolder() + File.separator + "lang", "lang_en.yml"),
                        getResource("lang/lang_en.yml"));
        MessageUtil.reload(lang, this);
        Cfg.init();
        ProfessionsCfg.init();
        if (experienceManager != null) {
            try {
                experienceManager.save();
            } catch (IOException e) {
                log.warning("Error saving data.yml");
                e.printStackTrace();
            }
        }
        experienceManager = new ExperienceManager();
        experienceManager.load();
        BrowseConfig.load();
        PConfigManager.resetPlayerConfigs();
        runSaveTask();
    }

    @Override
    public void onLoad() {
        RECIPE_ITEM.registerItem("amount", RecipeItem::getAmount);
        RECIPE_ITEM.registerItem("itemName",
                i -> (i instanceof RecipeEconomyItem) ? ((RecipeEconomyItem) i).getItemName() : null);
        RECIPE.registerItem("name", Recipe::getName);
        RECIPE.registerItem("price", Recipe::getPrice);
        RECIPE.registerItem("neededLevels", Recipe::getNeededLevels);
        RECIPE.registerItem("neededXp", Recipe::getNeededXp);
        CRAFTING_TABLE.registerItem("name", CraftingTable::getName);
        CRAFTING_TABLE.registerItem("inventoryName", CraftingTable::getInventoryName);
        CRAFTING_TABLE.registerItem("masteryUnlock", CraftingTable::getMasteryUnlock);
        CRAFTING_TABLE.registerItem("masteryFee", CraftingTable::getMasteryFee);
        CRAFTING_INVENTORY.registerItem("name", CustomGUI::getName);
        CRAFTING_INVENTORY.registerItem("inventoryName", CustomGUI::getInventoryName);

        if (Bukkit.getPluginManager().isPluginEnabled("Sapphire")) {
            RECIPE_ITEM.registerChild("customItem",
                    Sapphire.RISE_ITEM,
                    i -> {
                        if (i instanceof RecipeEconomyItem) {
                            ItemType itemType = ((RecipeEconomyItem) i).asItemType();
                            if (itemType instanceof SapphireItemProvider.SapphireItemType) {
                                return ((SapphireItemProvider.SapphireItemType) itemType).getRiseItem();
                            }
                        }
                        return null;
                    });
        }
        RECIPE_ITEM.registerChild("item", PlaceholderRegistry.ITEM, RecipeItem::getItemStack);
        RECIPE.registerChild("result", RECIPE_ITEM, Recipe::getResult);
        CALCULATED_RECIPE.registerChild("recipe", RECIPE, CalculatedRecipe::getRecipe);
        CALCULATED_RECIPE.registerChild("icon", PlaceholderRegistry.ITEM, CalculatedRecipe::getIcon);

        super.onLoad();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.reloadConfig();
        LevelFunction.generate(200);
        this.getCommand("craft").setExecutor(new Commands());
        getServer().getPluginManager().registerEvents(this, this);
    }

    public void closeAll() {
        ProfessionsCfg.getGuiMap().values().forEach(CustomGUI::closeAll);
        BrowseGUI.closeAll();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        try {
            experienceManager.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.closeAll();
    }

    private void runSaveTask() {
        if (saveTask != null && !saveTask.isCancelled())
            saveTask.cancel();

        long period = Cfg.dataSaveInterval;

        BukkitTask task = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            try {
                experienceManager.save();
            } catch (IOException e) {
                System.out.println("Could not save data files.");
                e.printStackTrace();
            }
        }, period, period);
        this.saveTask = task;
    }

    /**
     * Gets the experience manager
     *
     * @return experience manager
     */
    public static ExperienceManager getExperienceManager() {
        return experienceManager;
    }

    private final HashMap<UUID, Double> cachedCooldowns = new HashMap<>();

    public double getPlayerCooldown(Player player) {
        if (cachedCooldowns.containsKey(player.getUniqueId()))
            return cachedCooldowns.get(player.getUniqueId());

        double num = 0d;
        for (PermissionAttachmentInfo permission : player.getEffectivePermissions()) {
            String perm = permission.getPermission();
            if (perm.startsWith("fusion.cooldown")) {
                String mod = perm.substring(perm.lastIndexOf(".") + 1);
                if (mod.equals("*"))
                    num = 1d;
                else {
                    try {
                        num = Integer.parseInt(mod) / 100d;
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        cachedCooldowns.put(player.getUniqueId(), num);
        return num;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (Cfg.craftingQueue) {
            Bukkit.getConsoleSender().sendMessage("Player joined: " + player.getName());
            PlayerConfig config = PConfigManager.getPlayerConfig(player);
            int finishedQueueAmount = config.getFinishedQueueAmount();
            if(finishedQueueAmount > 0) {
                MessageUtil.sendMessage("fusion.queue.finished", Bukkit.getConsoleSender(), new MessageData("amount", finishedQueueAmount));
                MessageUtil.sendMessage("fusion.queue.finished", player, new MessageData("amount", finishedQueueAmount));
            }
        }
    }
}
