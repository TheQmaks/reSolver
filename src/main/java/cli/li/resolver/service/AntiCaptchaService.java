package cli.li.resolver.service;

import java.util.Map;
import java.util.HashMap;
import java.math.BigDecimal;

import org.json.JSONObject;

import cli.li.resolver.captcha.*;
import cli.li.resolver.logger.BurpLoggerAdapter;

/**
 * Implementation of Anti-Captcha service for solving CAPTCHAs
 */
public class AntiCaptchaService implements ICaptchaService {
    // API endpoints
    private static final String API_BASE_URL = "https://api.anti-captcha.com/";
    private static final String CREATE_TASK_URL = API_BASE_URL + "createTask";
    private static final String GET_TASK_RESULT_URL = API_BASE_URL + "getTaskResult";
    private static final String GET_BALANCE_URL = API_BASE_URL + "getBalance";

    // Constants
    private static final int POLL_INTERVAL_MS = 2000; // 2 seconds between result polling requests
    private static final int MAX_POLLS = 30; // Maximum number of attempts to get the result

    private String apiKey = "";
    private boolean enabled = true;
    private int priority = 1;
    private BigDecimal balance = BigDecimal.ZERO;
    private final CaptchaServiceStatistics statistics = new CaptchaServiceStatistics();
    private final Map<CaptchaType, ICaptchaSolver> solvers = new HashMap<>();
    private final BurpLoggerAdapter logger;

    public AntiCaptchaService() {
        this.logger = BurpLoggerAdapter.getInstance();

        // Initialize solvers for supported CAPTCHA types
        solvers.put(CaptchaType.RECAPTCHA_V2, new AntiCaptchaRecaptchaV2Solver(this));
        solvers.put(CaptchaType.RECAPTCHA_V3, new AntiCaptchaRecaptchaV3Solver(this));
        logger.info("AntiCaptchaService", "Service initialized");
    }

    @Override
    public String getName() {
        return "Anti-Captcha";
    }

    @Override
    public String getId() {
        return "anticaptcha";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        logger.info("AntiCaptchaService", "Service " + (enabled ? "enabled" : "disabled"));
    }

    @Override
    public String getApiKey() {
        return apiKey;
    }

    @Override
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
        logger.info("AntiCaptchaService", "API key updated");
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public void setPriority(int priority) {
        this.priority = priority;
        logger.info("AntiCaptchaService", "Priority set to " + priority);
    }

    @Override
    public BigDecimal getBalance() {
        if (!validateApiKey()) {
            logger.warning("AntiCaptchaService", "Cannot check balance: Invalid API key");
            return BigDecimal.ZERO;
        }

        try {
            logger.info("AntiCaptchaService", "Checking balance");
            BaseHttpClient httpClient = new HttpClientImpl(apiKey);

            // Create JSON object for balance request
            JSONObject requestJson = new JSONObject();
            requestJson.put("clientKey", apiKey);

            // Anti-Captcha API only works with JSON
            Map<String, String> params = new HashMap<>();
            params.put("json", requestJson.toString());

            logger.debug("AntiCaptchaService", "Sending balance check request to Anti-Captcha API");
            String response = httpClient.post(GET_BALANCE_URL, params);
            logger.debug("AntiCaptchaService", "Received balance response from Anti-Captcha API");

            JSONObject responseJson = new JSONObject(response);

            if (responseJson.getInt("errorId") == 0) {
                balance = new BigDecimal(responseJson.getDouble("balance"));
                logger.info("AntiCaptchaService", "Balance retrieved: " + balance);
            } else {
                // Error handling
                String errorMessage = responseJson.getString("errorDescription");
                logger.error("AntiCaptchaService", "Error getting balance: " + errorMessage);
                throw new Exception("Error getting balance: " + errorMessage);
            }
        } catch (Exception e) {
            logger.error("AntiCaptchaService", "Error checking balance: " + e.getMessage(), e);
        }

        return balance;
    }

