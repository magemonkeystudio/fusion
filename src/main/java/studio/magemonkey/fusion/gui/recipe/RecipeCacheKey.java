package studio.magemonkey.fusion.gui.recipe;

import java.util.Arrays;
import java.util.Objects;

/**
 * Key used for caching a CalculatedRecipe. Uniquely identifies a player’s view of one recipe.
 */
public class RecipeCacheKey {
    private final String recipeId;
    private final byte[] inventoryHash;
    private final int playerLevel;
    private final double playerMoney;

    // TODO add:
    /*
        * - Vanilla Exp
        * - Conditions.McMMO Map
        * - Conditions.Fabled Map
        * - Conditions.Aura Map
        * - Conditions.ProfessionLevels Map
     */

    public RecipeCacheKey(String recipeId, byte[] inventoryHash, int playerLevel, double playerMoney) {
        this.recipeId = recipeId;
        this.inventoryHash = Arrays.copyOf(inventoryHash, inventoryHash.length);
        this.playerLevel = playerLevel;
        this.playerMoney = playerMoney;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RecipeCacheKey that)) return false;
        return playerLevel == that.playerLevel &&
                Double.compare(that.playerMoney, playerMoney) == 0 &&
                Objects.equals(recipeId, that.recipeId) &&
                Arrays.equals(inventoryHash, that.inventoryHash);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(recipeId, playerLevel, playerMoney);
        result = 31 * result + Arrays.hashCode(inventoryHash);
        return result;
    }
}
