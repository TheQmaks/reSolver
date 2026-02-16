package cli.li.resolver.provider.selection;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Circuit breaker for provider fault tolerance.
 * Prevents repeatedly trying a provider that is consistently failing.
 *
 * States: CLOSED (normal) -> OPEN (after threshold failures) -> HALF_OPEN (after cooldown) -> CLOSED
 */
public class CircuitBreaker {

    private enum State { CLOSED, OPEN, HALF_OPEN }

    private static final int FAILURE_THRESHOLD = 5;
    private static final long COOLDOWN_SECONDS = 60;

    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private volatile Instant openedAt = Instant.EPOCH;

    /**
     * Record a successful call. Resets the circuit breaker to CLOSED state.
     */
    public void recordSuccess() {
        consecutiveFailures.set(0);
        state.set(State.CLOSED);
    }

    /**
     * Record a failed call. If failures exceed the threshold, opens the circuit.
     */
    public void recordFailure() {
        int failures = consecutiveFailures.incrementAndGet();
        if (failures >= FAILURE_THRESHOLD) {
            if (state.compareAndSet(State.CLOSED, State.OPEN) ||
                    state.compareAndSet(State.HALF_OPEN, State.OPEN)) {
                openedAt = Instant.now();
            }
        }
    }

    /**
     * Check if the circuit is open (provider should be skipped).
     * If the cooldown period has elapsed, transitions to HALF_OPEN to allow a test request.
     *
     * @return true if the circuit is open and the provider should be skipped
     */
    public boolean isOpen() {
        State currentState = state.get();
        if (currentState == State.CLOSED) {
            return false;
        }
        if (currentState == State.OPEN) {
            if (Instant.now().isAfter(openedAt.plusSeconds(COOLDOWN_SECONDS))) {
                state.compareAndSet(State.OPEN, State.HALF_OPEN);
                return false;
            }
            return true;
        }
        // HALF_OPEN - allow one test request
        return false;
    }

    /**
     * Get the current number of consecutive failures.
     *
     * @return consecutive failure count
     */
    public int getConsecutiveFailures() {
        return consecutiveFailures.get();
    }

    /**
     * Reset the circuit breaker to its initial state.
     */
    public void reset() {
        state.set(State.CLOSED);
        consecutiveFailures.set(0);
        openedAt = Instant.EPOCH;
    }
}
