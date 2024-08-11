package studio.magemonkey.fusion.cfg.professions;

import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.container.Job;
import com.gamingmesh.jobs.container.JobProgression;
import com.gmail.nossr50.api.ExperienceAPI;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.mcMMO;
import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.ability.Ability;
import dev.aurelium.auraskills.api.mana.ManaAbility;
import dev.aurelium.auraskills.api.registry.NamespacedId;
import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.api.stat.Stat;
import dev.aurelium.auraskills.api.user.SkillsUser;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.codex.legacy.item.ItemBuilder;
import studio.magemonkey.codex.util.SerializationBuilder;
import studio.magemonkey.codex.util.messages.MessageData;
import studio.magemonkey.codex.util.messages.MessageUtil;
import studio.magemonkey.fabled.Fabled;
import studio.magemonkey.fusion.*;
import studio.magemonkey.fusion.cfg.BrowseConfig;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.cfg.hooks.HookType;
import studio.magemonkey.fusion.cfg.player.FusionPlayer;
import studio.magemonkey.fusion.cfg.player.PlayerLoader;
import studio.magemonkey.fusion.util.ChatUT;
import studio.magemonkey.fusion.util.PlayerUtil;
import studio.magemonkey.risecore.legacy.util.DeserializationWorker;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
@Getter
public class ProfessionConditions implements ConfigurationSerializable  {

    private final String profession;

    // Costs
    @Setter
    private double moneyCost;
    @Setter
    private int expCost;
    protected LinkedList<RecipeItem> requiredItems = new LinkedList<>();

    // Conditions
    @Setter
    private int professionLevel;
    @Setter
    private boolean isMastery;

    private final Map<String, Integer> professionConditions = new LinkedHashMap<>();
    private final Map<String, Integer> fabledClassConditions = new LinkedHashMap<>();
    private final Map<String, Integer> mcMMOConditions = new LinkedHashMap<>();
    private final Map<String, Integer> jobsConditions = new LinkedHashMap<>();
    private final Map<String, Integer> auraAbilityConditions = new LinkedHashMap<>();
    private final Map<String, Integer> auraManaAbilityConditions = new LinkedHashMap<>();
    private final Map<String, Integer> auraSkillsConditions = new LinkedHashMap<>();
    private final Map<String, Integer> auraStatsConditions = new LinkedHashMap<>();

    @Getter
    protected List<String> requiredItemNames;

    public ProfessionConditions(String profession,
                                double moneyCost,
                                int expCost,
                                LinkedList<String> requiredItemNames,
                                int professionLevel,
                                boolean isMastery,
                                Map<String, Integer> professionConditions,
                                Map<String, Integer> fabledClassConditions,
                                Map<String, Integer> mcMMOConditions,
                                Map<String, Integer> jobsConditions,
                                Map<String, Integer> auraAbilityConditions,
                                Map<String, Integer> auraManaAbilityConditions,
                                Map<String, Integer> auraSkillsConditions,
                                Map<String, Integer> auraStatsConditions) {
        this.profession = profession;
        this.moneyCost = moneyCost;
        this.expCost = expCost;
        this.requiredItemNames = requiredItemNames;
        this.requiredItems = requiredItems.stream().map(RecipeItem::fromConfig).collect(Collectors.toCollection(LinkedList::new));
        this.professionLevel = professionLevel;
        this.isMastery = isMastery;
        this.professionConditions.putAll(professionConditions);
        this.fabledClassConditions.putAll(fabledClassConditions);
        this.mcMMOConditions.putAll(mcMMOConditions);
        this.jobsConditions.putAll(jobsConditions);
        this.auraAbilityConditions.putAll(auraAbilityConditions);
        this.auraManaAbilityConditions.putAll(auraManaAbilityConditions);
        this.auraSkillsConditions.putAll(auraSkillsConditions);
        this.auraStatsConditions.putAll(auraStatsConditions);
    }

