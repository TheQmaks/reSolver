package cli.li.resolver.service;

import cli.li.resolver.captcha.*;
import cli.li.resolver.captcha.CaptchaType;
import cli.li.resolver.captcha.ICaptchaService;
import cli.li.resolver.captcha.ICaptchaSolver;
import cli.li.resolver.captcha.ServiceConfig;
import cli.li.resolver.settings.SettingsManager;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manager for CAPTCHA solving services
 */
public class ServiceManager {
    private final CaptchaServiceRegistry serviceRegistry;
    private final SettingsManager settingsManager;

    public ServiceManager(CaptchaServiceRegistry serviceRegistry, SettingsManager settingsManager) {
        this.serviceRegistry = serviceRegistry;
        this.settingsManager = settingsManager;

        // Load service configurations from settings
        loadServiceConfigs();
    }

    /**
     * Load service configurations from settings
     */
    private void loadServiceConfigs() {
        Map<String, ServiceConfig> configs = settingsManager.loadServiceConfigs();

        // Apply configurations to services
        for (ICaptchaService service : serviceRegistry.getAllServices()) {
            ServiceConfig config = configs.get(service.getId());
            if (config != null) {
                service.setApiKey(config.getApiKey());
                service.setEnabled(config.isEnabled());
                service.setPriority(config.getPriority());
            }
        }
    }

    /**
     * Save service configurations to settings
     */
    public void saveServiceConfigs() {
        Map<String, ServiceConfig> configs = new HashMap<>();

        // Create configurations from services
        for (ICaptchaService service : serviceRegistry.getAllServices()) {
            ServiceConfig config = new ServiceConfig(
                    service.getApiKey(),
                    service.isEnabled(),
                    service.getPriority()
            );
            configs.put(service.getId(), config);
        }

        settingsManager.saveServiceConfigs(configs);
    }

    /**
     * Get all available services sorted by priority
     * @return List of services sorted by priority
     */
    public List<ICaptchaService> getServicesInPriorityOrder() {
        return serviceRegistry.getAllServices().stream()
                .filter(ICaptchaService::isEnabled)
                .sorted(Comparator.comparingInt(ICaptchaService::getPriority))
                .collect(Collectors.toList());
    }

    /**
     * Get a CAPTCHA solver for the specified type
     * Uses the first available service that supports the type
     * @param captchaType CAPTCHA type
     * @return CAPTCHA solver or null if no service can solve it
     */
    public ICaptchaSolver getSolverForType(CaptchaType captchaType) {
        for (ICaptchaService service : getServicesInPriorityOrder()) {
            ICaptchaSolver solver = service.getSolver(captchaType);
            if (solver != null) {
                return solver;
            }
        }
        return null;
    }

    /**
     * Check if any enabled service has a valid API key
     * @return true if at least one service is properly configured
     */
    public boolean hasConfiguredServices() {
        return serviceRegistry.getAllServices().stream()
                .anyMatch(service -> service.isEnabled() && service.validateApiKey());
    }

    /**
     * Check and update balances for all enabled services
     */
    public void updateAllBalances() {
        for (ICaptchaService service : serviceRegistry.getAllServices()) {
            if (service.isEnabled()) {
                try {
                    service.getBalance(); // This will update the balance internally
                } catch (Exception ignored) {
                    // Ignore exceptions during balance check
                }
            }
        }
    }
}