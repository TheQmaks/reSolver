package cli.li.resolver.thread;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import cli.li.resolver.settings.SettingsManager;

/**
 * Manager for thread pool used to solve CAPTCHAs.
 * Uses ConcurrentLinkedQueue for tracking active tasks instead of synchronized ArrayList.
 */
public class ThreadPoolManager {
    private final ExecutorService threadPool;
    private final SettingsManager settingsManager;
    private final AtomicInteger activeThreads = new AtomicInteger(0);
    private final ConcurrentLinkedQueue<Future<?>> activeTasks = new ConcurrentLinkedQueue<>();

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
        // Clean completed futures before adding new ones
        activeTasks.removeIf(Future::isDone);

        activeThreads.incrementAndGet();
        Future<T> future = threadPool.submit(() -> {
            try {
                return task.call();
            } finally {
                activeThreads.decrementAndGet();
            }
        });

        activeTasks.add(future);

        return future;
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
     * Cancel all running tasks
     * @return Number of tasks cancelled
     */
    public int cancelAllTasks() {
        int cancelledCount = 0;

        Future<?> future;
        while ((future = activeTasks.poll()) != null) {
            if (!future.isDone() && !future.isCancelled()) {
                future.cancel(true);
                cancelledCount++;
            }
        }

        return cancelledCount;
    }

    /**
     * Shutdown the thread pool
     */
    public void shutdown() {
        // Cancel all tasks on shutdown
        cancelAllTasks();

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
