package studio.magemonkey.fusion.cfg.professions;

import com.gamingmesh.jobs.container.Job;
import com.gamingmesh.jobs.container.JobProgression;
import com.gmail.nossr50.api.ExperienceAPI;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.ability.Ability;
import dev.aurelium.auraskills.api.mana.ManaAbility;
import dev.aurelium.auraskills.api.registry.NamespacedId;
import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.api.stat.Stat;
import dev.aurelium.auraskills.api.user.SkillsUser;
import lombok.Getter;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import com.gamingmesh.jobs.Jobs;
import org.bukkit.entity.Player;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.codex.util.messages.MessageData;
import studio.magemonkey.codex.util.messages.MessageUtil;
import studio.magemonkey.fabled.Fabled;
import studio.magemonkey.fusion.ExperienceManager;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.BrowseConfig;
import studio.magemonkey.fusion.cfg.hooks.HookType;
import studio.magemonkey.fusion.cfg.player.FusionPlayer;
import studio.magemonkey.fusion.util.PlayerUtil;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

@Getter
public class ProfessionCondition {

    private final String profession;

    private final double moneyCost;
    private final int expCost;

    private final Map<String, Integer> professionConditions = new TreeMap<>();
    private final Map<String, Integer> fabledClassConditions = new TreeMap<>();
    private final Map<String, Integer> mcMMOConditions = new TreeMap<>();
    private final Map<String, Integer> jobsConditions = new TreeMap<>();
    private final Map<String, Integer> auraAbilityConditions = new TreeMap<>();
    private final Map<String, Integer> auraManaAbilityConditions = new TreeMap<>();
    private final Map<String, Integer> auraSkillsConditions = new TreeMap<>();
    private final Map<String, Integer> auraStatsConditions = new TreeMap<>();

    public ProfessionCondition(String profession, ConfigurationSection config) {
        this.profession = profession;
        this.moneyCost = config.getDouble("cost.money", 0.0);
        this.expCost = config.getInt("cost.experience", 0);

        if(config.isSet("conditions.professions")) {
            for (String key : config.getConfigurationSection("conditions.professions").getKeys(false)) {
                professionConditions.put(key, config.getInt("conditions.professions." + key));
            }
        }

        if(config.isSet("conditions.fabled")) {
            if (Fusion.getHookManager().isHooked(HookType.Fabled)) {
                for (String key : config.getConfigurationSection("conditions.fabled").getKeys(false)) {
                    fabledClassConditions.put(key, config.getInt("conditions.fabled." + key));
                }
            }
        }
        if(Fusion.getHookManager().isHooked(HookType.mcMMO)) {
            if(config.isSet("conditions.mcmmo")) {
                for (String key : config.getConfigurationSection("conditions.mcmmo").getKeys(false)) {
                    mcMMOConditions.put(key, config.getInt("conditions.mcmmo." + key));
                }
            }
        }
        if(Fusion.getHookManager().isHooked(HookType.Jobs)) {
            if(config.isSet("conditions.jobs")) {
                for (String key : config.getConfigurationSection("conditions.jobs").getKeys(false)) {
                    jobsConditions.put(key, config.getInt("conditions.jobs." + key));
                }
            }
        }
        if(Fusion.getHookManager().isHooked(HookType.AuraSkills)) {
            if(config.isSet("conditions.aura_abilities")) {
                for (String key : config.getConfigurationSection("conditions.aura_abilities").getKeys(false)) {
                    auraAbilityConditions.put(key, config.getInt("conditions.aura_abilities." + key));
                }
            }
            if(config.isSet("conditions.aura_mana_abilities")) {
                for (String key : config.getConfigurationSection("conditions.aura_mana_abilities").getKeys(false)) {
                    auraAbilityConditions.put(key, config.getInt("conditions.aura_mana_abilities." + key));
                }
            }
            if(config.isSet("conditions.aura_skills")) {
                for(String key : config.getConfigurationSection("conditions.aura_skills").getKeys(false)) {
                    auraSkillsConditions.put(key, config.getInt("conditions.aura_skills." + key));
                }
            }
            if(config.isSet("conditions.aura_stats")) {
                for(String key : config.getConfigurationSection("conditions.aura_stats").getKeys(false)) {
                    auraSkillsConditions.put(key, config.getInt("conditions.aura_stats." + key));
                }
            }
        }
    }

