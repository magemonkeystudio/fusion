package studio.magemonkey.fusion.util;

import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.container.Job;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.ability.Ability;
import dev.aurelium.auraskills.api.mana.ManaAbility;
import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.api.stat.Stat;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.api.DivinityAPI;
import studio.magemonkey.divinity.modules.api.QModuleDrop;
import studio.magemonkey.fabled.Fabled;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.cfg.hooks.HookType;
import studio.magemonkey.fusion.Fusion;

import java.util.*;

public class TabCacher {

    public static       UUID                 GlobalUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    public static final Map<UUID, TabCacher> PlayerTabs = new TreeMap<>();

    public final Map<String, List<String>> CachedTabs = new TreeMap<>();

    public static void cache(UUID uuid, String key, List<String> tabList) {
        PlayerTabs.putIfAbsent(uuid, new TabCacher());
        PlayerTabs.get(uuid).CachedTabs.put(key, tabList);
    }

    public static boolean isNotCached(UUID uuid, String key) {
        return !PlayerTabs.containsKey(uuid) || !PlayerTabs.get(uuid).CachedTabs.containsKey(key);
    }

    public static TabCacher get(UUID uuid) {
        return PlayerTabs.get(uuid);
    }

    public static void clearTabs(UUID uuid) {
        PlayerTabs.remove(uuid);
    }

    public static void clearTabs(UUID uuid, String key) {
        PlayerTabs.get(uuid).CachedTabs.remove(key);
    }

    public static void clearAllCaches(String key) {
        PlayerTabs.values().forEach(cacher -> cacher.CachedTabs.remove(key));
    }

