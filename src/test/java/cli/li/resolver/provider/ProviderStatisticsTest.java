package cli.li.resolver.provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("ProviderStatistics")
class ProviderStatisticsTest {

    private ProviderStatistics statistics;

    @BeforeEach
    void setUp() {
        statistics = new ProviderStatistics();
    }

    @Test
    @DisplayName("initial state has all values at zero")
    void initialStateAllZeros() {
        assertThat(statistics.getTotalRequests()).isZero();
        assertThat(statistics.getSuccessfulRequests()).isZero();
        assertThat(statistics.getFailedRequests()).isZero();
        assertThat(statistics.getSuccessRate()).isEqualTo(0.0);
        assertThat(statistics.getAvgSolveTimeMs()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("recordSuccess increments totalRequests and successfulRequests")
    void recordSuccessIncrementsCounts() {
        statistics.recordSuccess(1500);

        assertThat(statistics.getTotalRequests()).isEqualTo(1);
        assertThat(statistics.getSuccessfulRequests()).isEqualTo(1);
        assertThat(statistics.getFailedRequests()).isZero();
        assertThat(statistics.getSuccessRate()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("recordFailure increments totalRequests and failedRequests")
    void recordFailureIncrementsCounts() {
        statistics.recordFailure();

        assertThat(statistics.getTotalRequests()).isEqualTo(1);
        assertThat(statistics.getSuccessfulRequests()).isZero();
        assertThat(statistics.getFailedRequests()).isEqualTo(1);
        assertThat(statistics.getSuccessRate()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("mixed successes and failures produce correct success rate and average solve time")
    void mixedSuccessesAndFailures() {
        statistics.recordSuccess(1000);
        statistics.recordSuccess(2000);
        statistics.recordSuccess(3000);
        statistics.recordFailure();

        assertThat(statistics.getTotalRequests()).isEqualTo(4);
        assertThat(statistics.getSuccessfulRequests()).isEqualTo(3);
        assertThat(statistics.getFailedRequests()).isEqualTo(1);
        assertThat(statistics.getSuccessRate()).isCloseTo(75.0, within(0.01));
        assertThat(statistics.getAvgSolveTimeMs()).isCloseTo(2000.0, within(0.01));
    }

    @Test
    @DisplayName("reset clears all counters back to zero")
    void resetClearsEverything() {
        statistics.recordSuccess(1000);
        statistics.recordSuccess(2000);
        statistics.recordFailure();

        statistics.reset();

        assertThat(statistics.getTotalRequests()).isZero();
        assertThat(statistics.getSuccessfulRequests()).isZero();
        assertThat(statistics.getFailedRequests()).isZero();
        assertThat(statistics.getSuccessRate()).isEqualTo(0.0);
        assertThat(statistics.getAvgSolveTimeMs()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("avgSolveTimeMs is zero when there are no successful requests")
    void avgSolveTimeZeroWhenNoSuccesses() {
        statistics.recordFailure();
        statistics.recordFailure();

        assertThat(statistics.getAvgSolveTimeMs()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("multiple successes accumulate solve times correctly")
    void multipleSuccessesAccumulateSolveTimes() {
        statistics.recordSuccess(500);
        statistics.recordSuccess(1500);

        assertThat(statistics.getAvgSolveTimeMs()).isCloseTo(1000.0, within(0.01));
    }
}
