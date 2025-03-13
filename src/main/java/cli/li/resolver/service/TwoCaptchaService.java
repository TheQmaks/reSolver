package cli.li.resolver.service;

import java.util.Map;
import java.util.HashMap;
import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cli.li.resolver.captcha.*;

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

    public TwoCaptchaService() {
        // Initialize solvers for supported CAPTCHA types
        solvers.put(CaptchaType.RECAPTCHA_V2, new TwoCaptchaRecaptchaV2Solver(this));
        solvers.put(CaptchaType.RECAPTCHA_V3, new TwoCaptchaRecaptchaV3Solver(this));
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
            Map<String, String> params = new HashMap<>();
            params.put("action", "getbalance");
            params.put("json", "0");

            String response = httpClient.get(BALANCE_URL, params);
            return new BigDecimal(response);
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
     * Generic method for sending a CAPTCHA solving request
     */
    protected String solveCaptchaGeneric(Map<String, String> params) throws CaptchaSolverException {
        if (!validateApiKey()) {
            throw new CaptchaSolverException("Invalid API key");
        }

        try {
            BaseHttpClient httpClient = new HttpClientImpl(apiKey);

            // Send CAPTCHA for solving
            params.put("json", "0"); // Response in text format
            String inResponse = httpClient.post(IN_URL, params);

            Matcher inMatcher = OK_PATTERN.matcher(inResponse);
            if (!inMatcher.find()) {
                throw new CaptchaSolverException("Error sending CAPTCHA to 2Captcha: " + inResponse);
            }

            // Got task ID
            String captchaId = inMatcher.group(1);

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

                String resResponse = httpClient.get(RES_URL, resParams);

                if (resResponse.contains("CAPCHA_NOT_READY")) {
                    // CAPTCHA not solved yet, continue waiting
                    continue;
                }

                Matcher resMatcher = OK_PATTERN.matcher(resResponse);
                if (resMatcher.find()) {
                    // CAPTCHA successfully solved
                    return resMatcher.group(1);
                } else {
                    // Error in solving
                    throw new CaptchaSolverException("Error solving CAPTCHA: " + resResponse);
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

    private record TwoCaptchaRecaptchaV2Solver(TwoCaptchaService service) implements ICaptchaSolver {
        @Override
        public String solve(CaptchaRequest request) throws CaptchaSolverException {
            Map<String, String> params = new HashMap<>();
            params.put("method", "userrecaptcha");
            params.put("googlekey", request.siteKey());
            params.put("pageurl", request.url());

            // Add additional parameters if present
            Map<String, String> additionalParams = request.additionalParams();
            if (additionalParams.containsKey("invisible")) {
                params.put("invisible", additionalParams.get("invisible"));
            }
            if (additionalParams.containsKey("proxy")) {
                params.put("proxy", additionalParams.get("proxy"));
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
            Map<String, String> params = new HashMap<>();
            params.put("method", "userrecaptcha");
            params.put("googlekey", request.siteKey());
            params.put("pageurl", request.url());
            params.put("version", "v3");

            // For reCAPTCHA v3, the "action" parameter is required
            Map<String, String> additionalParams = request.additionalParams();
            // Default value
            params.put("action", additionalParams.getOrDefault("action", "verify"));

            // Minimum score (0.3 by default)
            if (additionalParams.containsKey("min_score")) {
                params.put("min_score", additionalParams.get("min_score"));
            }

            return service.solveCaptchaGeneric(params);
        }

        @Override
        public CaptchaType getSupportedType() {
            return CaptchaType.RECAPTCHA_V3;
        }
    }
}