    public static List<String> getTabs(UUID uuid, String key, String arg) {
        List<String> entries = new ArrayList<>();
        if (isNotCached(uuid, key)) {
            switch (key) {
                case "station":
                    for (Material material : Material.values()) {
                        if (material.isAir()) continue;
                        entries.add(material.toString().toLowerCase());
                    }
                    if (Bukkit.getPluginManager().isPluginEnabled("Divinity")) {
                        DivinityAPI.getModuleManager()
                                .getCustomItemsManager()
                                .getItems()
                                .forEach((k) -> {
                                    entries.add("DIVINITY_" + k.getId().toLowerCase());
                                    entries.add("DIV_CUSTOM_" + k.getId().toLowerCase());
                                });
                    }
                    break;
                case "items":
                    for (Material material : Material.values()) {
                        if (material.isAir()) continue;
                        entries.add(material.toString().toLowerCase());
                    }

                    if (Bukkit.getPluginManager().isPluginEnabled("Divinity")) {
                        DivinityAPI.getModuleManager()
                                .getCustomItemsManager()
                                .getItems()
                                .forEach((k) -> entries.add("DIVINITY_" + k.getId().toLowerCase()));
                    }
                    if (Fusion.getInstance().getHookManager().isHooked(HookType.Divinity)) {
                        String random = QModuleDrop.RANDOM_ID;
                        var mc = Divinity.getInstance().getModuleCache();
                        for (String id : mc.getTierManager().getItemIds()) {
                            if (id.equals(random)) continue;
                            entries.add("DIV_ITEMGEN_" + id);
                            entries.add("DIV_ITEMGEN_" + id + "_NOENCH");
                        }
                        for (String id : mc.getGemManager().getItemIds()) {
                            if (!id.equals(random)) entries.add("DIV_GEM_" + id);
                        }
                        for (String id : mc.getEssenceManager().getItemIds()) {
                            if (!id.equals(random)) entries.add("DIV_ESSENCE_" + id);
                        }
                        for (String id : mc.getRuneManager().getItemIds()) {
                            if (!id.equals(random)) entries.add("DIV_RUNE_" + id);
                        }
                        if (mc.getArrowManager() != null)
                            mc.getArrowManager().getItemIds().stream()
                                    .filter(id -> !id.equals(random))
                                    .forEach(id -> entries.add("DIV_ARROW_" + id));
                        if (mc.getConsumablesManager() != null)
                            mc.getConsumablesManager().getItemIds().stream()
                                    .filter(id -> !id.equals(random))
                                    .forEach(id -> entries.add("DIV_CONSUMABLE_" + id));
                        mc.getCustomItemsManager().getItems()
                                .forEach(k -> entries.add("DIV_CUSTOM_" + k.getId().toLowerCase()));
                        if (mc.getFortifyManager() != null)
                            mc.getFortifyManager().getItemIds().stream()
                                    .filter(id -> !id.equals(random))
                                    .forEach(id -> entries.add("DIV_FORTIFY_" + id));
                        if (mc.getIdentifyManager() != null)
                            mc.getIdentifyManager().getItemIds().stream()
                                    .filter(id -> !id.equals(random))
                                    .forEach(id -> entries.add("DIV_IDENTIFY_" + id));
                        if (mc.getMagicDustManager() != null)
                            mc.getMagicDustManager().getItemIds().stream()
                                    .filter(id -> !id.equals(random))
                                    .forEach(id -> entries.add("DIV_DUST_" + id));
                        if (mc.getRepairManager() != null)
                            mc.getRepairManager().getItemIds().stream()
                                    .filter(id -> !id.equals(random))
                                    .forEach(id -> entries.add("DIV_REPAIR_" + id));
                        if (mc.getResolveManager() != null)
                            mc.getResolveManager().getItemIds().stream()
                                    .filter(id -> !id.equals(random))
                                    .forEach(id -> entries.add("DIV_DISMANTLE_" + id));
                        if (mc.getRefineManager() != null)
                            mc.getRefineManager().getItemIds().stream()
                                    .filter(id -> !id.equals(random))
                                    .forEach(id -> entries.add("DIV_REFINE_" + id));
                        if (mc.getExtractManager() != null)
                            mc.getExtractManager().getItemIds().stream()
                                    .filter(id -> !id.equals(random))
                                    .forEach(id -> entries.add("DIV_EXTRACTOR_" + id));
                    }
                    break;
                case "professions":
                    entries.addAll(ProfessionsCfg.getMap().keySet());
                    break;
                case "fabled":
                    if (Bukkit.getPluginManager().isPluginEnabled("Fabled")) {
                        entries.addAll(Fabled.getClasses().keySet());
                    }
                    break;
                case "mcmmo":
                    if (Bukkit.getPluginManager().isPluginEnabled("mcMMO")) {
                        for (PrimarySkillType skill : PrimarySkillType.values()) {
                            entries.add(skill.name().toLowerCase());
                        }
                    }
                    break;
                case "jobs":
                    if (Bukkit.getPluginManager().isPluginEnabled("Jobs")) {
                        for (Job job : Jobs.getJobs()) {
                            entries.add(job.getName().toLowerCase());
                        }
                    }
                    break;
                case "aura_abilities":
                case "aura_mana_abilities":
                case "aura_skills":
                case "aura_stats":
                    if (Bukkit.getPluginManager().isPluginEnabled("AuraSkills") || Bukkit.getPluginManager()
                            .isPluginEnabled("AureliumSkills")) {
                        switch (key) {
                            case "aura_abilities":
                                for (Ability ability : AuraSkillsApi.get().getGlobalRegistry().getAbilities())
                                    entries.add(ability.getId().getKey());
                                break;
                            case "aura_mana_abilities":
                                for (ManaAbility ability : AuraSkillsApi.get().getGlobalRegistry().getManaAbilities())
                                    entries.add(ability.getId().getKey());
                                break;
                            case "aura_skills":
                                for (Skill skill : AuraSkillsApi.get().getGlobalRegistry().getSkills())
                                    entries.add(skill.getId().getKey());
                                break;
                            case "aura_stats":
                                for (Stat stat : AuraSkillsApi.get().getGlobalRegistry().getStats())
                                    entries.add(stat.getId().getKey());
                                break;
                        }
                    }
                    break;
            }
            cache(uuid, key, entries);
        }

        List<String> cachedTabsCopy = new ArrayList<>(PlayerTabs.get(uuid).CachedTabs.get(key));
        for (String entry : cachedTabsCopy) {
            if (entry.toLowerCase().startsWith(arg.toLowerCase())) {
                entries.add(entry);
            }
        }
        return entries;
    }

    public static List<String> getProfessionTabs(UUID uuid, String key, String arg) {
        List<String> entries = new ArrayList<>();
        if (isNotCached(uuid, key)) {
            if (Bukkit.getPluginManager().isPluginEnabled("Divinity")) {
                DivinityAPI.getModuleManager()
                        .getCustomItemsManager()
                        .getItems()
                        .forEach((k) -> entries.add("DIVINITY_" + k.getId().toLowerCase()));
            }
            cache(uuid, key, entries);
        }

        List<String> cachedTabsCopy = new ArrayList<>(PlayerTabs.get(uuid).CachedTabs.get(key)); // Create a copy
        for (String entry : cachedTabsCopy) {
            if (entry.toLowerCase().startsWith(arg.toLowerCase())) {
                entries.add(entry);
            }
        }
        return entries;
    }

