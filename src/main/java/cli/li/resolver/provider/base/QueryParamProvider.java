package cli.li.resolver.provider.base;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import burp.api.montoya.http.RequestOptions;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.HttpRequestResponse;

import cli.li.resolver.ResolverExtension;
import cli.li.resolver.provider.CaptchaProvider;
import cli.li.resolver.provider.SolveRequest;
import cli.li.resolver.captcha.exception.CaptchaSolverException;

/**
 * Abstract base class for providers using form-encoded in.php/res.php protocol
 * (2Captcha-compatible). Subclasses provide the base URL and parameter maps.
 */
public abstract class QueryParamProvider implements CaptchaProvider {

    private static final int POLL_INTERVAL = 2000;
    private static final int MAX_POLLS = 60;
    private static final Pattern OK_PATTERN = Pattern.compile("OK\\|(.+)");

    /**
     * Get the base URL for the provider API (must end with '/').
     *
     * @return the base URL
     */
    protected abstract String baseUrl();

    /**
     * Build the parameters for the task submission request (in.php).
     *
     * @param request the solve request
     * @return map of form parameters
     */
    protected abstract Map<String, String> buildSubmitParams(SolveRequest request);

    /**
     * Build the parameters for the result polling request (res.php).
     *
     * @param apiKey the API key
     * @param taskId the task ID returned from submission
     * @return map of form parameters
     */
    protected abstract Map<String, String> buildResultParams(String apiKey, String taskId);

    /**
     * Build the parameters for the balance check request (res.php).
     *
     * @param apiKey the API key
     * @return map of form parameters
     */
    protected abstract Map<String, String> buildBalanceParams(String apiKey);

    @Override
    public String solve(SolveRequest request) throws CaptchaSolverException {
        try {
            // Build and send task submission request
            Map<String, String> submitParams = buildSubmitParams(request);
            String submitUrl = baseUrl() + "in.php";
            String submitResponse = sendPost(submitUrl, buildFormData(submitParams));

            // Parse "OK|taskId" response
            if (submitResponse.startsWith("ERROR")) {
                throw new CaptchaSolverException("Error creating task: " + submitResponse);
            }

            Matcher matcher = OK_PATTERN.matcher(submitResponse);
            if (!matcher.find()) {
                throw new CaptchaSolverException("Invalid response format: " + submitResponse);
            }

            String taskId = matcher.group(1);

            // Poll for result
            String resultUrl = baseUrl() + "res.php";
            Map<String, String> resultParams = buildResultParams(request.apiKey(), taskId);
            String resultFormData = buildFormData(resultParams);

            for (int attempt = 0; attempt < MAX_POLLS; attempt++) {
                Thread.sleep(POLL_INTERVAL);

                String resultResponse = sendPost(resultUrl, resultFormData);

                if ("CAPCHA_NOT_READY".equals(resultResponse)) {
                    continue;
                }

                if (resultResponse.startsWith("ERROR")) {
                    throw new CaptchaSolverException("Error getting result: " + resultResponse);
                }

                matcher = OK_PATTERN.matcher(resultResponse);
                if (matcher.find()) {
                    return matcher.group(1);
                } else {
                    throw new CaptchaSolverException("Invalid result format: " + resultResponse);
                }
            }

            throw new CaptchaSolverException("Max polling attempts reached, CAPTCHA not solved");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CaptchaSolverException("CAPTCHA solving interrupted", e);
        } catch (CaptchaSolverException e) {
            throw e;
        } catch (Exception e) {
            throw new CaptchaSolverException("Error solving CAPTCHA: " + e.getMessage(), e);
        }
    }

    @Override
    public BigDecimal fetchBalance(String apiKey) throws Exception {
        String url = baseUrl() + "res.php";
        Map<String, String> balanceParams = buildBalanceParams(apiKey);
        String response = sendPost(url, buildFormData(balanceParams));

        if (response.startsWith("ERROR")) {
            throw new Exception("Error getting balance: " + response);
        }

        try {
            return new BigDecimal(response.trim());
        } catch (NumberFormatException e) {
            throw new Exception("Error parsing balance: " + response, e);
        }
    }

    @Override
    public boolean isValidKeyFormat(String apiKey) {
        return apiKey != null && !apiKey.isEmpty()
                && apiKey.length() >= 10 && apiKey.matches("[a-zA-Z0-9]+");
    }

    /**
     * Send a POST request with form-encoded body using Burp's Montoya API.
     *
     * @param url      the target URL
     * @param formData the URL-encoded form data string
     * @return the response body as a string
     * @throws Exception if the request fails
     */
    private String sendPost(String url, String formData) throws Exception {
        HttpRequest request = HttpRequest.httpRequestFromUrl(url)
                .withMethod("POST")
                .withHeader("Content-Type", "application/x-www-form-urlencoded")
                .withBody(formData);

        HttpRequestResponse requestResponse = ResolverExtension.api.http()
                .sendRequest(request, RequestOptions.requestOptions().withUpstreamTLSVerification());

        if (requestResponse.response() == null) {
            throw new Exception("No response received from server: " + url);
        }

        int statusCode = requestResponse.response().statusCode();
        if (statusCode < 200 || statusCode >= 300) {
            throw new Exception("HTTP error code: " + statusCode
                    + ", response: " + requestResponse.response().bodyToString());
        }

        return requestResponse.response().bodyToString();
    }

    /**
     * Build URL-encoded form data from a map of parameters.
     *
     * @param params the parameters to encode
     * @return URL-encoded form data string
     */
    private String buildFormData(Map<String, String> params) {
        return params.entrySet().stream()
                .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
                        + "="
                        + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }
}
