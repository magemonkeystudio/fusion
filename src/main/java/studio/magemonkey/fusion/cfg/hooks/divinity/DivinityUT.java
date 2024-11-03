package studio.magemonkey.fusion.cfg.hooks.divinity;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import studio.magemonkey.divinity.modules.list.itemgenerator.ItemGeneratorManager;
import studio.magemonkey.divinity.modules.list.itemgenerator.ResourceManager;
import studio.magemonkey.divinity.stats.tiers.Tier;
import studio.magemonkey.divinity.utils.ItemUtils;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class DivinityUT {

    public static Set<String> getAllCombinations(ItemGeneratorManager.GeneratorItem item, Material material, Tier tier, int level) {
        String name = item.getName();
        String itemGroupName = ItemUtils.getItemGroupNameFor(new ItemStack(material));
        Set<String> names = new HashSet<>();

        // Create maps for prefixes and suffixes for each category
        Map<ResourceManager.ResourceCategory, Set<String>> prefixes = new LinkedHashMap<>();
        Map<ResourceManager.ResourceCategory, Set<String>> suffixes = new LinkedHashMap<>();

        // Populate prefixes and suffixes by category
        populateAffixes(prefixes, suffixes, material, tier);

        // Process all combinations
        generatePrefixSuffixCombinations(names, name, itemGroupName, prefixes, suffixes);

        if(level > 0) {
            // Replace %ITEM_LEVEL% with level
            Set<String> newNames = new HashSet<>();
            for (String n : names) {
                newNames.add(n.replace("%ITEM_LEVEL%", String.valueOf(level)));
            }
            names = newNames;
        }
        return names;
    }

    private static void populateAffixes(Map<ResourceManager.ResourceCategory, Set<String>> prefixes,
                                        Map<ResourceManager.ResourceCategory, Set<String>> suffixes,
                                        Material material, Tier tier) {
        prefixes.put(ResourceManager.ResourceCategory.TIER, new HashSet<>(ItemGeneratorManager.getMatchingTierPrefixes(tier)));
        prefixes.put(ResourceManager.ResourceCategory.MATERIAL, new HashSet<>(ItemGeneratorManager.getMatchingMaterialPrefixes(material)));
        prefixes.put(ResourceManager.ResourceCategory.SUBTYPE, new HashSet<>(ItemGeneratorManager.getMatchingTypePrefixes(new ItemStack(material))));

        suffixes.put(ResourceManager.ResourceCategory.TIER, new HashSet<>(ItemGeneratorManager.getMatchingTierSuffixes(tier)));
        suffixes.put(ResourceManager.ResourceCategory.MATERIAL, new HashSet<>(ItemGeneratorManager.getMatchingMaterialSuffixes(material)));
        suffixes.put(ResourceManager.ResourceCategory.SUBTYPE, new HashSet<>(ItemGeneratorManager.getMatchingTypeSuffixes(new ItemStack(material))));
    }

    private static void generatePrefixSuffixCombinations(Set<String> names, String name, String itemGroupName,
                                                         Map<ResourceManager.ResourceCategory, Set<String>> prefixes,
                                                         Map<ResourceManager.ResourceCategory, Set<String>> suffixes) {
        // Replace %item_type% first
        String baseName = name.replace("%item_type%", itemGroupName);

        // Get all valid prefix combinations
        Set<String> prefixCombinations = generateCombinations(prefixes, baseName, true);

        // For each valid prefix combination, generate suffixes
        for (String prefixName : prefixCombinations) {
            Set<String> suffixCombinations = generateCombinations(suffixes, prefixName, false);
            if (suffixCombinations.isEmpty()) {
                names.add(cleanupName(prefixName)); // Clean up and add prefix only
            } else {
                suffixCombinations.forEach(suffixName -> names.add(cleanupName(suffixName))); // Clean up and add prefix + suffix combinations
            }
        }
    }

    // Helper method to clean up extra spaces
    private static String cleanupName(String name) {
        return name.trim().replaceAll("\\s{2,}", " ")
                .replace("%BASE_NAME% ", "");
    }

    private static Set<String> generateCombinations(Map<ResourceManager.ResourceCategory, Set<String>> affixes,
                                                    String name, boolean isPrefix) {
        Set<String> combinations = new HashSet<>();
        combinations.add(name.trim());

        // For each affix category, replace placeholders if affixes exist
        for (Map.Entry<ResourceManager.ResourceCategory, Set<String>> entry : affixes.entrySet()) {
            String placeholder = getPlaceholder(entry.getKey(), isPrefix);
            Set<String> values = entry.getValue();

            if (!values.isEmpty()) {
                Set<String> newCombinations = new HashSet<>();
                for (String existingName : combinations) {
                    for (String value : values) {
                        newCombinations.add(existingName.replace(placeholder, value).trim());
                    }
                }
                combinations = newCombinations;
            } else {
                // If no affixes for this category, remove the placeholder from the name
                Set<String> cleanedCombinations = new HashSet<>();
                for (String existingName : combinations) {
                    cleanedCombinations.add(existingName.replace(placeholder, "").trim());
                }
                combinations = cleanedCombinations;
            }
        }

        return combinations;
    }

    private static String getPlaceholder(ResourceManager.ResourceCategory category, boolean isPrefix) {
        return switch (category) {
            case TIER -> isPrefix ? "%prefix_tier%" : "%suffix_tier%";
            case MATERIAL -> isPrefix ? "%prefix_material%" : "%suffix_material%";
            case SUBTYPE -> isPrefix ? "%prefix_type%" : "%suffix_type%";
        };
    }
}
