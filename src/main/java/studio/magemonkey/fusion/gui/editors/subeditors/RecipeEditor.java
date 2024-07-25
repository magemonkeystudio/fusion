package studio.magemonkey.fusion.gui.editors.subeditors;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import studio.magemonkey.fusion.CraftingTable;
import studio.magemonkey.fusion.cfg.editors.EditorRegistry;
import studio.magemonkey.fusion.gui.editors.Editor;

public class RecipeEditor extends Editor implements Listener {

    private final Player player;
    private final CraftingTable table;

    public RecipeEditor(Player player, CraftingTable table) {
        super(EditorRegistry.getRecipeEditorCfg().getTitle(), 54);
        this.player = player;
        this.table = table;
    }
}
