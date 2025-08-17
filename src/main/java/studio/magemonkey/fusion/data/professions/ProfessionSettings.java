package studio.magemonkey.fusion.data.professions;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.api.DelayedCommand;
import studio.magemonkey.codex.legacy.item.ItemBuilder;
import studio.magemonkey.codex.util.DeserializationWorker;
import studio.magemonkey.codex.util.SerializationBuilder;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.data.recipes.RecipeCustomItem;
import studio.magemonkey.fusion.data.recipes.RecipeItem;

import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
public class ProfessionSettings implements ConfigurationSerializable {

    private final String profession;

    // Icon related fields
    private RecipeItem recipeItem;
    private String iconNamespace;

    // Optional item fields
    private String name;
    private int customModelData = -1;
    private List<String> lore = new ArrayList<>();
    private boolean unbreakable;
    private Map<Enchantment, Integer> enchantments = new HashMap<>();
    private Set<ItemFlag> flags = new HashSet<>();
    private String color;
    private boolean cancelDrop;
    private List<DelayedCommand> commandsOnClick = new LinkedList<>();


    // Hiding related fields
    private Boolean hideNoPermission;
    private Boolean hideRecipeLimitReached;

    public ProfessionSettings(String profession, Boolean hideNoPermission,
                              Boolean hideRecipeLimitReached, String iconNamespace, String name, int customModelData, List<String> lore, boolean unbreakable, Map<Enchantment, Integer> enchantments, Set<ItemFlag> flags, String color, boolean cancelDrop, List<DelayedCommand> commandsOnClick) {
        this.profession = profession;
        this.hideNoPermission = hideNoPermission;
        this.hideRecipeLimitReached = hideRecipeLimitReached;

        this.iconNamespace = iconNamespace;
        this.name = name;
        this.customModelData = customModelData;
        this.lore = lore;
        this.unbreakable = unbreakable;
        this.enchantments = enchantments;
        this.flags = flags;
        this.color = color;
        this.cancelDrop = cancelDrop;
        this.commandsOnClick = commandsOnClick;

        // Setup of the icon
        generateIcon(iconNamespace);
    }

    public ProfessionSettings(String profession, ConfigurationSection config) {
        this.profession = profession;
        this.hideNoPermission = config.getBoolean("settings.hiding.hideNoPermission");
        this.hideRecipeLimitReached = config.getBoolean("settings.hiding.hideRecipeLimitReached");

        // Setup of the icon
        String iconNamespace = config.getString("settings.icon.item");
        if (config.isSet("settings.icon.optionals") && !config.getConfigurationSection("settings.icon.optionals").getKeys(false).isEmpty()) {
            name = config.getString("settings.icon.optionals.name");
            customModelData = config.getInt("settings.icon.optionals.customModelData", -1);
            lore = config.getStringList("settings.icon.optionals.lore");
            unbreakable = config.getBoolean("settings.icon.optionals.unbreakable");
            ConfigurationSection enchantmentsSection = config.getConfigurationSection("settings.icon.optionals.enchantments");
            if (enchantmentsSection != null) {
                enchantments = new HashMap<>();
                for (String enchantment : enchantmentsSection.getKeys(false)) {
                    enchantments.put(Enchantment.getByName(enchantment), enchantmentsSection.getInt(enchantment));
                }
            }
            List<String> flagsList = config.getStringList("settings.icon.optionals.flags");
            if(!flagsList.isEmpty()) {
                flags = new HashSet<>();
                if(flagsList.contains("*")) {
                    flags.addAll(Arrays.asList(ItemFlag.values()));
                } else {
                    for (String flag : flagsList) {
                        flags.add(ItemFlag.valueOf(flag));
                    }
                }
            }
            color = config.getString("settings.icon.optionals.color");
            cancelDrop = config.getBoolean("settings.icon.optionals.cancelDrop");
            this.commandsOnClick = config.getList("settings.icon.optionals.commandsOnClick", new LinkedList<>()).stream().map(entry -> new DelayedCommand()).collect(Collectors.toList());
        }
        generateIcon(iconNamespace);
    }

