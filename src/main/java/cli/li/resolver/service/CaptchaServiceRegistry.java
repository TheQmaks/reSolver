package cli.li.resolver.service;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import cli.li.resolver.service.captcha.ICaptchaService;

/**
 * Registry for available CAPTCHA services
 */
public class CaptchaServiceRegistry {
    private final Map<String, ICaptchaService> services = new ConcurrentHashMap<>();

    /**
     * Register a CAPTCHA service
     * @param service CAPTCHA service to register
     */
    public void registerService(ICaptchaService service) {
        services.put(service.getId(), service);
    }

    /**
     * Unregister a CAPTCHA service
     * @param serviceId ID of the service to unregister
     */
    public void unregisterService(String serviceId) {
        services.remove(serviceId);
    }

    /**
     * Get a CAPTCHA service by ID
     * @param serviceId Service ID
     * @return CAPTCHA service or null if not found
     */
    public ICaptchaService getService(String serviceId) {
        return services.get(serviceId);
    }

    /**
     * Get all registered CAPTCHA services
     * @return List of all registered services
     */
    public List<ICaptchaService> getAllServices() {
        return new ArrayList<>(services.values());
    }
}