    public static List<String> getConditionsTabs(String[] args) {
        List<String> entries = new ArrayList<>();
        if (args.length == 1) {
            entries.add("<conditionKey>");
            entries.add("professions");
            if (Bukkit.getPluginManager().isPluginEnabled("Fabled"))
                entries.add("fabled");
            if (Bukkit.getPluginManager().isPluginEnabled("mcMMO"))
                entries.add("mcmmo");
            if (Bukkit.getPluginManager().isPluginEnabled("Jobs"))
                entries.add("jobs");
            if (Bukkit.getPluginManager().isPluginEnabled("AuraSkills") || Bukkit.getPluginManager()
                    .isPluginEnabled("AureliumSkills")) {
                entries.add("aura_abilities");
                entries.add("aura_mana_abilities");
                entries.add("aura_skills");
                entries.add("aura_stats");
            }
        } else if (args.length == 2) {
            entries.add("<conditionValue>");
            switch (args[0].toLowerCase()) {
                case "professions":
                    entries.addAll(TabCacher.getTabs(TabCacher.GlobalUUID, "professions", args[1]));
                    break;
                case "fabled":
                    entries.addAll(TabCacher.getTabs(TabCacher.GlobalUUID, "fabled", args[1]));
                    break;
                case "mcmmo":
                    entries.addAll(TabCacher.getTabs(TabCacher.GlobalUUID, "mcmmo", args[1]));
                    break;
                case "jobs":
                    entries.addAll(TabCacher.getTabs(TabCacher.GlobalUUID, "jobs", args[1]));
                    break;
                case "aura_abilities":
                    entries.addAll(TabCacher.getTabs(TabCacher.GlobalUUID, "aura_abilities", args[1]));
                    break;
                case "aura_mana_abilities":
                    entries.addAll(TabCacher.getTabs(TabCacher.GlobalUUID, "aura_mana_abilities", args[1]));
                    break;
                case "aura_skills":
                    entries.addAll(TabCacher.getTabs(TabCacher.GlobalUUID, "aura_skills", args[1]));
                    break;
                case "aura_stats":
                    entries.addAll(TabCacher.getTabs(TabCacher.GlobalUUID, "aura_stats", args[1]));
                    break;
            }
        } else if (args.length == 3) {
            entries.add("<level>");
            entries.add("1");
            entries.add("10");
            entries.add("25");
            entries.add("50");
            entries.add("100");
        }
        return entries;
    }

    public static List<String> getEnchantmentsTab(String[] args) {
        List<String> entries = new ArrayList<>();
        if (args.length == 1) {
            entries.add("<enchantment>");
            // TODO change from deprecation once existent
            for (Enchantment enchantment : Enchantment.values()) {
                entries.add(enchantment.getKey().getKey().toLowerCase());
            }
        } else if (args.length == 2) {
            entries.add("<level>");
            entries.add("1");
            entries.add("2");
            entries.add("3");
            entries.add("4");
            entries.add("5");
        }
        return entries;
    }

    public static List<String> getFlagsTab(String[] args) {
        List<String> entries = new ArrayList<>();
        if (args.length == 1) {
            entries.add("<flag>");
            for (ItemFlag flag : ItemFlag.values()) {
                entries.add(flag.name().toLowerCase());
            }
        }
        return entries;
    }

    /** Level hints for DIV_ item tab completion (single value or range). */
    public static List<String> getLevelHints(String partial) {
        List<String> hints = new ArrayList<>();
        hints.add("<level>");
        hints.add("1");
        hints.add("5");
        hints.add("10");
        hints.add("20");
        hints.add("1:5");
        hints.add("1:10");
        List<String> result = new ArrayList<>();
        for (String h : hints) {
            if (h.toLowerCase().startsWith(partial.toLowerCase())) result.add(h);
        }
        return result;
    }

    /** Material type suggestions for a given DIV_ITEMGEN item ID. */
    public static List<String> getItemGenMaterialTypes(String itemId, String partial) {
        List<String> entries = new ArrayList<>();
        if (!Fusion.getInstance().getHookManager().isHooked(HookType.Divinity)) return entries;
        var tierMgr = Divinity.getInstance().getModuleCache().getTierManager();
        if (tierMgr == null) return entries;
        String cleanId = itemId.toUpperCase().endsWith("_NOENCH")
                ? itemId.substring(0, itemId.length() - 7)
                : itemId;
        var genItem = tierMgr.getItemById(cleanId.toLowerCase());
        if (genItem == null) return entries;
        entries.add("<material>");
        for (studio.magemonkey.codex.api.items.ItemType mat : genItem.getMaterialsList()) {
            String id = mat.getID().toLowerCase();
            if (id.toLowerCase().startsWith(partial.toLowerCase())) entries.add(id);
        }
        return entries;
    }
}
