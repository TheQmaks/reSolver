package cli.li.resolver.service.captcha;

import java.math.BigDecimal;
import cli.li.resolver.captcha.model.CaptchaServiceStatistics;
import cli.li.resolver.captcha.model.CaptchaType;
import cli.li.resolver.captcha.solver.ICaptchaSolver;

/**
 * Interface for CAPTCHA solving services
 */
public interface ICaptchaService {
    /**
     * Get the service name
     * @return Service name
     */
    String getName();

    /**
     * Get the service ID
     * @return Service ID
     */
    String getId();

    /**
     * Check if service is enabled
     * @return true if service is enabled
     */
    boolean isEnabled();

    /**
     * Enable or disable the service
     * @param enabled Enable status
     */
    void setEnabled(boolean enabled);

    /**
     * Get the API key
     * @return API key
     */
    String getApiKey();

    /**
     * Set the API key
     * @param apiKey API key
     */
    void setApiKey(String apiKey);

    /**
     * Get the service priority
     * @return Priority value (lower is higher priority)
     */
    int getPriority();

    /**
     * Set the service priority
     * @param priority Priority value
     */
    void setPriority(int priority);

    /**
     * Get the current balance
     * @return Balance as BigDecimal
     */
    BigDecimal getBalance();

    /**
     * Check if the API key is valid
     * @return true if API key is valid
     */
    boolean validateApiKey();

    /**
     * Get solver for specific CAPTCHA type
     * @param captchaType Type of CAPTCHA
     * @return CAPTCHA solver instance
     */
    ICaptchaSolver getSolver(CaptchaType captchaType);

    /**
     * Get service statistics
     * @return Service statistics
     */
    CaptchaServiceStatistics getStatistics();
}
