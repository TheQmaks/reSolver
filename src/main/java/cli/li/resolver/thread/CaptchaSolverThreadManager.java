package cli.li.resolver.thread;

import java.util.concurrent.*;

import cli.li.resolver.captcha.CaptchaRequest;
import cli.li.resolver.captcha.ICaptchaSolver;
import cli.li.resolver.captcha.CaptchaSolverException;

/**
 * Manager for CAPTCHA solver threads
 */
public record CaptchaSolverThreadManager(ThreadPoolManager threadPoolManager, QueueManager queueManager,
                                         HighLoadDetector highLoadDetector) {

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

        // Check if we're in high load situation
        boolean isHighLoad = highLoadDetector.isHighLoad();

        // Create solve task
        CaptchaSolveTask task = new CaptchaSolveTask(solver, request);

        // Handle according to strategy
        if (isHighLoad && strategy == TaskStrategy.NON_BLOCKING) {
            // In high load with non-blocking strategy, reject if pool is full
            if (threadPoolManager.getActiveThreadCount() >= threadPoolManager.getPoolSize()) {
                throw new RejectedExecutionException("Thread pool is full and non-blocking strategy is used");
            }
        }

        // Submit task to thread pool
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
        int retries = 0;
        CaptchaSolverException lastException = null;

        while (retries <= maxRetries) {
            try {
                Future<String> future = submitTask(solver, request);
                return future.get(30, TimeUnit.SECONDS); // Default timeout of 30 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CaptchaSolverException("Task interrupted", e);
            } catch (ExecutionException e) {
                lastException = new CaptchaSolverException("Execution error", e.getCause());
                retries++;
            } catch (TimeoutException e) {
                lastException = new CaptchaSolverException("Task timed out", e);
                retries++;
            } catch (RejectedExecutionException e) {
                lastException = new CaptchaSolverException("Task rejected", e);
                retries++;
            }
        }

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