package studio.magemonkey.fusion.gui.editors.subeditors.pattern;

import lombok.Getter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import studio.magemonkey.codex.api.DelayedCommand;
import studio.magemonkey.codex.legacy.item.ItemBuilder;
import studio.magemonkey.fusion.CraftingTable;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.InventoryPattern;
import studio.magemonkey.fusion.cfg.editors.EditorRegistry;
import studio.magemonkey.fusion.commands.EditorCriteria;
import studio.magemonkey.fusion.commands.FusionEditorCommand;
import studio.magemonkey.fusion.gui.editors.Editor;
import studio.magemonkey.fusion.util.InventoryUtils;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class PatternItemEditor extends Editor implements Listener {

    private final Player player;
    private final CraftingTable table;
    private final InventoryPattern pattern;
    private final char c;
    private final boolean isCategoryPattern;

    @Getter
    private ItemBuilder builder;

    public PatternItemEditor(Editor parentEditor, Player player, CraftingTable table, char c, boolean isCategoryPattern) {
        super(parentEditor, EditorRegistry.getPatternItemEditorCfg().getSubTitle(c), 27);
        this.player = player;
        this.table = table;
        this.c = c;
        this.isCategoryPattern = isCategoryPattern;
        this.pattern = isCategoryPattern ? table.getCatPattern() : table.getPattern();
        ItemStack item = pattern.getItems().get(c);
        builder = ItemBuilder.newItem(item);
        setIcons(EditorRegistry.getPatternItemEditorCfg().getSubIcons(c, builder, pattern.getCommands(c)));
        initialize();
        Fusion.registerListener(this);
    }

    private void initialize() {
        InventoryUtils.fillInventory(getInventory(), getIcons().get("fill"));
        setItem(10, getIcons().get("name"));
        setItem(11, getIcons().get("lore"));
        setItem(13, getIcons().get("patternItem"));
        setItem(15, getIcons().get("glowing"));
        setItem(16, getIcons().get("commands"));
        setItem(26, getIcons().get("back"));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() != getInventory()) return;
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        boolean hasChanges = false;

        switch (event.getSlot()) {
            case 10 ->
                    FusionEditorCommand.suggestUsage(player, EditorCriteria.Profession_Pattern_Edit_Name, "/fusion-editor " + builder.getName());
            case 11 -> {
                if(event.isLeftClick()) {
                    FusionEditorCommand.suggestUsage(player, EditorCriteria.Profession_Pattern_Edit_Lore, "/fusion-editor <lore>");
                } else {
                    if(builder.getLore().isEmpty()) {
                        return;
                    }
                    builder.getLore().remove(builder.getLore().size() - 1);
                }
            }
            case 13 ->
                    FusionEditorCommand.suggestUsage(player, EditorCriteria.Profession_Pattern_Edit_Pattern, "/fusion-editor " + builder.getMaterial().name() + " " + builder.getAmount() + " " + builder.getAmount());
            case 15 -> {
                if (!builder.getEnchants().isEmpty()) {
                    builder.clearEnchants();
                    builder.clearFlags();
                } else {
                    builder.enchant(Enchantment.UNBREAKING, 1);
                    builder.flag(ItemFlag.HIDE_ENCHANTS);
                }
                hasChanges = true;
            }
            case 16 -> {
                if (event.isLeftClick()) {
                    FusionEditorCommand.suggestUsage(player, EditorCriteria.Profession_Pattern_Add_Commands, "/fusion-editor <caster> <delay> <command without />");
                } else {
                    if(pattern.getCommands(c).isEmpty()) {
                        return;
                    }
                    DelayedCommand command = new ArrayList<>(pattern.getCommands(c)).get(pattern.getCommands(c).size() - 1);
                    pattern.getCommands(c).remove(command);
                }
            }
            case 26 -> {
                reload(false);
                ((PatternItemsEditor) getParentEditor()).reload(true);
                return;
            }
        }

        if (hasChanges) {
            reload(true);
        }
    }

    public void reload(boolean open) {
        setIcons(EditorRegistry.getPatternItemEditorCfg().getSubIcons(c, builder, pattern.getCommands(c)));
        if (isCategoryPattern) {
            table.getCatPattern().getItems().put(c, builder.build());
        } else {
            table.getPattern().getItems().put(c, builder.build());
        }
        initialize();
        if (open)
            open(player);
    }

    public void addCommand(DelayedCommand command) {
        pattern.getCommands(c).add(command);
    }
}
