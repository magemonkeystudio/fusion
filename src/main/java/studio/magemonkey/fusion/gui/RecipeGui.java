package studio.magemonkey.fusion.gui;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import studio.magemonkey.codex.api.DelayedCommand;
import studio.magemonkey.codex.api.Replacer;
import studio.magemonkey.codex.util.ItemUtils;
import studio.magemonkey.codex.util.messages.MessageData;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.crafting.CraftingChecks;
import studio.magemonkey.fusion.data.professions.pattern.*;
import studio.magemonkey.fusion.data.recipes.*;
import studio.magemonkey.fusion.gui.recipe.*;
import studio.magemonkey.fusion.util.ChatUT;
import java.util.*;

/** Shared recipe browsing and lifecycle only. Crafting belongs to the mode services. */
@Getter
public abstract class RecipeGui {
    protected final Player player;
    protected final CraftingTable table;
    protected final Category category;
    protected final String name;
    protected final String inventoryName;
    protected Inventory inventory;
    protected InventoryPattern pattern;
    protected CompiledPattern layout;
    protected final Map<Integer, Recipe> recipes = new HashMap<>();
    protected final SlotRenderer renderer = new SlotRenderer();
    private final GuiUpdateCoordinator updates;
    protected Map<Character, ItemStack> decorations;
    private int page;
    private int recipeCount;
    private boolean opened;
    private RecipeViewSnapshot renderSnapshot;

    protected RecipeGui(Player player, CraftingTable table, Category category) {
        this.player = player;
        this.table = table;
        this.name = table.getName();
        this.inventoryName = ChatUT.hexString(table.getInventoryName());
        this.category = category != null ? category : new Category("master", "PAPER", table.getRecipePattern(), 1);
        if (this.category.getName().equals("master")) {
            this.category.getRecipes().clear();
            this.category.getRecipes().addAll(table.getRecipes().values());
        }
        updates = new GuiUpdateCoordinator(this::reloadRecipes, this::pulse);
        setPattern();
        inventory = Bukkit.createInventory(null, layout.size(), inventoryName);
    }

    public final void setPattern() {
        pattern = category.getPattern() == null ? table.getRecipePattern() : category.getPattern();
        Map<Character, SlotRole> roles = new HashMap<>(table.getRecipePattern().getRoles());
        roles.putAll(pattern.getRoles());
        layout = PatternCompiler.compile(pattern.getPattern(), roles);
        // Merge into a session copy; never mutate category or profession configuration.
        decorations = new HashMap<>(table.getRecipePattern().getItems());
        decorations.putAll(pattern.getItems());
    }
    public final void resetPattern() { setPattern(); reloadRecipesTask(); }
    public final void initialize() { setPattern(); inventory = Bukkit.createInventory(null, layout.size(), inventoryName); }

    public final void open(Player target) {
        if (!player.getUniqueId().equals(target.getUniqueId())) throw new IllegalArgumentException("GUI belongs to another player");
        RecipeGui previous = ProfessionGuiRegistry.getLatestRecipeGui().get(player.getUniqueId());
        if (previous != null) previous.close(player, previous.getInventory());
        setPattern();
        if (inventory.getSize() != layout.size()) inventory = Bukkit.createInventory(null, layout.size(), inventoryName);
        // Open first: Bukkit closes the previous inventory synchronously during this call.
        target.openInventory(inventory);
        ProfessionGuiRegistry.getLatestRecipeGui().put(player.getUniqueId(), this);
        opened = true;
        onOpen();
        reloadRecipes();
        updates.open();
    }

    public final void reloadRecipesTask() { updates.request(); }
    public final void reloadRecipes() {
        if (!opened || !player.isOnline()) return;
        try {
            List<Recipe> available = new ArrayList<>(category.getName().equals("master")
                    ? table.getRecipes().values() : category.getRecipes());
            available.removeIf(recipe -> recipe.isHidden(player));
            List<Integer> slots = layout.slots(SlotRole.RECIPE_LIST);
            recipeCount = available.size();
            page = clampPage(page, recipeCount, slots.size());
            var counts = CraftingChecks.inventory(player);
            var snapshot = RecipeViewSnapshot.capture(player, table);
            renderSnapshot = snapshot;
            recipes.clear();
            for (int offset = 0; offset < slots.size(); offset++) {
                int index = page * slots.size() + offset;
                int slot = slots.get(offset);
                if (index < available.size()) {
                    Recipe recipe = available.get(index);
                    CalculatedRecipe calculated = CalculatedRecipe.create(recipe, new HashMap<>(counts), player, table, snapshot);
                    recipes.put(slot, recipe);
                    renderer.set(inventory, slot, calculated.getIcon());
                } else renderer.set(inventory, slot, table.getFillItem());
            }
            renderDynamic();
            updateBlockedSlots(viewData());
        } catch (Exception exception) {
            Fusion.getInstance().getLogger().warning("Cannot refresh crafting GUI for " + player.getName() + ": " + exception.getMessage());
            // Do not leave clickable bindings from a partially rendered page.
            recipes.clear();
        }
    }

