package cli.li.resolver.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import burp.api.montoya.MontoyaApi;

import cli.li.resolver.logger.LoggerService;
import cli.li.resolver.settings.SettingsManager;
import cli.li.resolver.stats.StatisticsCollector;
import cli.li.resolver.thread.CaptchaSolverThreadManager;
import cli.li.resolver.service.ServiceManager;

/**
 * Main UI manager for the extension
 */
public class UIManager {
    private final ServiceManager serviceManager;
    private final SettingsManager settingsManager;
    private final StatisticsCollector statisticsCollector;
    private final CaptchaSolverThreadManager threadManager;
    private final LoggerService logger;

    private JPanel mainPanel;
    private JTabbedPane tabbedPane;
    
    private ServicesPanel servicesPanel;
    private SettingsPanel settingsPanel;
    private StatisticsPanel statisticsPanel;
    private HelpPanel helpPanel;
    private LogsPanel logsPanel;

    public UIManager(MontoyaApi api, ServiceManager serviceManager, SettingsManager settingsManager,
                     StatisticsCollector statisticsCollector, CaptchaSolverThreadManager threadManager) {
        this.serviceManager = serviceManager;
        this.settingsManager = settingsManager;
        this.statisticsCollector = statisticsCollector;
        this.threadManager = threadManager;
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
            statisticsPanel = new StatisticsPanel(statisticsCollector);
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
        tabbedPane.addTab("Settings", settingsPanel);
        tabbedPane.addTab("Help", helpPanel);
        tabbedPane.addTab("Logs", logsPanel);
        logger.debug("UIManager", "All panels added to tabbed pane");
        
        // Add tabbed pane to main panel
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        
        // Initialize auto-refresh timer for statistics
        javax.swing.Timer timer = new javax.swing.Timer(5000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                statisticsPanel.refreshData();
                servicesPanel.refreshData();
            }
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
        return mainPanel;
    }
}