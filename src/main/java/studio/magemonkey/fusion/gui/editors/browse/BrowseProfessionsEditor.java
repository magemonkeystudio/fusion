package studio.magemonkey.fusion.gui.editors.browse;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.cfg.editors.EditorCriteria;
import studio.magemonkey.fusion.cfg.editors.EditorRegistry;
import studio.magemonkey.fusion.commands.FusionEditorCommand;
import studio.magemonkey.fusion.data.professions.ProfessionConditions;
import studio.magemonkey.fusion.gui.editors.Editor;
import studio.magemonkey.fusion.util.InventoryUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BrowseProfessionsEditor extends Editor implements Listener {

    private final Player       player;
    private final BrowseEditor browseEditor;

    @Getter
    private BrowseProfessionEditor browseProfessionEditor;

    private final HashMap<Inventory, HashMap<Integer, ProfessionConditions>> slots = new HashMap<>();
    private List<ProfessionConditions> professionList = new ArrayList<>();
    private final Set<Integer>         populatedPages = new HashSet<>();

    public BrowseProfessionsEditor(BrowseEditor browseEditor, Player player) {
        super(browseEditor, EditorRegistry.getBrowseProfessionCfg().getTitle(), 54);
        this.player = player;
        this.browseEditor = browseEditor;
        setIcons(EditorRegistry.getBrowseProfessionCfg().getIcons(browseEditor));

        initialize();
        Fusion.registerListener(this);
    }

    public void initialize() {
        slots.clear();
        populatedPages.clear();
        getNestedInventories().clear();

        professionList = new ArrayList<>();
        for (ProfessionConditions entry : browseEditor.getProfessionConditions().values()) {
            if (!ProfessionsCfg.getMap().containsKey(entry.getProfession())) {
                Fusion.getInstance().getLogger()
                        .warning("Profession " + entry.getProfession()
                                + " not found for BrowseEditor. You might want to remove it from browse.yml to avoid problems.");
                Fusion.getInstance().getLogger().warning("Skipping profession: " + entry.getProfession());
                continue;
            }
            professionList.add(entry);
        }

        int             pageCount   = professionList.isEmpty() ? 1 : (int) Math.ceil(professionList.size() / 36.0);
        List<Inventory> inventories = new ArrayList<>();
        for (int p = 0; p < pageCount; p++) {
            Inventory inv = InventoryUtils.createFilledInventory(null,
                    EditorRegistry.getBrowseProfessionCfg().getTitle(),
                    54,
                    getIcons().get("fill"));
            inv.setItem(4, getIcons().get("add"));
            inv.setItem(48, getIcons().get("previous"));
            inv.setItem(50, getIcons().get("next"));
            inv.setItem(53, getIcons().get("back"));
            inventories.add(inv);
        }
        setNestedInventories(inventories);
    }

    private void populatePage(int page) {
        if (page < 0 || page >= getNestedInventories().size()) return;
        Inventory                              inv      = getNestedInventories().get(page);
        HashMap<Integer, ProfessionConditions> invSlots = new HashMap<>();
        int                                    start    = page * 36;
        int                                    end      = Math.min(start + 36, professionList.size());
        for (int i = start; i < end; i++) {
            ProfessionConditions entry  = professionList.get(i);
            int                  invDex = (i - start) + 9;
            inv.setItem(invDex, EditorRegistry.getBrowseProfessionCfg().getProfessionIcon(entry));
            invSlots.put(invDex, entry);
        }
        slots.put(inv, invSlots);
        populatedPages.add(page);
    }

    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int page) {
        List<Inventory> invs     = getNestedInventories();
        int             safePage = (page >= 0 && page < invs.size()) ? page : invs.size() - 1;
        if (!populatedPages.contains(safePage)) {
            populatePage(safePage);
        }
        player.openInventory(invs.get(safePage));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        int invdex = getNestedInventories().indexOf(event.getInventory());
        if (invdex < 0) return;
        event.setCancelled(true);
        Player  player     = (Player) event.getWhoClicked();
        int     slot       = event.getSlot();
        int     size       = getNestedInventories().size();
        boolean hasChanges = false;

        switch (event.getSlot()) {
            case 4 -> FusionEditorCommand.suggestUsage(player, EditorCriteria.Browse_Add_Profession, "/fusion-editor ");
            case 48 -> open(player, (size + invdex - 1) % size);
            case 50 -> open(player, (invdex + 1) % size);
            case 53 -> openParent(player);
            default -> {
                if (slots.containsKey(getNestedInventories().get(invdex))
                        && slots.get(getNestedInventories().get(invdex)).containsKey(slot)) {
                    ProfessionConditions entry = slots.get(getNestedInventories().get(invdex)).get(slot);
                    if (!event.isShiftClick()) {
                        if (event.isLeftClick()) {
                            browseProfessionEditor = new BrowseProfessionEditor(this, player, entry);
                            browseProfessionEditor.open(player);
                        } else if (event.isRightClick()) {
                            browseEditor.getProfessions().remove(entry.getProfession());
                            browseEditor.getProfessionConditions().remove(entry.getProfession());
                            hasChanges = true;
                        }
                    } else {
                        ProfessionConditions conditions =
                                browseEditor.getProfessionConditions().get(entry.getProfession());
                        if (event.isLeftClick()) {
                            // Put the Recipe one more to the left in the Map
                            browseEditor.moveEntry(conditions, -1);
                            hasChanges = true;
                        } else if (event.isRightClick()) {
                            // Put the Recipe one more to the right in the Map
                            browseEditor.moveEntry(conditions, 1);
                            hasChanges = true;
                        }
                    }
                }
            }
        }

        if (hasChanges) {
            reload(true);
        }
    }

    public void reload(boolean open) {
        setIcons(EditorRegistry.getBrowseProfessionCfg().getIcons(browseEditor));
        initialize();
        if (open)
            open(player);
    }
}
