package studio.magemonkey.fusion.gui.recipe;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import studio.magemonkey.fusion.Fusion;

/** One owned task per open session; multiple requests in one tick coalesce. */
public final class GuiUpdateCoordinator implements AutoCloseable {
    private final Runnable fullRefresh;
    private final Runnable pulse;
    private BukkitTask task;
    private boolean dirty;
    private int ticks;
    public GuiUpdateCoordinator(Runnable fullRefresh, Runnable pulse) {
        this.fullRefresh = fullRefresh; this.pulse = pulse;
    }
    public void open() {
        close();
        dirty = true;
        ticks = 0;
        task = Bukkit.getScheduler().runTaskTimer(Fusion.getInstance(), this::tick, 1L, 1L);
    }
    public void request() { dirty = true; }
    public void tick() {
        boolean refreshed = dirty;
        if (refreshed) { dirty = false; fullRefresh.run(); }
        if (++ticks % 20 == 0 && !refreshed) pulse.run();
    }
    @Override public void close() {
        if (task != null) task.cancel();
        task = null;
        dirty = false;
    }
}