    public ProfessionSettings(String profession, DeserializationWorker dw) {
        this.profession = profession;
        Map<String, Object> settingsSection = dw.getSection("settings");

        if (settingsSection == null) {
            this.hideNoPermission = null;
            this.hideRecipeLimitReached = null;
        } else {
            Map<String, Object> hidingSettings = (Map<String, Object>) settingsSection.get("hiding");
            this.hideNoPermission =
                    (hidingSettings != null && hidingSettings.get("noPermission") != null) ? (boolean) hidingSettings.get("noPermission")
                            : null;
            this.hideRecipeLimitReached = (hidingSettings != null && hidingSettings.get("recipeLimitReached") != null)
                    ? (boolean) hidingSettings.get("recipeLimitReached") : null;

            // Setup of the icon
            Map<String, Object> iconSettings = (Map<String, Object>) settingsSection.get("icon");
            if (iconSettings == null) {
                Fusion.getInstance().getLogger().warning("Icon settings not found for in " + profession + " on recipe load.");
                return;
            }
            String iconNamespace = (String) iconSettings.get("item");
            Map<String, Object> optionalIconSettings = (Map<String, Object>) iconSettings.get("optionals");
            if (optionalIconSettings != null && !optionalIconSettings.isEmpty()) {
                if(optionalIconSettings.get("name") != null)
                    name = (String) optionalIconSettings.get("name");
                if(optionalIconSettings.get("customModelData") != null)
                    customModelData = (int) optionalIconSettings.get("customModelData");
                if(optionalIconSettings.get("lore") != null)
                    lore = (List<String>) optionalIconSettings.get("lore");
                if (optionalIconSettings.get("unbreakable") != null)
                    unbreakable = (boolean) optionalIconSettings.get("unbreakable");
                if(optionalIconSettings.get("enchantments") != null) {
                    enchantments = new HashMap<>();
                    Map<String, Integer> enchantmentsSection = (Map<String, Integer>) optionalIconSettings.get("enchantments");
                    if (enchantmentsSection != null) {
                        for (Map.Entry<String, Integer> enchantment : enchantmentsSection.entrySet()) {
                            enchantments.put(Enchantment.getByName(enchantment.getKey().toLowerCase()), enchantment.getValue());
                        }
                    }
                }
                if (optionalIconSettings.get("flags") != null) {
                    flags = new HashSet<>();
                    List<String> flagsList = (List<String>) optionalIconSettings.get("flags");
                    if(flagsList.contains("*")) {
                        flags.addAll(Arrays.asList(ItemFlag.values()));
                    } else {
                        for (String flag : flagsList) {
                            flags.add(ItemFlag.valueOf(flag));
                        }
                    }
                }
                if (optionalIconSettings.get("color") != null)
                    color = (String) optionalIconSettings.get("color");
                if (optionalIconSettings.get("cancelDrop") != null)
                    cancelDrop = (boolean) optionalIconSettings.get("cancelDrop");

                List<Map<String, Object>> commands = (List<Map<String, Object>>) optionalIconSettings.getOrDefault("commandsOnClick", new ArrayList<>());
                if (commands != null) {
                    for (Map<String, Object> command : commands) {
                        this.commandsOnClick.add(new DelayedCommand(command));
                    }
                }
            }
            generateIcon(iconNamespace);
        }
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> settingsMap = new HashMap<>();

        Map<String, Object> hiding = new HashMap<>(3);
        if (hideNoPermission != null) hiding.put("noPermission", hideNoPermission);
        if (hideRecipeLimitReached != null) hiding.put("recipeLimitReached", hideRecipeLimitReached);
        if (!hiding.isEmpty()) {
            settingsMap.put("hiding", hiding);
        }

        Map<String, Object> iconSettings = new HashMap<>(3);
        iconSettings.put("item", iconNamespace);
        Map<String, Object> optionalIconSettings = new HashMap<>(10);
        if (name != null) optionalIconSettings.put("name", name);
        if (customModelData >= 0) optionalIconSettings.put("customModelData", customModelData);
        if (lore != null && !lore.isEmpty()) optionalIconSettings.put("lore", lore);
        if (unbreakable) optionalIconSettings.put("unbreakable", unbreakable);
        if (enchantments != null && !enchantments.isEmpty()) {
            Map<String, Integer> enchantmentsSection = new HashMap<>(enchantments.size());
            for (Map.Entry<Enchantment, Integer> enchantment : enchantments.entrySet()) {
                enchantmentsSection.put(enchantment.getKey().getName(), enchantment.getValue());
            }
            optionalIconSettings.put("enchantments", enchantmentsSection);
        }
        if (flags != null && !flags.isEmpty()) {
            List<String> flagsList = new ArrayList<>(flags.size());
            for (ItemFlag flag : flags) {
                flagsList.add(flag.name());
            }
            optionalIconSettings.put("flags", flagsList);
        }
        if (color != null) optionalIconSettings.put("color", color);
        if (cancelDrop) optionalIconSettings.put("cancelDrop", cancelDrop);
        if (commandsOnClick != null && !commandsOnClick.isEmpty()) optionalIconSettings.put("commandsOnClick", new ArrayList<>(this.commandsOnClick.stream().map(DelayedCommand::serialize).collect(Collectors.toList())));
        if(!optionalIconSettings.isEmpty())
            iconSettings.put("optionals", optionalIconSettings);
        settingsMap.put("icon", iconSettings);

        return SerializationBuilder.start(4).append("settings", settingsMap).build();
    }