    private void pulse() {
        if (!opened || player.getOpenInventory().getTopInventory() != inventory) { close(player, inventory); return; }
        // Requirements can change without an inventory event (cooldown, money, permissions).
        reloadRecipes();
    }

    protected MessageData[] viewData() {
        return new MessageData[]{new MessageData("level", renderSnapshot == null ? 0 : renderSnapshot.level()),
                new MessageData("bal", renderSnapshot == null ? 0 : renderSnapshot.balance()),
                new MessageData("category", category), new MessageData("gui", name),
                new MessageData("player", player.getName())};
    }
    public final void updateBlockedSlots(MessageData[] data) {
        for (int slot = 0; slot < layout.size(); slot++) {
            SlotRole role = layout.role(slot);
            if (role == SlotRole.RECIPE_LIST || role == SlotRole.QUEUE_LIST
                    || role == SlotRole.PREVIOUS_QUEUE_PAGE || role == SlotRole.NEXT_QUEUE_PAGE) continue;
            boolean hidden = role == SlotRole.PREVIOUS_PAGE && page == 0 && !category.hasPrevious()
                    || role == SlotRole.NEXT_PAGE && page >= clampPage(Integer.MAX_VALUE, recipeCount, layout.slots(SlotRole.RECIPE_LIST).size());
            ItemStack icon = hidden ? table.getFillItem() : decorations.get(layout.symbol(slot));
            if (icon == null) icon = table.getFillItem();
            renderer.set(inventory, slot, ItemUtils.replaceText(icon == null ? null : icon.clone(), data));
        }
    }

    public final void click(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0) return;
        if (slot >= layout.size()) {
            // Shift-click and collect-to-cursor can move items into/out of the top inventory.
            if (event.isShiftClick() || event.getAction() == InventoryAction.COLLECT_TO_CURSOR) event.setCancelled(true);
            reloadRecipesTask();
            return;
        }
        event.setCancelled(true);
        executeCommands(layout.symbol(slot), player);
        if (pattern.getCloseOnClickSlots().contains(layout.symbol(slot))) {
            Bukkit.getScheduler().runTask(Fusion.getInstance(), player::closeInventory);
            return;
        }
        switch (layout.role(slot)) {
            case PREVIOUS_PAGE -> {
                if (page == 0) ProfessionsCfg.getGUI(name).open(player);
                else { page--; reloadRecipesTask(); }
            }
            case NEXT_PAGE -> { page++; reloadRecipesTask(); }
            case RECIPE_LIST -> {
                Recipe recipe = recipes.get(slot);
                if (recipe != null) onCraft(recipe, event.isShiftClick() && event.isLeftClick());
                reloadRecipesTask();
            }
            default -> { onModeClick(slot, event); reloadRecipesTask(); }
        }
    }
    public void onDrag(InventoryDragEvent event) {
        if (event.getRawSlots().stream().anyMatch(slot -> slot < inventory.getSize())) event.setCancelled(true);
        reloadRecipesTask();
    }
    public void drop(PlayerDropItemEvent event) { reloadRecipesTask(); }
    public final void close(Player target, Inventory closed) {
        if (closed != inventory) return;
        opened = false;
        updates.close();
        onClose();
        ProfessionGuiRegistry.getLatestRecipeGui().remove(player.getUniqueId(), this);
    }
    public final void executeCommands(Character symbol, HumanEntity target) {
        DelayedCommand.invoke(Fusion.getInstance(), target, pattern.getCommands(symbol),
                Replacer.replacer("{crafting}", name), Replacer.replacer("{inventoryName}", inventoryName));
    }
    public static int clampPage(int page, int count, int size) {
        return size <= 0 ? 0 : Math.max(0, Math.min(page, Math.max(0, (count - 1) / size)));
    }
    public static void resetRecipeHashes() { /* No gameplay eligibility cache. */ }
    protected void onOpen() { }
    protected void onClose() { }
    protected abstract void onCraft(Recipe recipe, boolean bulk);
    protected abstract void renderDynamic();
    protected void onModeClick(int slot, InventoryClickEvent event) { }
}
