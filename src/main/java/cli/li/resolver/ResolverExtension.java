package cli.li.resolver;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.BurpExtension;
import burp.api.montoya.extension.ExtensionUnloadingHandler;

import cli.li.resolver.thread.CaptchaSolverThreadManager;
import cli.li.resolver.thread.HighLoadDetector;
import cli.li.resolver.thread.ThreadPoolManager;
import cli.li.resolver.service.ServiceManager;
import cli.li.resolver.ui.UIManager;
import cli.li.resolver.ui.UIHelper;
import cli.li.resolver.logger.LoggerService;
import cli.li.resolver.http.PlaceholderParser;
import cli.li.resolver.http.HttpRequestModifier;
import cli.li.resolver.settings.SettingsManager;
import cli.li.resolver.stats.StatisticsCollector;
import cli.li.resolver.detection.DetectionStore;
import cli.li.resolver.detection.ResponseAnalyzer;
import cli.li.resolver.provider.ProviderRegistry;
import cli.li.resolver.provider.selection.ProviderSelector;
import cli.li.resolver.provider.impl.TwoCaptchaProvider;
import cli.li.resolver.provider.impl.RuCaptchaProvider;
import cli.li.resolver.provider.impl.AntiCaptchaProvider;
import cli.li.resolver.provider.impl.CapMonsterProvider;
import cli.li.resolver.provider.impl.CapSolverProvider;
import cli.li.resolver.provider.impl.SolveCaptchaProvider;

/**
 * ResolverExtension is the main entry point for the reSolver Burp Suite extension.
 * It registers HTTP handlers, initializes services, and sets up the user interface.
 */
public class ResolverExtension implements BurpExtension, ExtensionUnloadingHandler {

    public static MontoyaApi api;
    private SettingsManager settingsManager;
    private ServiceManager serviceManager;
    private CaptchaSolverThreadManager threadManager;
    private StatisticsCollector statisticsCollector;
    private PlaceholderParser placeholderParser;
    private HttpRequestModifier requestModifier;
    private UIManager uiManager;
    private HighLoadDetector highLoadDetector;
    private ThreadPoolManager threadPoolManager;
    private ProviderRegistry providerRegistry;
    private ProviderSelector providerSelector;
    private DetectionStore detectionStore;
    private ResponseAnalyzer responseAnalyzer;
    private LoggerService logger;

    @Override
    public void initialize(MontoyaApi api) {
        ResolverExtension.api = api;
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

        // Apply log level from settings
        try {
            LoggerService.LogLevel level = LoggerService.LogLevel.valueOf(settingsManager.getLogLevel());
            logger.setMinLevel(level);
            logger.info("ResolverExtension", "Log level set to: " + level);
        } catch (IllegalArgumentException e) {
            logger.warning("ResolverExtension", "Invalid log level in settings, using default");
        }

        // Initialize thread management components
        threadPoolManager = new ThreadPoolManager(settingsManager);
        logger.info("ResolverExtension", "Thread pool manager initialized with size: " +
                threadPoolManager.getPoolSize());

        highLoadDetector = new HighLoadDetector(settingsManager);
        logger.info("ResolverExtension", "High load detector initialized with threshold: " +
                settingsManager.getHighLoadThreshold());

        threadManager = new CaptchaSolverThreadManager(threadPoolManager, highLoadDetector, logger);
        logger.info("ResolverExtension", "Thread manager initialized");

        // Initialize provider registry and register all providers
        providerRegistry = new ProviderRegistry();
        providerRegistry.discoverProviders();
        registerDefaultProviders();
        logger.info("ResolverExtension", "Provider registry initialized with " +
                providerRegistry.size() + " providers");

        // Initialize provider selector
        providerSelector = new ProviderSelector();
        logger.info("ResolverExtension", "Provider selector initialized");

        // Initialize service manager
        serviceManager = new ServiceManager(providerRegistry, providerSelector, settingsManager);
        logger.info("ResolverExtension", "Service manager initialized");

        // Initialize detection components
        detectionStore = new DetectionStore();
        responseAnalyzer = new ResponseAnalyzer(detectionStore);
        logger.info("ResolverExtension", "Detection components initialized");

        // Initialize statistics components
        statisticsCollector = new StatisticsCollector(serviceManager);
        logger.info("ResolverExtension", "Statistics components initialized");

        // Initialize HTTP processing components
        placeholderParser = new PlaceholderParser();
        requestModifier = new HttpRequestModifier(serviceManager, placeholderParser,
                statisticsCollector, responseAnalyzer, settingsManager);
        logger.info("ResolverExtension", "HTTP processing components initialized");

        // Initialize UI
        uiManager = new UIManager(api, serviceManager, settingsManager, statisticsCollector,
                threadManager, detectionStore);
        logger.info("ResolverExtension", "UI manager initialized");
    }

    /**
     * Register all default CAPTCHA providers manually.
     * SPI may not work reliably in Burp's classloader, so we register explicitly.
     */
    private void registerDefaultProviders() {
        providerRegistry.register(new TwoCaptchaProvider());
        providerRegistry.register(new RuCaptchaProvider());
        providerRegistry.register(new AntiCaptchaProvider());
        providerRegistry.register(new CapMonsterProvider());
        providerRegistry.register(new CapSolverProvider());
        providerRegistry.register(new SolveCaptchaProvider());

        logger.info("ResolverExtension", "Default CAPTCHA providers registered: " +
                "2Captcha, RuCaptcha, AntiCaptcha, CapMonster, CapSolver, SolveCaptcha");
    }

    /**
     * Clean up resources when the extension is unloaded
     */
    @Override
    public void extensionUnloaded() {
        logger.info("ResolverExtension", "Extension unloading started");

        // Dispose UI resources
        if (uiManager != null) {
            uiManager.dispose();
        }

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
