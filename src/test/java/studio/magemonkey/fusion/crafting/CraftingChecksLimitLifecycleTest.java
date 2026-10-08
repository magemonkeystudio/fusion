package studio.magemonkey.fusion.crafting;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import studio.magemonkey.fusion.cfg.Cfg;
import studio.magemonkey.fusion.data.player.FusionPlayer;
import studio.magemonkey.fusion.data.player.PlayerLoader;
import studio.magemonkey.fusion.data.player.PlayerRecipeLimit;
import studio.magemonkey.fusion.data.professions.pattern.Category;
import studio.magemonkey.fusion.data.queue.CraftingQueue;
import studio.magemonkey.fusion.data.queue.QueueItem;
import studio.magemonkey.fusion.data.recipes.CalculatedRecipe;
import studio.magemonkey.fusion.data.recipes.CraftingTable;
import studio.magemonkey.fusion.data.recipes.Recipe;
import studio.magemonkey.fusion.util.PlayerUtil;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CraftingChecksLimitLifecycleTest {
    MockedStatic<CalculatedRecipe> calculated;
    MockedStatic<PlayerLoader> loader;
    MockedStatic<PlayerUtil> permissions;
    Player player;
    CraftingTable table;
    Category category;
    Recipe recipe;
    FusionPlayer owner;
    CraftingQueue queue;
    AtomicReference<PlayerRecipeLimit> persistedLimit;
    boolean previousInstantCollect;

    @BeforeEach void setup() {
        previousInstantCollect = Cfg.instantCollect;
        Cfg.instantCollect = false;
        player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getContents()).thenReturn(new ItemStack[]{new ItemStack(Material.AIR)});
        table = mock(CraftingTable.class);
        category = mock(Category.class);
        recipe = mock(Recipe.class);
        owner = mock(FusionPlayer.class);
        queue = mock(CraftingQueue.class);
        when(table.getName()).thenReturn("smith");
        when(category.getName()).thenReturn("tools");
        when(recipe.getRecipePath()).thenReturn("smith.tools.pick");
        when(recipe.getCraftingTime()).thenReturn(10);
        when(queue.getCategory()).thenReturn(category);
        when(queue.getQueue()).thenReturn(new ArrayList<>());
        when(owner.getQueueSizes("smith", category)).thenReturn(new int[]{0, 0, 0});
        persistedLimit = new AtomicReference<>(new PlayerRecipeLimit(recipe.getRecipePath(), 0, -1));
        when(owner.getRecipeLimit(recipe)).thenAnswer(invocation -> persistedLimit.get());
        when(owner.hasRecipeLimitReached(recipe)).thenReturn(false);

        calculated = mockStatic(CalculatedRecipe.class);
        CalculatedRecipe calculatedRecipe = mock(CalculatedRecipe.class);
        when(calculatedRecipe.isCanCraft()).thenReturn(true);
        calculated.when(() -> CalculatedRecipe.create(eq(recipe), anyMap(), eq(player), eq(table))).thenReturn(calculatedRecipe);

        loader = mockStatic(PlayerLoader.class);
        loader.when(() -> PlayerLoader.getPlayer(player)).thenReturn(owner);
        permissions = mockStatic(PlayerUtil.class);
        permissions.when(() -> PlayerUtil.getPermOption(any(), anyString())).thenReturn(0);
    }

    @AfterEach void close() {
        Cfg.instantCollect = previousInstantCollect;
        if (permissions != null) permissions.close();
        if (loader != null) loader.close();
        if (calculated != null) calculated.close();
    }

    @Test void limitMinusOneAllowsQueueing() {
        when(recipe.getCraftingLimit()).thenReturn(2);
        persistedLimit.set(new PlayerRecipeLimit(recipe.getRecipePath(), 1, -1));
        assertTrue(CraftingChecks.canCraft(player, table, recipe, queue));
    }

    @Test void exactLimitBlocksQueueing() {
        when(recipe.getCraftingLimit()).thenReturn(2);
        persistedLimit.set(new PlayerRecipeLimit(recipe.getRecipePath(), 2, -1));
        when(owner.hasRecipeLimitReached(recipe)).thenReturn(true);
        assertFalse(CraftingChecks.canCraft(player, table, recipe, queue));
    }

    @Test void finishedButUncollectedCraftCountsOnceTowardLimit() {
        when(recipe.getCraftingLimit()).thenReturn(3);
        persistedLimit.set(new PlayerRecipeLimit(recipe.getRecipePath(), 1, -1));
        QueueItem finished = new QueueItem(11, "smith", category, recipe, System.currentTimeMillis(), recipe.getCraftingTime());
        when(queue.getQueue()).thenReturn(new ArrayList<>(java.util.List.of(finished)));
        assertTrue(CraftingChecks.canCraft(player, table, recipe, queue));
        when(recipe.getCraftingLimit()).thenReturn(2);
        assertFalse(CraftingChecks.canCraft(player, table, recipe, queue));
    }

    @Test void instantCollectStillHonorsExactLimitSemantics() {
        Cfg.instantCollect = true;
        when(recipe.getCraftingTime()).thenReturn(0);
        when(recipe.getCraftingLimit()).thenReturn(2);
        persistedLimit.set(new PlayerRecipeLimit(recipe.getRecipePath(), 1, -1));
        QueueItem queued = new QueueItem(15, "smith", category, recipe, System.currentTimeMillis(), 0);
        when(queue.getQueue()).thenReturn(new ArrayList<>(java.util.List.of(queued)));
        assertFalse(CraftingChecks.canCraft(player, table, recipe, queue));
    }
}
