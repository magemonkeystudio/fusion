package com.gotofinal.darkrise.crafting;

import com.gotofinal.darkrise.crafting.cfg.Cfg;
import com.gotofinal.darkrise.crafting.gui.CustomGUI;
import com.gotofinal.darkrise.economy.DarkRiseEconomy;
import com.gotofinal.darkrise.spigot.core.DarkRisePlugin;
import com.gotofinal.messages.Init;
import com.gotofinal.messages.api.chat.placeholder.PlaceholderType;

public class DarkRiseCrafting extends DarkRisePlugin
{
    public static final PlaceholderType<RecipeItem>       RECIPE_ITEM        = PlaceholderType.create("recipeItem", RecipeItem.class);
    public static final PlaceholderType<Recipe>           RECIPE             = PlaceholderType.create("recipe", Recipe.class);
    public static final PlaceholderType<CraftingTable>    CRAFTING_TABLE     = PlaceholderType.create("craftingTable", CraftingTable.class);
    public static final PlaceholderType<CalculatedRecipe> CALCULATED_RECIPE  = PlaceholderType.create("calculatedRecipe", CalculatedRecipe.class);
    public static final PlaceholderType<CustomGUI>        CRAFTING_INVENTORY = PlaceholderType.create("craftingInventory", CustomGUI.class);

    private static DarkRiseCrafting instance;

    {
        instance = this;
    }

    public static DarkRiseCrafting getInstance()
    {
        return instance;
    }

    @Override
    public void reloadConfigs()
    {
        Cfg.init();
        super.reloadConfigs();
    }

    @Override
    public void onLoad()
    {
        RECIPE_ITEM.registerItem("amount", RecipeItem::getAmount);
        RECIPE_ITEM.registerItem("itemName", RecipeItem::getItemName);
        RECIPE.registerItem("name", Recipe::getName);
        RECIPE.registerItem("price", Recipe::getPrice);
        CRAFTING_TABLE.registerItem("name", CraftingTable::getName);
        CRAFTING_TABLE.registerItem("inventoryName", CraftingTable::getInventoryName);
        CRAFTING_INVENTORY.registerItem("name", CustomGUI::getName);
        CRAFTING_INVENTORY.registerItem("inventoryName", CustomGUI::getInventoryName);

        RECIPE_ITEM.registerChild("customItem", DarkRiseEconomy.RISE_ITEM, RecipeItem::asRiseItem);
        RECIPE_ITEM.registerChild("item", Init.ITEM, RecipeItem::getItemStack);
        RECIPE.registerChild("result", RECIPE_ITEM, Recipe::getResult);
        CALCULATED_RECIPE.registerChild("recipe", RECIPE, CalculatedRecipe::getRecipe);
        CALCULATED_RECIPE.registerChild("icon", Init.ITEM, CalculatedRecipe::getIcon);
        super.onLoad();
    }

    @Override
    public void onEnable()
    {
        super.onEnable();
        this.reloadConfigs();
        Cfg.init();
        this.getCommand("craft").setExecutor(new Commands());
    }

    public void closeAll()
    {
        Cfg.getGuiMap().values().forEach(CustomGUI::closeAll);
    }

    @Override
    public void onDisable()
    {
        super.onDisable();
        this.closeAll();
    }
}
