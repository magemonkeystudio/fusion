package studio.magemonkey.fusion.gui.recipe;

import org.bukkit.entity.Player;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.fusion.cfg.sql.SQLManager;
import studio.magemonkey.fusion.data.player.*;
import studio.magemonkey.fusion.data.recipes.CraftingTable;
import studio.magemonkey.fusion.util.ExperienceManager;
import java.util.Map;

/** One short-lived read snapshot per refresh, never used to authorize a later click. */
public record RecipeViewSnapshot(int level, boolean mastered, int experience, double balance,
                                 boolean economyAvailable, Map<String, PlayerRecipeLimit> limits) {
    public static RecipeViewSnapshot capture(Player player, CraftingTable table) {
        var profession = PlayerLoader.getPlayer(player).getProfession(table);
        var vault = CodexEngine.get().getVault();
        return new RecipeViewSnapshot(profession == null ? 0 : profession.getLevel(),
                profession != null && profession.isMastered(), ExperienceManager.getTotalExperience(player),
                vault == null ? 0 : vault.getBalance(player), vault != null,
                SQLManager.recipeLimits().getRecipeLimits(player.getUniqueId()));
    }
    public PlayerRecipeLimit limit(String path) {
        PlayerRecipeLimit limit = limits.get(path);
        if (limit == null || (limit.getCooldownTimestamp() > 0 && !limit.hasCooldown()))
            return new PlayerRecipeLimit(path, 0, -1);
        return limit;
    }
}
