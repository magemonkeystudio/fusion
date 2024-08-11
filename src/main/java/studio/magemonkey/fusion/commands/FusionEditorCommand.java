package studio.magemonkey.fusion.commands;

import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.container.Job;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.registry.NamespacedId;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.codex.api.CommandType;
import studio.magemonkey.codex.api.DelayedCommand;
import studio.magemonkey.codex.items.ItemType;
import studio.magemonkey.codex.items.exception.CodexItemException;
import studio.magemonkey.codex.items.exception.MissingItemException;
import studio.magemonkey.codex.items.exception.MissingProviderException;
import studio.magemonkey.codex.util.messages.MessageData;
import studio.magemonkey.codex.util.messages.MessageUtil;
import studio.magemonkey.fabled.Fabled;
import studio.magemonkey.fusion.Category;
import studio.magemonkey.fusion.Recipe;
import studio.magemonkey.fusion.RecipeItem;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.cfg.editors.EditorRegistry;
import studio.magemonkey.fusion.gui.editors.BrowseEditor;
import studio.magemonkey.fusion.gui.editors.Editor;
import studio.magemonkey.fusion.gui.editors.ProfessionEditor;
import studio.magemonkey.fusion.util.ChatUT;
import studio.magemonkey.fusion.util.TabCacher;

import java.util.*;

public class FusionEditorCommand implements CommandExecutor, TabCompleter {

