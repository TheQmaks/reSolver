package cli.li.resolver.thread;

import java.util.concurrent.*;

import cli.li.resolver.logger.LoggerService;
import cli.li.resolver.captcha.model.CaptchaRequest;
import cli.li.resolver.captcha.solver.ICaptchaSolver;
import cli.li.resolver.captcha.exception.CaptchaSolverException;

/**
 * Manager for CAPTCHA solver threads
 */
public record CaptchaSolverThreadManager(ThreadPoolManager threadPoolManager, QueueManager queueManager,
                                         HighLoadDetector highLoadDetector, LoggerService logger) {

    // Default timeout in seconds if not specified
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    // Minimum timeout value
    private static final int MIN_TIMEOUT_SECONDS = 10;
    // Maximum timeout value
    private static final int MAX_TIMEOUT_SECONDS = 120;

    public CaptchaSolverThreadManager {
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
        // Get timeout from additional parameters or use default
        int timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
        
        try {
            if (request.additionalParams() != null && 
                request.additionalParams().containsKey(CaptchaRequest.PARAM_TIMEOUT_SECONDS)) {
                
                String timeoutValue = request.additionalParams().get(CaptchaRequest.PARAM_TIMEOUT_SECONDS);
                if (timeoutValue != null && !timeoutValue.isEmpty()) {
                    try {
                        int requestedTimeout = Integer.parseInt(timeoutValue);
                        // Clamp timeout between min and max values
                        timeoutSeconds = Math.max(MIN_TIMEOUT_SECONDS, Math.min(requestedTimeout, MAX_TIMEOUT_SECONDS));
                        
                        if (timeoutSeconds != requestedTimeout) {
                            logger.warning("CaptchaSolverThreadManager",
                                    "Requested timeout " + requestedTimeout + " seconds was outside allowed range. " +
                                    "Using " + timeoutSeconds + " seconds instead.");
                        }
                    } catch (NumberFormatException e) {
                        logger.warning("CaptchaSolverThreadManager",
                                "Invalid timeout value in additional parameters: '" + 
                                timeoutValue + "'. Using default of " + DEFAULT_TIMEOUT_SECONDS + " seconds.");
                    }
                }
            }
        } catch (Exception e) {
            // If something went wrong while getting the timeout, use default value
            logger.warning("CaptchaSolverThreadManager", 
                    "Exception while parsing timeout parameter: " + e.getMessage() + 
                    ". Using default of " + DEFAULT_TIMEOUT_SECONDS + " seconds.");
        }
        
        logger.info("CaptchaSolverThreadManager",
                "Executing CAPTCHA solving task with retry, max retries: " + maxRetries + 
                ", timeout: " + timeoutSeconds + " seconds");

        int retries = 0;
        CaptchaSolverException lastException = null;

        while (retries <= maxRetries) {
            Future<String> future = null;
            try {
                if (retries > 0) {
                    logger.warning("CaptchaSolverThreadManager",
                            "Retrying CAPTCHA solving, attempt " + retries + " of " + maxRetries);
                }

                future = submitTask(solver, request);
                logger.debug("CaptchaSolverThreadManager",
                        "Waiting for CAPTCHA solution with timeout of " + timeoutSeconds + " seconds");

                String result = future.get(timeoutSeconds, TimeUnit.SECONDS);

                logger.info("CaptchaSolverThreadManager",
                        "CAPTCHA solved successfully" + (retries > 0 ? " after " + retries + " retries" : ""));
                return result;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("CaptchaSolverThreadManager", "Task interrupted", e);
                throw new CaptchaSolverException("Task interrupted", e);

            } catch (ExecutionException e) {
                String errorMessage = (e.getCause() != null) ? e.getCause().getMessage() : "Unknown error";
                Throwable cause = (e.getCause() != null) ? e.getCause() : e;
                
                logger.error("CaptchaSolverThreadManager", "Execution error: " + errorMessage, cause);
                lastException = new CaptchaSolverException("Execution error: " + errorMessage, cause);
                retries++;
                
                // Cancel the current task if it exists and is not yet completed
                if (future != null && !future.isDone()) {
                    future.cancel(true);
                }

            } catch (TimeoutException e) {
                logger.error("CaptchaSolverThreadManager", "Task timed out after " + timeoutSeconds + " seconds", e);
                lastException = new CaptchaSolverException("Task timed out after " + timeoutSeconds + " seconds", e);
                retries++;
                
                // Important: cancel the task that's taking too long
                if (future != null && !future.isDone()) {
                    boolean cancelled = future.cancel(true);
                    logger.debug("CaptchaSolverThreadManager", 
                            "Cancellation of timed out task " + (cancelled ? "succeeded" : "failed"));
                }

            } catch (RejectedExecutionException e) {
                logger.error("CaptchaSolverThreadManager", "Task rejected by thread pool", e);
                lastException = new CaptchaSolverException("Task rejected", e);
                retries++;
            } catch (Exception e) {
                // Catch all other exceptions to prevent unexpected failures
                logger.error("CaptchaSolverThreadManager", "Unexpected error: " + e.getMessage(), e);
                lastException = new CaptchaSolverException("Unexpected error: " + e.getMessage(), e);
                retries++;
                
                // Cancel the current task if it exists and is not yet completed
                if (future != null && !future.isDone()) {
                    future.cancel(true);
                }
            }
            
            // Small delay before next retry
            if (retries <= maxRetries) {
                try {
                    Thread.sleep(500); // 500 milliseconds between retry attempts
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new CaptchaSolverException("Retry process interrupted", ie);
                }
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