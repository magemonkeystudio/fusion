package studio.magemonkey.fusion.data.recipes;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.codex.items.ItemType;
import studio.magemonkey.codex.items.exception.MissingItemException;
import studio.magemonkey.codex.items.exception.MissingProviderException;
import studio.magemonkey.codex.legacy.item.ItemBuilder;
import studio.magemonkey.codex.util.SerializationBuilder;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.cfg.hooks.ItemGenEntry;
import studio.magemonkey.fusion.cfg.hooks.divinity.DivinityRecipeMeta;
import studio.magemonkey.fusion.cfg.hooks.divinity.DivinityService;
import studio.magemonkey.fusion.data.professions.pattern.Category;
import studio.magemonkey.fusion.data.professions.pattern.InventoryPattern;
import studio.magemonkey.risecore.legacy.util.DeserializationWorker;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
public class CraftingTable implements ConfigurationSerializable {
    private String name;
    private String inventoryName;
    private InventoryPattern pattern;
    private InventoryPattern catPattern;
    private ItemStack fillItem;
    private Map<String, Recipe> recipes;
    private ItemType iconItem;
    private boolean useCategories = true;
    @Getter
    private LinkedHashMap<String, Category> categories = new LinkedHashMap<>();

    //Mastery!
    private int masteryUnlock;
    private int masteryFee;

    public CraftingTable(String name,
                         String inventoryName,
                         ItemType iconItem,
                         InventoryPattern pattern,
                         InventoryPattern catPattern,
                         boolean useCategories,
                         ItemStack fillItem,
                         int masteryUnlock,
                         int masteryFee,
                         Map<String, Recipe> recipes,
                         Map<String, Category> categories) {
        this.name = name;
        this.inventoryName = inventoryName;
        this.iconItem = iconItem;
        this.pattern = pattern;
        this.catPattern = catPattern;
        this.useCategories = useCategories;
        this.recipes = recipes;
        this.fillItem = fillItem;
        this.masteryUnlock = masteryUnlock;
        this.masteryFee = masteryFee;
        this.categories = new LinkedHashMap<>(categories);
    }

    public CraftingTable(String name,
                         String inventoryName,
                         ItemType iconItem,
                         InventoryPattern pattern,
                         ItemStack fillItem,
                         int masteryUnlock,
                         int masteryFee) {
        this.name = name;
        this.inventoryName = inventoryName;
        this.iconItem = iconItem;
        this.pattern = pattern;
        this.catPattern = null;
        this.recipes = new LinkedHashMap<>(5);
        this.fillItem = fillItem;
        this.masteryUnlock = masteryUnlock;
        this.masteryFee = masteryFee;
    }

