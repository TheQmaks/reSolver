package cli.li.resolver.http;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;
import org.json.JSONObject;

/**
 * Modern HTTP client implementation using Java 11 HttpClient API
 * for interacting with CAPTCHA service APIs
 */
public class HttpClientImpl implements BaseHttpClient {

    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    private static final String USER_AGENT = "reSolver Burp Suite Extension";
    
    // Use a singleton HttpClient instance for better performance
    private final HttpClient httpClient;

    public HttpClientImpl() {
        // Configure the HttpClient with default settings
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
                .build();
    }

    @Override
    public String post(String url, Map<String, String> params) throws Exception {
        return post(url, params, Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS));
    }
    
    @Override
    public String post(String url, Map<String, String> params, Duration timeout) throws Exception {
        // Build form data
        String formData = buildFormData(params);
        
        // Create POST request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(timeout)
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();
        
        // Send request and handle response
        return sendRequest(request);
    }

    @Override
    public String postJson(String url, String jsonBody) throws Exception {
        return postJson(url, jsonBody, Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS));
    }
    
    @Override
    public String postJson(String url, String jsonBody, Duration timeout) throws Exception {
        // Create POST request with JSON body
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .header("Content-Type", "application/json")
                .timeout(timeout)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        
        // Send request and handle response
        return sendRequest(request);
    }
    
    @Override
    public String postJson(String url, JSONObject jsonObject) throws Exception {
        return postJson(url, jsonObject.toString());
    }
    
    @Override
    public String postJson(String url, JSONObject jsonObject, Duration timeout) throws Exception {
        return postJson(url, jsonObject.toString(), timeout);
    }

    @Override
    public String get(String url, Map<String, String> params) throws Exception {
        return get(url, params, Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS));
    }
    
    @Override
    public String get(String url, Map<String, String> params, Duration timeout) throws Exception {
        // Create full URL with query parameters
        String fullUrl = url;
        if (!params.isEmpty()) {
            fullUrl += "?" + buildFormData(params);
        }
        
        // Create GET request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .header("User-Agent", USER_AGENT)
                .timeout(timeout)
                .GET()
                .build();
        
        // Send request and handle response
        return sendRequest(request);
    }
    
    /**
     * Sends an HTTP request and processes the response
     * 
     * @param request The HTTP request to send
     * @return The response body as a string
     * @throws IOException If an I/O error occurs
     * @throws InterruptedException If the operation is interrupted
     */
    private String sendRequest(HttpRequest request) throws IOException, InterruptedException {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            int statusCode = response.statusCode();
            if (statusCode >= 200 && statusCode < 300) {
                return response.body();
            } else {
                throw new IOException("HTTP error code: " + statusCode + ", response: " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            // Rethrow but with more context
            throw new IOException("HTTP request failed: " + request.uri() + " - " + e.getMessage(), e);
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
