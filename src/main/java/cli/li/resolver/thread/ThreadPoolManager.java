package cli.li.resolver.thread;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import cli.li.resolver.settings.SettingsManager;

/**
 * Manager for thread pool used to solve CAPTCHAs
 */
public class ThreadPoolManager {
    private final ExecutorService threadPool;
    private final SettingsManager settingsManager;
    private final AtomicInteger activeThreads = new AtomicInteger(0);

    public ThreadPoolManager(SettingsManager settingsManager) {
        this.settingsManager = settingsManager;

        // Create thread pool with size from settings
        int poolSize = settingsManager.getThreadPoolSize();
        threadPool = Executors.newFixedThreadPool(poolSize, new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "CAPTCHA-Solver-" + threadNumber.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    /**
     * Submit a task to the thread pool
     * @param task Task to execute
     * @param <T> Result type
     * @return Future representing the result
     */
    public <T> Future<T> submit(Callable<T> task) {
        activeThreads.incrementAndGet();
        return threadPool.submit(() -> {
            try {
                return task.call();
            } finally {
                activeThreads.decrementAndGet();
            }
        });
    }

    /**
     * Get the current size of the thread pool
     * @return Thread pool size
     */
    public int getPoolSize() {
        return settingsManager.getThreadPoolSize();
    }

    /**
     * Get the number of active threads
     * @return Active thread count
     */
    public int getActiveThreadCount() {
        return activeThreads.get();
    }

    /**
     * Shutdown the thread pool
     */
    public void shutdown() {
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            threadPool.shutdownNow();
        }
    }
}