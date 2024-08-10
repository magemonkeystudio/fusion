package studio.magemonkey.fusion.cfg.editors;

import lombok.Getter;
import org.bukkit.entity.Player;
import studio.magemonkey.fusion.cfg.editors.subeditors.CategoryEditorCfg;
import studio.magemonkey.fusion.cfg.editors.subeditors.PatternEditorCfg;
import studio.magemonkey.fusion.cfg.editors.subeditors.PatternItemEditorCfg;
import studio.magemonkey.fusion.cfg.editors.subeditors.RecipeEditorCfg;
import studio.magemonkey.fusion.gui.editors.Editor;
import studio.magemonkey.fusion.gui.editors.ProfessionEditor;

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public class EditorRegistry {

    private static final Map<UUID, Editor> editors = new TreeMap<>();

    /* Main Editor Configs */
    @Getter
    private static ProfessionEditorCfg professionEditorCfg;
    @Getter
    private static BrowseEditorCfg browseEditorCfg;

    /* Sub Editor Configs */
    @Getter
    private static PatternItemEditorCfg patternItemEditorCfg;
    @Getter
    private static PatternEditorCfg patternEditorCfg;
    @Getter
    private static RecipeEditorCfg recipeEditorCfg;
    @Getter
    private static CategoryEditorCfg categoryEditorCfg;


    public static void reload() {
        editors.clear();
        professionEditorCfg = new ProfessionEditorCfg();
        browseEditorCfg = new BrowseEditorCfg();

        patternItemEditorCfg = new PatternItemEditorCfg();
        patternEditorCfg = new PatternEditorCfg();
        recipeEditorCfg = new RecipeEditorCfg();
        categoryEditorCfg = new CategoryEditorCfg();
    }

    public static Editor getProfessionEditor(Player player, String profession) {
        if(!editors.containsKey(player.getUniqueId()) && profession != null)
            editors.put(player.getUniqueId(), new ProfessionEditor(player, profession));
        return editors.get(player.getUniqueId());
    }

    public static Editor getCurrentEditor(Player player) {
        return editors.get(player.getUniqueId());
    }
}
