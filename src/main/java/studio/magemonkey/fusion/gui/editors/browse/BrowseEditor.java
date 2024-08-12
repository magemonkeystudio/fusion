package studio.magemonkey.fusion.gui.editors.browse;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import studio.magemonkey.codex.util.messages.MessageData;
import studio.magemonkey.codex.util.messages.MessageUtil;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.InventoryPattern;
import studio.magemonkey.fusion.cfg.BrowseConfig;
import studio.magemonkey.fusion.cfg.editors.EditorRegistry;
import studio.magemonkey.fusion.cfg.professions.ProfessionConditions;
import studio.magemonkey.fusion.commands.EditorCriteria;
import studio.magemonkey.fusion.commands.FusionEditorCommand;
import studio.magemonkey.fusion.gui.editors.Editor;
import studio.magemonkey.fusion.gui.editors.pattern.PatternEditor;
import studio.magemonkey.fusion.gui.editors.pattern.PatternItemsEditor;
import studio.magemonkey.fusion.util.InventoryUtils;

import java.util.HashMap;
import java.util.LinkedList;

@Getter
public class BrowseEditor extends Editor implements Listener {

    private final Player player;

    /*
    // TODO:
    - [x] Make alternative for CraftingTable in PatternItemsEditor, PatternItemEditor & PatternEditor (InventoryView)
    - Make SubEditor for BrowseProfessionsEditor (overview of all professions)
    - Make SubEditor for BrowseProfessionEditor (single profession with its settings)
    - Make EditorCriteria for name and professions
     */
    // The settings that will be overriden before saving into browse.yml
    @Setter
    private String name;
    private final LinkedList<String> professions = new LinkedList<>();
    private final HashMap<String, ProfessionConditions> professionConditions = new HashMap<>();
    private final InventoryPattern browsePattern;

    private PatternItemsEditor patternItemsEditor;
    private PatternEditor patternEditor;
    private BrowseProfessionsEditor browseProfessionsEditor;

    public BrowseEditor(Player player, String name, LinkedList<String> professions, InventoryPattern browsePattern) {
        super(null, EditorRegistry.getBrowseEditorCfg().getTitle(), 27);
        this.player = player;

        this.name = name;
        this.professions.addAll(professions);
        for (String profession : BrowseConfig.getProfessions())
            this.professionConditions.put(profession, ProfessionConditions.copy(BrowseConfig.getProfessionConditions().get(profession)));
        this.browsePattern = InventoryPattern.copy(browsePattern);

        setIcons(EditorRegistry.getBrowseEditorCfg().getIcons(this));
        initialize();
        Fusion.registerListener(this);
    }

    private void initialize() {
        InventoryUtils.fillInventory(getInventory(), getIcons().get("fill"));
        setItem(10, getIcons().get("name"));
        setItem(12, getIcons().get("patternItems"));
        setItem(13, getIcons().get("pattern"));
        setItem(16, getIcons().get("professions"));
        setItem(18, getIcons().get("save"));
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
                    FusionEditorCommand.suggestUsage(player, EditorCriteria.Browse_Edit_Name, "/fusion-editor <newName>");
            case 12 -> {
                if (patternItemsEditor == null)
                    patternItemsEditor = new PatternItemsEditor(this, player);
                patternItemsEditor.open(player);
            }
            case 13 -> {
                if (patternEditor == null)
                    patternEditor = new PatternEditor(this, player);
                patternEditor.open(player);
            }
            case 16 -> {
                if (browseProfessionsEditor == null)
                    browseProfessionsEditor = new BrowseProfessionsEditor(this, player);
                browseProfessionsEditor.open(player);
            }
            case 18 -> {
                player.closeInventory();
                MessageUtil.sendMessage("editor.changesSaved", player, new MessageData("file", "browse.yml"));
                EditorRegistry.removeCurrentEditor(player);
                FusionEditorCommand.removeEditorCriteria(player.getUniqueId());
            }
            case 26 -> {
                player.closeInventory();
                hasChanges = true;
            }
        }

        if (hasChanges) {
            reload(true);
        }
    }

    public void reload(boolean open) {
        setIcons(EditorRegistry.getBrowseEditorCfg().getIcons(this));
        initialize();
        if (open)
            open(player);
    }
}
