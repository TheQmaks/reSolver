package cli.li.resolver.service.captcha;

import java.util.Map;
import java.util.HashMap;
import java.math.BigDecimal;
import java.time.Duration;

import org.json.JSONObject;

import cli.li.resolver.captcha.model.CaptchaType;
import cli.li.resolver.captcha.exception.CaptchaSolverException;
import cli.li.resolver.http.BaseHttpClient;
import cli.li.resolver.http.HttpClientImpl;
import cli.li.resolver.service.captcha.impl.AntiCaptchaRecaptchaV2Solver;
import cli.li.resolver.service.captcha.impl.AntiCaptchaRecaptchaV3Solver;

/**
 * Implementation of Anti-Captcha service for solving CAPTCHAs
 */
public class AntiCaptchaService extends AbstractCaptchaService {
    // API endpoints
    private static final String API_BASE_URL = "https://api.anti-captcha.com/";
    private static final String CREATE_TASK_URL = API_BASE_URL + "createTask";
    private static final String GET_TASK_RESULT_URL = API_BASE_URL + "getTaskResult";
    private static final String GET_BALANCE_URL = API_BASE_URL + "getBalance";
    
    public AntiCaptchaService() {
        super();
        priority = 1; // Default priority for Anti-Captcha
    }

    @Override
    protected void initializeSolvers() {
        // Initialize solvers for supported CAPTCHA types
        solvers.put(CaptchaType.RECAPTCHA_V2, new AntiCaptchaRecaptchaV2Solver(this));
        solvers.put(CaptchaType.RECAPTCHA_V3, new AntiCaptchaRecaptchaV3Solver(this));
    }

    @Override
    protected String getApiBaseUrl() {
        return API_BASE_URL;
    }

    @Override
    protected String getCreateTaskUrl() {
        return CREATE_TASK_URL;
    }

    @Override
    protected String getTaskResultUrl() {
        return GET_TASK_RESULT_URL;
    }

