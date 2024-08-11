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
import studio.magemonkey.divinity.api.DivinityAPI;
import studio.magemonkey.fabled.Fabled;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;

import java.util.*;

public class TabCacher {

    public static UUID GlobalUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
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

    public static List<String> getTabs(UUID uuid, String key, String arg) {
        List<String> entries = new ArrayList<>();
        if (isNotCached(uuid, key)) {
            switch (key) {
                case "items":
                    for (Material material : Material.values()) {
                        if (material.isAir()) continue;
                        entries.add(material.toString().toLowerCase());
                    }

                    if (Bukkit.getPluginManager().isPluginEnabled("Divinity")) {
                        DivinityAPI.getModuleManager().getCustomItemsManager().getItems().forEach((k) -> entries.add("DIVINITY_" + k.getId().toLowerCase()));
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
                    if (Bukkit.getPluginManager().isPluginEnabled("AuraSkills") || Bukkit.getPluginManager().isPluginEnabled("AureliumSkills")) {
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
                DivinityAPI.getModuleManager().getCustomItemsManager().getItems().forEach((k) -> entries.add("DIVINITY_" + k.getId().toLowerCase()));
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
}