    public ProfessionConditions(String profession, ConfigurationSection config) {
        this.profession = profession;
        this.moneyCost = config.getDouble("costs.money", 0.0);
        this.expCost = config.getInt("costs.exp", 0);
        this.requiredItems = config.getStringList("costs.items")
                .stream()
                .map(RecipeItem::fromConfig)
                .collect(Collectors.toCollection(LinkedList::new));
        this.requiredItemNames = config.getStringList("costs.items");

        this.professionLevel = config.getInt("conditions.professionLevel", 0);
        this.isMastery = config.getBoolean("conditions.mastery", false);

        if (config.isSet("conditions.professions")) {
            for (String key : config.getConfigurationSection("conditions.professions").getKeys(false)) {
                professionConditions.put(key, config.getInt("conditions.professions." + key));
            }
        }

        if (config.isSet("conditions.fabled")) {
            if (Fusion.getHookManager().isHooked(HookType.Fabled)) {
                for (String key : config.getConfigurationSection("conditions.fabled").getKeys(false)) {
                    fabledClassConditions.put(key, config.getInt("conditions.fabled." + key));
                }
            }
        }
        if (Fusion.getHookManager().isHooked(HookType.mcMMO)) {
            if (config.isSet("conditions.mcmmo")) {
                for (String key : config.getConfigurationSection("conditions.mcmmo").getKeys(false)) {
                    mcMMOConditions.put(key, config.getInt("conditions.mcmmo." + key));
                }
            }
        }
        if (Fusion.getHookManager().isHooked(HookType.Jobs)) {
            if (config.isSet("conditions.jobs")) {
                for (String key : config.getConfigurationSection("conditions.jobs").getKeys(false)) {
                    jobsConditions.put(key, config.getInt("conditions.jobs." + key));
                }
            }
        }
        if (Fusion.getHookManager().isHooked(HookType.AuraSkills)) {
            if (config.isSet("conditions.aura_abilities")) {
                for (String key : config.getConfigurationSection("conditions.aura_abilities").getKeys(false)) {
                    auraAbilityConditions.put(key, config.getInt("conditions.aura_abilities." + key));
                }
            }
            if (config.isSet("conditions.aura_mana_abilities")) {
                for (String key : config.getConfigurationSection("conditions.aura_mana_abilities").getKeys(false)) {
                    auraAbilityConditions.put(key, config.getInt("conditions.aura_mana_abilities." + key));
                }
            }
            if (config.isSet("conditions.aura_skills")) {
                for (String key : config.getConfigurationSection("conditions.aura_skills").getKeys(false)) {
                    auraSkillsConditions.put(key, config.getInt("conditions.aura_skills." + key));
                }
            }
            if (config.isSet("conditions.aura_stats")) {
                for (String key : config.getConfigurationSection("conditions.aura_stats").getKeys(false)) {
                    auraSkillsConditions.put(key, config.getInt("conditions.aura_stats." + key));
                }
            }
        }
    }

