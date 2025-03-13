package cli.li.resolver.thread;

import java.util.concurrent.*;

import cli.li.resolver.captcha.CaptchaRequest;
import cli.li.resolver.captcha.ICaptchaSolver;
import cli.li.resolver.captcha.CaptchaSolverException;
import cli.li.resolver.logger.LoggerService;

/**
 * Manager for CAPTCHA solver threads
 */
public record CaptchaSolverThreadManager(ThreadPoolManager threadPoolManager, QueueManager queueManager,
                                         HighLoadDetector highLoadDetector) {
    private static LoggerService logger;

    public CaptchaSolverThreadManager {
        this.logger = logger;
        logger.info("CaptchaSolverThreadManager", "Thread manager initialized");
    }

    /**
     * Submit a CAPTCHA solving task with default strategy
     *
     * @param solver  CAPTCHA solver
     * @param request CAPTCHA request
     * @return Future representing the result
     * @throws RejectedExecutionException If the task cannot be scheduled for execution
     */
    public Future<String> submitTask(ICaptchaSolver solver, CaptchaRequest request) throws RejectedExecutionException {
        return submitTask(solver, request, TaskStrategy.BLOCKING);
    }

    /**
     * Submit a CAPTCHA solving task with specified strategy
     *
     * @param solver   CAPTCHA solver
     * @param request  CAPTCHA request
     * @param strategy Task strategy
     * @return Future representing the result
     * @throws RejectedExecutionException If the task cannot be scheduled for execution
     */
    public Future<String> submitTask(ICaptchaSolver solver, CaptchaRequest request, TaskStrategy strategy) throws RejectedExecutionException {
        // Register request for load detection
        highLoadDetector.registerRequest();
        logger.debug("CaptchaSolverThreadManager",
                "Submitting CAPTCHA solving task for " + request.captchaType() +
                        " using strategy: " + strategy);

        // Check if we're in high load situation
        boolean isHighLoad = highLoadDetector.isHighLoad();
        if (isHighLoad) {
            logger.warning("CaptchaSolverThreadManager",
                    "System under high load: " + highLoadDetector.getRequestsInLastMinute() +
                            " requests in the last minute");
        }

        // Create solve task
        CaptchaSolveTask task = new CaptchaSolveTask(solver, request);

        // Handle according to strategy
        if (isHighLoad && strategy == TaskStrategy.NON_BLOCKING) {
            // In high load with non-blocking strategy, reject if pool is full
            int activeThreads = threadPoolManager.getActiveThreadCount();
            int poolSize = threadPoolManager.getPoolSize();

            logger.debug("CaptchaSolverThreadManager",
                    "Active threads: " + activeThreads + " of " + poolSize);

            if (activeThreads >= poolSize) {
                logger.warning("CaptchaSolverThreadManager",
                        "Task rejected: Thread pool is full (" + activeThreads + "/" + poolSize + ") " +
                                "and using non-blocking strategy");
                throw new RejectedExecutionException("Thread pool is full and non-blocking strategy is used");
            }
        }

        // Submit task to thread pool
        logger.info("CaptchaSolverThreadManager",
                "Submitting task to thread pool for " + request.captchaType() +
                        " CAPTCHA, site key: " + request.siteKey());
        return threadPoolManager.submit(task);
    }

    /**
     * Execute a task with retry logic
     *
     * @param solver     CAPTCHA solver
     * @param request    CAPTCHA request
     * @param maxRetries Maximum number of retries
     * @return Solved CAPTCHA token
     * @throws CaptchaSolverException If solving fails after all retries
     */
    public String executeWithRetry(ICaptchaSolver solver, CaptchaRequest request, int maxRetries) throws CaptchaSolverException {
        logger.info("CaptchaSolverThreadManager",
                "Executing CAPTCHA solving task with retry, max retries: " + maxRetries);

        int retries = 0;
        CaptchaSolverException lastException = null;

        while (retries <= maxRetries) {
            try {
                if (retries > 0) {
                    logger.warning("CaptchaSolverThreadManager",
                            "Retrying CAPTCHA solving, attempt " + retries + " of " + maxRetries);
                }

                Future<String> future = submitTask(solver, request);
                logger.debug("CaptchaSolverThreadManager",
                        "Waiting for CAPTCHA solution with timeout of 30 seconds");

                String result = future.get(30, TimeUnit.SECONDS); // Default timeout of 30 seconds

                logger.info("CaptchaSolverThreadManager",
                        "CAPTCHA solved successfully" + (retries > 0 ? " after " + retries + " retries" : ""));
                return result;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("CaptchaSolverThreadManager", "Task interrupted", e);
                throw new CaptchaSolverException("Task interrupted", e);

            } catch (ExecutionException e) {
                logger.error("CaptchaSolverThreadManager",
                        "Execution error: " + e.getCause().getMessage(), e.getCause());
                lastException = new CaptchaSolverException("Execution error", e.getCause());
                retries++;

            } catch (TimeoutException e) {
                logger.error("CaptchaSolverThreadManager", "Task timed out after 30 seconds", e);
                lastException = new CaptchaSolverException("Task timed out", e);
                retries++;

            } catch (RejectedExecutionException e) {
                logger.error("CaptchaSolverThreadManager", "Task rejected by thread pool", e);
                lastException = new CaptchaSolverException("Task rejected", e);
                retries++;
            }
        }

        logger.error("CaptchaSolverThreadManager",
                "Failed to solve CAPTCHA after " + maxRetries + " retries");
        throw new CaptchaSolverException("Failed after " + maxRetries + " retries", lastException);
    }

    /**
     * Get the thread pool manager
     *
     * @return Thread pool manager
     */
    @Override
    public ThreadPoolManager threadPoolManager() {
        return threadPoolManager;
    }

    /**
     * Get the queue manager
     *
     * @return Queue manager
     */
    @Override
    public QueueManager queueManager() {
        return queueManager;
    }

    /**
     * Get the high load detector
     *
     * @return High load detector
     */
    @Override
    public HighLoadDetector highLoadDetector() {
        return highLoadDetector;
    }
}