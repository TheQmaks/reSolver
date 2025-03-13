package cli.li.resolver.http;

import java.util.*;

import burp.api.montoya.http.handler.*;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.requests.HttpRequest;

import cli.li.resolver.captcha.CaptchaRequest;
import cli.li.resolver.captcha.ICaptchaSolver;
import cli.li.resolver.service.ServiceManager;
import cli.li.resolver.stats.StatisticsCollector;
import cli.li.resolver.captcha.CaptchaSolverException;
import cli.li.resolver.thread.CaptchaSolverThreadManager;

/**
 * Modifier for HTTP requests to solve CAPTCHAs
 */
public class HttpRequestModifier implements HttpHandler {
    private final ServiceManager serviceManager;
    private final CaptchaSolverThreadManager threadManager;
    private final PlaceholderParser placeholderParser;
    private final StatisticsCollector statisticsCollector;

    public HttpRequestModifier(ServiceManager serviceManager, CaptchaSolverThreadManager threadManager, PlaceholderParser placeholderParser, StatisticsCollector statisticsCollector) {
        this.serviceManager = serviceManager;
        this.threadManager = threadManager;
        this.placeholderParser = placeholderParser;
        this.statisticsCollector = statisticsCollector;
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
     * Implementation of HttpHandler interface for processing HTTP responses
     * No modifications needed for responses
     */
    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived responseReceived) {
        // We don't modify responses
        return ResponseReceivedAction.continueWith(responseReceived);
    }

    /**
     * Solve a CAPTCHA using available services
     * @param captchaRequest CAPTCHA request
     * @return Solved token or null if failed
     * @throws CaptchaSolverException If solving fails
     */
    private String solveCaptcha(CaptchaRequest captchaRequest) throws CaptchaSolverException {
        // Get a solver for the CAPTCHA type
        ICaptchaSolver solver = serviceManager.getSolverForType(captchaRequest.captchaType());
        if (solver == null) {
            throw new CaptchaSolverException("No solver available for CAPTCHA type: " + captchaRequest.captchaType());
        }

        // Try to solve the CAPTCHA with retry logic
        long startTime = System.currentTimeMillis();
        try {
            String token = threadManager.executeWithRetry(solver, captchaRequest, 3);
            long endTime = System.currentTimeMillis();

            // Record statistics
            statisticsCollector.recordSolveAttempt(
                    captchaRequest.captchaType(),
                    solver,
                    true,
                    endTime - startTime
            );

            return token;
        } catch (CaptchaSolverException e) {
            long endTime = System.currentTimeMillis();

            // Record failed attempt
            statisticsCollector.recordSolveAttempt(
                    captchaRequest.captchaType(),
                    solver,
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