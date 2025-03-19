package cli.li.resolver;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.BurpExtension;
import burp.api.montoya.extension.ExtensionUnloadingHandler;

import cli.li.resolver.thread.*;
import cli.li.resolver.service.*;
import cli.li.resolver.ui.UIManager;
import cli.li.resolver.ui.UIHelper;
import cli.li.resolver.logger.LoggerService;
import cli.li.resolver.http.PlaceholderParser;
import cli.li.resolver.http.HttpRequestModifier;
import cli.li.resolver.settings.SettingsManager;
import cli.li.resolver.stats.RequestStatsTracker;
import cli.li.resolver.stats.StatisticsCollector;
import cli.li.resolver.service.captcha.CapMonsterService;
import cli.li.resolver.service.captcha.TwoCaptchaService;
import cli.li.resolver.service.captcha.AntiCaptchaService;

/**
 * ResolverExtension is the main entry point for the reSolver Burp Suite extension.
 * It registers HTTP handlers, initializes services, and sets up the user interface.
 */
public class ResolverExtension implements BurpExtension, ExtensionUnloadingHandler {

    public static MontoyaApi api;
    private ServiceManager serviceManager;
    private CaptchaServiceRegistry serviceRegistry;
    private SettingsManager settingsManager;
    private CaptchaSolverThreadManager threadManager;
    private RequestStatsTracker statsTracker;
    private StatisticsCollector statisticsCollector;
    private PlaceholderParser placeholderParser;
    private HttpRequestModifier requestModifier;
    private UIManager uiManager;
    private HighLoadDetector highLoadDetector;
    private ThreadPoolManager threadPoolManager;
    private QueueManager queueManager;
    private LoggerService logger;

    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;
        api.extension().setName("reSolver - CAPTCHA solver for Burp Suite");

        // Initialize logger adapter immediately after API is set
        logger = LoggerService.getInstance();

        logger.info("ResolverExtension", "Starting reSolver extension initialization");

        // Initialize components
        initializeComponents();

        // Register HTTP handler
        api.http().registerHttpHandler(requestModifier);
        logger.info("ResolverExtension", "HTTP handler registered");

        // Add extension tab to Burp UI
        api.userInterface().registerSuiteTab("reSolver", uiManager.getUI());
        logger.info("ResolverExtension", "UI tab registered");
        
        // Initialize UI helper for proper dialog parents
        UIHelper.initialize();
        logger.info("ResolverExtension", "UI helper initialized");

        // Register unloading handler for clean resource management
        api.extension().registerUnloadingHandler(this);
        logger.info("ResolverExtension", "Extension unloading handler registered");

        logger.info("ResolverExtension", "reSolver extension has been loaded successfully");
    }

    private void initializeComponents() {
        logger.info("ResolverExtension", "Initializing components");

        // Initialize settings manager
        settingsManager = new SettingsManager();
        logger.info("ResolverExtension", "Settings manager initialized");

        // Initialize thread management components
        threadPoolManager = new ThreadPoolManager(settingsManager);
        logger.info("ResolverExtension", "Thread pool manager initialized with size: " + threadPoolManager.getPoolSize());

        queueManager = new QueueManager(settingsManager);
        logger.info("ResolverExtension", "Queue manager initialized with size: " + settingsManager.getQueueSize());

        highLoadDetector = new HighLoadDetector(settingsManager);
        logger.info("ResolverExtension", "High load detector initialized with threshold: " + settingsManager.getHighLoadThreshold());

        threadManager = new CaptchaSolverThreadManager(threadPoolManager, queueManager, highLoadDetector, logger);
        logger.info("ResolverExtension", "Thread manager initialized");

        // Initialize services
        serviceRegistry = new CaptchaServiceRegistry();
        registerDefaultServices();
        logger.info("ResolverExtension", "CAPTCHA services registered");

        serviceManager = new ServiceManager(serviceRegistry, settingsManager);
        logger.info("ResolverExtension", "Service manager initialized");

        // Initialize statistics components
        statsTracker = new RequestStatsTracker();
        statisticsCollector = new StatisticsCollector(serviceManager);
        logger.info("ResolverExtension", "Statistics components initialized");

        // Initialize HTTP processing components
        placeholderParser = new PlaceholderParser();
        requestModifier = new HttpRequestModifier(serviceManager, threadManager, placeholderParser, statisticsCollector);
        logger.info("ResolverExtension", "HTTP processing components initialized");

        // Initialize UI
        uiManager = new UIManager(api, serviceManager, settingsManager, statisticsCollector, threadManager);
        logger.info("ResolverExtension", "UI manager initialized");
    }

    private void registerDefaultServices() {
        // Register built-in CAPTCHA solving services
        serviceRegistry.registerService(new TwoCaptchaService());
        serviceRegistry.registerService(new AntiCaptchaService());
        serviceRegistry.registerService(new CapMonsterService());

        logger.info("ResolverExtension", "Default CAPTCHA services registered: 2Captcha, AntiCaptcha, CapMonster");
    }

    /**
     * Clean up resources when the extension is unloaded
     */
    @Override
    public void extensionUnloaded() {
        logger.info("ResolverExtension", "Extension unloading started");
        
        // Clean up threads and executors
        if (threadManager != null) {
            threadManager.shutdown();
        }
        
        if (threadPoolManager != null) {
            threadPoolManager.shutdown();
        }
        
        if (highLoadDetector != null) {
            highLoadDetector.shutdown();
        }
        
        logger.info("ResolverExtension", "Extension resources successfully released");
    }
}