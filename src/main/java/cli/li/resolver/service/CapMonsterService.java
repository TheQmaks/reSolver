package cli.li.resolver.service;

import java.util.Map;
import java.util.HashMap;
import java.math.BigDecimal;

import org.json.JSONObject;

import cli.li.resolver.captcha.*;

/**
 * Implementation of CapMonster service for solving CAPTCHAs
 */
public class CapMonsterService implements ICaptchaService {
    // API endpoints (CapMonster uses the same API format as Anti-Captcha)
    private static final String API_BASE_URL = "https://api.capmonster.cloud/";
    private static final String CREATE_TASK_URL = API_BASE_URL + "createTask";
    private static final String GET_TASK_RESULT_URL = API_BASE_URL + "getTaskResult";
    private static final String GET_BALANCE_URL = API_BASE_URL + "getBalance";

    // Constants
    private static final int POLL_INTERVAL_MS = 2000; // 2 seconds between result polling requests
    private static final int MAX_POLLS = 30; // Maximum number of attempts to get the result

    private String apiKey = "";
    private boolean enabled = true;
    private int priority = 2;
    private BigDecimal balance = BigDecimal.ZERO;
    private final CaptchaServiceStatistics statistics = new CaptchaServiceStatistics();
    private final Map<CaptchaType, ICaptchaSolver> solvers = new HashMap<>();

    public CapMonsterService() {
        // Initialize solvers for supported CAPTCHA types
        solvers.put(CaptchaType.RECAPTCHA_V2, new CapMonsterRecaptchaV2Solver(this));
        solvers.put(CaptchaType.RECAPTCHA_V3, new CapMonsterRecaptchaV3Solver(this));
    }

    @Override
    public String getName() {
        return "CapMonster";
    }

    @Override
    public String getId() {
        return "capmonster";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String getApiKey() {
        return apiKey;
    }

    @Override
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public BigDecimal getBalance() {
        if (!validateApiKey()) {
            return BigDecimal.ZERO;
        }

        try {
            BaseHttpClient httpClient = new HttpClientImpl(apiKey);

            // Create JSON object for balance request
            JSONObject requestJson = new JSONObject();
            requestJson.put("clientKey", apiKey);

            // CapMonster API only works with JSON
            Map<String, String> params = new HashMap<>();
            params.put("json", requestJson.toString());

            String response = httpClient.post(GET_BALANCE_URL, params);
            JSONObject responseJson = new JSONObject(response);

            if (responseJson.getInt("errorId") == 0) {
                balance = new BigDecimal(responseJson.getDouble("balance"));
            } else {
                // Error handling
                String errorMessage = responseJson.getString("errorDescription");
                throw new Exception("Error getting balance: " + errorMessage);
            }
        } catch (Exception e) {
            // Error logging should be implemented in a real application
        }

        return balance;
    }

    @Override
    public boolean validateApiKey() {
        return apiKey != null && !apiKey.isEmpty();
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
     * Generic method for sending a CAPTCHA solving request to CapMonster
     */
    protected String solveCaptchaGeneric(JSONObject taskJson) throws CaptchaSolverException {
        if (!validateApiKey()) {
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

            String createTaskResponse = httpClient.post(CREATE_TASK_URL, params);
            JSONObject createTaskJson = new JSONObject(createTaskResponse);

            if (createTaskJson.getInt("errorId") != 0) {
                throw new CaptchaSolverException("Error creating task: " + createTaskJson.getString("errorDescription"));
            }

            int taskId = createTaskJson.getInt("taskId");

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

                String resultResponse = httpClient.post(GET_TASK_RESULT_URL, params);
                JSONObject resultJson = new JSONObject(resultResponse);

                if (resultJson.getInt("errorId") != 0) {
                    throw new CaptchaSolverException("Error getting result: " + resultJson.getString("errorDescription"));
                }

                String status = resultJson.getString("status");
                if ("ready".equals(status)) {
                    // Task is solved
                    JSONObject solution = resultJson.getJSONObject("solution");
                    return solution.getString("token");
                } else if ("processing".equals(status)) {
                    // Task is still processing, continue waiting
                    continue;
                } else {
                    throw new CaptchaSolverException("Unexpected task status: " + status);
                }
            }

            throw new CaptchaSolverException("Timeout waiting for CAPTCHA solution");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CaptchaSolverException("Solving interrupted", e);
        } catch (Exception e) {
            throw new CaptchaSolverException("Error solving CAPTCHA", e);
        }
    }

    // Solver classes for specific CAPTCHA types

    private record CapMonsterRecaptchaV2Solver(CapMonsterService service) implements ICaptchaSolver {
        @Override
        public String solve(CaptchaRequest request) throws CaptchaSolverException {
            JSONObject taskJson = new JSONObject();
            taskJson.put("type", "NoCaptchaTaskProxyless");
            taskJson.put("websiteURL", request.url());
            taskJson.put("websiteKey", request.siteKey());

            // Handle additional parameters
            Map<String, String> additionalParams = request.additionalParams();
            if (additionalParams.containsKey("is_invisible") && "true".equals(additionalParams.get("is_invisible"))) {
                taskJson.put("isInvisible", true);
            }

            return service.solveCaptchaGeneric(taskJson);
        }

        @Override
        public CaptchaType getSupportedType() {
            return CaptchaType.RECAPTCHA_V2;
        }
    }

    private record CapMonsterRecaptchaV3Solver(CapMonsterService service) implements ICaptchaSolver {
        @Override
        public String solve(CaptchaRequest request) throws CaptchaSolverException {
            JSONObject taskJson = new JSONObject();
            taskJson.put("type", "RecaptchaV3TaskProxyless");
            taskJson.put("websiteURL", request.url());
            taskJson.put("websiteKey", request.siteKey());

            // For reCAPTCHA v3, the "action" parameter is required
            Map<String, String> additionalParams = request.additionalParams();
            if (additionalParams.containsKey("action")) {
                taskJson.put("pageAction", additionalParams.get("action"));
            } else {
                taskJson.put("pageAction", "verify"); // Default value
            }

            // Minimum score (0.3 by default for CapMonster)
            if (additionalParams.containsKey("min_score")) {
                taskJson.put("minScore", Double.parseDouble(additionalParams.get("min_score")));
            }

            return service.solveCaptchaGeneric(taskJson);
        }

        @Override
        public CaptchaType getSupportedType() {
            return CaptchaType.RECAPTCHA_V3;
        }
    }
}