package cli.li.resolver.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cli.li.resolver.util.ApiKeyUtils;
import cli.li.resolver.logger.LoggerService;
import cli.li.resolver.settings.SettingsManager;
import cli.li.resolver.captcha.exception.CaptchaSolverException;
import cli.li.resolver.provider.CaptchaProvider;
import cli.li.resolver.provider.ProviderConfig;
import cli.li.resolver.provider.ProviderRegistry;
import cli.li.resolver.provider.ProviderService;
import cli.li.resolver.provider.SolveRequest;
import cli.li.resolver.provider.selection.CircuitBreaker;
import cli.li.resolver.provider.selection.ProviderSelector;

/**
 * Manager for CAPTCHA solving services using the new provider system
 */
public class ServiceManager {
    private final ProviderRegistry providerRegistry;
    private final ProviderSelector providerSelector;
    private final SettingsManager settingsManager;
    private final LoggerService logger;
    private final List<ProviderService> providerServices;

    public ServiceManager(ProviderRegistry providerRegistry, ProviderSelector providerSelector,
                          SettingsManager settingsManager) {
        this.providerRegistry = providerRegistry;
        this.providerSelector = providerSelector;
        this.settingsManager = settingsManager;
        this.logger = LoggerService.getInstance();

        // Create ProviderService instances from the registry's providers
        this.providerServices = new ArrayList<>();
        int defaultPriority = 0;
        for (CaptchaProvider provider : providerRegistry.getAll()) {
            providerServices.add(new ProviderService(provider, defaultPriority++));
        }

        logger.info("ServiceManager", "Service manager initialized with " +
                providerServices.size() + " providers");

        // Load service configurations from settings
        loadServiceConfigs();
    }

    /**
     * Load service configurations from settings
     */
    private void loadServiceConfigs() {
        logger.info("ServiceManager", "Loading service configurations from settings");
        Map<String, ProviderConfig> configs = settingsManager.loadServiceConfigs();

        // Apply configurations to provider services
        for (ProviderService ps : providerServices) {
            ProviderConfig config = configs.get(ps.getId());
            if (config != null) {
                ps.setApiKey(config.apiKey());
                ps.setEnabled(config.enabled());
                ps.setPriority(config.priority());

                // Mask API key for logging
                String maskedApiKey = ApiKeyUtils.maskApiKey(config.apiKey());

                logger.info("ServiceManager", "Configured provider: " + ps.getDisplayName() +
                        " (ID: " + ps.getId() + "), " +
                        "API key: " + maskedApiKey + ", " +
                        "enabled: " + config.enabled() + ", " +
                        "priority: " + config.priority());
            } else {
                logger.info("ServiceManager", "No saved configuration for provider: " +
                        ps.getDisplayName() + " (ID: " + ps.getId() + "), using defaults");
            }
        }
    }

    /**
     * Save service configurations to settings
     */
    public void saveServiceConfigs() {
        logger.info("ServiceManager", "Saving service configurations to settings");
        Map<String, ProviderConfig> configs = new HashMap<>();

        for (ProviderService ps : providerServices) {
            ProviderConfig config = new ProviderConfig(
                    ps.getApiKey(),
                    ps.isEnabled(),
                    ps.getPriority()
            );
            configs.put(ps.getId(), config);

            // Mask API key for logging
            String maskedApiKey = ApiKeyUtils.maskApiKey(ps.getApiKey());

            logger.info("ServiceManager", "Saving configuration for provider: " +
                    ps.getDisplayName() + " (ID: " + ps.getId() + "), " +
                    "API key: " + maskedApiKey + ", " +
                    "enabled: " + ps.isEnabled() + ", " +
                    "priority: " + ps.getPriority());
        }

        settingsManager.saveServiceConfigs(configs);
        logger.info("ServiceManager", "Service configurations saved successfully");
    }

    /**
     * Solve a CAPTCHA using available providers with fallback
     * @param solveRequest The solve request
     * @return Solved CAPTCHA token
     * @throws CaptchaSolverException If solving fails with all providers
     */
    public String solve(SolveRequest solveRequest) throws CaptchaSolverException {
        logger.info("ServiceManager", "Solving CAPTCHA type: " + solveRequest.type());

        // Use ProviderSelector to get ordered providers for the given CAPTCHA type
        List<ProviderService> orderedProviders = providerSelector.selectOrdered(
                solveRequest.type(), providerServices);

        if (orderedProviders.isEmpty()) {
            throw new CaptchaSolverException("No available provider for CAPTCHA type: " +
                    solveRequest.type());
        }

        CaptchaSolverException lastException = null;

        for (ProviderService ps : orderedProviders) {
            CircuitBreaker breaker = providerSelector.getCircuitBreaker(ps.getId());
            try {
                logger.info("ServiceManager", "Trying provider: " + ps.getDisplayName() +
                        " for type: " + solveRequest.type());

                // Build request with this provider's API key
                SolveRequest requestWithKey = new SolveRequest(
                        ps.getApiKey(),
                        solveRequest.type(),
                        solveRequest.siteKey(),
                        solveRequest.pageUrl(),
                        solveRequest.params()
                );

                String token = ps.solve(requestWithKey);
                breaker.recordSuccess();

                logger.info("ServiceManager", "CAPTCHA solved successfully by provider: " +
                        ps.getDisplayName());
                return token;

            } catch (CaptchaSolverException e) {
                breaker.recordFailure();
                lastException = e;
                logger.warning("ServiceManager", "Provider " + ps.getDisplayName() +
                        " failed: " + e.getMessage() + ", trying next provider");
            }
        }

        throw new CaptchaSolverException("All providers failed to solve CAPTCHA type: " +
                solveRequest.type(),
                lastException);
    }

    /**
     * Get all provider services sorted by priority
     * @return List of all provider services sorted by priority
     */
    public List<ProviderService> getAllProviderServices() {
        List<ProviderService> sorted = new ArrayList<>(providerServices);
        sorted.sort(Comparator.comparingInt(ProviderService::getPriority));
        return sorted;
    }

    /**
     * Force-refresh balances for all configured providers, bypassing the cache.
     */
    public void refreshAllBalances() {
        logger.info("ServiceManager", "Force-refreshing balances for all configured providers");

        for (ProviderService ps : providerServices) {
            if (ps.isConfigured()) {
                ps.forceRefreshBalance();
                logger.debug("ServiceManager", "Triggered balance refresh for provider: " +
                        ps.getDisplayName());
            }
        }
    }

    /**
     * Check if any provider is fully configured and ready to use
     * @return true if at least one provider is configured
     */
    public boolean hasConfiguredServices() {
        boolean hasConfigured = providerServices.stream()
                .anyMatch(ProviderService::isConfigured);

        if (!hasConfigured) {
            logger.warning("ServiceManager", "No properly configured CAPTCHA providers found");
        } else {
            logger.debug("ServiceManager", "Found properly configured CAPTCHA providers");
        }

        return hasConfigured;
    }
}
