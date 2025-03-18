package cli.li.resolver.stats;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import cli.li.resolver.service.ServiceManager;
import cli.li.resolver.captcha.model.CaptchaType;
import cli.li.resolver.captcha.solver.ICaptchaSolver;
import cli.li.resolver.service.captcha.ICaptchaService;

/**
 * Collector for CAPTCHA solving statistics
 */
public class StatisticsCollector {
    private final ServiceManager serviceManager;
    private final Map<CaptchaType, TypeStats> typeStats = new ConcurrentHashMap<>();
    private final AtomicInteger totalAttempts = new AtomicInteger(0);
    private final AtomicInteger successfulAttempts = new AtomicInteger(0);
    private final AtomicLong totalSolvingTimeMs = new AtomicLong(0);

    public StatisticsCollector(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }

    /**
     * Record a CAPTCHA solve attempt
     * @param captchaType CAPTCHA type
     * @param solver CAPTCHA solver used
     * @param success Whether the attempt was successful
     * @param solvingTimeMs Time taken to solve the CAPTCHA
     */
    public void recordSolveAttempt(CaptchaType captchaType, ICaptchaSolver solver, boolean success, long solvingTimeMs) {
        // Update global statistics
        totalAttempts.incrementAndGet();
        if (success) {
            successfulAttempts.incrementAndGet();
            totalSolvingTimeMs.addAndGet(solvingTimeMs);
        }

        // Update type-specific statistics
        TypeStats stats = typeStats.computeIfAbsent(captchaType, k -> new TypeStats());
        stats.recordAttempt(success, solvingTimeMs);

        // Update service-specific statistics
        for (ICaptchaService service : serviceManager.getServicesInPriorityOrder()) {
            if (service.getSolver(captchaType) == solver) {
                service.getStatistics().recordAttempt(captchaType, success, solvingTimeMs);
                break;
            }
        }
    }

    /**
     * Get the total number of attempts
     * @return Total attempt count
     */
    public int getTotalAttempts() {
        return totalAttempts.get();
    }

    /**
     * Get the number of successful attempts
     * @return Successful attempt count
     */
    public int getSuccessfulAttempts() {
        return successfulAttempts.get();
    }

    /**
     * Get the success rate
     * @return Success rate (0-1)
     */
    public double getSuccessRate() {
        int total = totalAttempts.get();
        return total > 0 ? (double) successfulAttempts.get() / total : 0;
    }

    /**
     * Get the average solving time
     * @return Average solving time in milliseconds
     */
    public double getAverageSolvingTimeMs() {
        int successful = successfulAttempts.get();
        return successful > 0 ? (double) totalSolvingTimeMs.get() / successful : 0;
    }

    /**
     * Get statistics for all CAPTCHA types
     * @return Map of CAPTCHA type to statistics
     */
    public Map<CaptchaType, TypeStats> getTypeStats() {
        return new HashMap<>(typeStats);
    }

    /**
     * Reset all statistics
     */
    public void reset() {
        totalAttempts.set(0);
        successfulAttempts.set(0);
        totalSolvingTimeMs.set(0);
        typeStats.clear();

        // Reset service statistics
        for (ICaptchaService service : serviceManager.getServicesInPriorityOrder()) {
            service.getStatistics(); // Create a new statistics object
        }
    }

    /**
     * CAPTCHA type statistics
     */
    public static class TypeStats {
        private final AtomicInteger attempts = new AtomicInteger(0);
        private final AtomicInteger successful = new AtomicInteger(0);
        private final AtomicLong totalTimeMs = new AtomicLong(0);

        /**
         * Record an attempt
         * @param success Whether the attempt was successful
         * @param timeMs Time taken
         */
        public void recordAttempt(boolean success, long timeMs) {
            attempts.incrementAndGet();
            if (success) {
                successful.incrementAndGet();
                totalTimeMs.addAndGet(timeMs);
            }
        }

        /**
         * Get the total number of attempts
         * @return Total attempt count
         */
        public int getAttempts() {
            return attempts.get();
        }

        /**
         * Get the number of successful attempts
         * @return Successful attempt count
         */
        public int getSuccessful() {
            return successful.get();
        }

        /**
         * Get the success rate
         * @return Success rate (0-1)
         */
        public double getSuccessRate() {
            int total = attempts.get();
            return total > 0 ? (double) successful.get() / total : 0;
        }

        /**
         * Get the average solving time
         * @return Average solving time in milliseconds
         */
        public double getAverageSolvingTimeMs() {
            int successCount = successful.get();
            return successCount > 0 ? (double) totalTimeMs.get() / successCount : 0;
        }
    }
}