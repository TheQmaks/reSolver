package cli.li.resolver.http;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import burp.api.montoya.http.handler.HttpHandler;
import burp.api.montoya.http.handler.HttpRequestToBeSent;
import burp.api.montoya.http.handler.HttpResponseReceived;
import burp.api.montoya.http.handler.RequestToBeSentAction;
import burp.api.montoya.http.handler.ResponseReceivedAction;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.requests.HttpRequest;

import cli.li.resolver.logger.LoggerService;
import cli.li.resolver.service.ServiceManager;
import cli.li.resolver.settings.SettingsManager;
import cli.li.resolver.stats.StatisticsCollector;
import cli.li.resolver.captcha.model.CaptchaRequest;
import cli.li.resolver.captcha.model.CaptchaType;
import cli.li.resolver.provider.SolveRequest;
import cli.li.resolver.detection.ResponseAnalyzer;
import cli.li.resolver.captcha.exception.CaptchaSolverException;

/**
 * Modifier for HTTP requests to solve CAPTCHAs and analyzer for HTTP responses
 */
public class HttpRequestModifier implements HttpHandler {
    private final ServiceManager serviceManager;
    private final PlaceholderParser placeholderParser;
    private final StatisticsCollector statisticsCollector;
    private final ResponseAnalyzer responseAnalyzer;
    private final SettingsManager settingsManager;
    private final LoggerService logger;

    public HttpRequestModifier(ServiceManager serviceManager, PlaceholderParser placeholderParser,
                               StatisticsCollector statisticsCollector, ResponseAnalyzer responseAnalyzer,
                               SettingsManager settingsManager) {
        this.serviceManager = serviceManager;
        this.placeholderParser = placeholderParser;
        this.statisticsCollector = statisticsCollector;
        this.responseAnalyzer = responseAnalyzer;
        this.settingsManager = settingsManager;
        this.logger = LoggerService.getInstance();
    }