    @SuppressWarnings("unchecked")
    public CraftingTable(Map<String, Object> map) throws MissingProviderException, MissingItemException {
        this.recipes = new LinkedHashMap<>(5);
        DeserializationWorker dw = DeserializationWorker.start(map);
        this.name = dw.getString("name");
        this.inventoryName = dw.getString("inventoryName");
        this.pattern = new InventoryPattern(dw.getSection("pattern"));
        this.catPattern =
                dw.getSection("categoryPattern") != null && dw.getSection("categoryPattern").containsKey("pattern") ? new InventoryPattern(dw.getSection("categoryPattern"))
                        : null;
        this.masteryUnlock = dw.getInt("masteryUnlock");
        this.masteryFee = dw.getInt("masteryFee");
        this.useCategories = dw.getBoolean("useCategories", true);
        this.iconItem = CodexEngine.get().getItemManager().getItemType(dw.getString("icon"));
        if (dw.getSection("pattern.items.fillItem") != null)
            this.fillItem = new ItemBuilder(dw.getSection("pattern.items.fillItem")).build();
        else
//            this.fillItem = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
            this.fillItem = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        Collection<Category> categoriesList = new ArrayList<>();
        dw.deserializeCollection(categoriesList, "categories", Category.class);
        categoriesList.stream()
                .filter(c -> c.getIconItem() != null)
                .forEach(c -> {
                    if (c.getPattern() == null)
                        c.setPattern(catPattern);
                    categories.put(c.getName(), c);
                });

        List<Map<?, ?>> recipesSection = dw.getList("recipes", new ArrayList<>(2));
        for (Map<?, ?> recipeData : recipesSection) {
            try {
                Map<?, ?> results = (Map<?, ?>) recipeData.get("results");
                String itemResult = (String) results.get("item");
                if (itemResult.startsWith("DIVINITY_ITEMGEN") || itemResult.startsWith("DIVINITY_IT")) {
                    String[] itemArgs = itemResult.split("_", 3);
                    if (itemArgs.length < 3) {
                        Fusion.getInstance().error("Invalid entry in config of " + this.name + " (ItemGenerator entry) in crafting table. Value: " + recipeData);
                        continue;
                    }
                    String[] params = itemArgs[2].split(":");
                    String namespace = params[0];
                    if (!DivinityService.isCached(namespace)) {
                        if (!DivinityService.cache(namespace)) {
                            Fusion.getInstance().error("Invalid entry in config of " + this.name + " (ItemGenerator entry) in crafting table. Value: " + recipeData);
                            continue;
                        }
                    }

                    int level = -1;
                    int amount = 1;
                    ItemType type = null;

                    if (params.length >= 2) {
                        level = Integer.parseInt(params[1]);
                    }
                    if (params.length >= 3) {
                        amount = Integer.parseInt(params[2]);
                    }
                    if (params.length >= 4) {
                        type = CodexEngine.get().getItemManager().getItemType(params[3]);
                    }

                    ItemGenEntry entry = DivinityService.ItemGenResults.get(namespace);
                    if (entry == null) {
                        Fusion.getInstance().error("Invalid entry in config of " + this.name + " (ItemGenerator entry) in crafting table. Value: " + recipeData);
                        continue;
                    }
                    Map<ItemType, Set<String>> names = entry.loadNames(type);

                    Category category = null;
                    if (recipeData.containsKey("category") && recipeData.get("category") instanceof String) {
                        category = categories.get(recipeData.get("category"));
                    }

                    for (Map.Entry<ItemType, Set<String>> nameEntry : names.entrySet()) {
                        for (String name : nameEntry.getValue()) {
                            DivinityRecipeMeta meta = new DivinityRecipeMeta(entry, level, amount, nameEntry.getKey(), name);
                            Recipe recipe = new Recipe(this, (Map<String, Object>) recipeData, meta);
                            recipe.setName(recipe.getName() + "_" + UUID.randomUUID());
                            recipes.put(recipe.getName(), recipe);
                            if (category != null) {
                                category.getRecipes().add(recipe);
                            }
                        }
                    }
                } else {
                    Recipe recipe = new Recipe(this, (Map<String, Object>) recipeData);
                    this.recipes.put(recipe.getName(), recipe);

                    if (recipeData.containsKey("category") && recipeData.get("category") instanceof String) {
                        Category category = categories.get(recipeData.get("category"));

                        if (category == null) {
                            continue;
                        }

                        category.getRecipes().add(recipe);
                    }
                }
            } catch (Exception e) {
                Fusion.getInstance()
                        .error("Exception when reading config, Invalid entry in config of " + this.name
                                + " crafting table. Value: " + recipeData);
                e.printStackTrace();
            }
        }
    }

    public List<String> getCategoryList() {
        return new ArrayList<>(categories.keySet());
    }

    public Recipe getRecipe(String str) {
        return this.recipes.get(str);
    }

    public void addRecipe(Recipe recipe) {
        this.recipes.put(recipe.getName(), recipe);
    }

    public boolean getUseCategories() {
        return useCategories;
    }

    public Collection<Recipe> getRecipes(Collection<ItemStack> items, Player p) {
        if (items.isEmpty()) {
            return new ArrayList<>(this.recipes.values());
        }
        return this.recipes.values()
                .stream()
                .unordered()
                .filter(r -> r.isValid(items, p, this))
                .collect(Collectors.toList());
    }

    public Category getCategory(String name) {
        return this.categories.get(name);
    }

