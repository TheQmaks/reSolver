package cli.li.resolver.captcha;

import java.util.Map;
import java.util.HashMap;

/**
 * Class for tracking CAPTCHA service statistics
 */
public class CaptchaServiceStatistics {
    private int totalAttempts = 0;
    private int successfulAttempts = 0;
    private int failedAttempts = 0;
    private long totalSolvingTimeMs = 0;

    // Statistics per CAPTCHA type
    private final Map<CaptchaType, TypeStatistics> typeStats = new HashMap<>();

    public void recordAttempt(CaptchaType type, boolean success, long solvingTimeMs) {
        totalAttempts++;
        if (success) {
            successfulAttempts++;
        } else {
            failedAttempts++;
        }
        totalSolvingTimeMs += solvingTimeMs;

        // Update type-specific statistics
        TypeStatistics stats = typeStats.computeIfAbsent(type, k -> new TypeStatistics());
        stats.recordAttempt(success, solvingTimeMs);
    }

    public int getTotalAttempts() {
        return totalAttempts;
    }

    public int getSuccessfulAttempts() {
        return successfulAttempts;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public double getSuccessRate() {
        return totalAttempts > 0 ? (double) successfulAttempts / totalAttempts : 0;
    }

    public long getAverageSolvingTimeMs() {
        return successfulAttempts > 0 ? totalSolvingTimeMs / successfulAttempts : 0;
    }

    public Map<CaptchaType, TypeStatistics> getTypeStats() {
        return typeStats;
    }

    public static class TypeStatistics {
        private int attempts = 0;
        private int successful = 0;
        private int failed = 0;
        private long totalTimeMs = 0;

        public void recordAttempt(boolean success, long timeMs) {
            attempts++;
            if (success) {
                successful++;
            } else {
                failed++;
            }
            totalTimeMs += timeMs;
        }

        public int getAttempts() {
            return attempts;
        }

        public int getSuccessful() {
            return successful;
        }

        public int getFailed() {
            return failed;
        }

        public double getSuccessRate() {
            return attempts > 0 ? (double) successful / attempts : 0;
        }

        public long getAverageTimeMs() {
            return successful > 0 ? totalTimeMs / successful : 0;
        }
    }
}