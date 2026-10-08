package studio.magemonkey.fusion.gui;

import org.junit.jupiter.api.Test;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.*;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.gui.recipe.GuiUpdateCoordinator;
import static org.mockito.Mockito.*;

class GuiUpdateCoordinatorTest {
    @Test void requestsCoalesceAndReopenCancelsOldTask() {
        try (var bukkit = mockStatic(Bukkit.class); var fusion = mockStatic(Fusion.class)) {
            BukkitScheduler scheduler = mock(BukkitScheduler.class);
            BukkitTask first = mock(BukkitTask.class), second = mock(BukkitTask.class);
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            Fusion plugin = mock(Fusion.class);
            fusion.when(Fusion::getInstance).thenReturn(plugin);
            when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), eq(1L), eq(1L))).thenReturn(first, second);
            Runnable refresh = mock(Runnable.class), pulse = mock(Runnable.class);
            var coordinator = new GuiUpdateCoordinator(refresh, pulse);
            coordinator.open();
            coordinator.request(); coordinator.request(); coordinator.tick();
            verify(refresh, times(1)).run();
            coordinator.open();
            verify(first).cancel();
            coordinator.close();
            verify(second).cancel();
        }
    }
}
