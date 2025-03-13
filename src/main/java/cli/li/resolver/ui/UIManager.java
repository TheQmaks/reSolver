package cli.li.resolver.ui;

import java.awt.*;
import javax.swing.*;

import burp.api.montoya.MontoyaApi;

import cli.li.resolver.service.ServiceManager;
import cli.li.resolver.settings.SettingsManager;
import cli.li.resolver.stats.StatisticsCollector;
import cli.li.resolver.thread.CaptchaSolverThreadManager;
import cli.li.resolver.logger.LoggerService;
import cli.li.resolver.logger.BurpLoggerAdapter;

/**
 * Main UI manager for the extension
 */
public class UIManager {
    private final MontoyaApi api;
    private final ServiceManager serviceManager;
    private final SettingsManager settingsManager;
    private final StatisticsCollector statisticsCollector;
    private final CaptchaSolverThreadManager threadManager;
    private final BurpLoggerAdapter logger;

    private JTabbedPane tabbedPane;
    private ServicesPanel servicesPanel;
    private SettingsPanel settingsPanel;
    private StatisticsPanel statisticsPanel;
    private HelpPanel helpPanel;
    private LogsPanel logsPanel; // Added LogsPanel

    public UIManager(MontoyaApi api, ServiceManager serviceManager, SettingsManager settingsManager,
                     StatisticsCollector statisticsCollector, CaptchaSolverThreadManager threadManager) {
        this.api = api;
        this.serviceManager = serviceManager;
        this.settingsManager = settingsManager;
        this.statisticsCollector = statisticsCollector;
        this.threadManager = threadManager;
        this.logger = BurpLoggerAdapter.getInstance();

        // Log initialization of UI manager
        logger.info("UIManager", "Initializing UI components");

        // Initialize UI components
        initializeUI();
    }

    /**
     * Initialize UI components
     */
    private void initializeUI() {
        tabbedPane = new JTabbedPane();

        // Create panels
        logger.debug("UIManager", "Creating UI panels");

        servicesPanel = new ServicesPanel(serviceManager);
        logger.debug("UIManager", "Services panel created");

        settingsPanel = new SettingsPanel(settingsManager, threadManager);
        logger.debug("UIManager", "Settings panel created");

        statisticsPanel = new StatisticsPanel(statisticsCollector);
        logger.debug("UIManager", "Statistics panel created");

        helpPanel = new HelpPanel();
        logger.debug("UIManager", "Help panel created");

        logsPanel = new LogsPanel(); // Initialize LogsPanel
        logger.debug("UIManager", "Logs panel created");

        // Add panels to tabbed pane
        tabbedPane.addTab("Services", servicesPanel);
        tabbedPane.addTab("Settings", settingsPanel);
        tabbedPane.addTab("Statistics", statisticsPanel);
        tabbedPane.addTab("Logs", logsPanel); // Add Logs tab
        tabbedPane.addTab("Help", helpPanel);
        logger.debug("UIManager", "All panels added to tabbed pane");

        // Initialize auto-refresh timer for statistics
        Timer timer = new Timer(5000, e -> {
            statisticsPanel.refreshData();
            servicesPanel.refreshData();
        });
        timer.start();
        logger.debug("UIManager", "Auto-refresh timer started");

        logger.info("UIManager", "UI components initialized successfully");
    }

    /**
     * Get the main UI component
     * @return Main UI component
     */
    public Component getUI() {
        return tabbedPane;
    }
}