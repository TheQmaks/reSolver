package cli.li.resolver.thread;

import cli.li.resolver.logger.LoggerService;

/**
 * Manager for CAPTCHA solver threads.
 * Provides thread pool management, high load detection, and task cancellation.
 */
public record CaptchaSolverThreadManager(ThreadPoolManager threadPoolManager,
                                         HighLoadDetector highLoadDetector, LoggerService logger) {

    public CaptchaSolverThreadManager {
        logger.info("CaptchaSolverThreadManager", "Thread manager initialized");
    }

    /**
     * Shutdown the thread manager and all its resources
     */
    public void shutdown() {
        logger.info("CaptchaSolverThreadManager", "Shutting down thread manager");

        if (threadPoolManager != null) {
            threadPoolManager.shutdown();
        }

        if (highLoadDetector != null) {
            highLoadDetector.shutdown();
        }

        logger.info("CaptchaSolverThreadManager", "Thread manager shutdown complete");
    }

    /**
     * Cancel all current CAPTCHA solving tasks
     * @return Number of tasks cancelled
     */
    public int cancelAllTasks() {
        logger.info("CaptchaSolverThreadManager", "Cancelling all current CAPTCHA solving tasks");

        if (threadPoolManager != null) {
            int cancelled = threadPoolManager.cancelAllTasks();
            logger.info("CaptchaSolverThreadManager", "Cancelled " + cancelled + " running CAPTCHA solving tasks");
            return cancelled;
        }

        return 0;
    }
}
