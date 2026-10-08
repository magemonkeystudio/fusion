package studio.magemonkey.fusion.gui;

import org.bukkit.Bukkit;
import org.bukkit.boss.*;
import org.bukkit.entity.Player;
import studio.magemonkey.fusion.cfg.CraftingRequirementsCfg;
import studio.magemonkey.fusion.crafting.ManualCraftingService;
import studio.magemonkey.fusion.data.professions.pattern.Category;
import studio.magemonkey.fusion.data.recipes.*;
import studio.magemonkey.fusion.gui.recipe.SlotRole;

public final class ManualRecipeGUI extends RecipeGui {
    private final ManualCraftingService service;
    private BossBar bar;
    public ManualRecipeGUI(Player player, CraftingTable table, Category category) {
        super(player, table, category);
        service = new ManualCraftingService(player, table);
    }
    @Override protected void onCraft(Recipe recipe, boolean bulk) {
        service.start(recipe, this::reloadRecipesTask).report(player);
    }
    @Override protected void renderDynamic() {
        // Queue placeholders in existing manual-mode patterns are decoration, not craft state.
        for (int slot : layout.slots(SlotRole.QUEUE_LIST)) renderer.set(inventory, slot, table.getFillItem());
        for (int slot : layout.slots(SlotRole.PREVIOUS_QUEUE_PAGE)) renderer.set(inventory, slot, table.getFillItem());
        for (int slot : layout.slots(SlotRole.NEXT_QUEUE_PAGE)) renderer.set(inventory, slot, table.getFillItem());
        if (!service.isRunning()) { removeBar(); return; }
        if (bar == null) {
            bar = Bukkit.createBossBar("", BarColor.BLUE, BarStyle.SOLID);
            bar.addPlayer(player);
        }
        bar.setTitle(CraftingRequirementsCfg.getBossBarTitle(service.getRecipe().getSettings().getRecipeItem().getItemStack()));
        bar.setProgress(service.getDuration() == 0 ? 1 : Math.min(1, Math.max(0,
                1 - (double) service.getRemainingSeconds() / service.getDuration())));
    }
    @Override protected void onClose() { service.cancel(); removeBar(); }
    private void removeBar() { if (bar != null) bar.removeAll(); bar = null; }
}
