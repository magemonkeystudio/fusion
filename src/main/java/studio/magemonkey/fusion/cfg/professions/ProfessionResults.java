package studio.magemonkey.fusion.cfg.professions;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.api.DelayedCommand;
import studio.magemonkey.codex.legacy.item.ItemBuilder;
import studio.magemonkey.codex.util.SerializationBuilder;
import studio.magemonkey.fusion.Recipe;
import studio.magemonkey.fusion.RecipeItem;
import studio.magemonkey.risecore.legacy.util.DeserializationWorker;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Setter
public class ProfessionResults implements ConfigurationSerializable {

    private final String profession;

    // Rewards
    private long professionExp;
    private int vanillaExp;
    private RecipeItem resultItem;
    private List<DelayedCommand> commands = new LinkedList<>();

    private String resultName;

    public ProfessionResults(String profession, ConfigurationSection config) {
        this.profession = profession;
        this.professionExp = config.getLong("rewards.professionExp");
        this.vanillaExp = config.getInt("rewards.vanillaExp");
        this.resultItem = RecipeItem.fromConfig(config.get("rewards.item"));
        this.resultName = config.getString("rewards.item");
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

            this.resultItem = RecipeItem.fromConfig(resultsSection.get("item"));
            this.resultName = (String) resultsSection.get("item");
        }
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return SerializationBuilder.start(4)
                .append("professionExp", this.professionExp)
                .append("vanillaExp", this.vanillaExp)
                .append("item", this.resultName)
                .build();
    }
}
