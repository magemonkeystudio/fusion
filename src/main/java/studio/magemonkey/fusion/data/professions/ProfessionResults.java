package studio.magemonkey.fusion.data.professions;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.api.DelayedCommand;
import studio.magemonkey.codex.util.DeserializationWorker;
import studio.magemonkey.codex.util.SerializationBuilder;
import studio.magemonkey.fusion.data.recipes.RecipeItem;

import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
public class ProfessionResults implements ConfigurationSerializable {

    private final String profession;

    // Rewards
    private long                 professionExp;
    private int                  vanillaExp;
    private List<RecipeItem>     items     = new LinkedList<>();
    private List<String>         itemNames = new LinkedList<>();
    private List<DelayedCommand> commands  = new LinkedList<>();

    public ProfessionResults(String profession,
                             long professionExp,
                             int vanillaExp,
                             List<String> itemNames,
                             List<DelayedCommand> commands) {
        this.profession = profession;
        this.professionExp = professionExp;
        this.vanillaExp = vanillaExp;
        this.itemNames = itemNames;
        this.commands = commands;

        for (String itemName : itemNames) {
            this.items.add(RecipeItem.fromConfig(itemName));
        }
    }

    public ProfessionResults(String profession, ConfigurationSection config) {
        this.profession = profession;
        this.professionExp = config.getLong("rewards.professionExp");
        this.vanillaExp = config.getInt("rewards.vanillaExp");
        this.commands = config.getList("rewards.commands", new LinkedList<>())
                .stream()
                .map(entry -> new DelayedCommand())
                .collect(Collectors.toList());
        this.itemNames = config.getList("rewards.items", new LinkedList<>())
                .stream()
                .map(Object::toString)
                .collect(Collectors.toList());
        for (String itemName : itemNames) {
            this.items.add(RecipeItem.fromConfig(itemName));
        }
    }

    public ProfessionResults(String profession, DeserializationWorker dw) {
        this.profession = profession;

        Map<String, Object> resultsSection = dw.getSection("results");
        if (resultsSection != null) {
            Object professionExpObj = resultsSection.getOrDefault("professionExp", 0);
            if (professionExpObj instanceof Number) {
                this.professionExp = ((Number) professionExpObj).longValue();
            } else {
                this.professionExp = 0;
            }

            Object vanillaExpObj = resultsSection.getOrDefault("vanillaExp", 0);
            if (vanillaExpObj instanceof Number) {
                this.vanillaExp = ((Number) vanillaExpObj).intValue();
            } else {
                this.vanillaExp = 0;
            }

            List<Map<String, Object>> commands =
                    (List<Map<String, Object>>) resultsSection.getOrDefault("commands", new ArrayList<>());
            if (commands != null) {
                for (Map<String, Object> command : commands) {
                    this.commands.add(new DelayedCommand(command));
                }
            }

            List<Object> items = (List<Object>) resultsSection.getOrDefault("items", new ArrayList<>());
            if (items != null) {
                items.forEach(item -> {
                    this.itemNames.add(item.toString());
                    this.items.add(RecipeItem.fromConfig(item));
                });
            }
        }
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("professionExp", this.professionExp);
        resultMap.put("vanillaExp", this.vanillaExp);
        resultMap.put("commands",
                new ArrayList<>(this.commands.stream().map(DelayedCommand::serialize).collect(Collectors.toList())));
        resultMap.put("items", new ArrayList<>(this.itemNames));
        return SerializationBuilder.start(4).append("results", resultMap).build();
    }

    public static ProfessionResults copy(ProfessionResults results) {
        List<DelayedCommand> cmds = new ArrayList<>();
        for (DelayedCommand cmd : results.getCommands()) {
            cmds.add(new DelayedCommand(cmd.getAs(), cmd.getCmd(), cmd.getDelay()));
        }

        return new ProfessionResults(results.getProfession(),
                results.getProfessionExp(),
                results.getVanillaExp(),
                new ArrayList<>(results.getItemNames()),
                cmds);
    }

    public boolean hasCommandsOrItems() {
        return professionExp > 0 || vanillaExp > 0 || !commands.isEmpty() || !itemNames.isEmpty();
    }
}
