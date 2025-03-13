package cli.li.resolver.captcha.model;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Statistics for CAPTCHA service usage
 */
public class CaptchaServiceStatistics {
    private final AtomicInteger totalRequests = new AtomicInteger(0);
    private final AtomicInteger successfulRequests = new AtomicInteger(0);
    private final AtomicInteger failedRequests = new AtomicInteger(0);
    private final AtomicLong totalSolvingTimeMs = new AtomicLong(0);
    private final Map<CaptchaType, TypeStats> typesStats = new ConcurrentHashMap<>();
    
    /**
     * Register a successful CAPTCHA solving request
     */
    public void registerSuccess() {
        totalRequests.incrementAndGet();
        successfulRequests.incrementAndGet();
    }
    
    /**
     * Register a failed CAPTCHA solving request
     */
    public void registerFailure() {
        totalRequests.incrementAndGet();
        failedRequests.incrementAndGet();
    }
    
    /**
     * Record a CAPTCHA solving attempt with detailed stats
     * 
     * @param captchaType Type of CAPTCHA
     * @param success Whether the attempt was successful
     * @param solvingTimeMs Time taken to solve the CAPTCHA
     */
    public void recordAttempt(CaptchaType captchaType, boolean success, long solvingTimeMs) {
        // Update global statistics
        totalRequests.incrementAndGet();
        if (success) {
            successfulRequests.incrementAndGet();
            totalSolvingTimeMs.addAndGet(solvingTimeMs);
        } else {
            failedRequests.incrementAndGet();
        }
        
        // Update type-specific statistics
        TypeStats stats = typesStats.computeIfAbsent(captchaType, k -> new TypeStats());
        stats.recordAttempt(success, solvingTimeMs);
    }
    
    /**
     * Get total number of requests
     * @return Total requests count
     */
    public int getTotalRequests() {
        return totalRequests.get();
    }
    
    /**
     * Get number of successful requests
     * @return Successful requests count
     */
    public int getSuccessfulRequests() {
        return successfulRequests.get();
    }
    
    /**
     * Get number of failed requests
     * @return Failed requests count
     */
    public int getFailedRequests() {
        return failedRequests.get();
    }
    
    /**
     * Get success rate (percentage)
     * @return Success rate as percentage (0-100)
     */
    public double getSuccessRate() {
        int total = totalRequests.get();
        if (total == 0) {
            return 0.0;
        }
        return (double) successfulRequests.get() / total * 100.0;
    }
    
    /**
     * Get average solving time in milliseconds
     * @return Average solving time or 0 if no successful solves
     */
    public double getAverageSolvingTimeMs() {
        int successful = successfulRequests.get();
        if (successful == 0) {
            return 0.0;
        }
        return (double) totalSolvingTimeMs.get() / successful;
    }
    
    /**
     * Get statistics for a specific CAPTCHA type
     * @param captchaType Type of CAPTCHA
     * @return TypeStats object or null if no stats exist for this type
     */
    public TypeStats getTypeStats(CaptchaType captchaType) {
        return typesStats.get(captchaType);
    }
    
    /**
     * Get all type-specific statistics
     * @return Map of CAPTCHA type to TypeStats
     */
    public Map<CaptchaType, TypeStats> getAllTypeStats() {
        return typesStats;
    }
    
    /**
     * Reset all statistics
     */
    public void reset() {
        totalRequests.set(0);
        successfulRequests.set(0);
        failedRequests.set(0);
        totalSolvingTimeMs.set(0);
        typesStats.clear();
    }
    
    /**
     * Statistics for a specific CAPTCHA type
     */
    public static class TypeStats {
        private final AtomicInteger attempts = new AtomicInteger(0);
        private final AtomicInteger successes = new AtomicInteger(0);
        private final AtomicLong totalSolvingTimeMs = new AtomicLong(0);
        
        /**
         * Record an attempt
         * @param success Whether the attempt was successful
         * @param solvingTimeMs Time taken to solve
         */
        public void recordAttempt(boolean success, long solvingTimeMs) {
            attempts.incrementAndGet();
            if (success) {
                successes.incrementAndGet();
                totalSolvingTimeMs.addAndGet(solvingTimeMs);
            }
        }
        
        /**
         * Get number of attempts
         * @return Attempt count
         */
        public int getAttempts() {
            return attempts.get();
        }
        
        /**
         * Get number of successful attempts
         * @return Success count
         */
        public int getSuccesses() {
            return successes.get();
        }
        
        /**
         * Get success rate (percentage)
         * @return Success rate as percentage (0-100)
         */
        public double getSuccessRate() {
            int total = attempts.get();
            if (total == 0) {
                return 0.0;
            }
            return (double) successes.get() / total * 100.0;
        }
        
        /**
         * Get average solving time in milliseconds
         * @return Average solving time or 0 if no successful solves
         */
        public double getAverageSolvingTimeMs() {
            int success = successes.get();
            if (success == 0) {
                return 0.0;
            }
            return (double) totalSolvingTimeMs.get() / success;
        }
    }
}
