package cli.li.resolver.service.captcha;

import java.util.Map;
import java.util.HashMap;
import java.time.Duration;
import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cli.li.resolver.http.HttpClientImpl;
import cli.li.resolver.http.BaseHttpClient;
import cli.li.resolver.captcha.model.CaptchaType;
import cli.li.resolver.captcha.exception.CaptchaSolverException;
import cli.li.resolver.service.captcha.impl.TwoCaptchaRecaptchaV2Solver;
import cli.li.resolver.service.captcha.impl.TwoCaptchaRecaptchaV3Solver;

/**
 * Implementation of 2Captcha service for solving CAPTCHAs
 */
public class TwoCaptchaService extends AbstractCaptchaService {
    // API endpoints
    private static final String API_BASE_URL = "https://2captcha.com/";
    private static final String IN_URL = API_BASE_URL + "in.php";
    private static final String RES_URL = API_BASE_URL + "res.php";
    private static final String BALANCE_URL = API_BASE_URL + "res.php";

    // Regex patterns
    private static final Pattern OK_PATTERN = Pattern.compile("OK\\|(.+)");

    public TwoCaptchaService() {
        super();
        priority = 0; // Default priority for 2Captcha
    }

    @Override
    protected void initializeSolvers() {
        // Initialize solvers for supported CAPTCHA types
        solvers.put(CaptchaType.RECAPTCHA_V2, new TwoCaptchaRecaptchaV2Solver(this));
        solvers.put(CaptchaType.RECAPTCHA_V3, new TwoCaptchaRecaptchaV3Solver(this));
    }

    @Override
    protected String getApiBaseUrl() {
        return API_BASE_URL;
    }

    @Override
    protected String getCreateTaskUrl() {
        return IN_URL;
    }

    @Override
    protected String getTaskResultUrl() {
        return RES_URL;
    }

    @Override
    protected String getBalanceUrl() {
        return BALANCE_URL;
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
    protected String sendBalanceRequest() throws Exception {
        BaseHttpClient httpClient = new HttpClientImpl();
        
        // Parameters for balance check
        Map<String, String> params = new HashMap<>();
        params.put("key", apiKey);
        params.put("action", "getbalance");
        
        logger.debug(getClass().getSimpleName(), "Sending balance check request to 2Captcha API");
        // Use reasonable timeout for balance check to prevent hanging threads
        String response = httpClient.post(BALANCE_URL, params, Duration.ofSeconds(5));
        logger.debug(getClass().getSimpleName(), "Received balance response from 2Captcha API");
        
        return response;
    }

    @Override
    protected BigDecimal parseBalanceResponse(String response) throws Exception {
        if (response.startsWith("ERROR")) {
            String errorMessage = response.substring("ERROR_".length());
            logger.error(getClass().getSimpleName(), "Error getting balance: " + errorMessage);
            throw new Exception("Error getting balance: " + errorMessage);
        }
        
        try {
            BigDecimal balance = new BigDecimal(response);
            return balance;
        } catch (NumberFormatException e) {
            logger.error(getClass().getSimpleName(), "Error parsing balance: " + response, e);
            throw new Exception("Error parsing balance: " + response, e);
        }
    }

    /**
     * Generic method for sending a CAPTCHA solving request to 2Captcha
     */
    @Override
    protected String solveCaptchaInternal(Map<String, Object> taskParams) throws CaptchaSolverException {
        if (!validateApiKey()) {
            logger.error(getClass().getSimpleName(), "Cannot solve CAPTCHA: Invalid API key");
            throw new CaptchaSolverException("Invalid API key");
        }

        try {
            BaseHttpClient httpClient = new HttpClientImpl();
            
            // Add API key to parameters
            Map<String, String> params = new HashMap<>();
            params.put("key", apiKey);
            
            // Add task parameters
            for (Map.Entry<String, Object> entry : taskParams.entrySet()) {
                params.put(entry.getKey(), entry.getValue().toString());
            }
            
            // Send request to create task
            logger.info(getClass().getSimpleName(), "Sending CAPTCHA to 2Captcha API");
            String inResponse = httpClient.post(IN_URL, params);
            
            // Parse the response
            if (inResponse.startsWith("ERROR")) {
                String errorMessage = inResponse.substring("ERROR_".length());
                logger.error(getClass().getSimpleName(), "Error creating task: " + errorMessage);
                throw new CaptchaSolverException("Error creating task: " + errorMessage);
            }
            
            // Extract task ID from response
            Matcher matcher = OK_PATTERN.matcher(inResponse);
            if (!matcher.find()) {
                logger.error(getClass().getSimpleName(), "Invalid response format: " + inResponse);
                throw new CaptchaSolverException("Invalid response format: " + inResponse);
            }
            
            String taskId = matcher.group(1);
            logger.info(getClass().getSimpleName(), "CAPTCHA task created with ID: " + taskId);
            
            // Prepare parameters for result check
            Map<String, String> resultParams = new HashMap<>();
            resultParams.put("key", apiKey);
            resultParams.put("action", "get");
            resultParams.put("id", taskId);
            
            // Poll for result
            int attempts = 0;
            while (attempts < MAX_POLLS) {
                Thread.sleep(POLL_INTERVAL_MS);
                attempts++;
                
                logger.debug(getClass().getSimpleName(), 
                        "Polling for result, attempt " + attempts + " of " + MAX_POLLS);
                
                String resResponse = httpClient.post(RES_URL, resultParams);
                
                if ("CAPCHA_NOT_READY".equals(resResponse)) {
                    // Task is still processing, continue waiting
                    continue;
                }
                
                if (resResponse.startsWith("ERROR")) {
                    String errorMessage = resResponse.substring("ERROR_".length());
                    logger.error(getClass().getSimpleName(), "Error getting result: " + errorMessage);
                    throw new CaptchaSolverException("Error getting result: " + errorMessage);
                }
                
                matcher = OK_PATTERN.matcher(resResponse);
                if (matcher.find()) {
                    // Task is solved
                    String token = matcher.group(1);
                    
                    // Truncate token for logging
                    String truncatedToken = token.length() > 20 ? 
                            token.substring(0, 10) + "..." + token.substring(token.length() - 10) : token;
                            
                    logger.info(getClass().getSimpleName(), 
                            "CAPTCHA solved successfully, token: " + truncatedToken);
                    return token;
                } else {
                    logger.error(getClass().getSimpleName(), "Invalid result format: " + resResponse);
                    throw new CaptchaSolverException("Invalid result format: " + resResponse);
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
        Map<String, Object> params = new HashMap<>();
        params.put("method", "userrecaptcha");
        params.put("googlekey", siteKey);
        params.put("pageurl", pageUrl);
        
        // Add any extra parameters
        if (extraParams != null) {
            params.putAll(extraParams);
        }
        
        return solveCaptchaInternal(params);
    }

    // Helper method for RecaptchaV3 solver
    public String solveRecaptchaV3(String siteKey, String pageUrl, String action, Double minScore, Map<String, String> extraParams) throws CaptchaSolverException {
        Map<String, Object> params = new HashMap<>();
        params.put("method", "userrecaptcha");
        params.put("googlekey", siteKey);
        params.put("pageurl", pageUrl);
        params.put("version", "v3");
        
        if (action != null && !action.isEmpty()) {
            params.put("action", action);
        }
        
        if (minScore != null) {
            params.put("min_score", minScore.toString());
        }
        
        // Add any extra parameters
        if (extraParams != null) {
            params.putAll(extraParams);
        }
        
        return solveCaptchaInternal(params);
    }
}
