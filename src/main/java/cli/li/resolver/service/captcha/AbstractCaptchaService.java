package cli.li.resolver.service.captcha;

import java.util.Map;
import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;

import cli.li.resolver.captcha.model.CaptchaServiceStatistics;
import cli.li.resolver.captcha.exception.CaptchaSolverException;
import cli.li.resolver.captcha.model.CaptchaType;
import cli.li.resolver.captcha.solver.ICaptchaSolver;
import cli.li.resolver.service.captcha.ICaptchaService;
import cli.li.resolver.logger.LoggerService;

/**
 * Abstract base class for CAPTCHA service implementations.
 * This class provides common functionality for all CAPTCHA services.
 */
public abstract class AbstractCaptchaService implements ICaptchaService {
    // Constants
    protected static final int POLL_INTERVAL_MS = 2000; // 2 seconds between result polling requests
    protected static final int MAX_POLLS = 30; // Maximum number of attempts to get the result

    // Common fields
    protected String apiKey = "";
    protected boolean enabled = true;
    protected int priority = 0;
    protected BigDecimal balance = BigDecimal.ZERO;
    protected final CaptchaServiceStatistics statistics = new CaptchaServiceStatistics();
    protected final Map<CaptchaType, ICaptchaSolver> solvers = new ConcurrentHashMap<>();
    protected final LoggerService logger;

    protected AbstractCaptchaService() {
        this.logger = LoggerService.getInstance();
        initializeSolvers();
        logger.info(getClass().getSimpleName(), "Service initialized");
    }

    /**
     * Initialize the solvers for this service.
     * This method should be implemented by each service to register its solver implementations.
     */
    protected abstract void initializeSolvers();

    /**
     * Get the base URL for API requests.
     * @return The base API URL
     */
    protected abstract String getApiBaseUrl();

    /**
     * Get the URL for creating a task.
     * @return The task creation URL
     */
    protected abstract String getCreateTaskUrl();

    /**
     * Get the URL for retrieving task results.
     * @return The task result URL
     */
    protected abstract String getTaskResultUrl();

    /**
     * Get the URL for checking account balance.
     * @return The balance URL
     */
    protected abstract String getBalanceUrl();

    /**
     * Send a request to check the account balance.
     * @return The response from the balance API
     * @throws Exception if an error occurs during the request
     */
    protected abstract String sendBalanceRequest() throws Exception;

    /**
     * Parse the response from the balance API.
     * @param response The response to parse
     * @return The parsed balance
     * @throws Exception if an error occurs during parsing
     */
    protected abstract BigDecimal parseBalanceResponse(String response) throws Exception;

    /**
     * Internal method to solve CAPTCHA with the provided params
     *
     * @param params Parameters for the CAPTCHA solving
     * @return Response from the CAPTCHA service
     * @throws CaptchaSolverException If an error occurs during the CAPTCHA solving process
     */
    protected abstract String solveCaptchaInternal(Map<String, Object> params) throws CaptchaSolverException;

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        logger.info(getClass().getSimpleName(), "Service " + (enabled ? "enabled" : "disabled"));
    }

    @Override
    public String getApiKey() {
        return apiKey;
    }

    @Override
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
        logger.info(getClass().getSimpleName(), "API key updated");
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public void setPriority(int priority) {
        this.priority = priority;
        logger.info(getClass().getSimpleName(), "Priority set to " + priority);
    }

    @Override
    public BigDecimal getBalance() {
        if (!validateApiKey()) {
            logger.warning(getClass().getSimpleName(), "Cannot check balance: Invalid API key");
            return BigDecimal.ZERO;
        }

        try {
            logger.info(getClass().getSimpleName(), "Checking balance");
            
            String response = sendBalanceRequest();
            balance = parseBalanceResponse(response);
            
            logger.info(getClass().getSimpleName(), "Balance retrieved: " + balance);
        } catch (Exception e) {
            logger.error(getClass().getSimpleName(), "Error checking balance: " + e.getMessage(), e);
        }

        return balance;
    }

    @Override
    public boolean validateApiKey() {
        boolean valid = apiKey != null && !apiKey.isEmpty();
        if (!valid) {
            logger.warning(getClass().getSimpleName(), "API key validation failed: key is empty");
        }
        return valid;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ICaptchaSolver getSolver(CaptchaType captchaType) {
        return solvers.get(captchaType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CaptchaServiceStatistics getStatistics() {
        return statistics;
    }
}
