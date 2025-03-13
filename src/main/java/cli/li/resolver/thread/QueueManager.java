package cli.li.resolver.thread;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import cli.li.resolver.settings.SettingsManager;

/**
 * Manager for queue of CAPTCHA solving requests
 */
public class QueueManager {
    private final SettingsManager settingsManager;
    private final BlockingQueue<CaptchaSolveTask> taskQueue;

    public QueueManager(SettingsManager settingsManager) {
        this.settingsManager = settingsManager;

        // Create queue with size from settings
        int queueSize = settingsManager.getQueueSize();
        taskQueue = new LinkedBlockingQueue<>(queueSize);
    }

    /**
     * Add a task to the queue
     * @param task Task to add
     * @return true if the task was added, false if the queue is full
     */
    public boolean offerTask(CaptchaSolveTask task) {
        return taskQueue.offer(task);
    }

    /**
     * Add a task to the queue with a timeout
     * @param task Task to add
     * @param timeout Timeout
     * @param unit Timeout unit
     * @return true if the task was added, false if the queue is full or timeout elapsed
     * @throws InterruptedException If interrupted while waiting
     */
    public boolean offerTask(CaptchaSolveTask task, long timeout, TimeUnit unit) throws InterruptedException {
        return taskQueue.offer(task, timeout, unit);
    }

    /**
     * Take a task from the queue (blocking)
     * @return Next task
     * @throws InterruptedException If interrupted while waiting
     */
    public CaptchaSolveTask takeTask() throws InterruptedException {
        return taskQueue.take();
    }

    /**
     * Poll a task from the queue with a timeout
     * @param timeout Timeout
     * @param unit Timeout unit
     * @return Task or null if queue is empty or timeout elapsed
     * @throws InterruptedException If interrupted while waiting
     */
    public CaptchaSolveTask pollTask(long timeout, TimeUnit unit) throws InterruptedException {
        return taskQueue.poll(timeout, unit);
    }

    /**
     * Get the current size of the queue
     * @return Queue size
     */
    public int getQueueSize() {
        return taskQueue.size();
    }

    /**
     * Get the remaining capacity of the queue
     * @return Remaining capacity
     */
    public int getRemainingCapacity() {
        return taskQueue.remainingCapacity();
    }
}