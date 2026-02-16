package cli.li.resolver.thread;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;

import cli.li.resolver.settings.SettingsManager;

/**
 * Detector for high load situations using a sliding window of timestamps.
 * No scheduled executor is needed - timestamps older than 60 seconds are
 * cleaned up lazily when queried.
 */
public class HighLoadDetector {
    private static final long WINDOW_MS = 60_000L; // 60 seconds

    private final SettingsManager settingsManager;
    private final ConcurrentLinkedDeque<Long> timestamps = new ConcurrentLinkedDeque<>();

    public HighLoadDetector(SettingsManager settingsManager) {
        this.settingsManager = settingsManager;
    }

    /**
     * Register a new request by adding the current timestamp
     */
    public void registerRequest() {
        timestamps.addLast(System.currentTimeMillis());
    }

    /**
     * Check if current load is high.
     * Removes expired timestamps and compares the count with the threshold.
     * @return true if load is high
     */
    public boolean isHighLoad() {
        cleanExpired();
        return timestamps.size() > settingsManager.getHighLoadThreshold();
    }

    /**
     * Get the number of requests in the last minute.
     * Removes expired timestamps before counting.
     * @return Request count in the sliding window
     */
    public int getRequestsInLastMinute() {
        cleanExpired();
        return timestamps.size();
    }

    /**
     * Remove timestamps older than the sliding window (60 seconds)
     */
    private void cleanExpired() {
        long cutoff = System.currentTimeMillis() - WINDOW_MS;
        Iterator<Long> it = timestamps.iterator();
        while (it.hasNext()) {
            if (it.next() < cutoff) {
                it.remove();
            } else {
                // Timestamps are added in order, so once we find one that's not expired,
                // all subsequent ones are also not expired
                break;
            }
        }
    }

    /**
     * Shutdown - no-op since there is no scheduled executor to shut down
     */
    public void shutdown() {
        // No executor to shut down; sliding window is self-cleaning
    }
}
