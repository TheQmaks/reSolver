package cli.li.resolver.thread;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import cli.li.resolver.settings.SettingsManager;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HighLoadDetector")
class HighLoadDetectorTest {

    private SettingsManager settingsManager;
    private HighLoadDetector detector;

    @BeforeEach
    void setUp() {
        // Real SettingsManager - default highLoadThreshold is 50
        settingsManager = new SettingsManager();
        // Set a low threshold for testing
        settingsManager.setHighLoadThreshold(3);
        detector = new HighLoadDetector(settingsManager);
    }

    @Test
    @DisplayName("initial state returns zero requests in last minute")
    void initialStateReturnsZeroRequests() {
        assertThat(detector.getRequestsInLastMinute()).isEqualTo(0);
    }

    @Test
    @DisplayName("initial state is not high load")
    void initialStateIsNotHighLoad() {
        assertThat(detector.isHighLoad()).isFalse();
    }

    @Test
    @DisplayName("after one registerRequest, getRequestsInLastMinute returns 1")
    void afterOneRegisterRequestReturnsOne() {
        detector.registerRequest();

        assertThat(detector.getRequestsInLastMinute()).isEqualTo(1);
    }

    @Test
    @DisplayName("multiple registerRequest calls increase count")
    void multipleRegisterRequestCallsIncreaseCount() {
        detector.registerRequest();
        detector.registerRequest();
        detector.registerRequest();

        assertThat(detector.getRequestsInLastMinute()).isEqualTo(3);
    }

    @Test
    @DisplayName("isHighLoad returns true when count exceeds threshold")
    void isHighLoadReturnsTrueWhenCountExceedsThreshold() {
        detector.registerRequest();
        detector.registerRequest();
        detector.registerRequest();
        detector.registerRequest();

        assertThat(detector.isHighLoad()).isTrue();
    }

    @Test
    @DisplayName("isHighLoad returns false when count is below threshold")
    void isHighLoadReturnsFalseWhenCountBelowThreshold() {
        detector.registerRequest();
        detector.registerRequest();

        assertThat(detector.isHighLoad()).isFalse();
    }

    @Test
    @DisplayName("isHighLoad returns false when count equals threshold")
    void isHighLoadReturnsFalseWhenCountEqualsThreshold() {
        detector.registerRequest();
        detector.registerRequest();
        detector.registerRequest();

        assertThat(detector.isHighLoad()).isFalse();
    }

    @Test
    @DisplayName("shutdown completes without error")
    void shutdownCompletesWithoutError() {
        detector.registerRequest();
        detector.shutdown();

        // shutdown is a no-op, but should not throw
    }
}
