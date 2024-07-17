package studio.magemonkey.fusion;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.codex.util.messages.MessageData;
import studio.magemonkey.codex.util.messages.MessageUtil;
import studio.magemonkey.fusion.cfg.Cfg;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.cfg.player.PlayerLoader;
import studio.magemonkey.fusion.cfg.sql.DatabaseType;
import studio.magemonkey.fusion.cfg.sql.SQLManager;
import studio.magemonkey.fusion.gui.BrowseGUI;
import studio.magemonkey.fusion.gui.CustomGUI;
import studio.magemonkey.fusion.gui.PlayerInitialGUI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Commands implements CommandExecutor, TabCompleter {

    private final Map<String, ConfirmationAction> confirmation = new HashMap<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Fusion instance = Fusion.getInstance();
        if ((args.length == 2) || (args.length == 3)) {
            if (args[0].equalsIgnoreCase("use")) {
                if (!Utils.hasCraftingUsePermission(sender, null)) {
                    return true;
                }
                CustomGUI eq = ProfessionsCfg.getGuiMap().get(args[1]);
                if (eq == null) {
                    MessageUtil.sendMessage("fusion.notACrafting",
                            sender,
                            new MessageData("name", args[1]),
                            new MessageData("sender", sender));
                    return true;
                }
                if (args.length == 3) {
                    if (!instance.checkPermission(sender, "fusion.admin.use")) {
                        return true;
                    }
                    Player target = Bukkit.getPlayer(args[2]);
                    if (target == null) {
                        MessageUtil.sendMessage("notAPlayer",
                                sender,
                                new MessageData("name", args[2]),
                                new MessageData("sender", sender));
                        return true;
                    }

                    //TODO ?? Make sure they have unlocked this crafting menu

                    PlayerInitialGUI.open(eq, target);
                    MessageUtil.sendMessage("fusion.useConfirmOther",
                            sender,
                            new MessageData("craftingInventory", eq),
                            new MessageData("sender", sender),
                            new MessageData("target", target));
                    return true;
                } else {
                    if (sender instanceof Player) {
                        if (!Utils.hasCraftingUsePermission(sender, eq.getName())) {
                            return true;
                        }
                        //Make sure they have unlocked this crafting menu
                        if (!PlayerLoader.getPlayer(((Player) sender).getUniqueId()).hasProfession(eq.getName())) {
                            MessageUtil.sendMessage("fusion.error.notUnlocked", sender);
                            return true;
                        }

                        PlayerInitialGUI.open(eq, Bukkit.getPlayer(((Player) sender).getUniqueId()));

                        MessageUtil.sendMessage("fusion.useConfirm",
                                sender,
                                new MessageData("craftingInventory", eq),
                                new MessageData("player", sender));
                        return true;
                    } else {
                        MessageUtil.sendMessage("fusion.help", sender, new MessageData("sender", sender),
                                new MessageData("text", label + " " + StringUtils.join(args, ' ')));
                    }
                }
            } else if (args[0].equalsIgnoreCase("master")) {
                if (sender instanceof Player) {
                    Player        player  = (Player) sender;
                    String        guiName = args[1];
                    CraftingTable table   = ProfessionsCfg.getTable(guiName);
                    if (table == null) {
                        MessageUtil.sendMessage("fusion.notACrafting",
                                sender,
                                new MessageData("name", args[1]),
                                new MessageData("sender", sender));
                        return true;
                    }

                    if (PlayerLoader.getPlayer(((Player) sender).getUniqueId()).hasMastered(table.getName())) {
                        MessageUtil.sendMessage("fusion.error.alreadyMastered",
                                sender,
                                new MessageData("sender", sender),
                                new MessageData("craftingTable", table));
                        return true;
                    }

                    if (LevelFunction.getLevel(player, table) < table.getMasteryUnlock()) {
                        MessageUtil.sendMessage("fusion.error.noMasteryLevel",
                                sender,
                                new MessageData("sender", sender),
                                new MessageData("craftingTable", table));
                        return true;
                    }

                    if (!CodexEngine.get().getVault().canPay(player, table.getMasteryFee())) {
                        MessageUtil.sendMessage("fusion.error.noMasteryFunds",
                                sender,
                                new MessageData("sender", sender),
                                new MessageData("craftingTable", table));
                        return true;
                    }

                    PlayerLoader.getPlayer(((Player) sender).getUniqueId()).setMastered(table.getName(), true);
                    MessageUtil.sendMessage("fusion.mastered",
                            sender,
                            new MessageData("sender", sender),
                            new MessageData("craftingTable", table));
                    return true;
                } else {
                    MessageUtil.sendMessage("fusion.help", sender, new MessageData("sender", sender),
                            new MessageData("text", label + " " + StringUtils.join(args, ' ')));
                }
            } else if (args[0].equalsIgnoreCase("forget")) {
                if (sender instanceof Player) {
                    Player        player = (Player) sender;
                    CraftingTable table  = ProfessionsCfg.getTable(args[1]);
                    if (table == null) {
                        MessageUtil.sendMessage("fusion.notACrafting",
                                sender,
                                new MessageData("name", args[1]),
                                new MessageData("sender", sender));
                        return true;
                    }
                    ConfirmationAction action = () -> {
                        PlayerLoader.getPlayer(player.getUniqueId()).removeProfession(table.getName());
                        MessageUtil.sendMessage("fusion.forgotten",
                                sender,
                                new MessageData("sender", sender),
                                new MessageData("craftingTable", table));
                    };

                    confirmation.put(player.getUniqueId().toString(), action);
                    MessageUtil.sendMessage("fusion.forget.confirm",
                            sender,
                            new MessageData("sender", sender),
                            new MessageData("craftingTable", table));

                    Bukkit.getScheduler().runTaskLater(Fusion.getInstance(),
                            () -> confirmation.remove(player.getUniqueId().toString()), 15 * 20L);

                    return true;
                } else {
                    MessageUtil.sendMessage("fusion.help", sender, new MessageData("sender", sender),
                            new MessageData("text", label + " " + StringUtils.join(args, ' ')));
                }
            } else if(args[0].equalsIgnoreCase("storage")) {
                String storage = args[1];
                DatabaseType type = DatabaseType.valueOf(Cfg.getConfig().getString("storage.type", "LOCALE").toUpperCase());
                switch (storage.toLowerCase()) {
                    case "locale":
                        if(type == DatabaseType.LOCALE) {
                            MessageUtil.sendMessage("fusion.error.alreadyUsedStorage", sender, new MessageData("storage", storage));
                            return true;
                        }
                        SQLManager.swapToLocale();
                        MessageUtil.sendMessage("fusion.storageChanged", sender, new MessageData("storage", storage));
                        break;
                    case "sql":
                        if(type == DatabaseType.MARIADB || type == DatabaseType.MYSQL) {
                            MessageUtil.sendMessage("fusion.error.alreadyUsedStorage", sender, new MessageData("storage", storage));
                            return true;
                        }
                        SQLManager.swapToSql();
                        MessageUtil.sendMessage("fusion.storageChanged", sender, new MessageData("storage", storage));
                        break;
                    default:
                        MessageUtil.sendMessage("fusion.error.invalidStorage", sender, new MessageData("storage", storage));
                        break;
                }
                return true;
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
                for (Map.Entry<String, CraftingTable> entry : ProfessionsCfg.getMap().entrySet()) {
                    MessageUtil.sendMessage("fusion.level.format", sender,
                            new MessageData("category", entry.getValue().getName()),
                            new MessageData("level", LevelFunction.getLevel((Player) sender, entry.getValue())),
                            new MessageData("experience",
                                    PlayerLoader.getPlayer(((Player) sender).getUniqueId()).getExperience(entry.getValue())));
                }

                return true;
            } else if (args[0].equalsIgnoreCase("auto")) {
                if (!(sender instanceof Player)) {
                    MessageUtil.sendMessage("senderIsNotPlayer", sender, new MessageData("sender", sender));
                    return true;
                }
                if(Cfg.craftingQueue) {
                    MessageUtil.sendMessage("fusion.error.autoDisabled", sender);
                    return true;
                }
                if (!instance.checkPermission(sender, "fusion.auto")) {
                    return true;
                }
                Player player = (Player) sender;

                boolean autoOn = PlayerLoader.getPlayer(player).isAutoCrafting();

                PlayerLoader.getPlayer(player).setAutoCrafting((autoOn = !autoOn));
                MessageUtil.sendMessage("fusion.autoToggle", player, new MessageData("state", autoOn ? "on" : "off"));

                return true;
            } else if (args[0].equalsIgnoreCase("confirm")) {
                String id = sender instanceof Player ? ((Player) sender).getUniqueId().toString() : "console";

                if (confirmation.containsKey(id)) {
                    confirmation.get(id).doAction();
                    confirmation.remove(id);
                } else {
                    MessageUtil.sendMessage("fusion.nothingToConfirm", sender, new MessageData("sender", sender));
                }

                return true;
            } else if (args[0].equalsIgnoreCase("reload")) {
                if (!instance.checkPermission(sender, "fusion.reload")) {
                    return true;
                }
                instance.closeAll();
                instance.reloadConfig();
                instance.reloadLang();
                MessageUtil.sendMessage("fusion.reload", sender, new MessageData("sender", sender));
                return true;
            }
        }
        MessageUtil.sendMessage("fusion.help", sender, new MessageData("sender", sender),
                new MessageData("text", label + " " + StringUtils.join(args, ' ')));
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> entries = new ArrayList<>();
        if(args.length == 1) {
            if("browse".startsWith(args[0])) entries.add("browse");
            if("level".startsWith(args[0])) entries.add("level");
            if("confirm".startsWith(args[0])) entries.add("confirm");
            if("use".startsWith(args[0])) entries.add("use");
            if("master".startsWith(args[0])) entries.add("master");
            if("forget".startsWith(args[0])) entries.add("forget");
            if(Fusion.getInstance().checkPermission(sender, "fusion.admin") && "storage".startsWith(args[0])) entries.add("storage");
            if(Fusion.getInstance().checkPermission(sender, "fusion.auto") && "auto".startsWith(args[0])) entries.add("auto");
            if(Fusion.getInstance().checkPermission(sender, "fusion.reload") && "reload".startsWith(args[0])) entries.add("reload");
        } else if(args.length == 2) {
            List<Profession> professions = new ArrayList<>(PlayerLoader.getPlayer(((Player) sender).getUniqueId()).getProfessions());
            if(args[0].equalsIgnoreCase("use")) {
                for(String name : professions.stream().map(Profession::getName).collect(Collectors.toList())) {
                    if(name.startsWith(args[1])) entries.add(name);
                }
            } else if(args[0].equalsIgnoreCase("master")) {
                for(String name : professions.stream().filter(Profession::isMastered).map(Profession::getName).collect(Collectors.toList())) {
                    if(name.startsWith(args[1])) entries.add(name);
                }
            } else if(args[0].equalsIgnoreCase("forget")) {
                for(String name : ProfessionsCfg.getMap().keySet()) {
                    if(name.startsWith(args[1])) entries.add(name);
                }
            } else if(args[0].equalsIgnoreCase("storage") && Fusion.getInstance().checkPermission(sender, "fusion.admin")) {
                if("locale".startsWith(args[1])) entries.add("locale");
                if("sql".startsWith(args[1])) entries.add("sql");
            }
        } else if(args.length == 3) {
            if(Fusion.getInstance().checkPermission(sender, "fusion.admin.use") && args[0].equalsIgnoreCase("use")) {
                for(Player player : Bukkit.getOnlinePlayers()) {
                    if(player.getName().startsWith(args[2])) entries.add(player.getName());
                }
            }
        }
        return entries;
    }


    public interface ConfirmationAction {
        void doAction();
    }
}
