package cli.li.resolver.thread;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.ArrayList;

import cli.li.resolver.settings.SettingsManager;

/**
 * Manager for thread pool used to solve CAPTCHAs
 */
public class ThreadPoolManager {
    private final ExecutorService threadPool;
    private final SettingsManager settingsManager;
    private final AtomicInteger activeThreads = new AtomicInteger(0);
    private final List<Future<?>> activeTasks = new ArrayList<>();

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
        Future<T> future = threadPool.submit(() -> {
            try {
                return task.call();
            } finally {
                activeThreads.decrementAndGet();
                synchronized (activeTasks) {
                    activeTasks.removeIf(f -> f.isDone() || f.isCancelled());
                }
            }
        });
        
        synchronized (activeTasks) {
            activeTasks.add(future);
        }
        
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
        
        synchronized (activeTasks) {
            // Remove all completed tasks from the list
            activeTasks.removeIf(future -> future.isDone() || future.isCancelled());
            
            // Cancel all remaining active tasks
            for (Future<?> future : activeTasks) {
                if (!future.isDone() && !future.isCancelled()) {
                    future.cancel(true);
                    cancelledCount++;
                }
            }
            
            // Clear the task list
            activeTasks.clear();
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