    /**
     * Implementation of HttpHandler interface for processing HTTP requests
     * @param requestToBeSent HTTP request to be sent
     * @return Action containing modified HTTP request with solved CAPTCHAs
     */
    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent requestToBeSent) {
        // Find all placeholders
        List<PlaceholderLocation> placeholders = placeholderParser.findPlaceholders(requestToBeSent);

        // If no placeholders, return the original request
        if (placeholders.isEmpty()) {
            return RequestToBeSentAction.continueWith(requestToBeSent);
        }

        // Check if we have configured services
        if (!serviceManager.hasConfiguredServices()) {
            // No configured services, return the original request
            return RequestToBeSentAction.continueWith(requestToBeSent);
        }

        // Process placeholders and solve CAPTCHAs
        Map<String, String> solvedTokens = new HashMap<>();
        for (PlaceholderLocation placeholder : placeholders) {
            try {
                String token = solveCaptcha(placeholder.captchaRequest());
                if (token != null) {
                    solvedTokens.put(placeholder.placeholder(), token);
                }
            } catch (CaptchaSolverException e) {
                logger.warning("HttpRequestModifier", "Failed to solve CAPTCHA: " + e.getMessage());
                // Failed to solve CAPTCHA, continue with next placeholder
            }
        }

        // If no CAPTCHAs were solved, return the original request
        if (solvedTokens.isEmpty()) {
            return RequestToBeSentAction.continueWith(requestToBeSent);
        }

        // Replace placeholders with solved tokens
        HttpRequest modifiedRequest = replacePlaceholders(requestToBeSent, solvedTokens);
        return RequestToBeSentAction.continueWith(modifiedRequest, requestToBeSent.annotations());
    }

    /**
     * Implementation of HttpHandler interface for processing HTTP responses.
     * Analyzes HTML responses for CAPTCHA presence using the ResponseAnalyzer.
     */
    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived responseReceived) {
        // Skip auto-detection if disabled in settings
        if (!settingsManager.isAutoDetectionEnabled()) {
            return ResponseReceivedAction.continueWith(responseReceived);
        }

        // Check Content-Type header for text/html to trigger auto-detection
        try {
            String contentType = null;
            for (HttpHeader header : responseReceived.headers()) {
                if (header.name().equalsIgnoreCase("Content-Type")) {
                    contentType = header.value();
                    break;
                }
            }

            if (contentType != null && contentType.contains("text/html")) {
                String responseBody = responseReceived.bodyToString();
                String url = responseReceived.initiatingRequest().url();
                if (responseBody != null && !responseBody.isEmpty()) {
                    responseAnalyzer.analyze(url, responseBody);
                }
            }
        } catch (Exception e) {
            logger.error("HttpRequestModifier", "Error analyzing response: " + e.getMessage(), e);
        }

        return ResponseReceivedAction.continueWith(responseReceived);
    }

    /**
     * Solve a CAPTCHA using the ServiceManager's provider-based solve method
     * @param captchaRequest CAPTCHA request from placeholder parser
     * @return Solved token or null if failed
     * @throws CaptchaSolverException If solving fails
     */
    private String solveCaptcha(CaptchaRequest captchaRequest) throws CaptchaSolverException {
        // Build a SolveRequest from the CaptchaRequest
        CaptchaType captchaType = captchaRequest.captchaType();
        SolveRequest solveRequest = new SolveRequest(
                "", // API key will be filled by ServiceManager per provider
                captchaType.getCode(),
                captchaRequest.siteKey(),
                captchaRequest.url(),
                captchaRequest.additionalParams()
        );

        long startTime = System.currentTimeMillis();
        try {
            String token = serviceManager.solve(solveRequest);
            long endTime = System.currentTimeMillis();

            // Record statistics
            statisticsCollector.recordSolveAttempt(
                    captchaType,
                    "provider",
                    true,
                    endTime - startTime
            );

            return token;
        } catch (CaptchaSolverException e) {
            long endTime = System.currentTimeMillis();

            // Record failed attempt
            statisticsCollector.recordSolveAttempt(
                    captchaType,
                    "provider",
                    false,
                    endTime - startTime
            );

            throw e;
        }
    }

    /**
     * Replace placeholders in a request with solved tokens
     * @param request Original request
     * @param solvedTokens Map of placeholders to solved tokens
     * @return Modified request
     */
    private HttpRequest replacePlaceholders(HttpRequest request, Map<String, String> solvedTokens) {
        // Start with the original request
        HttpRequest modifiedRequest = request;

        // Replace placeholders in URL
        String url = modifiedRequest.url();
        if (url != null) {
            boolean urlModified = false;
            for (Map.Entry<String, String> entry : solvedTokens.entrySet()) {
                if (url.contains(entry.getKey())) {
                    url = url.replace(entry.getKey(), entry.getValue());
                    urlModified = true;
                }
            }
            if (urlModified) {
                String path = url;
                // Extract path+query from full URL for withPath()
                int schemeEnd = url.indexOf("://");
                if (schemeEnd >= 0) {
                    int pathStart = url.indexOf('/', schemeEnd + 3);
                    if (pathStart >= 0) {
                        path = url.substring(pathStart);
                    }
                }
                modifiedRequest = modifiedRequest.withPath(path);
            }
        }

        // Replace placeholders in body
        String body = modifiedRequest.bodyToString();
        if (body != null && !body.isEmpty()) {
            for (Map.Entry<String, String> entry : solvedTokens.entrySet()) {
                body = body.replace(entry.getKey(), entry.getValue());
            }
            modifiedRequest = modifiedRequest.withBody(body);
        }

        // Replace placeholders in headers
        List<HttpHeader> originalHeaders = modifiedRequest.headers();
        List<HttpHeader> updatedHeaders = new ArrayList<>();
        boolean headersModified = false;

        for (HttpHeader header : originalHeaders) {
            String value = header.value();
            boolean headerModified = false;

            for (Map.Entry<String, String> entry : solvedTokens.entrySet()) {
                if (value.contains(entry.getKey())) {
                    value = value.replace(entry.getKey(), entry.getValue());
                    headerModified = true;
                }
            }

            if (headerModified) {
                updatedHeaders.add(HttpHeader.httpHeader(header.name(), value));
                headersModified = true;
            } else {
                updatedHeaders.add(header);
            }
        }

        if (headersModified) {
            // Create a new request with all original headers removed
            HttpRequest tempRequest = modifiedRequest;
            for (HttpHeader header : originalHeaders) {
                tempRequest = tempRequest.withRemovedHeader(header.name());
            }

            // Add the updated headers
            for (HttpHeader header : updatedHeaders) {
                tempRequest = tempRequest.withAddedHeader(header);
            }

            modifiedRequest = tempRequest;
        }

        return modifiedRequest;
    }
}
