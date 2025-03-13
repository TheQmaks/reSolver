package cli.li.resolver.thread;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ScheduledExecutorService;

import cli.li.resolver.settings.SettingsManager;

/**
 * Detector for high load situations
 */
public class HighLoadDetector {
    private final SettingsManager settingsManager;
    private final AtomicInteger requestsInLastMinute = new AtomicInteger(0);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public HighLoadDetector(SettingsManager settingsManager) {
        this.settingsManager = settingsManager;

        // Schedule task to reset counter every minute
        scheduler.scheduleAtFixedRate(() -> {
            requestsInLastMinute.set(0);
        }, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * Register a new request
     */
    public void registerRequest() {
        requestsInLastMinute.incrementAndGet();
    }

    /**
     * Check if current load is high
     * @return true if load is high
     */
    public boolean isHighLoad() {
        return requestsInLastMinute.get() > settingsManager.getHighLoadThreshold();
    }

    /**
     * Get the number of requests in the last minute
     * @return Request count
     */
    public int getRequestsInLastMinute() {
        return requestsInLastMinute.get();
    }

    /**
     * Shutdown the scheduler
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            scheduler.shutdownNow();
        }
    }
}