    @Override
    protected String getBalanceUrl() {
        return GET_BALANCE_URL;
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
    protected String sendBalanceRequest() throws Exception {
        BaseHttpClient httpClient = new HttpClientImpl();
        
        // Create proper JSON request for AntiCaptcha API
        JSONObject requestJson = new JSONObject();
        requestJson.put("clientKey", apiKey);
        
        logger.debug(getClass().getSimpleName(), "Sending balance check request to Anti-Captcha API");
        // Use reasonable timeout for balance check to prevent hanging threads
        String response = httpClient.postJson(GET_BALANCE_URL, requestJson.toString(), Duration.ofSeconds(5));
        logger.debug(getClass().getSimpleName(), "Received balance response from Anti-Captcha API");
        
        return response;
    }

    @Override
    protected BigDecimal parseBalanceResponse(String response) throws Exception {
        JSONObject responseJson = new JSONObject(response);

        if (responseJson.getInt("errorId") == 0) {
            BigDecimal balance = new BigDecimal(responseJson.getDouble("balance"));
            return balance;
        } else {
            // Error handling
            String errorMessage = responseJson.getString("errorDescription");
            logger.error(getClass().getSimpleName(), "Error getting balance: " + errorMessage);
            throw new Exception("Error getting balance: " + errorMessage);
        }
    }

    /**
     * Generic method for sending a CAPTCHA solving request to Anti-Captcha
     */
    @Override
    protected String solveCaptchaInternal(Map<String, Object> taskParams) throws CaptchaSolverException {
        if (!validateApiKey()) {
            logger.error(getClass().getSimpleName(), "Cannot solve CAPTCHA: Invalid API key");
            throw new CaptchaSolverException("Invalid API key");
        }

        try {
            BaseHttpClient httpClient = new HttpClientImpl();
            
            // Create task creation request strictly following the API docs
            JSONObject taskJson = new JSONObject();
            for (Map.Entry<String, Object> entry : taskParams.entrySet()) {
                taskJson.put(entry.getKey(), entry.getValue());
            }

            // Create the root request object
            JSONObject requestJson = new JSONObject();
            requestJson.put("clientKey", apiKey);
            requestJson.put("task", taskJson);

            logger.info(getClass().getSimpleName(), "Sending CAPTCHA to Anti-Captcha API");
            logger.debug(getClass().getSimpleName(), "Task type: " + taskJson.getString("type"));
            logger.debug(getClass().getSimpleName(), "Request JSON: " + requestJson.toString());

            // Send task creation request using postJson
            String createTaskResponse = httpClient.postJson(getCreateTaskUrl(), requestJson.toString());
            JSONObject createTaskJson = new JSONObject(createTaskResponse);
            logger.debug(getClass().getSimpleName(), "Response: " + createTaskResponse);

            if (createTaskJson.getInt("errorId") != 0) {
                String errorMessage = createTaskJson.optString("errorDescription", "Unknown error");
                logger.error(getClass().getSimpleName(), "Error creating task: " + errorMessage);
                throw new CaptchaSolverException("Error creating task: " + errorMessage);
            }

            int taskId = createTaskJson.getInt("taskId");
            logger.info(getClass().getSimpleName(), "CAPTCHA task created with ID: " + taskId);

            // Request to get the result
            JSONObject getResultJson = new JSONObject();
            getResultJson.put("clientKey", apiKey);
            getResultJson.put("taskId", taskId);

            // Wait for the solution
            int attempts = 0;
            while (attempts < MAX_POLLS) {
                Thread.sleep(POLL_INTERVAL_MS);
                attempts++;

                logger.debug(getClass().getSimpleName(),
                        "Polling for result, attempt " + attempts + " of " + MAX_POLLS);

                // Send the get result request using postJson
                String resultResponse = httpClient.postJson(getTaskResultUrl(), getResultJson.toString());
                JSONObject resultJson = new JSONObject(resultResponse);
                logger.debug(getClass().getSimpleName(), "Result response: " + resultResponse);

                if (resultJson.getInt("errorId") != 0) {
                    String errorMessage = resultJson.optString("errorDescription", "Unknown error");
                    logger.error(getClass().getSimpleName(), "Error getting result: " + errorMessage);
                    throw new CaptchaSolverException("Error getting result: " + errorMessage);
                }

                String status = resultJson.getString("status");
                if ("ready".equals(status)) {
                    // CAPTCHA successfully solved
                    JSONObject solution = resultJson.getJSONObject("solution");
                    String token;
                    
                    // Try to get gRecaptchaResponse first (for reCAPTCHA v2/v3)
                    if (solution.has("gRecaptchaResponse")) {
                        token = solution.getString("gRecaptchaResponse");
                    } else {
                        // Fall back to generic token field if gRecaptchaResponse is not present
                        token = solution.getString("token");
                    }
                    
                    // Truncate token for logging
                    String truncatedToken = token.length() > 20 ?
                            token.substring(0, 10) + "..." + token.substring(token.length() - 10) : token;
                            
                    logger.info(getClass().getSimpleName(),
                            "CAPTCHA solved successfully, token: " + truncatedToken);
                    return token;
                }
            }

            // Max attempts reached
            logger.error(getClass().getSimpleName(), "Max polling attempts reached, CAPTCHA not solved");
            throw new CaptchaSolverException("Max polling attempts reached, CAPTCHA not solved");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error(getClass().getSimpleName(), "CAPTCHA solving interrupted", e);
            throw new CaptchaSolverException("CAPTCHA solving interrupted", e);
        } catch (Exception e) {
            if (!(e instanceof CaptchaSolverException)) {
                logger.error(getClass().getSimpleName(), "Error solving CAPTCHA: " + e.getMessage(), e);
                throw new CaptchaSolverException("Error solving CAPTCHA", e);
            }
            throw (CaptchaSolverException) e;
        }
    }

    // Helper method for RecaptchaV2 solver
    public String solveRecaptchaV2(String siteKey, String pageUrl, Map<String, String> extraParams) throws CaptchaSolverException {
        // Create task object with proper structure
        Map<String, Object> params = new HashMap<>();
        
        // Create proper task object
        JSONObject taskObj = new JSONObject();
        taskObj.put("type", "NoCaptchaTaskProxyless");
        taskObj.put("websiteURL", pageUrl);
        taskObj.put("websiteKey", siteKey);
        
        // Add any extra parameters to task object
        if (extraParams != null) {
            for (Map.Entry<String, String> entry : extraParams.entrySet()) {
                taskObj.put(entry.getKey(), entry.getValue());
            }
        }
        
        // Convert JSONObject to Map
        for (String key : taskObj.keySet()) {
            params.put(key, taskObj.get(key));
        }
        
        return solveCaptchaInternal(params);
    }

    // Helper method for RecaptchaV3 solver
    public String solveRecaptchaV3(String siteKey, String pageUrl, String action, Double minScore, Map<String, String> extraParams) throws CaptchaSolverException {
        // Create task object with proper structure
        Map<String, Object> params = new HashMap<>();
        
        // Create proper task object
        JSONObject taskObj = new JSONObject();
        taskObj.put("type", "RecaptchaV3TaskProxyless");
        taskObj.put("websiteURL", pageUrl);
        taskObj.put("websiteKey", siteKey);
        
        if (action != null && !action.isEmpty()) {
            taskObj.put("pageAction", action);
        }
        
        if (minScore != null) {
            taskObj.put("minScore", minScore);
        }
        
        // Add any extra parameters to task object
        if (extraParams != null) {
            for (Map.Entry<String, String> entry : extraParams.entrySet()) {
                taskObj.put(entry.getKey(), entry.getValue());
            }
        }
        
        // Convert JSONObject to Map
        for (String key : taskObj.keySet()) {
            params.put(key, taskObj.get(key));
        }
        
        return solveCaptchaInternal(params);
    }
}
