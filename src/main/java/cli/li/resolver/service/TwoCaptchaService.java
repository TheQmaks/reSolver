package cli.li.resolver.service;

import java.util.Map;
import java.util.HashMap;
import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cli.li.resolver.captcha.*;
import cli.li.resolver.logger.LoggerService;

/**
 * Implementation of 2Captcha service for solving CAPTCHAs
 */
public class TwoCaptchaService implements ICaptchaService {
    // API endpoints
    private static final String API_BASE_URL = "https://2captcha.com/";
    private static final String IN_URL = API_BASE_URL + "in.php";
    private static final String RES_URL = API_BASE_URL + "res.php";
    private static final String BALANCE_URL = API_BASE_URL + "res.php";

    // Regex patterns
    private static final Pattern OK_PATTERN = Pattern.compile("OK\\|(.+)");

    // Constants
    private static final int POLL_INTERVAL_MS = 2000; // 2 seconds between result polling requests
    private static final int MAX_POLLS = 30; // Maximum number of attempts to get the result

    private String apiKey = "";
    private boolean enabled = true;
    private int priority = 0;
    private BigDecimal balance = BigDecimal.ZERO;
    private final CaptchaServiceStatistics statistics = new CaptchaServiceStatistics();
    private final Map<CaptchaType, ICaptchaSolver> solvers = new HashMap<>();

    private final LoggerService logger;

    public TwoCaptchaService() {
        this.logger = LoggerService.getInstance();

        // Initialize solvers for supported CAPTCHA types
        solvers.put(CaptchaType.RECAPTCHA_V2, new TwoCaptchaRecaptchaV2Solver(this));
        solvers.put(CaptchaType.RECAPTCHA_V3, new TwoCaptchaRecaptchaV3Solver(this));
        logger.info("TwoCaptchaService", "Service initialized");
    }

    @Override
    public String getName() {
        return "2Captcha";
    }

    @Override
    public String getId() {
        return "2captcha";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        logger.info("TwoCaptchaService", "Service " + (enabled ? "enabled" : "disabled"));
    }

    @Override
    public String getApiKey() {
        return apiKey;
    }

    @Override
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
        logger.info("TwoCaptchaService", "API key updated");
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public void setPriority(int priority) {
        this.priority = priority;
        logger.info("TwoCaptchaService", "Priority set to " + priority);
    }

    @Override
    public BigDecimal getBalance() {
        if (!validateApiKey()) {
            logger.warning("TwoCaptchaService", "Cannot check balance: Invalid API key");
            return BigDecimal.ZERO;
        }

        try {
            logger.info("TwoCaptchaService", "Checking balance");
            BaseHttpClient httpClient = new HttpClientImpl(apiKey);
            Map<String, String> params = new HashMap<>();
            params.put("action", "getbalance");
            params.put("json", "0");

            String response = httpClient.get(BALANCE_URL, params);
            balance = new BigDecimal(response);
            logger.info("TwoCaptchaService", "Balance retrieved: " + balance);
            return balance;
        } catch (Exception e) {
            logger.error("TwoCaptchaService", "Error checking balance: " + e.getMessage(), e);
        }

        return balance;
    }

    @Override
    public boolean validateApiKey() {
        boolean valid = apiKey != null && !apiKey.isEmpty();
        if (!valid) {
            logger.warning("TwoCaptchaService", "API key validation failed: key is empty");
        }
        return valid;
    }

    @Override
    public ICaptchaSolver getSolver(CaptchaType captchaType) {
        return solvers.get(captchaType);
    }

    @Override
    public CaptchaServiceStatistics getStatistics() {
        return statistics;
    }

