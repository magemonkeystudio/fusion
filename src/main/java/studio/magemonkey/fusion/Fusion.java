package studio.magemonkey.fusion;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachmentInfo;
import studio.magemonkey.codex.config.legacy.LegacyConfigManager;
import studio.magemonkey.codex.legacy.RisePlugin;
import studio.magemonkey.codex.legacy.placeholder.PlaceholderRegistry;
import studio.magemonkey.codex.legacy.placeholder.PlaceholderType;
import studio.magemonkey.codex.util.ItemUtils;
import studio.magemonkey.codex.util.messages.MessageUtil;
import studio.magemonkey.fusion.cfg.BrowseConfig;
import studio.magemonkey.fusion.cfg.Cfg;
import studio.magemonkey.fusion.cfg.PConfigManager;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.cfg.player.PlayerLoader;
import studio.magemonkey.fusion.cfg.sql.SQLManager;
import studio.magemonkey.fusion.gui.BrowseGUI;
import studio.magemonkey.fusion.gui.CustomGUI;

import java.io.File;
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
    private static Fusion instance;

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
        SQLManager.init();
        BrowseConfig.load();
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

        if (!Bukkit.getPluginManager().isPluginEnabled("Sapphire")) {
            RECIPE_ITEM.registerChild("customItem",
                    ItemUtils.ITEM_TYPE,
                    i -> {
                        if (i instanceof RecipeEconomyItem) {
                            return ((RecipeEconomyItem) i).asItemType();
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
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            Fusion.getInstance().getLogger().info("Attempting to migrate data into SQL [ExperienceManager].");
            ExperienceManager.migrateIntoSQL();
            Fusion.getInstance().getLogger().info("Attempting to migrate data into SQL [PConfigManager].");
            PConfigManager.migrateIntoSQL();
        });
        LevelFunction.generate(200);
        this.getCommand("craft").setExecutor(new Commands());
        getServer().getPluginManager().registerEvents(this, this);
        runQueueTask();
    }

    public void closeAll() {
        ProfessionsCfg.getGuiMap().values().forEach(CustomGUI::closeAll);
        BrowseGUI.closeAll();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        PlayerLoader.clearCache();
        this.closeAll();
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

    private void notifyForQueue(Player player) {
        int finishedQueueAmount = PlayerLoader.getPlayer(player.getUniqueId()).getFinishedSize();
        if (finishedQueueAmount > 0) {
            Cfg.notifyForQueue(player, finishedQueueAmount);
        }
    }

    private void runQueueTask() {
        Bukkit.getScheduler().runTaskTimer(this,
                () -> Bukkit.getOnlinePlayers().forEach(this::notifyForQueue), 0, Cfg.finishedMessageInterval * 20L);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        PlayerLoader.loadPlayer(event.getPlayer());
        if (Cfg.craftingQueue) {
            notifyForQueue(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        PlayerLoader.unloadPlayer(event.getPlayer());
    }
}