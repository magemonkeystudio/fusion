package com.gotofinal.darkrise.crafting;

import com.gotofinal.darkrise.crafting.cfg.Cfg;
import com.gotofinal.darkrise.crafting.cfg.PConfigManager;
import com.gotofinal.darkrise.crafting.cfg.PlayerConfig;
import com.gotofinal.darkrise.crafting.gui.BrowseGUI;
import com.gotofinal.darkrise.crafting.gui.CustomGUI;
import com.gotofinal.darkrise.crafting.gui.PlayerInitialGUI;
import me.travja.darkrise.core.legacy.util.Vault;
import me.travja.darkrise.core.legacy.util.message.MessageData;
import me.travja.darkrise.core.legacy.util.message.MessageUtil;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class Commands implements CommandExecutor {

    private Map<String, ConfirmationAction> confirmation = new HashMap<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        ProRPGCrafting instance = ProRPGCrafting.getInstance();
        if ((args.length == 2) || (args.length == 3)) {
            if (args[0].equalsIgnoreCase("use")) {
                if (!Utils.hasCraftingUsePermission(sender, null)) {
                    return true;
                }
                CustomGUI eq = Cfg.getGuiMap().get(args[1]);
                if (eq == null) {
                    MessageUtil.sendMessage("crafting.notACrafting", sender, new MessageData("name", args[1]), new MessageData("sender", sender));
                    return true;
                }
                if (args.length == 3) {
                    if (!instance.checkPermission(sender, "crafting.admin.use")) {
                        return true;
                    }
                    Player target = Bukkit.getPlayer(args[2]);
                    if (target == null) {
                        MessageUtil.sendMessage("notAPlayer", sender, new MessageData("name", args[2]), new MessageData("sender", sender));
                        return true;
                    }

                    //TODO ?? Make sure they have unlocked this crafting menu

                    PlayerInitialGUI.open(eq, target);
                    MessageUtil.sendMessage("crafting.useConfirmOther", sender, new MessageData("craftingInventory", eq), new MessageData("sender", sender),
                            new MessageData("target", target));
                    return true;
                } else {
                    if (sender instanceof Player) {
                        if (!Utils.hasCraftingUsePermission(sender, eq.getName())) {
                            return true;
                        }
                        //Make sure they have unlocked this crafting menu
                        if (!PConfigManager.getPlayerConfig((Player) sender).hasProfession(eq.getName())) {
                            MessageUtil.sendMessage("crafting.error.notUnlocked", sender);
                            return true;
                        }

                        PlayerInitialGUI.open(eq, Bukkit.getPlayer(((Player) sender).getUniqueId()));

                        MessageUtil.sendMessage("crafting.useConfirm", sender, new MessageData("craftingInventory", eq), new MessageData("player", sender));
                        return true;
                    } else {
                        MessageUtil.sendMessage("crafting.help", sender, new MessageData("sender", sender),
                                new MessageData("text", label + " " + StringUtils.join(args, ' ')));
                    }
                }
            } else if (args[0].equalsIgnoreCase("master")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    String guiName = args[1];
                    CraftingTable table = Cfg.getTable(guiName);
                    if (table == null) {
                        MessageUtil.sendMessage("crafting.notACrafting", sender, new MessageData("name", args[1]), new MessageData("sender", sender));
                        return true;
                    }

                    if (PConfigManager.hasMastery(player, table.getName())) {
                        MessageUtil.sendMessage("crafting.error.alreadyMastered", sender, new MessageData("sender", sender), new MessageData("craftingTable", table));
                        return true;
                    }

                    if (LevelFunction.getLevel(player, table) < table.getMasteryUnlock()) {
                        MessageUtil.sendMessage("crafting.error.noMasteryLevel", sender, new MessageData("sender", sender), new MessageData("craftingTable", table));
                        return true;
                    }

                    if (!Vault.canPay(player, table.getMasteryFee())) {
                        MessageUtil.sendMessage("crafting.error.noMasteryFunds", sender, new MessageData("sender", sender), new MessageData("craftingTable", table));
                        return true;
                    }

                    PConfigManager.getPlayerConfig(player).setHasMastery(table.getName(), true);
                    MessageUtil.sendMessage("crafting.mastered", sender, new MessageData("sender", sender), new MessageData("craftingTable", table));
                    return true;
                } else {
                    MessageUtil.sendMessage("crafting.help", sender, new MessageData("sender", sender),
                            new MessageData("text", label + " " + StringUtils.join(args, ' ')));
                }
            } else if (args[0].equalsIgnoreCase("forget")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    CraftingTable table = Cfg.getTable(args[1]);
                    if (table == null) {
                        MessageUtil.sendMessage("crafting.notACrafting", sender, new MessageData("name", args[1]), new MessageData("sender", sender));
                        return true;
                    }
                    ConfirmationAction action = () -> {
                        PlayerConfig conf = PConfigManager.getPlayerConfig(player);
                        conf.removeProfession(table.getName());
                        MessageUtil.sendMessage("crafting.forgotten", sender, new MessageData("sender", sender), new MessageData("craftingTable", table));
                    };

                    confirmation.put(player.getUniqueId().toString(), action);
                    MessageUtil.sendMessage("crafting.forget.confirm", sender, new MessageData("sender", sender), new MessageData("craftingTable", table));

                    Bukkit.getScheduler().runTaskLater(ProRPGCrafting.getInstance(),
                            () -> confirmation.remove(player.getUniqueId().toString()), 15 * 20l);

                    return true;
                } else {
                    MessageUtil.sendMessage("crafting.help", sender, new MessageData("sender", sender),
                            new MessageData("text", label + " " + StringUtils.join(args, ' ')));
                }
            }
        } else if (args.length == 1) {
            if (args[0].equalsIgnoreCase("browse")) {
                if (!(sender instanceof Player)) {
                    MessageUtil.sendMessage("senderIsNotPlayer", sender, new MessageData("sender", sender));
                    return true;
                }
                Player player = (Player) sender;

                BrowseGUI.open(player);
                return true;
            } else if (args[0].equalsIgnoreCase("level")) {
                if (!(sender instanceof Player)) {
                    MessageUtil.sendMessage("senderIsNotPlayer", sender, new MessageData("sender", sender));
                    return true;
                }
                for (Map.Entry<String, CraftingTable> entry : Cfg.getMap().entrySet()) {
                    MessageUtil.sendMessage("crafting.level.format", sender,
                            new MessageData("category", entry.getValue().getName()),
                            new MessageData("level", LevelFunction.getLevel((Player) sender, entry.getValue())),
                            new MessageData("experience", ProRPGCrafting.getExperienceManager().getExperience((Player) sender, entry.getValue())));
                }

                return true;
            } else if (args[0].equalsIgnoreCase("auto")) {
                if (!(sender instanceof Player)) {
                    MessageUtil.sendMessage("senderIsNotPlayer", sender, new MessageData("sender", sender));
                    return true;
                }
                if (!instance.checkPermission(sender, "crafting.auto")) {
                    return true;
                }
                Player player = (Player) sender;

                boolean autoOn = PConfigManager.getPlayerConfig(player).isAutoCraft();

                PConfigManager.getPlayerConfig(player).setAutoCraft((autoOn = !autoOn));

                MessageUtil.sendMessage("crafting.autoToggle", player, new MessageData("state", autoOn ? "on" : "off"));

                return true;
            } else if (args[0].equalsIgnoreCase("confirm")) {
                String id = sender instanceof Player ? ((Player) sender).getUniqueId().toString() : "console";

                if (confirmation.containsKey(id)) {
                    confirmation.get(id).doAction();
                    confirmation.remove(id);
                } else {
                    MessageUtil.sendMessage("crafting.nothingToConfirm", sender, new MessageData("sender", sender));
                }

                return true;
            } else if (args[0].equalsIgnoreCase("reload")) {
                if (!instance.checkPermission(sender, "crafting.reload")) {
                    return true;
                }
                instance.closeAll();
                instance.reloadConfig();
                instance.reloadLang();
                MessageUtil.sendMessage("crafting.reload", sender, new MessageData("sender", sender));
                return true;
            }
        }
        MessageUtil.sendMessage("crafting.help", sender, new MessageData("sender", sender),
                new MessageData("text", label + " " + StringUtils.join(args, ' ')));
        return true;
    }


    public interface ConfirmationAction {
        void doAction();
    }
}
