package cli.li.resolver.ui;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import burp.api.montoya.MontoyaApi;

import cli.li.resolver.detection.DetectionStore;
import cli.li.resolver.logger.LoggerService;
import cli.li.resolver.settings.SettingsManager;
import cli.li.resolver.stats.StatisticsCollector;
import cli.li.resolver.thread.CaptchaSolverThreadManager;
import cli.li.resolver.service.ServiceManager;

/**
 * Main UI manager for the extension.
 * Creates and manages all tab panels and coordinates auto-refresh timers.
 */
public final class UIManager {
    private final ServiceManager serviceManager;
    private final SettingsManager settingsManager;
    private final StatisticsCollector statisticsCollector;
    private final CaptchaSolverThreadManager threadManager;
    private final DetectionStore detectionStore;
    private final LoggerService logger;

    private JPanel mainPanel;
    private JTabbedPane tabbedPane;

    private ServicesPanel servicesPanel;
    private SettingsPanel settingsPanel;
    private StatisticsPanel statisticsPanel;
    private DetectionPanel detectionPanel;
    private HelpPanel helpPanel;
    private LogsPanel logsPanel;

    private final List<Timer> timers = new ArrayList<>();

    public UIManager(MontoyaApi api, ServiceManager serviceManager, SettingsManager settingsManager,
                     StatisticsCollector statisticsCollector, CaptchaSolverThreadManager threadManager,
                     DetectionStore detectionStore) {
        this.serviceManager = serviceManager;
        this.settingsManager = settingsManager;
        this.statisticsCollector = statisticsCollector;
        this.threadManager = threadManager;
        this.detectionStore = detectionStore;
        this.logger = LoggerService.getInstance();

        // Log initialization of UI manager
        logger.info("UIManager", "Initializing UI components");

        initializeUI();
    }

    /**
     * Initialize UI components
     */
    private void initializeUI() {
        logger.debug("UIManager", "Initializing UI components");

        // Create main extension tab panel
        mainPanel = new JPanel(new BorderLayout());

        // Create tabbed pane
        tabbedPane = new JTabbedPane();

        // Create panels
        try {
            settingsPanel = new SettingsPanel(settingsManager, threadManager);
            servicesPanel = new ServicesPanel(serviceManager);
            statisticsPanel = new StatisticsPanel(statisticsCollector, serviceManager);
            detectionPanel = new DetectionPanel(detectionStore);
            helpPanel = new HelpPanel();
            logsPanel = new LogsPanel();

            logger.debug("UIManager", "All UI panels created successfully");
        } catch (Exception e) {
            logger.error("UIManager", "Error creating UI panels: " + e.getMessage());
            throw e;
        }

        // Add tabs to tabbedPane
        tabbedPane.addTab("Services", servicesPanel);
        tabbedPane.addTab("Statistics", statisticsPanel);
        tabbedPane.addTab("Detections", detectionPanel);
        tabbedPane.addTab("Settings", settingsPanel);
        tabbedPane.addTab("Help", helpPanel);
        tabbedPane.addTab("Logs", logsPanel);
        logger.debug("UIManager", "All panels added to tabbed pane");

        // Add tabbed pane to main panel
        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        // Initialize auto-refresh timer — only refresh the active tab to reduce overhead
        Timer refreshTimer = createTimer(5000, e -> {
            Component selected = tabbedPane.getSelectedComponent();
            if (selected == servicesPanel) {
                servicesPanel.refreshData();
            } else if (selected == statisticsPanel) {
                statisticsPanel.refreshData();
            }
        });
        refreshTimer.start();
        logger.debug("UIManager", "Auto-refresh timer started");

        logger.info("UIManager", "UI components initialized successfully");
    }

    /**
     * Create a Swing Timer that is tracked for lifecycle management.
     *
     * @param delayMs the delay between timer events in milliseconds
     * @param action  the action to perform on each timer tick
     * @return the created (but not yet started) Timer
     */
    private Timer createTimer(int delayMs, java.awt.event.ActionListener action) {
        Timer timer = new Timer(delayMs, action);
        timers.add(timer);
        return timer;
    }

    /**
     * Get the main UI component
     * @return Main UI component
     */
    public Component getUI() {
        return mainPanel;
    }

    /**
     * Dispose of all UI resources.
     * Stops all managed timers and calls dispose() on all BasePanel subclasses.
     */
    public void dispose() {
        logger.info("UIManager", "Disposing UI resources");

        // Stop all timers managed by this UIManager
        timers.forEach(Timer::stop);

        // Dispose all BasePanel instances
        if (settingsPanel != null) {
            settingsPanel.dispose();
        }
        if (detectionPanel != null) {
            detectionPanel.dispose();
        }
        if (logsPanel != null) {
            logsPanel.dispose();
        }

        logger.info("UIManager", "UI resources disposed");
    }
}
