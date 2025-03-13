package cli.li.resolver.ui;

import java.awt.*;
import javax.swing.*;

import burp.api.montoya.MontoyaApi;

import cli.li.resolver.service.ServiceManager;
import cli.li.resolver.settings.SettingsManager;
import cli.li.resolver.stats.StatisticsCollector;
import cli.li.resolver.thread.CaptchaSolverThreadManager;

/**
 * Main UI manager for the extension
 */
public class UIManager {
    private final MontoyaApi api;
    private final ServiceManager serviceManager;
    private final SettingsManager settingsManager;
    private final StatisticsCollector statisticsCollector;
    private final CaptchaSolverThreadManager threadManager;

    private JTabbedPane tabbedPane;
    private ServicesPanel servicesPanel;
    private SettingsPanel settingsPanel;
    private StatisticsPanel statisticsPanel;
    private HelpPanel helpPanel;

    public UIManager(MontoyaApi api, ServiceManager serviceManager, SettingsManager settingsManager,
                     StatisticsCollector statisticsCollector, CaptchaSolverThreadManager threadManager) {
        this.api = api;
        this.serviceManager = serviceManager;
        this.settingsManager = settingsManager;
        this.statisticsCollector = statisticsCollector;
        this.threadManager = threadManager;

        // Initialize UI components
        initializeUI();
    }

    /**
     * Initialize UI components
     */
    private void initializeUI() {
        tabbedPane = new JTabbedPane();

        // Create panels
        servicesPanel = new ServicesPanel(serviceManager);
        settingsPanel = new SettingsPanel(settingsManager, threadManager);
        statisticsPanel = new StatisticsPanel(statisticsCollector);
        helpPanel = new HelpPanel();

        // Add panels to tabbed pane
        tabbedPane.addTab("Services", servicesPanel);
        tabbedPane.addTab("Settings", settingsPanel);
        tabbedPane.addTab("Statistics", statisticsPanel);
        tabbedPane.addTab("Help", helpPanel);

        // Initialize auto-refresh timer for statistics
        Timer timer = new Timer(5000, e -> {
            statisticsPanel.refreshData();
            servicesPanel.refreshData();
        });
        timer.start();
    }

    /**
     * Get the main UI component
     * @return Main UI component
     */
    public Component getUI() {
        return tabbedPane;
    }
}