package studio.magemonkey.fusion.cfg;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import studio.magemonkey.codex.items.providers.VanillaProvider;
import studio.magemonkey.codex.legacy.item.ItemBuilder;
import studio.magemonkey.codex.legacy.item.ItemColors;
import studio.magemonkey.fusion.CraftingTable;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.InventoryPattern;
import studio.magemonkey.fusion.Utils;
import studio.magemonkey.fusion.gui.CustomGUI;
import studio.magemonkey.fusion.queue.QueueItem;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ProfessionsCfg {

    @Getter
    private static final Map<String, CraftingTable>     map    = new HashMap<>(4);
    @Getter
    private static final Map<String, CustomGUI>         guiMap = new HashMap<>(4);
    @Getter
    private static final Map<String, FileConfiguration> cfgs   = new HashMap<>(4);

    public static void init() {
        map.clear();
        guiMap.clear();
        File professionFolder = new File(Fusion.getInstance().getDataFolder(), "professions");
        if (!professionFolder.exists()) {
            professionFolder.mkdirs();
        }
        if (professionFolder.listFiles() == null) {
            Fusion.getInstance().getLogger().warning("There are no professions registered to load.");
            Fusion.getInstance().getLogger().warning("Initializing default profession 'craft'");
            HashMap<Character, ItemStack> items = new HashMap<>();
            items.put('0', ItemBuilder.newItem(Material.STONE).durability(ItemColors.BLACK).build());
            items.put('>', ItemBuilder.newItem(Material.BOOK).name("Next page").build());
            items.put('<', ItemBuilder.newItem(Material.BOOK).name("Prev page").build());
            InventoryPattern ip =
                    new InventoryPattern(new String[]{"=========", "=========", "=========", "=========",
                            Cfg.craftingQueue ? "{-------}" : "=========", "<0000000>"},
                            items);
            ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
            CraftingTable b = new CraftingTable("craft",
                    "Craft inventory name",
                    new VanillaProvider.VanillaItemType(Material.PAPER),
                    ip,
                    item,
                    0,
                    0);
            loadFrom("craft", b.serialize());
            return;
        }

        for (File file : Objects.requireNonNull(professionFolder.listFiles())) {
            if (file.getName().endsWith(".yml")) {
                FileConfiguration cfg = new YamlConfiguration();
                try {
                    cfg.load(file);
                    addCraftingQueueDefs(cfg);
                    cfg.save(file);
                    cfg.load(file);
                    // Get the YAMLs whole content as a map
                    Map<String, Object> _map = cfg.getValues(true);
                    CraftingTable       ct   = new CraftingTable(_map);
                    map.put(ct.getName(), ct);
                    cfgs.put(ct.getName(), cfg);
                } catch (Exception e) {
                    e.printStackTrace();
                    Fusion.getInstance().getLogger().warning("Can't load crafting table: " + e.getMessage());
                }
            }
        }

        for (Map.Entry<String, CraftingTable> entry : map.entrySet()) {
            String        key   = entry.getKey();
            CraftingTable value = entry.getValue();
            guiMap.put(key, new CustomGUI(key, value.getInventoryName(), value.getPattern()));
        }
    }

    public static CraftingTable getTable(String str) {
        return map.get(str);
    }

    public static CustomGUI getGUI(String str) {
        if (str == null) return null;
        return guiMap.get(str);
    }

    private static void addCraftingQueueDefs(FileConfiguration cfg) {
        if (!cfg.isSet("pattern.items.}")) {
            cfg.set("pattern.items.}.material", "LIME_DYE");
            cfg.set("pattern.items.}.amount", 1);
            cfg.set("pattern.items.}.durability", 0);
            cfg.set("pattern.items.}.unbreakable", false);
            cfg.set("pattern.items.}.name", "Next queued items");
            cfg.set("pattern.items.}.lore", List.of());
            cfg.set("pattern.items.}.flags", List.of());
            cfg.set("pattern.items.}.enchants", Map.of());
        }
        if (!cfg.isSet("pattern.items.{")) {
            cfg.set("pattern.items.{.material", "LIME_DYE");
            cfg.set("pattern.items.{.amount", 1);
            cfg.set("pattern.items.{.durability", 0);
            cfg.set("pattern.items.{.unbreakable", false);
            cfg.set("pattern.items.{.name", "Previous queued items");
            cfg.set("pattern.items.{.lore", List.of());
            cfg.set("pattern.items.{.flags", List.of());
            cfg.set("pattern.items.{.enchants", Map.of());
        }
        if (!cfg.isSet("pattern.items.queue-items.-")) {
            cfg.set("pattern.items.queue-items.-.material", "GRAY_STAINED_GLASS_PANE");
            cfg.set("pattern.items.queue-items.-.amount", 1);
            cfg.set("pattern.items.queue-items.-.durability", 0);
            cfg.set("pattern.items.queue-items.-.unbreakable", false);
            cfg.set("pattern.items.queue-items.-.name", "&cQueue Slot");
            cfg.set("pattern.items.queue-items.-.lore", List.of("&7This slot is empty."));
            cfg.set("pattern.items.queue-items.-.flags", List.of());
            cfg.set("pattern.items.queue-items.-.enchants", Map.of());
        }
        if (!cfg.isSet("pattern.items.queue-items.Unfinished")) {
            cfg.set("pattern.items.queue-items.Unfinished.material", "%material%");
            cfg.set("pattern.items.queue-items.Unfinished.amount", 1);
            cfg.set("pattern.items.queue-items.Unfinished.durability", 0);
            cfg.set("pattern.items.queue-items.Unfinished.unbreakable", false);
            cfg.set("pattern.items.queue-items.Unfinished.name", "&cQueue Slot");
            cfg.set("pattern.items.queue-items.Unfinished.lore",
                    List.of("&7&oThis item is in the crafting queue",
                            " ",
                            "&7Time left: &c%time%",
                            " ",
                            "&eClick to cancel"));
            cfg.set("pattern.items.queue-items.Unfinished.flags", List.of());
            cfg.set("pattern.items.queue-items.Unfinished.enchants", Map.of());
        }
        if (!cfg.isSet("pattern.items.queue-items.Finished")) {
            cfg.set("pattern.items.queue-items.Finished.material", "%material%");
            cfg.set("pattern.items.queue-items.Finished.amount", 1);
            cfg.set("pattern.items.queue-items.Finished.durability", 0);
            cfg.set("pattern.items.queue-items.Finished.unbreakable", false);
            cfg.set("pattern.items.queue-items.Finished.name", "&cQueue Slot");
            cfg.set("pattern.items.queue-items.Finished.lore",
                    List.of("&7&oThis item is in the crafting queue",
                            " ",
                            "&7The item is &afinished&7!",
                            " ",
                            "&eClick to obtain"));
            cfg.set("pattern.items.queue-items.Finished.flags", List.of());
            cfg.set("pattern.items.queue-items.Finished.enchants", Map.of());
        }
        if (!cfg.isSet("pattern.items.fillItem")) {
            cfg.set("pattern.items.fillItem.material", "BLACK_STAINED_GLASS_PANE");
            cfg.set("pattern.items.fillItem.amount", 1);
            cfg.set("pattern.items.fillItem.durability", 0);
            cfg.set("pattern.items.fillItem.unbreakable", false);
            cfg.set("pattern.items.fillItem.name", " ");
            cfg.set("pattern.items.fillItem.lore", List.of());
            cfg.set("pattern.items.fillItem.flags", List.of());
            cfg.set("pattern.items.fillItem.enchants", Map.of());
        }
    }

    public static boolean loadFrom(String key, Map<String, Object> map) {
        try {
            File professionFolder = new File(Fusion.getInstance().getDataFolder(), "professions");
            if (!professionFolder.exists()) {
                professionFolder.mkdirs();
            }

            File file = new File(professionFolder, key + ".yml");
            if (file.exists()) {
                Fusion.getInstance().getLogger().warning("Profession '" + key + "' was already migrated.");
                return true;
            }
            FileConfiguration cfg = new YamlConfiguration();
            cfg.set("name", key);
            cfg.set("icon", map.get("icon"));
            cfg.set("inventoryName", map.get("inventoryName"));
            cfg.set("useCategories", map.get("useCategories"));
            cfg.set("pattern", map.get("pattern"));
            cfg.set("masteryUnlock", map.get("masteryUnlock"));
            cfg.set("masteryFee", map.get("masteryFee"));
            cfg.set("categories", map.get("categories"));
            cfg.set("recipes", map.get("recipes"));
            cfg.save(file);
            Fusion.getInstance().getLogger().warning("Profession '" + key + "' migrated successfully.");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            Fusion.getInstance().getLogger().warning("Can't load crafting table: " + e.getMessage());
            return false;
        }
    }

    public static ItemStack getFillItem(String key) {
        String       name     = cfgs.get(key).getString("pattern.items.fillItem.name", " ");
        List<String> lore     = cfgs.get(key).getStringList("pattern.items.fillItem.lore");
        Material     material = Material.getMaterial(cfgs.get(key)
                .getString("pattern.items.fillItem.material", "BLACK_STAINED_GLASS_PANE"));
        return ItemBuilder.newItem(material).name(name).lore(lore).build();
    }

    public static ItemStack getQueueSlot(String key) {
        FileConfiguration cfg = cfgs.get(key);
        if (!cfg.isSet("pattern.items.queue-items.-")) {
            Fusion.getInstance()
                    .getLogger()
                    .warning("Profession '" + key + "' does not have a queue slot. Using default.");
            return ItemBuilder.newItem(Material.GRAY_STAINED_GLASS_PANE)
                    .name("&cQueue Slot")
                    .lore(List.of("&7This slot is empty."))
                    .build();
        }
        Material material =
                Material.getMaterial(cfg.getString("pattern.items.queue-items.-.material", "GRAY_STAINED_GLASS_PANE"));
        return ItemBuilder.newItem(material)
                .name(cfg.getString("pattern.items.queue-items.-.name", "&cQueue Slot"))
                .lore(cfg.getStringList("pattern.items.queue-items.-.lore"))
                .build();
    }

    public static ItemStack getQueueItem(String key, QueueItem item) {
        /* Fetch stored data to the queued item */
        //System.out.println("Fetching queued item for " + key + " with item " + item.getRecipe().getResult().getItemStack().getType());
        FileConfiguration cfg  = cfgs.get(key);
        String            path =
                item.isDone() ? "pattern.items.queue-items.Finished" : "pattern.items.queue-items.Unfinished";


        if (!cfg.isSet(path)) {
            Fusion.getInstance().getLogger().warning("Profession '" + key + "' is missing '" + path + "'.");
            return null;
        }
        ItemStack    result   = item.getRecipe().getResult().getItemStack();
        Material     material = Material.getMaterial(cfg.getString(path + ".material", "STONE")
                .replace("%material%", result.getType().toString())
                .toUpperCase());
        List<String> lore     = cfg.getStringList(path + ".lore");
        lore.replaceAll(s -> s.replace("%time%", Utils.getFormattedTime(item.getVisualRemainingItemTime())));
        return ItemBuilder.newItem(result).material(material).lore(lore).build();
    }
}
