package studio.magemonkey.fusion.cfg;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import studio.magemonkey.fusion.Fusion;

import java.util.ArrayList;
import java.util.List;

public class BukkitRecipeWrapper {

    public static List<NamespacedKey> getRecipeKeysForMaterials(List<Material> material) {

        List<NamespacedKey> entries = new ArrayList<>();

        for (Material mat : material) {
            if (mat.isAir()) continue;
            ItemStack stack = new ItemStack(mat);
            var recipes = Bukkit.getRecipesFor(stack);
            if (recipes.isEmpty()) {
                Fusion.getInstance().error("No recipes found for material: " + mat);
                continue;
            }
            for(Recipe recipe : recipes) {
                if (recipe instanceof Keyed keyed) {
                    NamespacedKey key = keyed.getKey();
                    entries.add(key);
                } else {
                    Bukkit.getLogger().info("Recipe has no key (likely a custom non-keyed recipe).");
                }
            }
        }
        return entries;
    }

    public static void enableBukkitRecipes(List<NamespacedKey> recipes) {
        for (NamespacedKey recipe : recipes) {
            Recipe bukkitRecipe = Fusion.getInstance().getServer().getRecipe(recipe);
            if (bukkitRecipe != null) {
                Fusion.getInstance().getServer().addRecipe(bukkitRecipe);
                Bukkit.getLogger().info("Enabled Bukkit recipe with key: " + recipe);
            } else {
                Fusion.getInstance().error("Bukkit recipe with key " + recipe + " not found.");
            }
        }
    }

    public static void disableBukkitRecipes(List<NamespacedKey> recipes) {
        for (NamespacedKey recipe : recipes) {
            Recipe bukkitRecipe = Fusion.getInstance().getServer().getRecipe(recipe);
            if (bukkitRecipe != null) {
                Fusion.getInstance().getServer().removeRecipe(recipe);
                Bukkit.getLogger().info("Disabled Bukkit recipe with key: " + recipe);
            } else {
                Fusion.getInstance().error("Bukkit recipe with key " + recipe + " not found.");
            }
        }
    }
}
