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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.codex.legacy.item.ItemBuilder;
import studio.magemonkey.codex.util.messages.MessageData;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.api.FusionAPI;
import studio.magemonkey.fusion.cfg.BrowseConfig;
import studio.magemonkey.fusion.cfg.Cfg;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.data.player.FusionPlayer;
import studio.magemonkey.fusion.data.player.PlayerLoader;
import studio.magemonkey.fusion.data.professions.CalculatedProfession;
import studio.magemonkey.fusion.data.professions.ProfessionConditions;
import studio.magemonkey.fusion.data.recipes.CalculatedRecipe;
import studio.magemonkey.fusion.data.recipes.CraftingTable;
import studio.magemonkey.fusion.data.recipes.RecipeItem;
import studio.magemonkey.fusion.gui.slot.Slot;
import studio.magemonkey.fusion.util.ChatUT;
import studio.magemonkey.fusion.util.PlayerUtil;
import studio.magemonkey.fusion.util.Utils;

import java.util.*;

public class BrowseGUI implements Listener {

    private static final HashMap<UUID, BrowseGUI> map = new HashMap<>();


    private         Inventory                          inventory;
    private final   Map<Integer, String>               slotMap                 = new HashMap<>();
    private final   Map<Integer, CalculatedProfession> calculatedProfessionMap = new HashMap<>();
    protected final String                             inventoryName;
    private final   Player                             player;

    private final ArrayList<Integer> slots = new ArrayList<>();

    private final ItemStack fillItem;

    public BrowseGUI(String inventoryName, Player player, ItemStack fill) {
        this.inventoryName = inventoryName;
        this.player = player;
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
            int                           slot      = 0;
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


                Collection<ItemStack> playerItems = PlayerUtil.getPlayerItems(player);
                CalculatedProfession calculatedProfession =
                        CalculatedProfession.create(BrowseConfig.getProfessionConditions(table.getName()),
                                playerItems,
                                player,
                                table);
                calculatedProfessionMap.put(k, calculatedProfession);

                ItemStack item;
                if (Cfg.showRequirementsOnBrowse) {
                    item = calculatedProfession.getIcon();
                } else {
                    item = table.getIconItem() != null ? table.getIconItem().create()
                            : ItemBuilder.newItem(Material.BEDROCK)
                                    .name(ChatColor.RED + table.getName())
                                    .newLoreLine(ChatColor.RED + "Missing icon in config.")
                                    .newLoreLine(ChatColor.RED + "Add 'icon: econ-item' under the profession.")
                                    .build();
                }

                // Set the profession name manually
                ItemMeta meta = item.getItemMeta();
                if (meta != null)
                    meta.setDisplayName(ChatUT.hexString(table.getInventoryName()));
                item.setItemMeta(meta);

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

    public static void joinProfession(Player player, ProfessionGuiRegistry gui) {
        if (gui == null) return;

        String       profession   = gui.getProfession();
        FusionPlayer fusionPlayer = PlayerLoader.getPlayer(player.getUniqueId());

        ProfessionConditions conditions = BrowseConfig.getProfessionConditions(profession);

        if (conditions == null) {
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
            CodexEngine.get()
                    .getMessageUtil()
                    .sendMessage("fusion.error.profNotAvailable", player, new MessageData("profession", profession));
            return;
        }

        if (!conditions.isValid(fusionPlayer))
            return;

        double moneyCost = conditions.getMoneyCost();
        int    expCost   = conditions.getExpCost();

        Collection<ItemStack> itemsToTake =
                new ArrayList<>(conditions.getRequiredItems().stream().map(RecipeItem::getItemStack).toList());
        Collection<ItemStack> taken     = new ArrayList<>(itemsToTake.size());
        PlayerInventory       inventory = player.getInventory();

        for (Iterator<ItemStack> iterator = itemsToTake.iterator(); iterator.hasNext(); ) {
            ItemStack toTake = iterator.next();
            for (ItemStack entry : PlayerUtil.getPlayerItems(player)) {
                ItemStack item = entry.clone();
                entry = entry.clone();
                if (item.hasItemMeta() && Objects.requireNonNull(item.getItemMeta()).hasLore()) {
                    item = item.clone();
                    entry.setAmount(toTake.getAmount());
                    if (CalculatedRecipe.isSimilar(toTake, item)) {
                        toTake = entry;
                        break;
                    }
                }
            }

            HashMap<Integer, ItemStack> notRemoved = inventory.removeItem(toTake);
            if (notRemoved.isEmpty()) {
                taken.add(toTake);
                iterator.remove();
                continue;
            }
            for (ItemStack itemStack : taken) {
                HashMap<Integer, ItemStack> notAdded = inventory.addItem(itemStack);
                if (notAdded.isEmpty()) {
                    break;
                }
                for (ItemStack stack : notAdded.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), stack);
                }
            }
            break;
        }

        if (!itemsToTake.isEmpty()) {
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
            CodexEngine.get()
                    .getMessageUtil()
                    .sendMessage("fusion.error.insufficientItems", player, new MessageData("profession", profession));
            return;
        }

        FusionAPI.getEventServices().getProfessionService().joinProfession(profession, player, moneyCost, expCost);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent e) {
        Inventory clickedInv = e.getClickedInventory();
        if (clickedInv == null) return;
        if (!clickedInv.equals(inventory)) return;
        if (e.getRawSlot() < 0) {
            return;
        }

        Player p = (Player) e.getWhoClicked();
        e.setCancelled(true);

        ProfessionGuiRegistry gui = ProfessionsCfg.getGUI(slotMap.get(e.getRawSlot()));
        joinProfession(p, gui);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) {
            return;
        }
        if (e.getInventory().equals(inventory)) {
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
        if (this.inventory.getViewers().contains(player)) {
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
        if (event.getInventory().equals(inventory))
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
