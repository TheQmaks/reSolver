package cli.li.resolver.provider;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Statistics for tracking provider performance.
 * All operations are thread-safe using atomic variables.
 */
public class ProviderStatistics {

    private final AtomicInteger totalRequests = new AtomicInteger(0);
    private final AtomicInteger successfulRequests = new AtomicInteger(0);
    private final AtomicInteger failedRequests = new AtomicInteger(0);
    private final AtomicLong totalSolveTimeMs = new AtomicLong(0);

    /**
     * Record a successful solve attempt.
     *
     * @param solveTimeMs time taken to solve in milliseconds
     */
    public void recordSuccess(long solveTimeMs) {
        totalRequests.incrementAndGet();
        successfulRequests.incrementAndGet();
        totalSolveTimeMs.addAndGet(solveTimeMs);
    }

    /**
     * Record a failed solve attempt.
     */
    public void recordFailure() {
        totalRequests.incrementAndGet();
        failedRequests.incrementAndGet();
    }

    /**
     * Get success rate as a percentage.
     *
     * @return success rate from 0 to 100, or 0 if no requests recorded
     */
    public double getSuccessRate() {
        int total = totalRequests.get();
        if (total == 0) {
            return 0.0;
        }
        return (double) successfulRequests.get() / total * 100.0;
    }

    /**
     * Get average solve time in milliseconds.
     *
     * @return average solve time, or 0 if no successful solves
     */
    public double getAvgSolveTimeMs() {
        int successful = successfulRequests.get();
        if (successful == 0) {
            return 0.0;
        }
        return (double) totalSolveTimeMs.get() / successful;
    }

    /**
     * Get total number of requests.
     *
     * @return total requests count
     */
    public int getTotalRequests() {
        return totalRequests.get();
    }

    /**
     * Get number of successful requests.
     *
     * @return successful requests count
     */
    public int getSuccessfulRequests() {
        return successfulRequests.get();
    }

    /**
     * Get number of failed requests.
     *
     * @return failed requests count
     */
    public int getFailedRequests() {
        return failedRequests.get();
    }

    /**
     * Reset all statistics to zero.
     */
    public void reset() {
        totalRequests.set(0);
        successfulRequests.set(0);
        failedRequests.set(0);
        totalSolveTimeMs.set(0);
    }
}
