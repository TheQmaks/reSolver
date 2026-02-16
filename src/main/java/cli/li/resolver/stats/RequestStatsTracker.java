package cli.li.resolver.stats;

import java.util.List;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import burp.api.montoya.http.message.requests.HttpRequest;

/**
 * Tracker for HTTP request statistics
 */
public class RequestStatsTracker {
    private final AtomicInteger totalRequests = new AtomicInteger(0);
    private final AtomicInteger modifiedRequests = new AtomicInteger(0);
    private final ConcurrentMap<String, TargetStats> targetStats = new ConcurrentHashMap<>();
    private final List<RequestLogEntry> recentRequests = new ArrayList<>();
    private static final int MAX_RECENT_REQUESTS = 100;

    /**
     * Record a new request
     * @param request HTTP request
     * @param modified Whether the request was modified
     */
    public synchronized void recordRequest(HttpRequest request, boolean modified) {
        totalRequests.incrementAndGet();
        if (modified) {
            modifiedRequests.incrementAndGet();
        }

        // Update target statistics
        String host = request.httpService().host();
        TargetStats stats = targetStats.computeIfAbsent(host, h -> new TargetStats());
        stats.recordRequest(modified);

        // Add to recent requests
        RequestLogEntry entry = new RequestLogEntry(
                request.url(),
                modified,
                Instant.now()
        );

        recentRequests.add(0, entry);
        if (recentRequests.size() > MAX_RECENT_REQUESTS) {
            recentRequests.remove(MAX_RECENT_REQUESTS);
        }
    }

    /**
     * Get the total number of requests
     * @return Total request count
     */
    public int getTotalRequests() {
        return totalRequests.get();
    }

    /**
     * Get the number of modified requests
     * @return Modified request count
     */
    public int getModifiedRequests() {
        return modifiedRequests.get();
    }

    /**
     * Get statistics for all targets
     * @return Map of target (host) to statistics
     */
    public ConcurrentMap<String, TargetStats> getTargetStats() {
        return targetStats;
    }

    /**
     * Get recent request log
     * @return List of recent request log entries
     */
    public synchronized List<RequestLogEntry> getRecentRequests() {
        return new ArrayList<>(recentRequests);
    }

    /**
     * Reset all statistics
     */
    public synchronized void reset() {
        totalRequests.set(0);
        modifiedRequests.set(0);
        targetStats.clear();
        recentRequests.clear();
    }

    /**
     * Target statistics
     */
    public static class TargetStats {
        private final AtomicInteger requests = new AtomicInteger(0);
        private final AtomicInteger modifiedRequests = new AtomicInteger(0);

        /**
         * Record a new request
         * @param modified Whether the request was modified
         */
        public void recordRequest(boolean modified) {
            requests.incrementAndGet();
            if (modified) {
                modifiedRequests.incrementAndGet();
            }
        }

        /**
         * Get the total number of requests
         * @return Total request count
         */
        public int getRequests() {
            return requests.get();
        }

        /**
         * Get the number of modified requests
         * @return Modified request count
         */
        public int getModifiedRequests() {
            return modifiedRequests.get();
        }

        /**
         * Get the percentage of modified requests
         * @return Modified request percentage
         */
        public double getModifiedPercentage() {
            int total = requests.get();
            return total > 0 ? (double) modifiedRequests.get() / total * 100 : 0;
        }
    }

    /**
     * Request log entry
     */
    public static class RequestLogEntry {
        private final String url;
        private final boolean modified;
        private final Instant timestamp;

        public RequestLogEntry(String url, boolean modified, Instant timestamp) {
            this.url = url;
            this.modified = modified;
            this.timestamp = timestamp;
        }

        public String getUrl() {
            return url;
        }

        public boolean isModified() {
            return modified;
        }

        public Instant getTimestamp() {
            return timestamp;
        }
    }
}
