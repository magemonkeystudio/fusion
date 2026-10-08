package studio.magemonkey.fusion.gui;

import org.junit.jupiter.api.Test;
import studio.magemonkey.fusion.gui.recipe.*;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class PatternCompilerTest {
    @Test void legacyRolesAndCustomSymbolsHaveSameLayout() {
        var legacy = PatternCompiler.compile(new String[]{"fooooffff", "f---f{f}f"}, Map.of());
        var custom = PatternCompiler.compile(new String[]{"frrrrffff", "fqqqf{f}f"},
                Map.of('r', SlotRole.RECIPE_LIST, 'q', SlotRole.QUEUE_LIST));
        assertEquals(legacy.slots(SlotRole.RECIPE_LIST), custom.slots(SlotRole.RECIPE_LIST));
        assertEquals(legacy.slots(SlotRole.QUEUE_LIST), custom.slots(SlotRole.QUEUE_LIST));
        assertEquals(18, custom.size());
    }
    @Test void rejectsMalformedAndIncompleteLayouts() {
        assertThrows(IllegalArgumentException.class, () -> PatternCompiler.compile(new String[]{"ooo"}, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> PatternCompiler.compile(new String[]{"fffffffff"}, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> PatternCompiler.compile(new String[]{"offf{fff}"}, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> PatternCompiler.compile(new String[7], Map.of()));
    }
    @Test void paginationClampsEmptyAndShrinkingPages() {
        assertEquals(0, RecipeGui.clampPage(5, 0, 3));
        assertEquals(1, RecipeGui.clampPage(2, 6, 3));
        assertEquals(0, RecipeGui.clampPage(4, 10, 0));
    }
}