    public static ProfessionSettings copy(ProfessionSettings results) {
        return new ProfessionSettings(
                results.profession,
                results.hideNoPermission,
                results.hideRecipeLimitReached,
                results.iconNamespace,
                results.name,
                results.customModelData,
                results.lore,
                results.unbreakable,
                results.enchantments,
                results.flags,
                results.color,
                results.cancelDrop,
                results.commandsOnClick);
    }

    private void generateIcon(String namespace) {
        this.iconNamespace = namespace;

        RecipeItem recipeItem = RecipeItem.fromConfig(namespace);
        ItemStack iconReference = recipeItem.getItemStack();
        if (!hasAnyOptionalFields()) {
            this.recipeItem = recipeItem;
        } else {
            ItemBuilder builder = ItemBuilder.newItem(iconReference);
            ItemMeta meta = iconReference.getItemMeta();
            if (name != null)
                builder.name(name);
            if (customModelData >= 0)
                meta.setCustomModelData(customModelData);
            if (lore != null)
                builder = builder.lore(lore);
            if (enchantments != null)
                builder = builder.enchant(enchantments);
            if (flags != null)
                builder = builder.flag(flags.toArray(new ItemFlag[0]));
            if (color != null && (meta instanceof LeatherArmorMeta leatherArmorMeta)) {
                String[] colorData = color.split(",");
                try {
                    leatherArmorMeta.setColor(Color.fromRGB(Integer.parseInt(colorData[0]), Integer.parseInt(colorData[1]), Integer.parseInt(colorData[2])));
                } catch (IllegalArgumentException e) {
                    Fusion.getInstance().getLogger().warning("Invalid color data for profession icon: " + color + " for profession " + profession + ". Using white instead.");
                    leatherArmorMeta.setColor(Color.fromRGB(255, 255, 255));
                }
                meta = leatherArmorMeta;
            } else if (color != null && (meta instanceof PotionMeta potionMeta)) {
                String[] colorData = color.split(",");
                try {
                    potionMeta.setColor(Color.fromRGB(Integer.parseInt(colorData[0]), Integer.parseInt(colorData[1]), Integer.parseInt(colorData[2])));
                } catch (IllegalArgumentException e) {
                    Fusion.getInstance().getLogger().warning("Invalid color data for profession icon: " + color + " for profession " + profession + ". Using white instead.");
                    potionMeta.setColor(Color.fromRGB(255, 255, 255));
                }
                meta = potionMeta;
            }
            builder = builder.data(meta);
            this.recipeItem = new RecipeCustomItem(builder, iconReference.getAmount(), false);
        }
    }

    private boolean hasAnyOptionalFields() {
        return name != null || customModelData >= 0 || (lore != null && !lore.isEmpty()) || (enchantments != null && !enchantments.isEmpty()) || (flags != null && !flags.isEmpty()) || color != null;
    }
}
