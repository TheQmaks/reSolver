package cli.li.resolver.captcha;

import java.net.URL;
import java.util.Map;
import java.io.IOException;
import java.net.URLEncoder;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;

/**
 * HTTP client implementation for interacting with CAPTCHA service APIs
 */
public class HttpClientImpl extends BaseHttpClient {

    private static final int DEFAULT_TIMEOUT = 30000; // 30 seconds
    private static final String USER_AGENT = "reSolver Burp Suite Extension";

    public HttpClientImpl(String apiKey) {
        super(apiKey);
    }

    @Override
    protected String executePost(String url, Map<String, String> params) throws Exception {
        HttpURLConnection connection = null;
        try {
            // Add API key to parameters
            params.put("key", apiKey);

            // Create connection
            connection = createConnection(url, "POST");

            // Send data
            String postData = buildPostData(params);
            try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
                outputStream.writeBytes(postData);
                outputStream.flush();
            }

            // Get response
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return readResponse(connection);
            } else {
                String errorResponse = readErrorResponse(connection);
                throw new IOException("HTTP error code: " + responseCode + ", response: " + errorResponse);
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Override
    protected String executeGet(String url, Map<String, String> params) throws Exception {
        HttpURLConnection connection = null;
        try {
            // Add API key to parameters
            params.put("key", apiKey);

            // Create URL with parameters
            String fullUrl = url;
            if (!params.isEmpty()) {
                fullUrl += "?" + buildPostData(params);
            }

            // Create connection
            connection = createConnection(fullUrl, "GET");

            // Get response
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return readResponse(connection);
            } else {
                String errorResponse = readErrorResponse(connection);
                throw new IOException("HTTP error code: " + responseCode + ", response: " + errorResponse);
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Creates an HTTP connection with settings
     */
    private HttpURLConnection createConnection(String url, String method) throws IOException {
        URL urlObj = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();

        // Configure connection
        connection.setRequestMethod(method);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setConnectTimeout(DEFAULT_TIMEOUT);
        connection.setReadTimeout(DEFAULT_TIMEOUT);

        if ("POST".equals(method)) {
            connection.setDoOutput(true);
        }

        return connection;
    }

    /**
     * Builds parameter string for POST request
     */
    private String buildPostData(Map<String, String> params) {
        return params.entrySet().stream()
                .map(e -> {
                    try {
                        return URLEncoder.encode(e.getKey(), "UTF-8") + "=" +
                                URLEncoder.encode(e.getValue(), "UTF-8");
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                })
                .collect(Collectors.joining("&"));
    }

    /**
     * Reads response body
     */
    private String readResponse(HttpURLConnection connection) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
                response.append('\n');
            }
            return response.toString().trim();
        }
    }

    /**
     * Reads error response body
     */
    private String readErrorResponse(HttpURLConnection connection) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
                response.append('\n');
            }
            return response.toString().trim();
        } catch (Exception e) {
            return "Unable to read error response: " + e.getMessage();
        }
    }
}