    public boolean isValid(FusionPlayer player) {
        Player _player = player.getPlayer();
        int unlocked = player.getUnlockedProfessions().size();
        int allowed = PlayerUtil.getPermOption(_player, "fusion.limit");

        MessageData[] data = {
                new MessageData("profession", profession),
                new MessageData("unlocked", unlocked),
                new MessageData("limit", allowed),
                new MessageData("cost.money", moneyCost),
                new MessageData("cost.experience", expCost),
                new MessageData("bal", CodexEngine.get().getVault().getBalance(_player)),
                new MessageData("condition.level", null),
                new MessageData("condition.name", null),
        };

        if (player.hasProfession(profession)) {
            _player.playSound(_player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
            MessageUtil.sendMessage("fusion.browse.alreadyUnlocked", _player, data);
            return false;
        }

        if (unlocked >= allowed) {
            _player.playSound(_player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
            MessageUtil.sendMessage("fusion.error.limitReached", _player, data);
            return false;
        }

        if (moneyCost > 0 && !CodexEngine.get().getVault().canPay(_player, moneyCost)) {
            _player.playSound(_player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
            MessageUtil.sendMessage("fusion.browse.noFundsMoney", _player, data);
            return false;
        }

        if (expCost > 0 && ExperienceManager.getTotalExperience(_player) < expCost) {
            _player.playSound(_player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
            MessageUtil.sendMessage("fusion.browse.noFundsExperience", _player, data);
            return false;
        }

        for(Map.Entry<String, Integer> entry : professionConditions.entrySet()) {
            if(!BrowseConfig.getProfessions().contains(entry.getKey())) {
                Fusion.getInstance().getLogger().warning("Invalid profession-condition in browse.yml: " + entry.getKey());
                continue;
            }
            if(player.getProfession(entry.getKey()).getLevel() < entry.getValue()) {
                _player.playSound(_player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
                //Append in data the profession and the level required
                data[data.length-2] = new MessageData("condition.level", entry.getValue());
                data[data.length-1] = new MessageData("condition.name", MessageUtil.getMessageAsString("fusion.browse.conditionFormatting.profession", entry.getKey(), new MessageData("profession", entry.getKey())));
                MessageUtil.sendMessage("fusion.browse.noConditionLevel", _player, data);
                return false;
            }
        }

        if(Fusion.getHookManager().isHooked(HookType.Fabled)) {
            for(Map.Entry<String, Integer> entry : fabledClassConditions.entrySet()) {
                if(!Fabled.getClasses().containsKey(entry.getKey())) {
                    Fusion.getInstance().getLogger().warning("Invalid fabled-class condition in browse.yml: " + entry.getKey());
                    continue;
                }
                int level = Fabled.getPlayerAccounts(_player).getActiveData().getClass(entry.getKey()).getLevel();
                if(level < entry.getValue()) {
                    _player.playSound(_player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
                    //Append in data the fabled class and the level required
                    data[data.length-2] = new MessageData("condition.level", entry.getValue());
                    data[data.length-1] = new MessageData("condition.name", MessageUtil.getMessageAsString("fusion.browse.conditionFormatting.fabled", entry.getKey(), new MessageData("class", Fabled.getClasses().get(entry.getKey()).getName())));
                    MessageUtil.sendMessage("fusion.browse.noConditionLevel", _player, data);
                    return false;
                }
            }
        }

        if(Fusion.getHookManager().isHooked(HookType.mcMMO)) {
            for(Map.Entry<String, Integer> entry : mcMMOConditions.entrySet()) {
                PrimarySkillType skill = PrimarySkillType.getSkill(entry.getKey().toUpperCase());
                if(skill == null) {
                    Fusion.getInstance().getLogger().warning("Invalid mcmmo-skill condition in browse.yml: " + entry.getKey());
                    continue;
                }

                int level = ExperienceAPI.getLevel(_player, skill);
                if(level < entry.getValue()) {
                    _player.playSound(_player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
                    //Append in data the mcMMO skill and the level required
                    data[data.length-2] = new MessageData("condition.level", entry.getValue());
                    data[data.length-1] = new MessageData("condition.name", MessageUtil.getMessageAsString("fusion.browse.conditionFormatting.mcmmo", entry.getKey(), new MessageData("skill", skill.getName())));
                    MessageUtil.sendMessage("fusion.browse.noConditionLevel", _player, data);
                    return false;
                }
            }
        }

        if(Fusion.getHookManager().isHooked(HookType.Jobs)) {
            for(Map.Entry<String, Integer> entry : jobsConditions.entrySet()) {
                Job job = Jobs.getJob(entry.getKey());
                if(job == null) {
                    Fusion.getInstance().getLogger().warning("Invalid job condition in browse.yml: " + entry.getKey());
                    continue;
                }
                JobProgression progression = Jobs.getPlayerManager().getJobsPlayer(_player).getJobProgression(job);
                int level = progression != null ? progression.getLevel() : 0;
                if(level < entry.getValue()) {
                    _player.playSound(_player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
                    //Append in data the job and the level required
                    data[data.length-2] = new MessageData("condition.level", entry.getValue());
                    data[data.length-1] = new MessageData("condition.name", MessageUtil.getMessageAsString("fusion.browse.conditionFormatting.jobs", entry.getKey(), new MessageData("job", job.getDisplayName())));
                    MessageUtil.sendMessage("fusion.browse.noConditionLevel", _player, data);
                    return false;
                }
            }
        }

        if(Fusion.getHookManager().isHooked(HookType.AuraSkills)) {
            SkillsUser user = AuraSkillsApi.get().getUser(_player.getUniqueId());
            for(Map.Entry<String, Integer> entry : auraAbilityConditions.entrySet()) {
                Ability ability = AuraSkillsApi.get().getGlobalRegistry().getAbility(NamespacedId.fromString(entry.getKey()));
                if(ability == null) {
                    Fusion.getInstance().getLogger().warning("Invalid aura-ability condition in browse.yml: " + entry.getKey());
                    continue;
                }

                int level = user.getAbilityLevel(ability);
                if(level < entry.getValue()) {
                    _player.playSound(_player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
                    //Append in data the aura ability and the level required
                    data[data.length-2] = new MessageData("condition.level", entry.getValue());
                    data[data.length-1] = new MessageData("condition.name", MessageUtil.getMessageAsString("fusion.browse.conditionFormatting.auraAbility", entry.getKey(), new MessageData("ability", ability.getDisplayName(Locale.ENGLISH))));
                    MessageUtil.sendMessage("fusion.browse.noConditionLevel", _player, data);
                    return false;
                }
            }

            for(Map.Entry<String, Integer> entry : auraManaAbilityConditions.entrySet()) {
                ManaAbility ability = AuraSkillsApi.get().getGlobalRegistry().getManaAbility(NamespacedId.fromString(entry.getKey()));
                if(ability == null) {
                    Fusion.getInstance().getLogger().warning("Invalid aura-mana-ability condition in browse.yml: " + entry.getKey());
                    continue;
                }

                int level = user.getManaAbilityLevel(ability);
                if(level < entry.getValue()) {
                    _player.playSound(_player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
                    //Append in data the aura ability and the level required
                    data[data.length-2] = new MessageData("condition.level", entry.getValue());
                    data[data.length-1] = new MessageData("condition.name", MessageUtil.getMessageAsString("fusion.browse.conditionFormatting.auraManaAbility", entry.getKey(), new MessageData("ability", ability.getDisplayName(Locale.ENGLISH))));
                    MessageUtil.sendMessage("fusion.browse.noConditionLevel", _player, data);
                    return false;
                }
            }

            for(Map.Entry<String, Integer> entry : auraSkillsConditions.entrySet()) {
                Skill skill = AuraSkillsApi.get().getGlobalRegistry().getSkill(NamespacedId.fromString(entry.getKey()));
                if(skill == null) {
                    Fusion.getInstance().getLogger().warning("Invalid aura-skill condition in browse.yml: " + entry.getKey());
                    continue;
                }
                int level = user.getSkillLevel(skill);
                if(level < entry.getValue()) {
                    _player.playSound(_player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
                    //Append in data the aura skill and the level required
                    data[data.length-2] = new MessageData("condition.level", entry.getValue());
                    data[data.length-1] = new MessageData("condition.name", MessageUtil.getMessageAsString("fusion.browse.conditionFormatting.auraSkill", entry.getKey(), new MessageData("skill", skill.getDisplayName(Locale.ENGLISH))));
                    MessageUtil.sendMessage("fusion.browse.noConditionLevel", _player, data);
                    return false;
                }
            }

            for(Map.Entry<String, Integer> entry : auraStatsConditions.entrySet()) {
                Stat stat = AuraSkillsApi.get().getGlobalRegistry().getStat(NamespacedId.fromString(entry.getKey()));
                if(stat == null) {
                    Fusion.getInstance().getLogger().warning("Invalid aura-stat condition in browse.yml: " + entry.getKey());
                    continue;
                }
                double level = user.getStatLevel(stat);
                if(level < entry.getValue()) {
                    _player.playSound(_player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
                    //Append in data the aura skill and the level required
                    data[data.length-2] = new MessageData("condition.level", entry.getValue());
                    data[data.length-1] = new MessageData("condition.name", MessageUtil.getMessageAsString("fusion.browse.conditionFormatting.auraSkill", entry.getKey(), new MessageData("skill", stat.name())));
                    MessageUtil.sendMessage("fusion.browse.noConditionLevel", _player, data);
                    return false;
                }
            }
        }
        return true;
    }

    public Map<String, Map<String, Integer>> getFullConditions() {
        Map<String, Map<String, Integer>> conditions = new TreeMap<>();
        conditions.put("profession", professionConditions);
        conditions.put("fabled", fabledClassConditions);
        conditions.put("mcmmo", mcMMOConditions);
        conditions.put("jobs", jobsConditions);
        conditions.put("auraAbility", auraAbilityConditions);
        conditions.put("auraManaAbility", auraManaAbilityConditions);
        conditions.put("auraSkill", auraSkillsConditions);
        conditions.put("auraStats", auraStatsConditions);
        return conditions;
    }
}
