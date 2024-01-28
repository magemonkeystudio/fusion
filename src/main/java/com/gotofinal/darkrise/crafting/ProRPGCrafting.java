package com.gotofinal.darkrise.crafting;

import com.gotofinal.darkrise.crafting.cfg.BrowseConfig;
import com.gotofinal.darkrise.crafting.cfg.Cfg;
import com.gotofinal.darkrise.crafting.gui.BrowseGUI;
import com.gotofinal.darkrise.crafting.gui.CustomGUI;
import com.gotofinal.darkrise.economy.DarkRiseEconomy;
import com.gotofinal.darkrise.economy.ProMCUtilitiesProvider;
import mc.promcteam.engine.items.ItemType;
import me.travja.darkrise.core.ConfigManager;
import me.travja.darkrise.core.RisePlugin;
import me.travja.darkrise.core.legacy.killme.chat.placeholder.PlaceholderType;
import me.travja.darkrise.core.legacy.util.Init;
import me.travja.darkrise.core.legacy.util.message.MessageUtil;
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
 * © 2024 ProMCTeam
 */
public class ProRPGCrafting extends RisePlugin implements Listener {
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

    private static ProRPGCrafting    instance;
    private static ExperienceManager experienceManager;

    private BukkitTask saveTask;

    {
        instance = this;
    }

    public static ProRPGCrafting getInstance() {
        return instance;
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        FileConfiguration lang =
                ConfigManager.loadConfigFile(new File(getDataFolder() + File.separator + "lang", "lang_en.yml"),
                        getResource("lang/lang_en.yml"));
        MessageUtil.reload(lang, this);
        Cfg.init();
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

        if (!Bukkit.getPluginManager().isPluginEnabled("ProMCUtilities")) {
            RECIPE_ITEM.registerChild("customItem",
                    DarkRiseEconomy.RISE_ITEM,
                    i -> {
                        if (i instanceof RecipeEconomyItem) {
                            ItemType itemType = ((RecipeEconomyItem) i).asItemType();
                            if (itemType instanceof ProMCUtilitiesProvider.ProMCUtilitiesItemType) {
                                return ((ProMCUtilitiesProvider.ProMCUtilitiesItemType) itemType).getRiseItem();
                            }
                        }
                        return null;
                    });
        }
        RECIPE_ITEM.registerChild("item", Init.ITEM, RecipeItem::getItemStack);
        RECIPE.registerChild("result", RECIPE_ITEM, Recipe::getResult);
        CALCULATED_RECIPE.registerChild("recipe", RECIPE, CalculatedRecipe::getRecipe);
        CALCULATED_RECIPE.registerChild("icon", Init.ITEM, CalculatedRecipe::getIcon);

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
        Cfg.getGuiMap().values().forEach(CustomGUI::closeAll);
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

    private HashMap<UUID, Double> cachedCooldowns = new HashMap<>();

    public double getPlayerCooldown(Player player) {
        if (cachedCooldowns.containsKey(player.getUniqueId()))
            return cachedCooldowns.get(player.getUniqueId());

        double num = 0d;
        for (PermissionAttachmentInfo permission : player.getEffectivePermissions()) {
            String perm = permission.getPermission();
            if (perm.startsWith("crafting.cooldown")) {
                String mod = perm.substring(perm.lastIndexOf(".") + 1);
                if (mod.equals("*"))
                    num = 1d;
                else {
                    try {
                        num = Integer.parseInt(mod) / 100d;
                    } catch (NumberFormatException e) {
                    }
                }
            }
        }
        cachedCooldowns.put(player.getUniqueId(), num);
        return num;
    }
}