    private static Map<UUID, EditorCriteria> editorCriteria = new HashMap<>();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player))
            return true;
        Player player = (Player) sender;
        Editor editor = EditorRegistry.getCurrentEditor(player);
        if (args.length == 0) {
            if (editor != null) {
                editor.open(player);
                return true;
            }
            MessageUtil.sendMessage("editor.invalidSyntax", player, new MessageData("syntax", "/fusion-editor <profession|browse>"));
            return true;
        }
        if (editor == null || !editorCriteria.containsKey(player.getUniqueId())) {
            switch (args[0].toLowerCase()) {
                case "profession":
                    if (args.length < 2) {
                        MessageUtil.sendMessage("editor.invalidSyntax", player, new MessageData("syntax", "/fusion-editor profession <professionName> [profession to copy]"));
                        return true;
                    }
                    String professionName = args[1];
                    switch (args.length) {
                        case 2:
                            if (ProfessionsCfg.getMap().containsKey(professionName)) {
                                EditorRegistry.getProfessionEditor(player, professionName).open(player);
                                MessageUtil.sendMessage("editor.editProfession", player, new MessageData("profession", professionName));
                            } else {
                                if (ProfessionsCfg.createNewProfession(professionName, null)) {
                                    EditorRegistry.getProfessionEditor(player, professionName).open(player);
                                    MessageUtil.sendMessage("editor.createdNewProfession", player, new MessageData("profession", professionName));
                                }
                            }
                            break;
                        case 3:
                            String refProfession = args[2];
                            if (ProfessionsCfg.getMap().containsKey(professionName)) {
                                MessageUtil.sendMessage("editor.professionAlreadyExists", player, new MessageData("profession", professionName));
                                return true;
                            }
                            if (!ProfessionsCfg.getMap().containsKey(refProfession)) {
                                MessageUtil.sendMessage("editor.invalidProfession", player, new MessageData("profession", refProfession));
                                return true;
                            }
                            if (ProfessionsCfg.createNewProfession(professionName, refProfession)) {
                                EditorRegistry.getProfessionEditor(player, professionName).open(player);
                                MessageUtil.sendMessage("editor.copyProfession", player, new MessageData("oldProfession", refProfession), new MessageData("newProfession", professionName));
                            }
                            break;
                        default:
                            MessageUtil.sendMessage("editor.invalidSyntax", player, new MessageData("syntax", "/fusion-editor profession <professionName> [profession to copy]"));
                            break;
                    }
                    break;
                case "browse":
                    break;
            }
            return true;
        }
        EditorCriteria criteria = editorCriteria.get(player.getUniqueId());

        if (editor instanceof ProfessionEditor) {
            ProfessionEditor professionEditor = (ProfessionEditor) editor;
            switch (criteria) {
                case Profession_Edit_Name -> updateProfessionName(professionEditor, args);

                case Profession_Category_Add, Profession_Category_Edit ->
                        updateCategory(professionEditor, args, criteria);

                case Profession_Pattern_Edit_Name -> updatePatternItemName(professionEditor, args);
                case Profession_Pattern_Edit_Lore -> addPatternItemLore(professionEditor, args);
                case Profession_Pattern_Edit_Pattern -> updatePatternItem(professionEditor, args);
                case Profession_Pattern_Add_Commands -> addPatternItemCommand(professionEditor, args);

                case Profession_Recipe_Edit_Name -> updateRecipeName(professionEditor, args);
                case Profession_Recipe_Add_Commands -> addRecipeCommand(professionEditor, args);
                case Profession_Recipe_Add -> addNewRecipe(professionEditor, args);
                case Profession_Recipe_Edit_ResultItem, Profession_Recipe_Add_Ingredients,
                        Profession_Recipe_Edit_Ingredients -> updateRecipeItems(professionEditor, args, criteria);
                case Profession_Recipe_Add_Conditions -> addRecipeConditions(professionEditor, args);

                default -> editor.open(player);
            }
        } else if (editor instanceof BrowseEditor) {
            BrowseEditor browseEditor = (BrowseEditor) editor;
        }
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> entries = new ArrayList<>();
        if (!(sender instanceof Player)) return null;
        Player player = (Player) sender;
        Editor editor = EditorRegistry.getCurrentEditor(player);
        if (editor == null || !editorCriteria.containsKey(player.getUniqueId())) {
            if (args.length == 1) {
                if ("profession".startsWith(args[0].toLowerCase())) entries.add("profession");
                if ("browse".startsWith(args[0].toLowerCase())) entries.add("browse");
            } else if (args.length == 2) {
                if ("profession".equalsIgnoreCase(args[0])) {
                    if ("<new profession>".startsWith(args[1].toLowerCase())) entries.add("<new profession>");
                    entries.addAll(TabCacher.getTabs(player.getUniqueId(), "professions", args[1]));
                }
            } else if (args.length == 3) {
                if ("profession".equalsIgnoreCase(args[0]) && !TabCacher.getTabs(player.getUniqueId(), "professions", args[2]).contains(args[1])) {
                    entries.add("<profession to copy>");
                    entries.addAll(TabCacher.getTabs(player.getUniqueId(), "professions", args[2]));
                }
            }
            return entries;
        }
        if (editor instanceof ProfessionEditor) {
            ProfessionEditor professionEditor = (ProfessionEditor) editor;
            EditorCriteria criteria = editorCriteria.get(player.getUniqueId());
            switch (criteria) {
                case Profession_Edit_Name:
                case Profession_Pattern_Edit_Name:
                    if (args.length == 1) {
                        entries.add("<newName>");
                        entries.add(professionEditor.getTable().getInventoryName());
                    }
                    break;
                case Profession_Category_Add:
                case Profession_Category_Edit:
                    if (args.length == 1) {
                        if (criteria == EditorCriteria.Profession_Category_Add) {
                            entries.add("<categoryName>");
                        } else if (professionEditor.getCategoryEditor().getLastEditedCategoryName().startsWith(args[0])) {
                            entries.add(professionEditor.getCategoryEditor().getLastEditedCategoryName());
                        }
                    } else if (args.length == 2) {
                        entries.addAll(TabCacher.getTabs(TabCacher.GlobalUUID, "items", args[1]));
                    }
                    break;
                case Profession_Recipe_Add:
                    if (args.length == 1) {
                        entries.add("<recipeName>");
                    } else if (args.length == 2) {
                        entries.addAll(TabCacher.getTabs(TabCacher.GlobalUUID, "items", args[1]));
                    } else if (args.length == 3) {
                        entries.add("<amount>");
                        entries.add("1");
                        entries.add("5");
                        entries.add("32");
                        entries.add("64");
                    }
                    break;
                case Profession_Pattern_Edit_Pattern:
                case Profession_Recipe_Edit_ResultItem:
                case Profession_Recipe_Add_Ingredients:
                case Profession_Recipe_Edit_Ingredients:
                    if (args.length == 1) {
                        entries.addAll(TabCacher.getTabs(TabCacher.GlobalUUID, "items", args[0]));
                    } else if (args.length == 2) {
                        entries.add("<amount>");
                        entries.add("1");
                        entries.add("5");
                        entries.add("32");
                        entries.add("64");
                    }
                    break;
                case Profession_Recipe_Edit_Name:
                    if (args.length == 1) {
                        entries.add("<newRecipeName>");
                        entries.add(professionEditor.getRecipeEditor().getRecipeItemEditor().getRecipeName());
                    }
                    break;
                case Profession_Recipe_Add_Commands:
                case Profession_Pattern_Add_Commands:
                    if (args.length == 1) {
                        if ("console".startsWith(args[0].toUpperCase())) entries.add("console");
                        if ("player".startsWith(args[0].toUpperCase())) entries.add("player");
                        if ("op".startsWith(args[0].toUpperCase())) entries.add("op");
                    } else if (args.length == 2) {
                        entries.add("0");
                        entries.add("<delay>");
                    } else if (args.length == 3) {
                        entries.add("<command without / >");
                        entries.add("{player}");
                    }
                    break;
                case Profession_Recipe_Add_Conditions:
                    if (args.length == 1) {
                        entries.add("<conditionKey>");
                        if ("professions".startsWith(args[0].toLowerCase())) entries.add("professions");
                        if (Bukkit.getPluginManager().isPluginEnabled("Fabled") && "fabled".startsWith(args[0].toLowerCase()))
                            entries.add("fabled");
                        if (Bukkit.getPluginManager().isPluginEnabled("mcMMO") && "mcmmo".startsWith(args[0].toLowerCase()))
                            entries.add("mcmmo");
                        if (Bukkit.getPluginManager().isPluginEnabled("Jobs") && "jobs".startsWith(args[0].toLowerCase()))
                            entries.add("jobs");
                        if (Bukkit.getPluginManager().isPluginEnabled("AuraSkills") || Bukkit.getPluginManager().isPluginEnabled("AureliumSkills")) {
                            if ("aura_abilities".startsWith(args[0].toLowerCase())) entries.add("aura_abilities");
                            if ("aura_mana_abilities".startsWith(args[0].toLowerCase()))
                                entries.add("aura_mana_abilities");
                            if ("aura_skills".startsWith(args[0].toLowerCase())) entries.add("aura_skills");
                            if ("aura_stats".startsWith(args[0].toLowerCase())) entries.add("aura_stats");
                        }
                    } else if (args.length == 2) {
                        entries.add("<conditionValue>");
                        switch (args[0].toLowerCase()) {
                            case "professions":
                                entries.addAll(TabCacher.getTabs(TabCacher.GlobalUUID, "professions", args[1]));
                                break;
                            case "fabled":
                                entries.addAll(TabCacher.getTabs(TabCacher.GlobalUUID, "fabled", args[1]));
                                break;
                            case "mcmmo":
                                entries.addAll(TabCacher.getTabs(TabCacher.GlobalUUID, "mcmmo", args[1]));
                                break;
                            case "jobs":
                                entries.addAll(TabCacher.getTabs(TabCacher.GlobalUUID, "jobs", args[1]));
                                break;
                            case "aura_abilities":
                                entries.addAll(TabCacher.getTabs(TabCacher.GlobalUUID, "aura_abilities", args[1]));
                                break;
                            case "aura_mana_abilities":
                                entries.addAll(TabCacher.getTabs(TabCacher.GlobalUUID, "aura_mana_abilities", args[1]));
                                break;
                            case "aura_skills":
                                entries.addAll(TabCacher.getTabs(TabCacher.GlobalUUID, "aura_skills", args[1]));
                                break;
                            case "aura_stats":
                                entries.addAll(TabCacher.getTabs(TabCacher.GlobalUUID, "aura_stats", args[1]));
                                break;
                        }
                    } else if (args.length == 3) {
                        entries.add("<level>");
                        entries.add("1");
                        entries.add("10");
                        entries.add("25");
                        entries.add("50");
                        entries.add("100");
                    }
                    break;
            }
        }
        return entries;
    }

    private boolean isValidItem(String item) {
        try {
            //If the material in uppercase is valid, return true
            Material.valueOf(item.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            // If the is a custom item from divinity without "DIVINITY_" prefix, return true
            try {
                return CodexEngine.get().getItemManager().getItemType(item) != null;
            } catch (MissingItemException | MissingProviderException e1) {
                return false;
            }
        }
    }

    public static void suggestUsage(Player player, EditorCriteria criteria, String suggestCommand) {
        if (criteria == null) return;
        editorCriteria.put(player.getUniqueId(), criteria);
        switch (criteria) {
            /* Profession */
            case Profession_Edit_Name:
                MessageUtil.sendMessage("editor.editorUsage", player, new MessageData("syntax", "<newName>"));
                break;
            /* Categories */
            case Profession_Category_Add:
            case Profession_Category_Edit:
                MessageUtil.sendMessage("editor.editorUsage", player, new MessageData("syntax", "<categoryName> <categoryIcon>"));
                break;
            /* Patterns */
            case Profession_Pattern_Edit_Name:
                MessageUtil.sendMessage("editor.editorUsage", player, new MessageData("syntax", "<name>"));
                break;
            case Profession_Pattern_Edit_Pattern:
                MessageUtil.sendMessage("editor.editorUsage", player, new MessageData("syntax", "<item> <amount>"));
            case Profession_Pattern_Edit_Lore:
                MessageUtil.sendMessage("editor.editorUsage", player, new MessageData("syntax", "<lore>"));
                break;
            case Profession_Pattern_Add_Commands:
                MessageUtil.sendMessage("editor.editorUsage", player, new MessageData("syntax", "<caster> <delay> <command without />"));
                break;
            /* Recipes */
            case Profession_Recipe_Add:
                MessageUtil.sendMessage("editor.editorUsage", player, new MessageData("syntax", "<recipeName> <resultItem> <amount>"));
                break;
            case Profession_Recipe_Edit_ResultItem:
                MessageUtil.sendMessage("editor.editorUsage", player, new MessageData("syntax", "<resultItem> <amount>"));
                break;
            case Profession_Recipe_Edit_Name:
                MessageUtil.sendMessage("editor.editorUsage", player, new MessageData("syntax", "<newRecipeName>"));
                break;
            case Profession_Recipe_Add_Ingredients:
            case Profession_Recipe_Edit_Ingredients:
                MessageUtil.sendMessage("editor.editorUsage", player, new MessageData("syntax", "<ingredient> <amount>"));
                break;
            case Profession_Recipe_Add_Commands:
                MessageUtil.sendMessage("editor.editorUsage", player, new MessageData("syntax", "<caster> <delay> <command without />"));
                break;
            case Profession_Recipe_Add_Conditions:
                MessageUtil.sendMessage("editor.editorUsage", player, new MessageData("syntax", "<conditionKey> <conditionValue> <level>"));
                break;
        }
        sendSuggestMessage(player, suggestCommand);
        player.closeInventory();
    }

    private static void sendSuggestMessage(Player player, String suggestedCommand) {
        if (suggestedCommand == null) return;
        BaseComponent[] components = MessageUtil.getMessageAsComponent("editor.editorClick");
        for (BaseComponent component : components) {
            component.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, suggestedCommand));
        }
        player.spigot().sendMessage(components);
    }

    /* Profession */
    private void updateProfessionName(ProfessionEditor professionEditor, String[] args) {
        StringBuilder professionNameBuilder = new StringBuilder();
        for (String arg : args) {
            professionNameBuilder.append(arg).append(" ");
        }
        String professionName = professionNameBuilder.toString().trim();
        String oldName = professionEditor.getTable().getName();
        Player player = professionEditor.getPlayer();
        professionEditor.getTable().setInventoryName(professionName);
        MessageUtil.sendMessage("editor.professionRenamed", player, new MessageData("oldName", oldName), new MessageData("newName", professionName));
        professionEditor.reload(true);
    }

    /* Categories */
    private void updateCategory(ProfessionEditor professionEditor, String[] args, EditorCriteria criteria) {
        if (args.length < 2) {
            MessageUtil.sendMessage("editor.invalidSyntax", professionEditor.getPlayer(), new MessageData("syntax", "<categoryName> <categoryIcon>"));
            return;
        }

        String categoryName = args[0];
        String categoryIcon = args[1];
        Player player = professionEditor.getPlayer();
        if (!isValidItem(categoryIcon)) {
            MessageUtil.sendMessage("editor.invalidItem", player, new MessageData("item", categoryIcon));
            return;
        }

        if (criteria == EditorCriteria.Profession_Category_Add) {
            professionEditor.getTable().getCategories().put(categoryName, new Category(Map.of("name", categoryName, "icon", categoryIcon, "order", professionEditor.getTable().getCategories().size())));
            MessageUtil.sendMessage("editor.categoryAdded", player, new MessageData("category", categoryName), new MessageData("profession", professionEditor.getTable().getName()));
        } else {
            if (!professionEditor.getTable().getCategories().containsKey(categoryName)) {
                MessageUtil.sendMessage("editor.categoryNotFound", player, new MessageData("category", categoryName));
                return;
            }
            professionEditor.getTable().getCategories().get(categoryName).setName(categoryName);
            ItemType oldIcon = professionEditor.getTable().getCategories().get(categoryName).getIconItem();
            try {
                professionEditor.getTable().getCategories().get(categoryName).setIconItem(CodexEngine.get().getItemManager().getItemType(categoryIcon));
                MessageUtil.sendMessage("editor.categoryEdited", player, new MessageData("category", categoryName), new MessageData("profession", professionEditor.getTable().getName()));
            } catch (CodexItemException e) {
                professionEditor.getTable().getCategories().get(categoryName).setIconItem(oldIcon);
                MessageUtil.sendMessage("editor.invalidItem", player, new MessageData("item", categoryIcon));
                return;
            }
        }
        professionEditor.getCategoryEditor().reload(true);
    }

    /* Patterns */
    private void updatePatternItemName(ProfessionEditor professionEditor, String[] args) {
        StringBuilder builder = new StringBuilder();
        for (String arg : args) {
            builder.append(arg).append(" ");
        }
        String professionName = builder.toString().trim();
        String oldName = professionEditor.getPatternItemsEditor().getPatternItemEditor().getBuilder().getName();
        Player player = professionEditor.getPlayer();
        professionEditor.getPatternItemsEditor().getPatternItemEditor().getBuilder().name(professionName);
        MessageUtil.sendMessage("editor.patternItemRenamed", player, new MessageData("oldName", oldName), new MessageData("newName", professionName));
        professionEditor.getPatternItemsEditor().getPatternItemEditor().reload(true);
    }

    private void updatePatternItem(ProfessionEditor professionEditor, String[] args) {
        Player player = professionEditor.getPlayer();
        if (args.length < 2) {
            MessageUtil.sendMessage("editor.invalidSyntax", professionEditor.getPlayer(), new MessageData("syntax", "<item> <amount>"));
            return;
        }
        try {
            Material material = Material.valueOf(args[0].toUpperCase());
            int amount = Integer.parseInt(args[1]);

            professionEditor.getPatternItemsEditor().getPatternItemEditor().getBuilder().material(material);
            professionEditor.getPatternItemsEditor().getPatternItemEditor().getBuilder().amount(amount);
            professionEditor.getPatternItemsEditor().getPatternItemEditor().reload(true);
            MessageUtil.sendMessage("editor.patternItemUpdated", player, new MessageData("item", args[0]), new MessageData("amount", amount));
        } catch (Exception e) {
            MessageUtil.sendMessage("editor.invalidItem", player, new MessageData("item", args[0]));
            MessageUtil.sendMessage("editor.invalidNumber", player, new MessageData("number", args[1]));
        }
    }

    private void addPatternItemLore(ProfessionEditor professionEditor, String[] args) {
        Player player = professionEditor.getPlayer();
        if (args.length < 1) {
            MessageUtil.sendMessage("editor.invalidSyntax", player, new MessageData("syntax", args));
            return;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            builder.append(args[i]);
            if (i < args.length - 1)
                builder.append(" ");
        }
        professionEditor.getPatternItemsEditor().getPatternItemEditor().getBuilder().newLoreLine(builder.toString());
        professionEditor.getPatternItemsEditor().getPatternItemEditor().reload(true);
        MessageUtil.sendMessage("editor.patternItemLoreAdded", player, new MessageData("lore", ChatUT.hexString(builder.toString())));
    }

    private void addPatternItemCommand(ProfessionEditor professionEditor, String[] args) {
        Player player = professionEditor.getPlayer();
        if (args.length < 3) {
            MessageUtil.sendMessage("editor.invalidCommand", player, new MessageData("command", args));
            return;
        }
        StringBuilder commandBuilder = new StringBuilder();
        try {
            CommandType commandType = CommandType.valueOf(args[0].toUpperCase());
            int delay = Integer.parseInt(args[1]);
            commandBuilder = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                commandBuilder.append(args[i]);
                if (i < args.length - 1)
                    commandBuilder.append(" ");
            }
            professionEditor.getPatternItemsEditor().getPatternItemEditor().addCommand(new DelayedCommand(Map.of("delay", delay, "as", commandType.name(), "cmd", commandBuilder.toString())));
            professionEditor.getPatternItemsEditor().getPatternItemEditor().reload(true);
        } catch (Exception e) {
            e.printStackTrace();
            MessageUtil.sendMessage("editor.invalidCommand", player, new MessageData("command", args[0] + " " + args[1] + " " + commandBuilder));
        }
    }

    /* Recipes */
    private void updateRecipeName(ProfessionEditor professionEditor, String[] args) {
        if (args.length < 1) {
            MessageUtil.sendMessage("editor.invalidSyntax", professionEditor.getPlayer(), new MessageData("syntax", "<newRecipeName>"));
            return;
        }

        String recipeName = args[0];
        Player player = professionEditor.getPlayer();
        String oldName = professionEditor.getRecipeEditor().getRecipeItemEditor().getRecipeName();
        for (Recipe recipe : professionEditor.getTable().getRecipes().values()) {
            if (recipe.getName().equalsIgnoreCase(recipeName)) {
                MessageUtil.sendMessage("editor.recipeAlreadyExists", player, new MessageData("recipe", recipeName));
                return;
            }
        }
        professionEditor.getRecipeEditor().getRecipeItemEditor().getRecipe().setName(recipeName);
        MessageUtil.sendMessage("editor.recipeRenamed", player, new MessageData("oldName", oldName), new MessageData("newName", recipeName));
        professionEditor.getRecipeEditor().getRecipeItemEditor().reload(true);
    }

    private void addRecipeCommand(ProfessionEditor professionEditor, String[] args) {
        Player player = professionEditor.getPlayer();
        if (args.length < 3) {
            MessageUtil.sendMessage("editor.invalidCommand", player, new MessageData("command", args));
            return;
        }
        StringBuilder commandBuilder = new StringBuilder();
        try {
            CommandType commandType = CommandType.valueOf(args[0].toUpperCase());
            int delay = Integer.parseInt(args[1]);
            commandBuilder = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                commandBuilder.append(args[i]);
                if (i < args.length - 1)
                    commandBuilder.append(" ");
            }
            professionEditor.getRecipeEditor().getRecipeItemEditor().getRecipe().getResults().getCommands().add(new DelayedCommand(Map.of("delay", delay, "as", commandType.name(), "cmd", commandBuilder.toString())));
            professionEditor.getRecipeEditor().getRecipeItemEditor().reload(true);
        } catch (Exception e) {
            e.printStackTrace();
            MessageUtil.sendMessage("editor.invalidCommand", player, new MessageData("command", args[0] + " " + args[1] + " " + commandBuilder));
        }
    }

    private void addNewRecipe(ProfessionEditor professionEditor, String[] args) {
        Player player = professionEditor.getPlayer();
        if (args.length < 3) {
            MessageUtil.sendMessage("editor.invalidSyntax", player, new MessageData("syntax", "<recipeName> <resultItem> <amount>"));
            return;
        }
        try {
            String recipeName = args[0];
            String itemName = args[1];
            int amount = Integer.parseInt(args[2]);
            for (Recipe recipe : professionEditor.getTable().getRecipes().values()) {
                if (recipe.getName().equalsIgnoreCase(recipeName)) {
                    MessageUtil.sendMessage("editor.recipeAlreadyExists", player, new MessageData("recipe", recipeName));
                    return;
                }
            }
            if (!isValidItem(itemName)) {
                MessageUtil.sendMessage("editor.invalidItem", player, new MessageData("item", itemName));
                return;
            }
            if (professionEditor.getTable().getRecipes().containsKey(recipeName)) {
                MessageUtil.sendMessage("editor.recipeAlreadyExists", player, new MessageData("recipe", recipeName));
                return;
            }
            Map<String, Object> recipeSettings = new LinkedHashMap<>();
            recipeSettings.put("name", recipeName);
            if (!professionEditor.getTable().getCategories().isEmpty())
                recipeSettings.put("category", professionEditor.getTable().getCategories().keySet().iterator().next());
            recipeSettings.put("craftingTime", 0);
            recipeSettings.put("results", Map.of("item", itemName + ":" + amount, "professionExp", 0, "vanillaExp", 0, "commands", new ArrayList<>()));
            recipeSettings.put("conditions", Map.of("professionLevel", 0, "mastery", false));
            recipeSettings.put("costs", Map.of("items", List.of("STONE:3"), "money", 0.0, "exp", 0));

            professionEditor.getTable().getRecipes().put(recipeName, new Recipe(recipeSettings));
            MessageUtil.sendMessage("editor.recipeAdded", player, new MessageData("recipe", recipeName), new MessageData("result", itemName));
            professionEditor.getRecipeEditor().reload(true);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            MessageUtil.sendMessage("editor.invalidNumber", player, new MessageData("number", args[2]));
        }
    }

    private void updateRecipeItems(ProfessionEditor professionEditor, String[] args, EditorCriteria criteria) {
        Player player = professionEditor.getPlayer();
        if (args.length != 2) {
            MessageUtil.sendMessage("editor.invalidSyntax", player, new MessageData("syntax", "<item> <amount>"));
            return;
        }
        try {
            String itemName = args[0];
            int amount = Integer.parseInt(args[1]);
            if (!isValidItem(itemName)) {
                MessageUtil.sendMessage("editor.invalidItem", player, new MessageData("item", itemName));
                return;
            }

            switch (criteria) {
                case Profession_Recipe_Edit_ResultItem:
                    professionEditor.getRecipeEditor().getRecipeItemEditor().getRecipe().getResults().setResultItem(RecipeItem.fromConfig(itemName + ":" + amount));
                    professionEditor.getRecipeEditor().getRecipeItemEditor().getRecipe().getResults().setResultName(itemName + ":" + amount);
                    MessageUtil.sendMessage("editor.resultEdited", player, new MessageData("recipe", professionEditor.getRecipeEditor().getRecipeItemEditor().getRecipe().getName()), new MessageData("result", itemName));
                    break;
                case Profession_Recipe_Add_Ingredients:
                    int i = 0;
                    boolean found = false;
                    for (RecipeItem ingredient : professionEditor.getRecipeEditor().getRecipeItemEditor().getRecipe().getConditions().getRequiredItems()) {
                        if (ingredient.toConfig().toString().split(":")[0].equalsIgnoreCase(itemName)) {
                            professionEditor.getRecipeEditor().getRecipeItemEditor().getRecipe().getConditions().getRequiredItems().set(i, RecipeItem.fromConfig(itemName + ":" + amount));
                            found = true;
                        }
                        i++;
                    }
                    if (!found)
                        professionEditor.getRecipeEditor().getRecipeItemEditor().getRecipe().getConditions().getRequiredItems().add(RecipeItem.fromConfig(itemName + ":" + amount));
                    break;
                case Profession_Recipe_Edit_Ingredients:
                    professionEditor.getRecipeEditor().getRecipeItemEditor().getRecipe().getConditions().getRequiredItems().clear();
                    professionEditor.getRecipeEditor().getRecipeItemEditor().getRecipe().getConditions().getRequiredItems().add(RecipeItem.fromConfig(itemName + ":" + amount));
                    break;
            }
            professionEditor.getRecipeEditor().getRecipeItemEditor().reload(true);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            MessageUtil.sendMessage("editor.invalidNumber", player, new MessageData("number", args[1]));
        }
    }

    private void addRecipeConditions(ProfessionEditor professionEditor, String[] args) {
        Player player = professionEditor.getPlayer();
        if (args.length != 3) {
            MessageUtil.sendMessage("editor.invalidSyntax", player, new MessageData("syntax", "<conditionKey> <conditionValue> <level>"));
            return;
        }
        String conditionKey = args[0];
        String conditionValue = args[1];
        int level = Integer.parseInt(args[2]);

        switch (conditionKey) {
            case "professions":
                if (!ProfessionsCfg.getMap().containsKey(conditionValue)) {
                    MessageUtil.sendMessage("editor.invalidConditionValue", player, new MessageData("key", conditionKey), new MessageData("value", conditionValue), new MessageData("level", args[2]));
                    return;
                }
                professionEditor.getRecipeEditor().getRecipeItemEditor().getRecipe().getConditions().getProfessionConditions().put(conditionValue, level);
                break;
            case "fabled":
                if (!Bukkit.getPluginManager().isPluginEnabled("Fabled")) return;
                if (!Fabled.getClasses().containsKey(conditionValue)) {
                    MessageUtil.sendMessage("editor.invalidConditionValue", player, new MessageData("key", conditionKey), new MessageData("value", conditionValue), new MessageData("level", args[2]));
                    return;
                }
                professionEditor.getRecipeEditor().getRecipeItemEditor().getRecipe().getConditions().getFabledClassConditions().put(conditionValue, level);
                break;
            case "mcmmo":
                if (!Bukkit.getPluginManager().isPluginEnabled("mcMMO")) return;
                try {
                    PrimarySkillType skillType = PrimarySkillType.valueOf(conditionValue.toUpperCase());
                    professionEditor.getRecipeEditor().getRecipeItemEditor().getRecipe().getConditions().getMcMMOConditions().put(conditionValue, level);
                } catch (IllegalArgumentException e) {
                    MessageUtil.sendMessage("editor.invalidConditionValue", player, new MessageData("key", conditionKey), new MessageData("value", conditionValue), new MessageData("level", args[2]));
                    return;
                }
                break;
            case "jobs":
                if (!Bukkit.getPluginManager().isPluginEnabled("Jobs")) return;
                Optional<Job> job = Jobs.getJobs().stream().filter(_job -> _job.getName().equalsIgnoreCase(conditionValue)).findFirst();
                if (job.isEmpty()) {
                    MessageUtil.sendMessage("editor.invalidConditionValue", player, new MessageData("key", conditionKey), new MessageData("value", conditionValue), new MessageData("level", args[2]));
                    return;
                }
                professionEditor.getRecipeEditor().getRecipeItemEditor().getRecipe().getConditions().getJobsConditions().put(conditionValue, level);
                break;
            case "aura_abilities":
            case "aura_mana_abilities":
            case "aura_skills":
            case "aura_stats":
                if (!Bukkit.getPluginManager().isPluginEnabled("AuraSkills") || !Bukkit.getPluginManager().isPluginEnabled("AureliumSkills"))
                    return;
                switch (conditionKey) {
                    case "aura_abilities":
                        if (AuraSkillsApi.get().getGlobalRegistry().getAbility(NamespacedId.fromString(conditionValue)) == null) {
                            MessageUtil.sendMessage("editor.invalidConditionValue", player, new MessageData("key", conditionKey), new MessageData("value", conditionValue), new MessageData("level", args[2]));
                            return;
                        }
                        professionEditor.getRecipeEditor().getRecipeItemEditor().getRecipe().getConditions().getAuraAbilityConditions().put(conditionValue, level);
                        break;
                    case "aura_mana_abilities":
                        if (AuraSkillsApi.get().getGlobalRegistry().getManaAbility(NamespacedId.fromString(conditionValue)) == null) {
                            MessageUtil.sendMessage("editor.invalidConditionValue", player, new MessageData("key", conditionKey), new MessageData("value", conditionValue), new MessageData("level", args[2]));
                            return;
                        }
                        professionEditor.getRecipeEditor().getRecipeItemEditor().getRecipe().getConditions().getAuraManaAbilityConditions().put(conditionValue, level);
                        break;
                    case "aura_skills":
                        if (AuraSkillsApi.get().getGlobalRegistry().getSkill(NamespacedId.fromString(conditionValue)) == null) {
                            MessageUtil.sendMessage("editor.invalidConditionValue", player, new MessageData("key", conditionKey), new MessageData("value", conditionValue), new MessageData("level", args[2]));
                            return;
                        }
                        professionEditor.getRecipeEditor().getRecipeItemEditor().getRecipe().getConditions().getAuraSkillsConditions().put(conditionValue, level);
                        break;
                    case "aura_stats":
                        if (AuraSkillsApi.get().getGlobalRegistry().getStat(NamespacedId.fromString(conditionValue)) == null) {
                            MessageUtil.sendMessage("editor.invalidConditionValue", player, new MessageData("key", conditionKey), new MessageData("value", conditionValue), new MessageData("level", args[2]));
                            return;
                        }
                        professionEditor.getRecipeEditor().getRecipeItemEditor().getRecipe().getConditions().getAuraStatsConditions().put(conditionValue, level);
                        break;
                }
            default:
                MessageUtil.sendMessage("editor.invalidConditionKey", player, new MessageData("key", conditionKey));
                return;
        }
        MessageUtil.sendMessage("editor.conditionAdded", player, new MessageData("key", conditionKey), new MessageData("value", conditionValue), new MessageData("level", args[2]));
        professionEditor.getRecipeEditor().getRecipeItemEditor().reload(true);
    }

    public static void removeEditorCriteria(UUID uuid) {
        editorCriteria.remove(uuid);
    }
}
