package cli.li.resolver.http;

import java.net.URI;
import java.util.Map;
import java.io.IOException;
import java.net.URLEncoder;

import org.json.JSONObject;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;

import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.http.message.HttpRequestResponse;

import cli.li.resolver.ResolverExtension;
import cli.li.resolver.logger.LoggerService;

/**
 * HTTP client implementation using Burp's HTTP API
 * for interacting with CAPTCHA service APIs
 */
public class HttpClientImpl implements BaseHttpClient {

    private static final String USER_AGENT = "reSolver Burp Suite Extension";
    private final LoggerService logger;
    
    public HttpClientImpl() {
        this.logger = LoggerService.getInstance();
        logger.debug("HttpClientImpl", "Initialized HTTP client using Burp API");
    }

    @Override
    public String post(String url, Map<String, String> params) throws Exception {
        // Build form data
        String formData = buildFormData(params);
        
        logger.debug("HttpClientImpl", "Sending POST request to " + url + " with form data");
        
        // Create POST request using Burp API
        HttpRequest request = HttpRequest.httpRequestFromUrl(url)
                .withMethod("POST")
                .withHeader("User-Agent", USER_AGENT)
                .withHeader("Content-Type", "application/x-www-form-urlencoded")
                .withBody(formData);
        
        // Send request and handle response
        return sendRequest(request, url);
    }

    @Override
    public String postJson(String url, String jsonBody) throws Exception {
        logger.debug("HttpClientImpl", "Sending POST request to " + url + " with JSON body");
        
        // Create POST request with JSON body using Burp API
        HttpRequest request = HttpRequest.httpRequestFromUrl(url)
                .withMethod("POST")
                .withHeader("User-Agent", USER_AGENT)
                .withHeader("Content-Type", "application/json")
                .withBody(jsonBody);
        
        // Send request and handle response
        return sendRequest(request, url);
    }
    
    @Override
    public String postJson(String url, JSONObject jsonObject) throws Exception {
        return postJson(url, jsonObject.toString());
    }

    @Override
    public String get(String url, Map<String, String> params) throws Exception {
        // Create full URL with query parameters
        String fullUrl = url;
        if (!params.isEmpty()) {
            fullUrl += "?" + buildFormData(params);
        }
        
        logger.debug("HttpClientImpl", "Sending GET request to " + fullUrl);
        
        // Create GET request using Burp API
        HttpRequest request = HttpRequest.httpRequestFromUrl(fullUrl)
                .withMethod("GET")
                .withHeader("User-Agent", USER_AGENT);
        
        // Send request and handle response
        return sendRequest(request, fullUrl);
    }
    
    /**
     * Sends an HTTP request using Burp's API and processes the response
     * 
     * @param request The HTTP request to send
     * @param url Original URL for logging
     * @return The response body as a string
     * @throws IOException If an I/O error occurs
     */
    private String sendRequest(HttpRequest request, String url) throws IOException {
        try {
            // Parse URL to extract necessary information
            new URI(url); // Validate the URL format
            
            // Use Burp's HTTP API to send the request
            // Note: Burp doesn't support explicit timeouts, but it will respect the system's
            // network timeout settings and proxy configurations automatically
            HttpRequestResponse requestResponse = ResolverExtension.api.http().sendRequest(request);
            HttpResponse response = requestResponse.response();
            
            if (response == null) {
                throw new IOException("No response received from server: " + url);
            }
            
            int statusCode = response.statusCode();
            if (statusCode >= 200 && statusCode < 300) {
                return response.bodyToString();
            } else {
                throw new IOException("HTTP error code: " + statusCode + ", response: " + response.bodyToString());
            }
        } catch (Exception e) {
            // Rethrow with more context
            throw new IOException("HTTP request failed: " + url + " - " + e.getMessage(), e);
        }
    }
    
    /**
     * Builds URL-encoded form data from a map of parameters
     * 
     * @param params The parameters to encode
     * @return URL-encoded form data string
     */
    private String buildFormData(Map<String, String> params) {
        return params.entrySet().stream()
                .map(entry -> {
                    try {
                        return URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.toString()) +
                               "=" +
                               URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.toString());
                    } catch (IOException e) {
                        throw new RuntimeException("Error encoding form data", e);
                    }
                })
                .collect(Collectors.joining("&"));
    }
}
