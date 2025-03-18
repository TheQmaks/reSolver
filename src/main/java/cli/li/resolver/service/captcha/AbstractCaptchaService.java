package cli.li.resolver.service.captcha;

import java.util.Map;
import java.time.Instant;
import java.time.Duration;
import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;

import cli.li.resolver.logger.LoggerService;
import cli.li.resolver.captcha.model.CaptchaType;
import cli.li.resolver.captcha.solver.ICaptchaSolver;
import cli.li.resolver.captcha.model.CaptchaServiceStatistics;
import cli.li.resolver.captcha.exception.CaptchaSolverException;

/**
 * Abstract base class for CAPTCHA service implementations.
 * This class provides common functionality for all CAPTCHA services.
 */
public abstract class AbstractCaptchaService implements ICaptchaService {
    // Constants
    protected static final int POLL_INTERVAL_MS = 2000; // 2 seconds between result polling requests
    protected static final int MAX_POLLS = 30; // Maximum number of attempts to get the result
    
    // Balance check cache settings (20 seconds)
    protected static final Duration BALANCE_CACHE_DURATION = Duration.ofSeconds(20);

    // Common fields
    protected String apiKey = "";
    protected boolean enabled = true;
    protected int priority = 0;
    protected BigDecimal balance = BigDecimal.ZERO;
    protected Instant lastBalanceCheckTime = Instant.EPOCH; // Initialize to epoch (1970) to force first check
    protected final CaptchaServiceStatistics statistics = new CaptchaServiceStatistics();
    protected final Map<CaptchaType, ICaptchaSolver> solvers = new ConcurrentHashMap<>();
    protected final LoggerService logger;
    
    // Flag to prevent parallel balance checks
    private volatile boolean isCheckingBalance = false;

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
    public synchronized BigDecimal getBalance() {
        // If no API key, return zero
        if (!validateApiKey()) {
            logger.warning(getClass().getSimpleName(), "Cannot check balance: Invalid API key");
            return BigDecimal.ZERO;
        }

        // Check if we need to refresh the balance based on the cache duration (20 seconds)
        boolean shouldRefreshBalance = Instant.now().isAfter(
                lastBalanceCheckTime.plus(BALANCE_CACHE_DURATION));
        
        // If cached value still valid, return immediately without starting a new thread
        if (!shouldRefreshBalance) {
            logger.debug(getClass().getSimpleName(), "Using cached balance: " + balance + " (expires in " + 
                Duration.between(Instant.now(), lastBalanceCheckTime.plus(BALANCE_CACHE_DURATION)).toSeconds() + " seconds)");
            return balance;
        }
        
        // Check if a balance check is already in progress
        if (isCheckingBalance) {
            logger.debug(getClass().getSimpleName(), "Balance check already in progress, using cached value: " + balance);
            return balance;
        }
        
        // Only refresh if cache expired (or first check) and no other check is in progress
        // Update in background thread to prevent UI freeze
        isCheckingBalance = true;
        new Thread(() -> {
            try {
                logger.info(getClass().getSimpleName(), "Checking balance (cache expired after " + 
                    BALANCE_CACHE_DURATION.toSeconds() + " seconds)");
                
                // Execute in background thread
                String response = sendBalanceRequest();
                BigDecimal newBalance = parseBalanceResponse(response);
                
                // Update cached balance value and timestamp
                synchronized (this) {
                    balance = newBalance;
                    lastBalanceCheckTime = Instant.now();
                }
                
                logger.info(getClass().getSimpleName(), "Balance retrieved: " + balance);
            } catch (Exception e) {
                logger.error(getClass().getSimpleName(), "Error checking balance: " + e.getMessage(), e);
                // Only update timestamp on error, keep last known good balance
                lastBalanceCheckTime = Instant.now();
            } finally {
                // Reset the flag to allow future balance checks
                isCheckingBalance = false;
            }
        }).start();
        
        // Return current value while check runs in background
        return balance;
    }

    @Override
    public boolean validateApiKey() {
        // Basic validation - check if key is not empty
        boolean valid = apiKey != null && !apiKey.isEmpty();
        if (!valid) {
            logger.warning(getClass().getSimpleName(), "API key validation failed: key is empty");
            return false;
        }
        
        // Check if key has valid format (subclasses can add more specific validation)
        if (!isValidKeyFormat(apiKey)) {
            logger.warning(getClass().getSimpleName(), "API key validation failed: invalid format");
            return false;
        }
        
        return true;
    }
    
    /**
     * Validates the format of the API key
     * Subclasses can override to provide service-specific validation
     * 
     * @param key The API key to validate
     * @return true if key format is valid
     */
    protected boolean isValidKeyFormat(String key) {
        // Basic validation - at least 10 characters, alphanumeric
        // This can be overridden by specific services with more precise validation
        return key.length() >= 10 && key.matches("[a-zA-Z0-9]+");
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
