package studio.magemonkey.fusion.data.queue;

import java.util.List;

/** Pure queue timing. Presentation never determines which craft may advance. */
public final class QueueProgress {
    private QueueProgress() { }

    public static void advance(List<QueueItem> items, long seconds) {
        long remaining = Math.max(0, seconds);
        for (QueueItem item : items) {
            if (item.isDone()) continue;
            int applied = (int) Math.min(remaining, item.getRemainingSeconds());
            item.progressOffline(applied);
            remaining -= applied;
            if (remaining == 0) break;
        }
        refreshTimes(items);
    }

    public static int refreshTimes(List<QueueItem> items) {
        long total = 0;
        for (QueueItem item : items) {
            total += item.getRemainingSeconds();
            item.setVisualRemainingItemTime(item.isDone() ? 0 : (int) Math.min(Integer.MAX_VALUE, total));
        }
        return (int) Math.min(Integer.MAX_VALUE, total);
    }
}