    @Override
    public boolean validateApiKey() {
        boolean valid = apiKey != null && !apiKey.isEmpty();
        if (!valid) {
            logger.warning("AntiCaptchaService", "API key validation failed: key is empty");
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
     * Generic method for sending a CAPTCHA solving request to Anti-Captcha
     */
    protected String solveCaptchaGeneric(JSONObject taskJson) throws CaptchaSolverException {
        if (!validateApiKey()) {
            logger.error("AntiCaptchaService", "Cannot solve CAPTCHA: Invalid API key");
            throw new CaptchaSolverException("Invalid API key");
        }

        try {
            BaseHttpClient httpClient = new HttpClientImpl(apiKey);

            // Create task creation request
            JSONObject requestJson = new JSONObject();
            requestJson.put("clientKey", apiKey);
            requestJson.put("task", taskJson);

            // Send task creation request
            Map<String, String> params = new HashMap<>();
            params.put("json", requestJson.toString());

            logger.info("AntiCaptchaService", "Sending CAPTCHA to Anti-Captcha API");
            logger.debug("AntiCaptchaService", "Task type: " + taskJson.getString("type"));

            String createTaskResponse = httpClient.post(CREATE_TASK_URL, params);
            JSONObject createTaskJson = new JSONObject(createTaskResponse);

            if (createTaskJson.getInt("errorId") != 0) {
                String errorMessage = createTaskJson.getString("errorDescription");
                logger.error("AntiCaptchaService", "Error creating task: " + errorMessage);
                throw new CaptchaSolverException("Error creating task: " + errorMessage);
            }

            int taskId = createTaskJson.getInt("taskId");
            logger.info("AntiCaptchaService", "CAPTCHA task created with ID: " + taskId);

            // Request to get the result
            JSONObject getResultJson = new JSONObject();
            getResultJson.put("clientKey", apiKey);
            getResultJson.put("taskId", taskId);

            params.clear();
            params.put("json", getResultJson.toString());

            // Wait for the solution
            int attempts = 0;
            while (attempts < MAX_POLLS) {
                Thread.sleep(POLL_INTERVAL_MS);
                attempts++;

                logger.debug("AntiCaptchaService",
                        "Polling for result, attempt " + attempts + " of " + MAX_POLLS);

                String resultResponse = httpClient.post(GET_TASK_RESULT_URL, params);
                JSONObject resultJson = new JSONObject(resultResponse);

                if (resultJson.getInt("errorId") != 0) {
                    String errorMessage = resultJson.getString("errorDescription");
                    logger.error("AntiCaptchaService", "Error getting result: " + errorMessage);
                    throw new CaptchaSolverException("Error getting result: " + errorMessage);
                }

                String status = resultJson.getString("status");
                if ("ready".equals(status)) {
                    // Task is solved
                    logger.info("AntiCaptchaService", "CAPTCHA solved successfully");
                    JSONObject solution = resultJson.getJSONObject("solution");
                    String token = solution.getString("token");

                    // Truncate token for logging
                    String truncatedToken = token.length() > 20 ?
                            token.substring(0, 10) + "..." + token.substring(token.length() - 10) : token;
                    logger.debug("AntiCaptchaService", "Received token: " + truncatedToken);

                    return token;
                } else if ("processing".equals(status)) {
                    // Task is still processing, continue waiting
                    logger.debug("AntiCaptchaService", "CAPTCHA still processing, waiting...");
                    continue;
                } else {
                    logger.error("AntiCaptchaService", "Unexpected task status: " + status);
                    throw new CaptchaSolverException("Unexpected task status: " + status);
                }
            }

            logger.error("AntiCaptchaService",
                    "Timeout waiting for CAPTCHA solution after " + MAX_POLLS + " attempts");
            throw new CaptchaSolverException("Timeout waiting for CAPTCHA solution");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("AntiCaptchaService", "Solving interrupted", e);
            throw new CaptchaSolverException("Solving interrupted", e);
        } catch (Exception e) {
            logger.error("AntiCaptchaService", "Error solving CAPTCHA: " + e.getMessage(), e);
            throw new CaptchaSolverException("Error solving CAPTCHA", e);
        }
    }

    // Solver classes for specific CAPTCHA types

    private record AntiCaptchaRecaptchaV2Solver(AntiCaptchaService service) implements ICaptchaSolver {
        @Override
        public String solve(CaptchaRequest request) throws CaptchaSolverException {
            service.logger.info("AntiCaptchaService",
                    "Solving reCAPTCHA v2 for site key: " + request.siteKey());

            JSONObject taskJson = new JSONObject();
            taskJson.put("type", "NoCaptchaTaskProxyless");
            taskJson.put("websiteURL", request.url());
            taskJson.put("websiteKey", request.siteKey());

            // Handle additional parameters
            Map<String, String> additionalParams = request.additionalParams();
            if (additionalParams.containsKey("is_invisible") && "true".equals(additionalParams.get("is_invisible"))) {
                taskJson.put("isInvisible", true);
                service.logger.debug("AntiCaptchaService", "Using invisible reCAPTCHA mode");
            }

            return service.solveCaptchaGeneric(taskJson);
        }

        @Override
        public CaptchaType getSupportedType() {
            return CaptchaType.RECAPTCHA_V2;
        }
    }

    private record AntiCaptchaRecaptchaV3Solver(AntiCaptchaService service) implements ICaptchaSolver {
        @Override
        public String solve(CaptchaRequest request) throws CaptchaSolverException {
            service.logger.info("AntiCaptchaService",
                    "Solving reCAPTCHA v3 for site key: " + request.siteKey());

            JSONObject taskJson = new JSONObject();
            taskJson.put("type", "RecaptchaV3TaskProxyless");
            taskJson.put("websiteURL", request.url());
            taskJson.put("websiteKey", request.siteKey());

            // For reCAPTCHA v3, the "action" parameter is required
            Map<String, String> additionalParams = request.additionalParams();
            if (additionalParams.containsKey("action")) {
                taskJson.put("pageAction", additionalParams.get("action"));
                service.logger.debug("AntiCaptchaService", "Using action: " + additionalParams.get("action"));
            } else {
                taskJson.put("pageAction", "verify"); // Default value
                service.logger.debug("AntiCaptchaService", "Using default action: verify");
            }

            // Minimum score (0.3 by default for Anti-Captcha)
            if (additionalParams.containsKey("min_score")) {
                taskJson.put("minScore", Double.parseDouble(additionalParams.get("min_score")));
                service.logger.debug("AntiCaptchaService",
                        "Using min score: " + additionalParams.get("min_score"));
            }

            return service.solveCaptchaGeneric(taskJson);
        }

        @Override
        public CaptchaType getSupportedType() {
            return CaptchaType.RECAPTCHA_V3;
        }
    }
}