    public ProfessionConditions(String profession, DeserializationWorker dw) {
        this.profession = profession;
        this.professionLevel = 0;
        this.moneyCost = 0;
        this.expCost = 0;

        Map<String, Object> costsSection = dw.getSection("costs");
        if (costsSection != null) {
            this.moneyCost = (double) costsSection.getOrDefault("money", 0.0);
            this.expCost = (int) costsSection.getOrDefault("exp", 0);
            this.requiredItems = ((List<String>) costsSection.get("items"))
                    .stream()
                    .map(RecipeItem::fromConfig)
                    .collect(Collectors.toCollection(LinkedList::new));
            this.requiredItemNames = (List<String>) costsSection.get("items");
        }

        Map<String, Object> conditionsSection = dw.getSection("conditions");


        if (conditionsSection != null) {
            this.professionLevel = (int) conditionsSection.getOrDefault("professionLevel", 0);
            this.isMastery = (boolean) conditionsSection.getOrDefault("mastery", false);
            Map<String, Object> conditions = (Map<String, Object>) conditionsSection.get("professions");
            if (conditions != null) {
                for (Map.Entry<String, Object> entry : conditions.entrySet()) {
                    professionConditions.put(entry.getKey(), (int) entry.getValue());
                }
            }

            conditions = (Map<String, Object>) conditionsSection.get("fabled");
            if (Fusion.getHookManager().isHooked(HookType.Fabled) && conditions != null) {
                for (Map.Entry<String, Object> entry : conditions.entrySet()) {
                    fabledClassConditions.put(entry.getKey(), (int) entry.getValue());
                }
            }

            conditions = (Map<String, Object>) conditionsSection.get("mcmmo");
            if (Fusion.getHookManager().isHooked(HookType.mcMMO) && conditions != null) {
                Bukkit.getConsoleSender().sendMessage("Found mcMMO conditions: " + conditions);
                for (Map.Entry<String, Object> entry : conditions.entrySet()) {
                    mcMMOConditions.put(entry.getKey(), (int) entry.getValue());
                }
            }

            conditions = (Map<String, Object>) conditionsSection.get("jobs");
            if (Fusion.getHookManager().isHooked(HookType.Jobs) && conditions != null) {
                for (Map.Entry<String, Object> entry : conditions.entrySet()) {
                    jobsConditions.put(entry.getKey(), (int) entry.getValue());
                }
            }

            if (Fusion.getHookManager().isHooked(HookType.AuraSkills)) {
                conditions = (Map<String, Object>) conditionsSection.get("aura_abilities");
                if (conditions != null) {
                    for (Map.Entry<String, Object> entry : conditions.entrySet()) {
                        auraAbilityConditions.put(entry.getKey(), (int) entry.getValue());
                    }
                }
                conditions = (Map<String, Object>) conditionsSection.get("aura_mana_abilities");
                if (conditions != null) {
                    for (Map.Entry<String, Object> entry : conditions.entrySet()) {
                        auraManaAbilityConditions.put(entry.getKey(), (int) entry.getValue());
                    }
                }
                conditions = (Map<String, Object>) conditionsSection.get("aura_skills");
                if (conditions != null) {
                    for (Map.Entry<String, Object> entry : conditions.entrySet()) {
                        auraSkillsConditions.put(entry.getKey(), (int) entry.getValue());
                    }
                }
                conditions = (Map<String, Object>) conditionsSection.get("aura_stats");
                if (conditions != null) {
                    for (Map.Entry<String, Object> entry : conditions.entrySet()) {
                        auraStatsConditions.put(entry.getKey(), (int) entry.getValue());
                    }
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
                new MessageData("bal", CodexEngine.get().getVault().getBalance(_player)),

                new MessageData("costs.money", moneyCost),
                new MessageData("costs.exp", expCost),

                new MessageData("conditions.professionLevel", professionLevel),
                new MessageData("conditions.mastery", isMastery),
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

        if (professionLevel > 0 && player.getProfession(profession).getLevel() < professionLevel) {
            _player.playSound(_player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
            MessageUtil.sendMessage("fusion.error.noLevel", _player, data);
            return false;
        }

        if (isMastery && !player.getProfession(profession).isMastered()) {
            _player.playSound(_player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
            MessageUtil.sendMessage("fusion.error.noMastery", _player, data);
            return false;
        }

        for (Map.Entry<String, Integer> entry : professionConditions.entrySet()) {
            if (!BrowseConfig.getProfessions().contains(entry.getKey())) {
                Fusion.getInstance().getLogger().warning("Invalid profession-condition in browse.yml: " + entry.getKey());
                continue;
            }
            if (player.getProfession(entry.getKey()).getLevel() < entry.getValue()) {
                _player.playSound(_player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
                //Append in data the profession and the level required
                data[data.length - 2] = new MessageData("condition.level", entry.getValue());
                data[data.length - 1] = new MessageData("condition.name", MessageUtil.getMessageAsString("fusion.conditionFormatting.profession", entry.getKey(), new MessageData("profession", entry.getKey())));
                MessageUtil.sendMessage("fusion.browse.noConditionLevel", _player, data);
                return false;
            }
        }

        if (Fusion.getHookManager().isHooked(HookType.Fabled)) {
            for (Map.Entry<String, Integer> entry : fabledClassConditions.entrySet()) {
                if (!Fabled.getClasses().containsKey(entry.getKey())) {
                    Fusion.getInstance().getLogger().warning("Invalid fabled-class condition in browse.yml: " + entry.getKey());
                    continue;
                }
                int level = Fabled.getPlayerAccounts(_player).getActiveData().getClass(entry.getKey()).getLevel();
                if (level < entry.getValue()) {
                    _player.playSound(_player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
                    //Append in data the fabled class and the level required
                    data[data.length - 2] = new MessageData("condition.level", entry.getValue());
                    data[data.length - 1] = new MessageData("condition.name", MessageUtil.getMessageAsString("fusion.conditionFormatting.fabled", entry.getKey(), new MessageData("class", Fabled.getClasses().get(entry.getKey()).getName())));
                    MessageUtil.sendMessage("fusion.browse.noConditionLevel", _player, data);
                    return false;
                }
            }
        }

        if (Fusion.getHookManager().isHooked(HookType.mcMMO)) {
            for (Map.Entry<String, Integer> entry : mcMMOConditions.entrySet()) {
                PrimarySkillType skill = PrimarySkillType.getSkill(entry.getKey().toUpperCase());
                if (skill == null) {
                    Fusion.getInstance().getLogger().warning("Invalid mcmmo-skill condition in browse.yml: " + entry.getKey());
                    continue;
                }

                int level = ExperienceAPI.getLevel(_player, skill);
                if (level < entry.getValue()) {
                    _player.playSound(_player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
                    //Append in data the mcMMO skill and the level required
                    data[data.length - 2] = new MessageData("condition.level", entry.getValue());
                    data[data.length - 1] = new MessageData("condition.name", MessageUtil.getMessageAsString("fusion.conditionFormatting.mcmmo", entry.getKey(), new MessageData("skill", skill.getName())));
                    MessageUtil.sendMessage("fusion.browse.noConditionLevel", _player, data);
                    return false;
                }
            }
        }

        if (Fusion.getHookManager().isHooked(HookType.Jobs)) {
            for (Map.Entry<String, Integer> entry : jobsConditions.entrySet()) {
                Job job = Jobs.getJob(entry.getKey());
                if (job == null) {
                    Fusion.getInstance().getLogger().warning("Invalid job condition in browse.yml: " + entry.getKey());
                    continue;
                }
                JobProgression progression = Jobs.getPlayerManager().getJobsPlayer(_player).getJobProgression(job);
                int level = progression != null ? progression.getLevel() : 0;
                if (level < entry.getValue()) {
                    _player.playSound(_player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
                    //Append in data the job and the level required
                    data[data.length - 2] = new MessageData("condition.level", entry.getValue());
                    data[data.length - 1] = new MessageData("condition.name", MessageUtil.getMessageAsString("fusion.conditionFormatting.jobs", entry.getKey(), new MessageData("job", job.getDisplayName())));
                    MessageUtil.sendMessage("fusion.browse.noConditionLevel", _player, data);
                    return false;
                }
            }
        }

        if (Fusion.getHookManager().isHooked(HookType.AuraSkills)) {
            SkillsUser user = AuraSkillsApi.get().getUser(_player.getUniqueId());
            for (Map.Entry<String, Integer> entry : auraAbilityConditions.entrySet()) {
                Ability ability = AuraSkillsApi.get().getGlobalRegistry().getAbility(NamespacedId.fromString(entry.getKey()));
                if (ability == null) {
                    Fusion.getInstance().getLogger().warning("Invalid aura-ability condition in browse.yml: " + entry.getKey());
                    continue;
                }

                int level = user.getAbilityLevel(ability);
                if (level < entry.getValue()) {
                    _player.playSound(_player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
                    //Append in data the aura ability and the level required
                    data[data.length - 2] = new MessageData("condition.level", entry.getValue());
                    data[data.length - 1] = new MessageData("condition.name", MessageUtil.getMessageAsString("fusion.conditionFormatting.auraAbility", entry.getKey(), new MessageData("ability", ability.getDisplayName(Locale.ENGLISH))));
                    MessageUtil.sendMessage("fusion.browse.noConditionLevel", _player, data);
                    return false;
                }
            }

            for (Map.Entry<String, Integer> entry : auraManaAbilityConditions.entrySet()) {
                ManaAbility ability = AuraSkillsApi.get().getGlobalRegistry().getManaAbility(NamespacedId.fromString(entry.getKey()));
                if (ability == null) {
                    Fusion.getInstance().getLogger().warning("Invalid aura-mana-ability condition in browse.yml: " + entry.getKey());
                    continue;
                }

                int level = user.getManaAbilityLevel(ability);
                if (level < entry.getValue()) {
                    _player.playSound(_player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
                    //Append in data the aura ability and the level required
                    data[data.length - 2] = new MessageData("condition.level", entry.getValue());
                    data[data.length - 1] = new MessageData("condition.name", MessageUtil.getMessageAsString("fusion.conditionFormatting.auraManaAbility", entry.getKey(), new MessageData("ability", ability.getDisplayName(Locale.ENGLISH))));
                    MessageUtil.sendMessage("fusion.browse.noConditionLevel", _player, data);
                    return false;
                }
            }

            for (Map.Entry<String, Integer> entry : auraSkillsConditions.entrySet()) {
                Skill skill = AuraSkillsApi.get().getGlobalRegistry().getSkill(NamespacedId.fromString(entry.getKey()));
                if (skill == null) {
                    Fusion.getInstance().getLogger().warning("Invalid aura-skill condition in browse.yml: " + entry.getKey());
                    continue;
                }
                int level = user.getSkillLevel(skill);
                if (level < entry.getValue()) {
                    _player.playSound(_player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
                    //Append in data the aura skill and the level required
                    data[data.length - 2] = new MessageData("condition.level", entry.getValue());
                    data[data.length - 1] = new MessageData("condition.name", MessageUtil.getMessageAsString("fusion.conditionFormatting.auraSkill", entry.getKey(), new MessageData("skill", skill.getDisplayName(Locale.ENGLISH))));
                    MessageUtil.sendMessage("fusion.browse.noConditionLevel", _player, data);
                    return false;
                }
            }

            for (Map.Entry<String, Integer> entry : auraStatsConditions.entrySet()) {
                Stat stat = AuraSkillsApi.get().getGlobalRegistry().getStat(NamespacedId.fromString(entry.getKey()));
                if (stat == null) {
                    Fusion.getInstance().getLogger().warning("Invalid aura-stat condition in browse.yml: " + entry.getKey());
                    continue;
                }
                double level = user.getStatLevel(stat);
                if (level < entry.getValue()) {
                    _player.playSound(_player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
                    //Append in data the aura skill and the level required
                    data[data.length - 2] = new MessageData("condition.level", entry.getValue());
                    data[data.length - 1] = new MessageData("condition.name", MessageUtil.getMessageAsString("fusion.conditionFormatting.auraSkill", entry.getKey(), new MessageData("skill", stat.name())));
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

    public List<Map.Entry<Boolean, String>> getConditionLines(Player player, CraftingTable craftingTable) {
        FusionPlayer fusionPlayer = PlayerLoader.getPlayer(player.getUniqueId());
        List<Map.Entry<Boolean, String>> lines = new ArrayList<>();
        String trueConditionLine = MessageUtil.getMessageAsString("fusion.gui.condition", "&6- &e$<condition.name>&8: &7(&a$<level>&8/&7$<condition.level>&7)");
        String falseConditionLine = MessageUtil.getMessageAsString("fusion.gui.condition", "&6- &e$<condition.name>&8: &7(&c$<level>&8/&7$<condition.level>&7)");

        for (Map.Entry<String, Integer> entry : professionConditions.entrySet()) {
            Profession profession = fusionPlayer.getProfession(entry.getKey());
            boolean condition = profession != null && profession.getLevel() >= entry.getValue();
            String finalLineFormat = ChatUT.hexString(condition ? trueConditionLine : falseConditionLine);
            lines.add(Map.entry(condition, finalLineFormat
                    .replace(MessageUtil.getReplacement("condition.name"),
                            MessageUtil.getMessageAsString("fusion.conditionFormatting.profession", null, new MessageData("profession", ProfessionsCfg.getTable(entry.getKey()).getName())))
                    .replace(MessageUtil.getReplacement("condition.level"), String.valueOf(entry.getValue()))
                    .replace(MessageUtil.getReplacement("level"), String.valueOf(profession != null ? fusionPlayer.getProfession(entry.getKey()).getLevel() : 0))
            ));
        }
        if (Fusion.getHookManager().isHooked(HookType.Fabled)) {
            for (Map.Entry<String, Integer> entry : fabledClassConditions.entrySet()) {
                boolean condition = Fabled.getPlayerAccounts(player).getActiveData().getClass(entry.getKey()).getLevel() >= entry.getValue();
                String finalLineFormat = ChatUT.hexString(condition ? trueConditionLine : falseConditionLine);
                lines.add(Map.entry(condition, finalLineFormat
                        .replace(MessageUtil.getReplacement("condition.name"),
                                MessageUtil.getMessageAsString("fusion.conditionFormatting.fabled", null, new MessageData("class", Fabled.getClasses().get(entry.getKey()).getName())))
                        .replace(MessageUtil.getReplacement("condition.level"), String.valueOf(entry.getValue()))
                        .replace(MessageUtil.getReplacement("level"), String.valueOf(Fabled.getPlayerAccounts(player).getActiveData().getClass(entry.getKey()).getLevel()))
                ));
            }
        }
        if (Fusion.getHookManager().isHooked(HookType.mcMMO)) {
            for (Map.Entry<String, Integer> entry : mcMMOConditions.entrySet()) {
                PrimarySkillType skill = PrimarySkillType.getSkill(entry.getKey().toUpperCase());
                boolean condition = ExperienceAPI.getLevel(player, skill) >= entry.getValue();
                String finalLineFormat = ChatUT.hexString(condition ? trueConditionLine : falseConditionLine);
                lines.add(Map.entry(condition, finalLineFormat
                        .replace(MessageUtil.getReplacement("condition.name"),
                                MessageUtil.getMessageAsString("fusion.conditionFormatting.mcmmo", null, new MessageData("skill", mcMMO.p.getSkillTools().getLocalizedSkillName(skill))))
                        .replace(MessageUtil.getReplacement("condition.level"), String.valueOf(entry.getValue()))
                        .replace(MessageUtil.getReplacement("level"), String.valueOf(ExperienceAPI.getLevel(player, skill)))
                ));
            }
        }
        if (Fusion.getHookManager().isHooked(HookType.Jobs)) {
            for (Map.Entry<String, Integer> entry : jobsConditions.entrySet()) {
                Job job = Jobs.getJob(entry.getKey());
                JobProgression progression = Jobs.getPlayerManager().getJobsPlayer(player).getJobProgression(job);
                boolean condition = progression != null && Jobs.getPlayerManager().getJobsPlayer(player).getJobProgression(job).getLevel() >= entry.getValue();
                String finalLineFormat = ChatUT.hexString(condition ? trueConditionLine : falseConditionLine);
                lines.add(Map.entry(condition, finalLineFormat
                        .replace(MessageUtil.getReplacement("condition.name"),
                                MessageUtil.getMessageAsString("fusion.conditionFormatting.jobs", null, new MessageData("job", job.getJobFullName())))
                        .replace(MessageUtil.getReplacement("condition.level"), String.valueOf(entry.getValue()))
                        .replace(MessageUtil.getReplacement("level"), String.valueOf(progression != null ? Jobs.getPlayerManager().getJobsPlayer(player).getJobProgression(job).getLevel() : 0))
                ));
            }
        }
        if (Fusion.getHookManager().isHooked(HookType.AuraSkills)) {
            for (Map.Entry<String, Integer> entry : auraAbilityConditions.entrySet()) {
                Ability ability = AuraSkillsApi.get().getGlobalRegistry().getAbility(NamespacedId.fromString(entry.getKey()));
                boolean condition = AuraSkillsApi.get().getUser(player.getUniqueId()).getAbilityLevel(ability) >= entry.getValue();
                String finalLineFormat = ChatUT.hexString(condition ? trueConditionLine : falseConditionLine);
                lines.add(Map.entry(condition, finalLineFormat
                        .replace(MessageUtil.getReplacement("condition.name"),
                                MessageUtil.getMessageAsString("fusion.conditionFormatting.auraAbility", null, new MessageData("ability", ability.getDisplayName(Locale.ENGLISH)))
                        )
                        .replace(MessageUtil.getReplacement("condition.level"), String.valueOf(entry.getValue()))
                        .replace(MessageUtil.getReplacement("level"), String.valueOf(AuraSkillsApi.get().getUser(player.getUniqueId()).getAbilityLevel(ability)))
                ));
            }
            for (Map.Entry<String, Integer> entry : auraManaAbilityConditions.entrySet()) {
                ManaAbility ability = AuraSkillsApi.get().getGlobalRegistry().getManaAbility(NamespacedId.fromString(entry.getKey()));
                boolean condition = AuraSkillsApi.get().getUser(player.getUniqueId()).getManaAbilityLevel(ability) >= entry.getValue();
                String finalLineFormat = ChatUT.hexString(condition ? trueConditionLine : falseConditionLine);
                lines.add(Map.entry(condition, finalLineFormat
                        .replace(MessageUtil.getReplacement("condition.name"),
                                MessageUtil.getMessageAsString("fusion.conditionFormatting.auraManaAbility", null, new MessageData("ability", ability.getDisplayName(Locale.ENGLISH)))
                        )
                        .replace(MessageUtil.getReplacement("condition.level"), String.valueOf(entry.getValue()))
                        .replace(MessageUtil.getReplacement("level"), String.valueOf(AuraSkillsApi.get().getUser(player.getUniqueId()).getManaAbilityLevel(ability)))
                ));
            }
            for (Map.Entry<String, Integer> entry : auraSkillsConditions.entrySet()) {
                Skill skill = AuraSkillsApi.get().getGlobalRegistry().getSkill(NamespacedId.fromString(entry.getKey()));
                boolean condition = AuraSkillsApi.get().getUser(player.getUniqueId()).getSkillLevel(skill) >= entry.getValue();
                String finalLineFormat = ChatUT.hexString(condition ? trueConditionLine : falseConditionLine);
                lines.add(Map.entry(condition, finalLineFormat
                        .replace(MessageUtil.getReplacement("condition.name"),
                                MessageUtil.getMessageAsString("fusion.conditionFormatting.auraSkill", null, new MessageData("skill", skill.getDisplayName(Locale.ENGLISH)))
                        )
                        .replace(MessageUtil.getReplacement("condition.level"), String.valueOf(entry.getValue()))
                        .replace(MessageUtil.getReplacement("level"), String.valueOf(AuraSkillsApi.get().getUser(player.getUniqueId()).getSkillLevel(skill)))
                ));
            }
            for (Map.Entry<String, Integer> entry : auraStatsConditions.entrySet()) {
                Stat stat = AuraSkillsApi.get().getGlobalRegistry().getStat(NamespacedId.fromString(entry.getKey()));
                boolean condition = AuraSkillsApi.get().getUser(player.getUniqueId()).getStatLevel(stat) >= entry.getValue();
                String finalLineFormat = ChatUT.hexString(condition ? trueConditionLine : falseConditionLine);
                lines.add(Map.entry(condition, finalLineFormat
                        .replace(MessageUtil.getReplacement("condition.name"),
                                MessageUtil.getMessageAsString("fusion.conditionFormatting.auraStat", null, new MessageData("stat", stat.name()))
                        )
                        .replace(MessageUtil.getReplacement("condition.level"), String.valueOf(entry.getValue()))
                        .replace(MessageUtil.getReplacement("level"), String.valueOf(AuraSkillsApi.get().getUser(player.getUniqueId()).getStatLevel(stat)))
                ));
            }
        }
        return lines;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> costsMap = new LinkedHashMap<>();
        costsMap.put("money", this.moneyCost);
        costsMap.put("exp", this.expCost);
        costsMap.put("items", this.requiredItemNames);

        Map<String, Object> conditionsMap = new LinkedHashMap<>();
        conditionsMap.put("professionLevel", this.professionLevel);
        conditionsMap.put("mastery", this.isMastery);
        if(!professionConditions.isEmpty())
            conditionsMap.put("professions", this.professionConditions);
        if(!fabledClassConditions.isEmpty())
            conditionsMap.put("fabled", this.fabledClassConditions);
        if(!mcMMOConditions.isEmpty())
            conditionsMap.put("mcmmo", this.mcMMOConditions);
        if(!jobsConditions.isEmpty())
            conditionsMap.put("jobs", this.jobsConditions);
        if(!auraAbilityConditions.isEmpty())
            conditionsMap.put("aura_abilities", this.auraAbilityConditions);
        if(!auraManaAbilityConditions.isEmpty())
            conditionsMap.put("aura_mana_abilities", this.auraManaAbilityConditions);
        if(!auraSkillsConditions.isEmpty())
            conditionsMap.put("aura_skills", this.auraSkillsConditions);
        if(!auraStatsConditions.isEmpty())
            conditionsMap.put("aura_stats", this.auraStatsConditions);

        SerializationBuilder builder = SerializationBuilder.start(4)
                .append("costs", costsMap)
                .append("conditions", conditionsMap);
        return builder.build();
    }

    public void removeLastCondition() {
        // Remove the last index of the condition
        Map.Entry<String, Integer> lastEntry = null;

        if(checkRemoveFromMap(lastEntry, auraStatsConditions)) return;
        if(checkRemoveFromMap(lastEntry, auraSkillsConditions)) return;
        if(checkRemoveFromMap(lastEntry, auraManaAbilityConditions)) return;
        if(checkRemoveFromMap(lastEntry, auraAbilityConditions)) return;
        if(checkRemoveFromMap(lastEntry, jobsConditions)) return;
        if(checkRemoveFromMap(lastEntry, mcMMOConditions)) return;
        if(checkRemoveFromMap(lastEntry, fabledClassConditions)) return;
        checkRemoveFromMap(lastEntry, professionConditions);
    }

    private boolean checkRemoveFromMap(Map.Entry<String, Integer> entry, Map<String, Integer> map) {
        for (Map.Entry<String, Integer> _entry : map.entrySet()) {
            entry = _entry;
        }
        if(entry != null) {
            map.remove(entry.getKey());
            return true;
        }
        return false;
    }

    public static ProfessionConditions copy(ProfessionConditions conditions) {
        Map<String, Integer> professionConditions = new LinkedHashMap<>(conditions.getProfessionConditions());
        Map<String, Integer> fabledClassConditions = new LinkedHashMap<>(conditions.getFabledClassConditions());
        Map<String, Integer> mcMMOConditions = new LinkedHashMap<>(conditions.getMcMMOConditions());
        Map<String, Integer> jobsConditions = new LinkedHashMap<>(conditions.getJobsConditions());
        Map<String, Integer> auraAbilityConditions = new LinkedHashMap<>(conditions.getAuraAbilityConditions());
        Map<String, Integer> auraManaAbilityConditions = new LinkedHashMap<>(conditions.getAuraManaAbilityConditions());
        Map<String, Integer> auraSkillsConditions = new LinkedHashMap<>(conditions.getAuraSkillsConditions());
        Map<String, Integer> auraStatsConditions = new LinkedHashMap<>(conditions.getAuraStatsConditions());


        return new ProfessionConditions(conditions.getProfession(),
                conditions.getMoneyCost(),
                conditions.getExpCost(),
                new LinkedList<>(conditions.getRequiredItemNames()),
                conditions.getProfessionLevel(),
                conditions.isMastery(),
                professionConditions,
                fabledClassConditions,
                mcMMOConditions,
                jobsConditions,
                auraAbilityConditions,
                auraManaAbilityConditions,
                auraSkillsConditions,
                auraStatsConditions);
    }
}
