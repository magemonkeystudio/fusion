package studio.magemonkey.fusion.commands;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
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
import studio.magemonkey.fusion.Category;
import studio.magemonkey.fusion.Recipe;
import studio.magemonkey.fusion.RecipeItem;
import studio.magemonkey.fusion.cfg.editors.EditorRegistry;
import studio.magemonkey.fusion.gui.editors.BrowseEditor;
import studio.magemonkey.fusion.gui.editors.Editor;
import studio.magemonkey.fusion.gui.editors.ProfessionEditor;
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

        if (editor == null || !editorCriteria.containsKey(player.getUniqueId())) {
            MessageUtil.sendMessage("editor.noEditor", player);
            return true;
        }
        EditorCriteria criteria = editorCriteria.get(player.getUniqueId());

        if (editor instanceof ProfessionEditor) {
            ProfessionEditor professionEditor = (ProfessionEditor) editor;
            switch (criteria) {
                case Profession_Edit_Name:
                    updateProfessionName(professionEditor, args);
                    break;
                case Profession_Category_Add:
                case Profession_Category_Edit:
                    updateCategory(professionEditor, args, criteria);
                    break;
                case Profession_Recipe_Edit_Name:
                    updateRecipeName(professionEditor, args);
                    break;
                case Profession_Recipe_Add_Commands:
                    updateRecipeCommands(professionEditor, args);
                    break;
                case Profession_Recipe_Add:
                    updateNewRecipe(professionEditor, args);
                    break;
                case Profession_Recipe_Edit_ResultItem:
                case Profession_Recipe_Add_Ingredients:
                case Profession_Recipe_Edit_Ingredients:
                    updateRecipeItems(professionEditor, args, criteria);
                    break;
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
        if (editor == null || !editorCriteria.containsKey(player.getUniqueId())) return null;
        if (editor instanceof ProfessionEditor) {
            ProfessionEditor professionEditor = (ProfessionEditor) editor;
            EditorCriteria criteria = editorCriteria.get(player.getUniqueId());
            switch (criteria) {
                case Profession_Edit_Name:
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
                    } else if (args.length == 2) {
                        entries.add("<conditionValue>");
                    } else if (args.length == 3) {
                        entries.add("<level>");
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
            case Profession_Edit_Name:
                MessageUtil.sendMessage("editor.editorUsage", player, new MessageData("syntax", "<newName>"));
                break;
            case Profession_Category_Add:
            case Profession_Category_Edit:
                MessageUtil.sendMessage("editor.editorUsage", player, new MessageData("syntax", "<categoryName> <categoryIcon>"));
                break;
            case Profession_Recipe_Add:
                MessageUtil.sendMessage("editor.editorUsage", player, new MessageData("syntax", "<recipeName> <resultItem> <amount>"));
                break;
            case Profession_Recipe_Edit_ResultItem:
                MessageUtil.sendMessage("editor.editorUsage", player, new MessageData("syntax", "<resultItem> [amount]"));
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

    private void updateProfessionName(ProfessionEditor professionEditor, String[] args) {
        String professionName = args[0];
        String oldName = professionEditor.getTable().getName();
        Player player = professionEditor.getPlayer();
        professionEditor.getTable().setInventoryName(professionName);
        MessageUtil.sendMessage("editor.professionRenamed", player, new MessageData("oldName", oldName), new MessageData("newName", professionName));
        professionEditor.reload(true);
    }

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

    private void updateRecipeCommands(ProfessionEditor professionEditor, String[] args) {
        Player player = professionEditor.getPlayer();
        if (args.length < 3) {
            MessageUtil.sendMessage("editor.invalidRecipeCommand", player, new MessageData("command", args));
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
            MessageUtil.sendMessage("editor.invalidRecipeCommand", player, new MessageData("command", args[0] + " " + args[1] + " " + commandBuilder));
        }
    }

    private void updateNewRecipe(ProfessionEditor professionEditor, String[] args) {
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
            MessageUtil.sendMessage("editor.invalidNumber", player, new MessageData("number", args[1]));
        }
    }

    private void updateRecipeConditions(ProfessionEditor professionEditor, String[] args) {
        Player player = professionEditor.getPlayer();
        if (args.length != 3) {
            MessageUtil.sendMessage("editor.invalidSyntax", player, new MessageData("syntax", "<conditionKey> <conditionValue> <level>"));
            return;
        }
        String conditionKey = args[0];
        String conditionValue = args[1];
        int level = Integer.parseInt(args[2]);

        // TODO validate condition key and value

        professionEditor.getRecipeEditor().getRecipeItemEditor().reload(true);
    }
}
