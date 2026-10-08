package studio.magemonkey.fusion.data;

import org.junit.jupiter.api.Test;
import studio.magemonkey.fusion.data.queue.*;
import studio.magemonkey.fusion.data.recipes.Recipe;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QueueProgressTest {
    private QueueItem item(int seconds) {
        Recipe recipe = mock(Recipe.class);
        when(recipe.getCraftingTime()).thenReturn(seconds);
        return new QueueItem(-1, "smith", null, recipe, 1000, 0);
    }
    @Test void countdownAdvancesWithoutGuiAndIgnoresVisualValues() {
        QueueItem item = item(10);
        item.setVisualRemainingItemTime(5000);
        QueueProgress.advance(List.of(item), 1);
        assertEquals(1, item.getSavedSeconds());
        assertEquals(9, item.getVisualRemainingItemTime());
        QueueProgress.advance(List.of(item), 1);
        assertEquals(8, item.getVisualRemainingItemTime());
    }
    @Test void advancesSequentiallyAcrossCompletionAndUpdatesWaitingEta() {
        QueueItem first = item(2), second = item(5);
        QueueProgress.advance(List.of(first, second), 1);
        assertEquals(1, first.getRemainingSeconds());
        assertEquals(0, second.getSavedSeconds());
        assertEquals(6, second.getVisualRemainingItemTime());
        QueueProgress.advance(List.of(first, second), 3);
        assertTrue(first.isDone());
        assertEquals(2, second.getSavedSeconds());
        assertEquals(3, second.getVisualRemainingItemTime());
    }
    @Test void completionAndZeroDurationAreStableAndNegativeElapsedIsIgnored() {
        QueueItem instant = item(0), regular = item(1);
        QueueProgress.advance(List.of(instant, regular), -5);
        assertTrue(instant.isDone());
        assertEquals(0, regular.getSavedSeconds());
        QueueProgress.advance(List.of(instant, regular), Long.MAX_VALUE);
        assertTrue(regular.isDone());
        assertEquals(1, regular.getSavedSeconds());
        assertEquals(0, regular.getVisualRemainingItemTime());
    }
}
