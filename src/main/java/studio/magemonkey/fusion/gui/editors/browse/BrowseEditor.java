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
import studio.magemonkey.fusion.Recipe;
import studio.magemonkey.fusion.cfg.BrowseConfig;
import studio.magemonkey.fusion.cfg.editors.EditorRegistry;
import studio.magemonkey.fusion.cfg.professions.ProfessionConditions;
import studio.magemonkey.fusion.cfg.editors.EditorCriteria;
import studio.magemonkey.fusion.commands.FusionEditorCommand;
import studio.magemonkey.fusion.gui.editors.Editor;
import studio.magemonkey.fusion.gui.editors.pattern.PatternEditor;
import studio.magemonkey.fusion.gui.editors.pattern.PatternItemsEditor;
import studio.magemonkey.fusion.util.InventoryUtils;

import java.util.*;

@Getter
public class BrowseEditor extends Editor implements Listener {

    private final Player player;

    // The settings that will be overriden before saving into browse.yml
    @Setter
    private String name;
    private final LinkedList<String> professions = new LinkedList<>();
    @Getter
    private final LinkedHashMap<String, ProfessionConditions> professionConditions = new LinkedHashMap<>();
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
                BrowseConfig.save(this);
                MessageUtil.sendMessage("editor.changesSaved", player, new MessageData("file", "browse.yml"));
                EditorRegistry.removeCurrentEditor(player);
                FusionEditorCommand.removeEditorCriteria(player.getUniqueId());
                BrowseConfig.load();
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

    public void moveEntry(ProfessionConditions conditions, int offset) {
        // Ensure the offset is either -1 (left) or 1 (right)
        if (offset != -1 && offset != 1) {
            throw new IllegalArgumentException("Offset must be -1 or 1");
        }

        List<Map.Entry<String, ProfessionConditions>> entries = new ArrayList<>(professionConditions.entrySet());
        int index = -1;

        // Find the index of the current entry
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getKey().equals(conditions.getProfession())) {
                index = i;
                break;
            }
        }

        if (index == -1) {
            return; // Key not found, do nothing
        }

        // Calculate the new index
        int newIndex = index + offset;

        // Check if the new index is within bounds
        if (newIndex < 0 || newIndex >= entries.size()) {
            return; // New index out of bounds, do nothing
        }

        // Remove and reinsert the entry at the new position
        Map.Entry<String, ProfessionConditions> entry = entries.remove(index);
        entries.add(newIndex, entry);

        // Clear the original map and reinsert the entries in the new order
        professionConditions.clear();
        professions.clear();
        for (Map.Entry<String, ProfessionConditions> e : entries) {
            professions.add(e.getKey());
            professionConditions.put(e.getKey(), e.getValue());
        }
    }
}
