package studio.magemonkey.fusion.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.codex.legacy.item.ItemBuilder;
import studio.magemonkey.codex.util.messages.MessageData;
import studio.magemonkey.codex.util.messages.MessageUtil;
import studio.magemonkey.fusion.CraftingTable;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.Profession;
import studio.magemonkey.fusion.Utils;
import studio.magemonkey.fusion.cfg.BrowseConfig;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.cfg.player.FusionPlayer;
import studio.magemonkey.fusion.cfg.player.PlayerLoader;
import studio.magemonkey.fusion.gui.slot.Slot;
import studio.magemonkey.fusion.util.PlayerUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BrowseGUI implements Listener {

    private static final HashMap<UUID, BrowseGUI> map = new HashMap<>();


    private Inventory inventory;
    private final Map<Integer, String> slotMap = new HashMap<>();
    protected final String inventoryName;
    private final Player player;
    private final UUID opener;

    private final ArrayList<Integer> slots = new ArrayList<>();

    private final ItemStack fillItem;

    public BrowseGUI(String inventoryName, Player player, ItemStack fill) {
        this.inventoryName = inventoryName;
        this.player = player;
        this.opener = player.getUniqueId();
        if (fill == null)
            fill = BrowseConfig.getBrowseFill();
        this.fillItem = fill;
        initialize();
        Bukkit.getServer().getPluginManager().registerEvents(this, Fusion.getInstance());
    }

    public static BrowseGUI open(Player player) {
        BrowseGUI gui = map.get(player.getUniqueId());
        if (gui == null) {
            gui = new BrowseGUI(BrowseConfig.getBrowseName(), player, null);
        } else {
            player.openInventory(gui.inventory);
        }
        return gui;
    }

    private void initialize() {
        map.remove(player.getUniqueId());
        int phSlot = -1;
        for (String row : BrowseConfig.getBrowsePattern().getPattern()) {
            for (char c : row.toCharArray()) {
                phSlot++;
                switch (c) {
                    case '=':
                    case 'o':
                        this.slots.add(phSlot);
                        break;
                }
            }
        }

        inventory = null;
        String title = ChatColor.translateAlternateColorCodes('&', BrowseConfig.getBrowseName());
        try {
            inventory = Bukkit.createInventory(player, BrowseConfig.getBrowsePattern().getPattern().length * 9, title);
            int i = 0;
            int k = this.slots.get(i);

            HashMap<Character, ItemStack> specItems = BrowseConfig.getBrowsePattern().getItems();
            int slot = 0;
            for (String pat : BrowseConfig.getBrowsePattern().getPattern()) {
                for (char c : pat.toCharArray()) {
                    if (specItems.containsKey(c))
                        inventory.setItem(slot, specItems.get(c));
                    slot++;
                }
            }

//            for (CraftingTable table : Cfg.getMap().values()) {
            for (String profession : BrowseConfig.getProfessions()) {
                if (i >= this.slots.size()) break;
                CraftingTable table = ProfessionsCfg.getTable(profession);
                // Removes items from the menu if the player doesn't have permission.
                if (table == null || !Utils.hasCraftingUsePermission(player, table.getName().toLowerCase()))
                    continue;

                ItemStack item = table.getIconItem() != null ? table.getIconItem().create()
                        : ItemBuilder.newItem(Material.BEDROCK)
                        .name(ChatColor.RED + table.getName())
                        .newLoreLine(ChatColor.RED + "Missing icon in config.")
                        .newLoreLine(ChatColor.RED + "Add 'icon: econ-item' under the profession.")
                        .build();

                inventory.setItem(k, item);
                this.slotMap.put(k, table.getName());

                k = this.slots.get(++i);
            }

            for (int j = i; j < this.slots.size(); j++) {
                inventory.setItem(this.slots.get(j), this.fillItem);
            }

            map.put(player.getUniqueId(), this);
            player.openInventory(inventory);

        } catch (Exception e) {
            if (inventory != null) {
                inventory.clear();
            }
            map.remove(player.getUniqueId());
            player.closeInventory();
            throw new RuntimeException("Exception was thrown on gui open for: " + player.getName(), e);
        }
    }

    private boolean isThis(InventoryView inv, Player player) {
        return inventoryName != null && inv.getTitle()
                .equals(ChatColor.translateAlternateColorCodes('&', this.inventoryName)) &&
                this.opener != null && this.opener.equals(player.getUniqueId());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent e) {
        Inventory clickedInv = e.getClickedInventory();
        if(clickedInv == null) return;
        if(!clickedInv.equals(inventory)) return;
        if (e.getRawSlot() < 0) {
            return;
        }

        Player p = (Player) e.getWhoClicked();
        e.setCancelled(true);

        CustomGUI guiToOpen = ProfessionsCfg.getGUI(this.slotMap.get(e.getRawSlot()));
        if (guiToOpen == null) return;

        String profession = guiToOpen.getName();
        FusionPlayer fusionPlayer = PlayerLoader.getPlayer(p.getUniqueId());

        int unlocked = fusionPlayer.getUnlockedProfessions().size();
        int allowed = PlayerUtil.getPermOption(p, "fusion.limit"); //Set the max number of unlockable professions.
        int cost = BrowseConfig.getProfCost(profession);

        MessageData[] data = {
                new MessageData("profession", profession),
                new MessageData("unlocked", unlocked),
                new MessageData("limit", allowed),
                new MessageData("cost", cost),
                new MessageData("bal", CodexEngine.get().getVault().getBalance(p))
        };

        if (fusionPlayer.hasProfession(profession)) {
            p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
            MessageUtil.sendMessage("fusion.error.profAlreadyUnlocked", p, data);
            return;
        }

        if (unlocked >= allowed) {
            p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
            MessageUtil.sendMessage("fusion.error.limitReached", p, data);
            return;
        }

        if (cost > 0 && !CodexEngine.get().getVault().canPay(p, cost)) {
            p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
            MessageUtil.sendMessage("fusion.error.profNoFunds", p, data);
            return;
        }

        fusionPlayer.addProfession(new Profession(-1, p.getUniqueId(), profession, 0, false, true));
        if (cost > 0)
            CodexEngine.get().getVault().take(p, cost);
        data[1] = new MessageData("unlocked", unlocked + 1);
        MessageUtil.sendMessage("fusion.unlockedProfession", p, data);
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);

//
//        PlayerInitialGUI.open(guiToOpen, p);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) {
            return;
        }
        if (this.isThis(e.getView(), (Player) e.getWhoClicked())) {
            if (e.getOldCursor().getType() == Material.BARRIER)
                e.setCancelled(true);
            if (e.getRawSlots().stream().anyMatch(this.slots::contains)) {
                e.setResult(Event.Result.DENY);
                return;
            }

            if (e.getNewItems().values().stream().anyMatch(i -> Slot.SPECIAL_CRAFTING_SLOT.canHoldItem(i) == null)) {
                e.setResult(Event.Result.DENY);
            }
        }
    }

    @EventHandler
    public void drop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        player.getOpenInventory();
        if (isThis(player.getOpenInventory(), event.getPlayer())) {
            ItemStack stack = event.getItemDrop().getItemStack();
            if (stack.getType() == Material.BARRIER) {
                event.getItemDrop().remove();
                if (player.getOpenInventory().getCursor() == null
                        || player.getOpenInventory().getCursor().getType() == Material.AIR)
                    player.getOpenInventory().setCursor(stack);
            }

        }
    }

    @EventHandler
    public void close(InventoryCloseEvent event) {
        if (isThis(event.getView(), (Player) event.getPlayer()))
            map.remove(event.getPlayer().getUniqueId());
    }

    public static void closeAll() {
        for (UUID id : map.keySet()) {
            Player target = Bukkit.getPlayer(id);
            if (target != null && target.isOnline())
                target.closeInventory();
        }

        map.clear();
    }
}
