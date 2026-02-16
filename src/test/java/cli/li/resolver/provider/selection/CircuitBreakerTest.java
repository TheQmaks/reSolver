package cli.li.resolver.provider.selection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CircuitBreaker")
class CircuitBreakerTest {

    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        circuitBreaker = new CircuitBreaker();
    }

    @Test
    @DisplayName("initial state is closed with zero consecutive failures")
    void initialStateIsClosedWithZeroFailures() {
        assertThat(circuitBreaker.isOpen()).isFalse();
        assertThat(circuitBreaker.getConsecutiveFailures()).isZero();
    }

    @Test
    @DisplayName("four failures keep the circuit closed")
    void fourFailuresKeepCircuitClosed() {
        for (int i = 0; i < 4; i++) {
            circuitBreaker.recordFailure();
        }

        assertThat(circuitBreaker.isOpen()).isFalse();
        assertThat(circuitBreaker.getConsecutiveFailures()).isEqualTo(4);
    }

    @Test
    @DisplayName("five failures open the circuit")
    void fiveFailuresOpenCircuit() {
        for (int i = 0; i < 5; i++) {
            circuitBreaker.recordFailure();
        }

        assertThat(circuitBreaker.isOpen()).isTrue();
        assertThat(circuitBreaker.getConsecutiveFailures()).isEqualTo(5);
    }

    @Test
    @DisplayName("recordSuccess after opening closes the circuit")
    void recordSuccessClosesCircuit() {
        for (int i = 0; i < 5; i++) {
            circuitBreaker.recordFailure();
        }
        assertThat(circuitBreaker.isOpen()).isTrue();

        circuitBreaker.recordSuccess();

        assertThat(circuitBreaker.isOpen()).isFalse();
        assertThat(circuitBreaker.getConsecutiveFailures()).isZero();
    }

    @Test
    @DisplayName("reset restores initial state")
    void resetRestoresInitialState() {
        for (int i = 0; i < 5; i++) {
            circuitBreaker.recordFailure();
        }
        assertThat(circuitBreaker.isOpen()).isTrue();

        circuitBreaker.reset();

        assertThat(circuitBreaker.isOpen()).isFalse();
        assertThat(circuitBreaker.getConsecutiveFailures()).isZero();
    }

    @Test
    @DisplayName("recordSuccess resets consecutive failure count before reaching threshold")
    void recordSuccessResetsFailureCountBeforeThreshold() {
        circuitBreaker.recordFailure();
        circuitBreaker.recordFailure();
        circuitBreaker.recordFailure();

        circuitBreaker.recordSuccess();

        assertThat(circuitBreaker.getConsecutiveFailures()).isZero();
        assertThat(circuitBreaker.isOpen()).isFalse();
    }

    @Test
    @DisplayName("failures beyond threshold keep circuit open")
    void failuresBeyondThresholdKeepCircuitOpen() {
        for (int i = 0; i < 7; i++) {
            circuitBreaker.recordFailure();
        }

        assertThat(circuitBreaker.isOpen()).isTrue();
        assertThat(circuitBreaker.getConsecutiveFailures()).isEqualTo(7);
    }

    @Test
    @DisplayName("circuit can be reopened after being closed by recordSuccess")
    void circuitCanBeReopenedAfterClose() {
        // Open the circuit
        for (int i = 0; i < 5; i++) {
            circuitBreaker.recordFailure();
        }
        assertThat(circuitBreaker.isOpen()).isTrue();

        // Close it
        circuitBreaker.recordSuccess();
        assertThat(circuitBreaker.isOpen()).isFalse();

        // Open it again
        for (int i = 0; i < 5; i++) {
            circuitBreaker.recordFailure();
        }
        assertThat(circuitBreaker.isOpen()).isTrue();
    }
}
