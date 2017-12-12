package com.gotofinal.darkrise.crafting;

import com.gotofinal.darkrise.crafting.cfg.Cfg;
import com.gotofinal.darkrise.crafting.gui.CustomGUI;
import com.gotofinal.darkrise.crafting.gui.PlayerInitialGUI;
import com.gotofinal.messages.api.messages.Message.MessageData;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Commands implements CommandExecutor
{
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        DarkRiseCrafting instance = DarkRiseCrafting.getInstance();
        if ((args.length == 2) || (args.length == 3))
        {
            if (args[0].equalsIgnoreCase("use"))
            {
                if (! Utils.hasCraftingUsePermission(sender, null))
                {
                    return true;
                }
                CustomGUI eq = Cfg.getGuiMap().get(args[1]);
                if (eq == null)
                {
                    return instance.sendMessage("crafting.notACrafting", sender, new MessageData("name", args[1]), new MessageData("sender", sender));
                }
                if (args.length == 3)
                {
                    if (! instance.checkPermission(sender, "crafting.admin.use"))
                    {
                        return true;
                    }
                    Player target = Bukkit.getPlayer(args[2]);
                    if (target == null)
                    {
                        return instance.sendMessage("notAPlayer", sender, new MessageData("name", args[2]), new MessageData("sender", sender));
                    }
                    PlayerInitialGUI.open(eq, target);
                    return instance.sendMessage("crafting.useConfirmOther", sender, new MessageData("craftingInventory", eq), new MessageData("sender", sender),
                                                new MessageData("target", target));
                }
                else
                {
                    if (sender instanceof Player)
                    {
                        if (! Utils.hasCraftingUsePermission(sender, eq.getName()))
                        {
                            return true;
                        }
                        PlayerInitialGUI.open(eq, Bukkit.getPlayer(((Player) sender).getUniqueId()));
                        return instance.sendMessage("crafting.useConfirm", sender, new MessageData("craftingInventory", eq), new MessageData("player", sender));
                    }
                    else
                    {
                        instance.sendMessage("crafting.help", sender, new MessageData("sender", sender),
                                             new MessageData("text", label + " " + StringUtils.join(args, ' ')));
                    }
                }
            }
        }
        if ((args.length == 1) && args[0].equalsIgnoreCase("reload"))
        {
            if (! instance.checkPermission(sender, "crafting.reload"))
            {
                return true;
            }
            instance.closeAll();
            instance.reloadConfigs();
            instance.reloadMessages();
            return instance.sendMessage("reload", sender, new MessageData("sender", sender));
        }
        return instance.sendMessage("crafting.help", sender, new MessageData("sender", sender),
                                    new MessageData("text", label + " " + StringUtils.join(args, ' ')));
    }
}