package studio.magemonkey.fusion.util;

import studio.magemonkey.fusion.data.recipes.Recipe;
import studio.magemonkey.fusion.data.recipes.RecipeItem;

import java.util.List;

/**
 * Amounts produced by a recipe, using the same result rules as the queue.
 */
public final class RecipeAmounts {

    private RecipeAmounts() {
    }

    public static int outputAmount(Recipe recipe) {
        return outputAmount(recipe, recipe.getResults().getItems());
    }

    public static int outputAmount(Recipe recipe, List<RecipeItem> resultItems) {
        if (!recipe.getResults().hasCommandsOrItems()) {
            return Math.max(0, recipe.getSettings().getRecipeItem().getAmount());
        }
        if (resultItems == null) {
            return 0;
        }
        return resultItems.stream()
                .filter(item -> item != null)
                .mapToInt(RecipeItem::getAmount)
                .filter(amount -> amount > 0)
                .sum();
    }
}
