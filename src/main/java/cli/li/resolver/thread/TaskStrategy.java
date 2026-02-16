package cli.li.resolver.thread;

/**
 * Enum for task execution strategies
 */
public enum TaskStrategy {
    /**
     * Blocking strategy - wait if thread pool is full
     */
    BLOCKING,

    /**
     * Non-blocking strategy - reject if thread pool is full
     */
    NON_BLOCKING,

    /**
     * Rate limited strategy - slow down requests if load is high
     */
    RATE_LIMITED
}