    public void updateCategoryOrder() {
        LinkedHashMap<String, Category> newCategories = new LinkedHashMap<>();
        // Compare the categories by their order and make a new map with the sorted categories
        this.categories.values().stream()
                .sorted(Comparator.comparingInt(Category::getOrder))
                .forEach(c -> newCategories.put(c.getName(), c));
        this.categories = newCategories;
    }

    public void moveEntry(Recipe recipe, int offset) {
        // Ensure the offset is either -1 (left) or 1 (right)
        if (offset != -1 && offset != 1) {
            throw new IllegalArgumentException("Offset must be -1 or 1");
        }

        List<Map.Entry<String, Recipe>> entries = new ArrayList<>(recipes.entrySet());
        int index = -1;

        // Find the index of the current entry
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getKey().equals(recipe.getName())) {
                index = i;
                break;
            }
        }

        if (index == -1) {
            return; // Key not found, do nothing
        }

        // Calculate the new index
        int newIndex = index + offset;

        // Check if the new index is within bounds
        if (newIndex < 0 || newIndex >= entries.size()) {
            return; // New index out of bounds, do nothing
        }

        // Remove and reinsert the entry at the new position
        Map.Entry<String, Recipe> entry = entries.remove(index);
        entries.add(newIndex, entry);

        // Clear the original map and reinsert the entries in the new order
        recipes.clear();
        for (Map.Entry<String, Recipe> e : entries) {
            recipes.put(e.getKey(), e.getValue());
        }
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return SerializationBuilder.start(4)
                .append("name", this.name)
                .append("icon", this.iconItem.getNamespacedID())
                .append("pattern", this.pattern.serialize())
                .append("categoryPattern", this.catPattern != null ? this.catPattern.serialize() : null)
                .append("inventoryName", this.inventoryName)
                .append("masteryUnlock", this.masteryUnlock)
                .append("masteryFee", this.masteryFee)
                .append("useCategories", useCategories)
                .append("recipes", this.recipes.values().stream().map(Recipe::serialize).collect(Collectors.toList()))
                .build();
    }

    public void save() {
        // Saving all changes to the file
        FileConfiguration config = ProfessionsCfg.getCfgs().get(this.name);
        File file = ProfessionsCfg.getFiles().get(this.name);
        Map<String, Object> map = this.serialize();

        Map<String, Object> patterntemsMap = (Map<String, Object>) map.get("pattern");
        Map<String, Object> catPatterntemsMap = (Map<String, Object>) map.get("categoryPattern");
        patterntemsMap.remove("f");
        patterntemsMap.remove("q");
        patterntemsMap.remove("o");
        if (this.catPattern != null) {
            catPatterntemsMap.remove("f");
            catPatterntemsMap.remove("q");
            catPatterntemsMap.remove("o");
        }
        config.set("name", map.get("name"));
        config.set("inventoryName", map.get("inventoryName"));
        config.set("icon", map.get("icon"));
        config.set("pattern", patterntemsMap);
        config.set("categoryPattern", catPatterntemsMap);
        config.set("masteryUnlock", map.get("masteryUnlock"));
        config.set("masteryFee", map.get("masteryFee"));
        config.set("useCategories", map.get("useCategories"));
        config.set("recipes", map.get("recipes"));
        try {
            config.save(file);
            ProfessionsCfg.init();
        } catch (IOException e) {
            Fusion.getInstance().getLogger().warning("Can't load crafting table: " + e.getMessage());
        }
    }

    // Static method to copy contents from one CraftingTable instance to another
    public static CraftingTable copy(CraftingTable source) {
        Map<String, Recipe> recipes = new LinkedHashMap<>();
        for (Recipe recipe : source.getRecipes().values()) {
            recipes.put(recipe.getName(), Recipe.copy(recipe));
        }

        Map<String, Category> categories = new LinkedHashMap<>();
        for (Category category : source.getCategories().values()) {
            categories.put(category.getName(), Category.copy(category));
        }
        return new CraftingTable(source.getName(), source.getInventoryName(), source.getIconItem(),
                InventoryPattern.copy(source.getPattern()), InventoryPattern.copy(source.getCatPattern()), source.getUseCategories(), source.getFillItem(), source.getMasteryUnlock(), source.getMasteryFee(),
                recipes, categories);
    }
}
