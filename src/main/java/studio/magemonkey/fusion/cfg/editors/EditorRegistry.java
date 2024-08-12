package studio.magemonkey.fusion.cfg.editors;

import lombok.Getter;
import org.bukkit.entity.Player;
import studio.magemonkey.fusion.InventoryPattern;
import studio.magemonkey.fusion.cfg.BrowseConfig;
import studio.magemonkey.fusion.cfg.editors.browse.BrowseEditorCfg;
import studio.magemonkey.fusion.cfg.editors.browse.BrowseProfessionCfg;
import studio.magemonkey.fusion.cfg.editors.professions.CategoryEditorCfg;
import studio.magemonkey.fusion.cfg.editors.pattern.PatternEditorCfg;
import studio.magemonkey.fusion.cfg.editors.pattern.PatternItemEditorCfg;
import studio.magemonkey.fusion.cfg.editors.professions.ProfessionEditorCfg;
import studio.magemonkey.fusion.cfg.editors.professions.RecipeEditorCfg;
import studio.magemonkey.fusion.gui.editors.browse.BrowseEditor;
import studio.magemonkey.fusion.gui.editors.Editor;
import studio.magemonkey.fusion.gui.editors.professions.ProfessionEditor;

import java.util.*;

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
    @Getter
    private static BrowseProfessionCfg browseProfessionCfg;


    public static void reload() {
        editors.clear();

        professionEditorCfg = new ProfessionEditorCfg();
        recipeEditorCfg = new RecipeEditorCfg();
        categoryEditorCfg = new CategoryEditorCfg();

        browseEditorCfg = new BrowseEditorCfg();
        browseProfessionCfg = new BrowseProfessionCfg();

        patternItemEditorCfg = new PatternItemEditorCfg();
        patternEditorCfg = new PatternEditorCfg();
    }

    public static Editor getProfessionEditor(Player player, String profession) {
        if(!editors.containsKey(player.getUniqueId()) && profession != null)
            editors.put(player.getUniqueId(), new ProfessionEditor(player, profession));
        return editors.get(player.getUniqueId());
    }

    public static Editor getBrowseEditor(Player player) {
        if(!editors.containsKey(player.getUniqueId())) {
            LinkedList<String> professions = new LinkedList<>(BrowseConfig.getProfessions());
            editors.put(player.getUniqueId(), new BrowseEditor(player, BrowseConfig.getBrowseName(), professions, InventoryPattern.copy(BrowseConfig.getBrowsePattern())));
        }
        return editors.get(player.getUniqueId());
    }

    public static Editor getCurrentEditor(Player player) {
        return editors.get(player.getUniqueId());
    }

    public static void removeCurrentEditor(Player player) {
        editors.remove(player.getUniqueId());
    }
}
