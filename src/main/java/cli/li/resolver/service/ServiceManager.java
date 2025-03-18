package cli.li.resolver.service;

import cli.li.resolver.captcha.model.CaptchaType;
import cli.li.resolver.service.captcha.ICaptchaService;
import cli.li.resolver.captcha.solver.ICaptchaSolver;
import cli.li.resolver.captcha.model.ServiceConfig;
import cli.li.resolver.logger.LoggerService;
import cli.li.resolver.settings.SettingsManager;
import cli.li.resolver.util.ApiKeyUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manager for CAPTCHA solving services
 */
public class ServiceManager {
    private final CaptchaServiceRegistry serviceRegistry;
    private final SettingsManager settingsManager;
    private final LoggerService logger;

    public ServiceManager(CaptchaServiceRegistry serviceRegistry, SettingsManager settingsManager) {
        this.serviceRegistry = serviceRegistry;
        this.settingsManager = settingsManager;
        this.logger = LoggerService.getInstance();

        logger.info("ServiceManager", "Service manager initialized");

        // Load service configurations from settings
        loadServiceConfigs();
    }

    /**
     * Load service configurations from settings
     */
    private void loadServiceConfigs() {
        logger.info("ServiceManager", "Loading service configurations from settings");
        Map<String, ServiceConfig> configs = settingsManager.loadServiceConfigs();

        // Apply configurations to services
        for (ICaptchaService service : serviceRegistry.getAllServices()) {
            ServiceConfig config = configs.get(service.getId());
            if (config != null) {
                service.setApiKey(config.getApiKey());
                service.setEnabled(config.isEnabled());
                service.setPriority(config.getPriority());

                // Mask API key for logging
                String maskedApiKey = ApiKeyUtils.maskApiKey(config.getApiKey());

                logger.info("ServiceManager", "Configured service: " + service.getName() +
                        " (ID: " + service.getId() + "), " +
                        "API key: " + maskedApiKey + ", " +
                        "enabled: " + config.isEnabled() + ", " +
                        "priority: " + config.getPriority());
            } else {
                logger.info("ServiceManager", "No saved configuration for service: " + service.getName() +
                        " (ID: " + service.getId() + "), using defaults");
            }
        }
    }

    /**
     * Save service configurations to settings
     */
    public void saveServiceConfigs() {
        logger.info("ServiceManager", "Saving service configurations to settings");
        Map<String, ServiceConfig> configs = new HashMap<>();

        // Create configurations from services
        for (ICaptchaService service : serviceRegistry.getAllServices()) {
            ServiceConfig config = new ServiceConfig(
                    service.getApiKey(),
                    service.isEnabled(),
                    service.getPriority()
            );
            configs.put(service.getId(), config);

            // Mask API key for logging
            String maskedApiKey = ApiKeyUtils.maskApiKey(service.getApiKey());

            logger.info("ServiceManager", "Saving configuration for service: " + service.getName() +
                    " (ID: " + service.getId() + "), " +
                    "API key: " + maskedApiKey + ", " +
                    "enabled: " + service.isEnabled() + ", " +
                    "priority: " + service.getPriority());
        }

        settingsManager.saveServiceConfigs(configs);
        logger.info("ServiceManager", "Service configurations saved successfully");
    }

    /**
     * Get all available services sorted by priority
     * @return List of services sorted by priority
     */
    public List<ICaptchaService> getServicesInPriorityOrder() {
        List<ICaptchaService> services = serviceRegistry.getAllServices().stream()
                .filter(ICaptchaService::isEnabled)
                .sorted(Comparator.comparingInt(ICaptchaService::getPriority))
                .collect(Collectors.toList());

        logger.debug("ServiceManager", "Retrieved " + services.size() + " enabled services in priority order");
        return services;
    }
    
    /**
     * Get all available services sorted by priority, including disabled ones
     * @return List of all services sorted by priority
     */
    public List<ICaptchaService> getAllServicesInPriorityOrder() {
        List<ICaptchaService> services = serviceRegistry.getAllServices().stream()
                .sorted(Comparator.comparingInt(ICaptchaService::getPriority))
                .collect(Collectors.toList());

        logger.debug("ServiceManager", "Retrieved " + services.size() + " services (enabled and disabled) in priority order");
        return services;
    }

    /**
     * Get a CAPTCHA solver for the specified type
     * Uses the first available service that supports the type
     * @param captchaType CAPTCHA type
     * @return CAPTCHA solver or null if no service can solve it
     */
    public ICaptchaSolver getSolverForType(CaptchaType captchaType) {
        logger.info("ServiceManager", "Looking for solver for CAPTCHA type: " + captchaType);

        for (ICaptchaService service : getServicesInPriorityOrder()) {
            // Skip services with invalid API keys
            if (!service.validateApiKey()) {
                logger.warning("ServiceManager", "Skipping service " + service.getName() + " due to invalid API key");
                continue;
            }
            
            ICaptchaSolver solver = service.getSolver(captchaType);
            if (solver != null) {
                logger.info("ServiceManager", "Found solver for " + captchaType +
                        " using service: " + service.getName() + " (priority: " + service.getPriority() + ")");
                return solver;
            }
        }

        logger.warning("ServiceManager", "No solver found for CAPTCHA type: " + captchaType);
        return null;
    }

    /**
     * Check if any enabled service has a valid API key
     * @return true if at least one service is properly configured
     */
    public boolean hasConfiguredServices() {
        boolean hasConfigured = serviceRegistry.getAllServices().stream()
                .anyMatch(service -> service.isEnabled() && service.validateApiKey());

        if (!hasConfigured) {
            logger.warning("ServiceManager", "No properly configured CAPTCHA services found");
        } else {
            logger.debug("ServiceManager", "Found properly configured CAPTCHA services");
        }

        return hasConfigured;
    }

    /**
     * Check and update balances for all enabled services
     */
    public void updateAllBalances() {
        logger.info("ServiceManager", "Updating balances for all enabled services");

        for (ICaptchaService service : serviceRegistry.getAllServices()) {
            if (service.isEnabled()) {
                updateBalance(service);
            }
        }

        logger.info("ServiceManager", "All balances updated");
    }
    
    /**
     * Update the balance for a specific service
     * @param service The service to update the balance for
     */
    public void updateBalance(ICaptchaService service) {
        if (service != null && service.isEnabled() && service.validateApiKey()) {
            try {
                logger.info("ServiceManager", "Checking balance for service: " + service.getName());
                service.getBalance(); // This will update the balance internally
                logger.info("ServiceManager", "Balance for " + service.getName() + ": " + service.getBalance());
            } catch (Exception e) {
                logger.error("ServiceManager", "Error checking balance for " + service.getName() + ": " + e.getMessage(), e);
                // Ignore exceptions during balance check
            }
        } else if (service != null) {
            logger.warning("ServiceManager", "Cannot update balance for " + service.getName() + ": Service is disabled or has invalid API key");
        }
    }
}