    /**
     * Generic method for sending a CAPTCHA solving request
     */
    protected String solveCaptchaGeneric(Map<String, String> params) throws CaptchaSolverException {
        if (!validateApiKey()) {
            logger.error("TwoCaptchaService", "Cannot solve CAPTCHA: Invalid API key");
            throw new CaptchaSolverException("Invalid API key");
        }

        try {
            BaseHttpClient httpClient = new HttpClientImpl(apiKey);

            // Send CAPTCHA for solving
            params.put("json", "0"); // Response in text format
            logger.info("TwoCaptchaService", "Sending CAPTCHA to 2Captcha API");
            logger.debug("TwoCaptchaService", "Request parameters: " + params);

            String inResponse = httpClient.post(IN_URL, params);
            logger.debug("TwoCaptchaService", "2Captcha API response: " + inResponse);

            Matcher inMatcher = OK_PATTERN.matcher(inResponse);
            if (!inMatcher.find()) {
                logger.error("TwoCaptchaService", "Error from 2Captcha API: " + inResponse);
                throw new CaptchaSolverException("Error sending CAPTCHA to 2Captcha: " + inResponse);
            }

            // Got task ID
            String captchaId = inMatcher.group(1);
            logger.info("TwoCaptchaService", "CAPTCHA task created with ID: " + captchaId);

            // Wait for solution
            Map<String, String> resParams = new HashMap<>();
            resParams.put("action", "get");
            resParams.put("id", captchaId);
            resParams.put("json", "0");

            int attempts = 0;
            while (attempts < MAX_POLLS) {
                // Pause before requesting the result
                Thread.sleep(POLL_INTERVAL_MS);
                attempts++;

                logger.debug("TwoCaptchaService",
                        "Polling for result, attempt " + attempts + " of " + MAX_POLLS);
                String resResponse = httpClient.get(RES_URL, resParams);
                logger.debug("TwoCaptchaService", "Poll response: " + resResponse);

                if (resResponse.contains("CAPCHA_NOT_READY")) {
                    // CAPTCHA not solved yet, continue waiting
                    logger.debug("TwoCaptchaService", "CAPTCHA not ready yet, waiting");
                    continue;
                }

                Matcher resMatcher = OK_PATTERN.matcher(resResponse);
                if (resMatcher.find()) {
                    // CAPTCHA successfully solved
                    String token = resMatcher.group(1);
                    // Truncate token for logging
                    String truncatedToken = token.length() > 20 ?
                            token.substring(0, 10) + "..." + token.substring(token.length() - 10) : token;
                    logger.info("TwoCaptchaService",
                            "CAPTCHA solved successfully, token: " + truncatedToken);
                    return token;
                } else {
                    // Error in solving
                    logger.error("TwoCaptchaService",
                            "Error solving CAPTCHA: " + resResponse);
                    throw new CaptchaSolverException("Error solving CAPTCHA: " + resResponse);
                }
            }

            logger.error("TwoCaptchaService",
                    "Timeout waiting for CAPTCHA solution after " + MAX_POLLS + " attempts");
            throw new CaptchaSolverException("Timeout waiting for CAPTCHA solution");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("TwoCaptchaService", "Solving interrupted", e);
            throw new CaptchaSolverException("Solving interrupted", e);
        } catch (Exception e) {
            logger.error("TwoCaptchaService", "Error solving CAPTCHA", e);
            throw new CaptchaSolverException("Error solving CAPTCHA", e);
        }
    }

    // Solver classes for specific CAPTCHA types

    private record TwoCaptchaRecaptchaV2Solver(TwoCaptchaService service) implements ICaptchaSolver {
        @Override
        public String solve(CaptchaRequest request) throws CaptchaSolverException {
            service.logger.info("TwoCaptchaService",
                    "Solving reCAPTCHA v2 for site key: " + request.siteKey());

            Map<String, String> params = new HashMap<>();
            params.put("method", "userrecaptcha");
            params.put("googlekey", request.siteKey());
            params.put("pageurl", request.url());

            // Add additional parameters if present
            Map<String, String> additionalParams = request.additionalParams();
            if (additionalParams.containsKey("invisible")) {
                params.put("invisible", additionalParams.get("invisible"));
                service.logger.debug("TwoCaptchaService", "Using invisible reCAPTCHA mode");
            }
            if (additionalParams.containsKey("proxy")) {
                params.put("proxy", additionalParams.get("proxy"));
                service.logger.debug("TwoCaptchaService", "Using proxy: " + additionalParams.get("proxy"));
            }

            return service.solveCaptchaGeneric(params);
        }

        @Override
        public CaptchaType getSupportedType() {
            return CaptchaType.RECAPTCHA_V2;
        }
    }

    private record TwoCaptchaRecaptchaV3Solver(TwoCaptchaService service) implements ICaptchaSolver {
        @Override
        public String solve(CaptchaRequest request) throws CaptchaSolverException {
            service.logger.info("TwoCaptchaService",
                    "Solving reCAPTCHA v3 for site key: " + request.siteKey());

            Map<String, String> params = new HashMap<>();
            params.put("method", "userrecaptcha");
            params.put("googlekey", request.siteKey());
            params.put("pageurl", request.url());
            params.put("version", "v3");

            // For reCAPTCHA v3, the "action" parameter is required
            Map<String, String> additionalParams = request.additionalParams();

            // Default action value
            String action = additionalParams.getOrDefault("action", "verify");
            params.put("action", action);
            service.logger.debug("TwoCaptchaService", "Using action: " + action);

            // Minimum score (0.3 by default)
            if (additionalParams.containsKey("min_score")) {
                params.put("min_score", additionalParams.get("min_score"));
                service.logger.debug("TwoCaptchaService",
                        "Using min score: " + additionalParams.get("min_score"));
            }

            return service.solveCaptchaGeneric(params);
        }

        @Override
        public CaptchaType getSupportedType() {
            return CaptchaType.RECAPTCHA_V3;
        }
    }
}