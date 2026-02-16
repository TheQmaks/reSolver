package cli.li.resolver.thread;

import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import cli.li.resolver.settings.SettingsManager;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ThreadPoolManager")
class ThreadPoolManagerTest {

    private SettingsManager settingsManager;
    private ThreadPoolManager manager;

    @BeforeEach
    void setUp() {
        // Real SettingsManager - default threadPoolSize is 10
        settingsManager = new SettingsManager();
        settingsManager.setThreadPoolSize(2);
        manager = new ThreadPoolManager(settingsManager);
    }

    @AfterEach
    void tearDown() {
        manager.shutdown();
    }

    @Test
    @DisplayName("submit returns a Future that can be awaited")
    void submitReturnsFutureThatCanBeAwaited() throws Exception {
        Future<String> future = manager.submit(() -> "hello");

        assertThat(future.get()).isEqualTo("hello");
    }

    @Test
    @DisplayName("getPoolSize returns the configured value")
    void getPoolSizeReturnsConfiguredValue() {
        assertThat(manager.getPoolSize()).isEqualTo(2);
    }

    @Test
    @DisplayName("getActiveThreadCount is initially 0")
    void getActiveThreadCountIsInitiallyZero() {
        assertThat(manager.getActiveThreadCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("cancelAllTasks on empty pool returns 0")
    void cancelAllTasksOnEmptyPoolReturnsZero() {
        assertThat(manager.cancelAllTasks()).isEqualTo(0);
    }

    @Test
    @DisplayName("shutdown completes without error")
    void shutdownCompletesWithoutError() {
        manager.shutdown();

        // Should not throw; tearDown will call shutdown again, which is safe
    }
}
