package com.gotofinal.darkrise.crafting;

import com.gotofinal.darkrise.crafting.cfg.Cfg;
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

import java.util.Map;

public class Commands implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        DarkRiseCrafting instance = DarkRiseCrafting.getInstance();
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
                    PlayerInitialGUI.open(eq, target);
                    MessageUtil.sendMessage("crafting.useConfirmOther", sender, new MessageData("craftingInventory", eq), new MessageData("sender", sender),
                            new MessageData("target", target));
                    return true;
                } else {
                    if (sender instanceof Player) {
                        if (!Utils.hasCraftingUsePermission(sender, eq.getName())) {
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
            } else if (args[0].equals("master")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    String guiName = args[1];
                    CraftingTable table = Cfg.getTable(guiName);
                    if (table == null) {
                        MessageUtil.sendMessage("crafting.notACrafting", sender, new MessageData("name", args[1]), new MessageData("sender", sender));
                        return true;
                    }

                    if(MasteryManager.hasMastery(player, table.getName())) {
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

                    MasteryManager.getPlayerConfig(player).setHasMastery(table.getName(), true);
                    MessageUtil.sendMessage("crafting.mastered", sender, new MessageData("sender", sender), new MessageData("craftingTable", table));
                    return true;
                } else {
                    MessageUtil.sendMessage("crafting.help", sender, new MessageData("sender", sender),
                            new MessageData("text", label + " " + StringUtils.join(args, ' ')));
                }
            }
        } else if ((args.length == 1) && args[0].equalsIgnoreCase("reload")) {
            if (!instance.checkPermission(sender, "crafting.reload")) {
                return true;
            }
            instance.closeAll();
            instance.reloadConfig();
            instance.reloadLang();
            MessageUtil.sendMessage("crafting.reload", sender, new MessageData("sender", sender));
            return true;
        } else if (args.length == 1 && args[0].equalsIgnoreCase("level")) {
            if (!(sender instanceof Player)) {
                MessageUtil.sendMessage("senderIsNotPlayer", sender, new MessageData("sender", sender));
                return true;
            }
            for (Map.Entry<String, CraftingTable> entry : Cfg.getMap().entrySet()) {
                MessageUtil.sendMessage("crafting.level.format", sender,
                        new MessageData("category", entry.getValue().getName()),
                        new MessageData("level", LevelFunction.getLevel((Player) sender, entry.getValue())),
                        new MessageData("experience", DarkRiseCrafting.getExperienceManager().getExperience((Player) sender, entry.getValue())));
            }

            return true;
        } else if(args.length == 1 && args[0].equalsIgnoreCase("auto")) {
            if (!(sender instanceof Player)) {
                MessageUtil.sendMessage("senderIsNotPlayer", sender, new MessageData("sender", sender));
                return true;
            }
            if (!instance.checkPermission(sender, "crafting.reload")) {
                return true;
            }
            Player player = (Player) sender;

            boolean autoOn = MasteryManager.getPlayerConfig(player).isAutoCraft();

            MasteryManager.getPlayerConfig(player).setAutoCraft((autoOn = !autoOn));

            MessageUtil.sendMessage("crafting.autoToggle", player, new MessageData("state", autoOn ? "on" : "off"));

            return true;
        }
        MessageUtil.sendMessage("crafting.help", sender, new MessageData("sender", sender),
                new MessageData("text", label + " " + StringUtils.join(args, ' ')));
        return true;
    }
}
