package studio.magemonkey.fusion.gui.editors.browse;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.cfg.editors.EditorRegistry;
import studio.magemonkey.fusion.cfg.professions.ProfessionConditions;
import studio.magemonkey.fusion.commands.EditorCriteria;
import studio.magemonkey.fusion.commands.FusionEditorCommand;
import studio.magemonkey.fusion.gui.editors.Editor;
import studio.magemonkey.fusion.util.InventoryUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class BrowseProfessionsEditor extends Editor implements Listener {

    private final Player player;
    private final BrowseEditor browseEditor;

    private BrowseProfessionEditor browseProfessionEditor;

    private final HashMap<Inventory, HashMap<Integer, ProfessionConditions>> slots = new HashMap<>();

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
        getNestedInventories().clear();

        HashMap<Integer, ProfessionConditions> invSlots = new HashMap<>();
        List<Inventory> inventories = new ArrayList<>();
        Inventory inv = null;
        int invSlot = 0;
        Collection<ProfessionConditions> professions = browseEditor.getProfessionConditions().values();
        for (ProfessionConditions entry : professions) {
            if(!ProfessionsCfg.getMap().containsKey(entry.getProfession())) {
                Fusion.getInstance().getLogger().warning("Profession " + entry.getProfession() + " not found for BrowseEditor. You might want to remove it from browse.yml to avoid problems.");
                Fusion.getInstance().getLogger().warning("Skipping profession: " + entry.getProfession());
                continue;
            }
            int invDex = invSlot % 36 + 9;
            if (invDex == 9) {
                if (inv != null)
                    inventories.add(inv);
                inv = InventoryUtils.createFilledInventory(null, EditorRegistry.getBrowseProfessionCfg().getTitle(), 54, getIcons().get("fill"));
                inv.setItem(4, getIcons().get("add"));
                inv.setItem(48, getIcons().get("previous"));
                inv.setItem(50, getIcons().get("next"));
                inv.setItem(53, getIcons().get("back"));
                invSlots = new HashMap<>();
            }
            inv.setItem(invDex, EditorRegistry.getBrowseProfessionCfg().getProfessionIcon(entry));
            invSlots.put(invDex, entry);
            slots.put(inv, invSlots);
            invSlot++;
        }
        if(inv == null) {
            inv = InventoryUtils.createFilledInventory(null, EditorRegistry.getBrowseProfessionCfg().getTitle(), 54, getIcons().get("fill"));
            inv.setItem(4, getIcons().get("add"));
            inv.setItem(48, getIcons().get("previous"));
            inv.setItem(50, getIcons().get("next"));
            inv.setItem(53, getIcons().get("back"));
        }
        inventories.add(inv);
        setNestedInventories(inventories);
    }

    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int page) {
        player.openInventory(getNestedInventories().get(page) != null ? getNestedInventories().get(page) : getNestedInventories().get(page - 1));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        int invdex = getNestedInventories().indexOf(event.getInventory());
        if (invdex < 0) return;
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();
        int size = getNestedInventories().size();
        boolean hasChanges = false;

        switch (event.getSlot()) {
            case 4 -> FusionEditorCommand.suggestUsage(player, EditorCriteria.Browse_Add_Profession, "/fusion-editor ");
            case 48 -> open(player, (size + invdex - 1) % size);
            case 50 -> open(player, (invdex + 1) % size);
            case 53 -> openParent(player);
            default -> {
                if (slots.containsKey(getNestedInventories().get(invdex)) && slots.get(getNestedInventories().get(invdex)).containsKey(slot)) {
                    ProfessionConditions entry = slots.get(getNestedInventories().get(invdex)).get(slot);
                    if (event.isLeftClick()) {
                        browseProfessionEditor = new BrowseProfessionEditor(this, player, entry);
                        browseProfessionEditor.open(player);
                    } else if (event.isRightClick()) {
                        browseEditor.getProfessions().remove(entry.getProfession());
                        browseEditor.getProfessionConditions().remove(entry.getProfession());
                        hasChanges = true;
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
