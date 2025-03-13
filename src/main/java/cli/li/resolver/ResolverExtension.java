package cli.li.resolver;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.BurpExtension;

import cli.li.resolver.thread.*;
import cli.li.resolver.service.*;
import cli.li.resolver.ui.UIManager;
import cli.li.resolver.http.PlaceholderParser;
import cli.li.resolver.http.HttpRequestModifier;
import cli.li.resolver.settings.SettingsManager;
import cli.li.resolver.stats.RequestStatsTracker;
import cli.li.resolver.stats.StatisticsCollector;

/**
 * ResolverExtension is the main entry point for the reSolver Burp Suite extension.
 * It registers HTTP handlers, initializes services, and sets up the user interface.
 */
public class ResolverExtension implements BurpExtension {

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

    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;
        api.extension().setName("reSolver - CAPTCHA solver for Burp Suite");

        // Initialize components
        initializeComponents();

        // Register HTTP handler
        api.http().registerHttpHandler(requestModifier);

        // Add extension tab to Burp UI
        api.userInterface().registerSuiteTab("reSolver", uiManager.getUI());

        api.logging().logToOutput("reSolver extension has been loaded");
    }

    private void initializeComponents() {
        // Initialize settings manager
        settingsManager = new SettingsManager(api);

        // Initialize thread management components
        threadPoolManager = new ThreadPoolManager(settingsManager);
        queueManager = new QueueManager(settingsManager);
        highLoadDetector = new HighLoadDetector(settingsManager);
        threadManager = new CaptchaSolverThreadManager(threadPoolManager, queueManager, highLoadDetector);

        // Initialize services
        serviceRegistry = new CaptchaServiceRegistry();
        registerDefaultServices();
        serviceManager = new ServiceManager(serviceRegistry, settingsManager);

        // Initialize statistics components
        statsTracker = new RequestStatsTracker();
        statisticsCollector = new StatisticsCollector(serviceManager);

        // Initialize HTTP processing components
        placeholderParser = new PlaceholderParser();
        requestModifier = new HttpRequestModifier(serviceManager, threadManager, placeholderParser, statisticsCollector);

        // Initialize UI
        uiManager = new UIManager(api, serviceManager, settingsManager, statisticsCollector, threadManager);
    }

    private void registerDefaultServices() {
        // Register built-in CAPTCHA solving services
        serviceRegistry.registerService(new TwoCaptchaService());
        serviceRegistry.registerService(new AntiCaptchaService());
        serviceRegistry.registerService(new CapMonsterService());
    }
}