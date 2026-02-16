package cli.li.resolver.provider.base;

import java.util.Map;
import java.math.BigDecimal;

import burp.api.montoya.http.RequestOptions;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.HttpRequestResponse;

import cli.li.resolver.ResolverExtension;
import cli.li.resolver.provider.CaptchaProvider;
import cli.li.resolver.provider.SolveRequest;
import cli.li.resolver.captcha.exception.CaptchaSolverException;

/**
 * Abstract base class for providers using JSON createTask/getTaskResult protocol
 * (Anti-Captcha-compatible). JSON is built manually without org.json.
 */
public abstract class JsonProtocolProvider implements CaptchaProvider {

    private static final int POLL_INTERVAL = 2000;
    private static final int MAX_POLLS = 60;

    /**
     * Get the base URL for the provider API (must end with '/').
     *
     * @return the base URL
     */
    protected abstract String baseUrl();

    /**
     * Build the task object fields for the createTask request.
     *
     * @param request the solve request
     * @return map of task fields
     */
    protected abstract Map<String, Object> buildTaskObject(SolveRequest request);

    /**
     * Extract the solution token from the solution JSON string.
     * The default implementation tries fields: "gRecaptchaResponse", "token", "text".
     *
     * @param solutionJson the solution JSON string
     * @return the extracted token
     * @throws CaptchaSolverException if no token can be extracted
     */
    protected String extractToken(String solutionJson) throws CaptchaSolverException {
        // Try standard token fields in order of priority
        String token = parseJsonString(solutionJson, "gRecaptchaResponse");
        if (token != null && !token.isEmpty()) {
            return token;
        }

        token = parseJsonString(solutionJson, "token");
        if (token != null && !token.isEmpty()) {
            return token;
        }

        token = parseJsonString(solutionJson, "text");
        if (token != null && !token.isEmpty()) {
            return token;
        }

        throw new CaptchaSolverException("Could not extract token from solution: " + solutionJson);
    }

    @Override
    public String solve(SolveRequest request) throws CaptchaSolverException {
        try {
            // Build createTask JSON
            Map<String, Object> taskFields = buildTaskObject(request);
            String taskJson = buildJsonObject(taskFields);
            String createTaskBody = "{\"clientKey\":" + escapeJsonString(request.apiKey())
                    + ",\"task\":" + taskJson + "}";

            // Send createTask request
            String createTaskUrl = baseUrl() + "createTask";
            String createResponse = sendPostJson(createTaskUrl, createTaskBody);

            // Check for errors
            int errorId = parseJsonInt(createResponse, "errorId");
            if (errorId != 0) {
                String errorDesc = parseJsonString(createResponse, "errorDescription");
                if (errorDesc == null) {
                    errorDesc = "Unknown error (errorId=" + errorId + ")";
                }
                throw new CaptchaSolverException("Error creating task: " + errorDesc);
            }

            // Extract taskId
            int taskId = parseJsonInt(createResponse, "taskId");
            if (taskId == 0) {
                throw new CaptchaSolverException("No taskId in response: " + createResponse);
            }

            // Build getTaskResult JSON
            String getResultBody = "{\"clientKey\":" + escapeJsonString(request.apiKey())
                    + ",\"taskId\":" + taskId + "}";

            // Poll for result
            String getResultUrl = baseUrl() + "getTaskResult";
            for (int attempt = 0; attempt < MAX_POLLS; attempt++) {
                Thread.sleep(POLL_INTERVAL);

                String resultResponse = sendPostJson(getResultUrl, getResultBody);

                // Check for errors
                errorId = parseJsonInt(resultResponse, "errorId");
                if (errorId != 0) {
                    String errorDesc = parseJsonString(resultResponse, "errorDescription");
                    if (errorDesc == null) {
                        errorDesc = "Unknown error (errorId=" + errorId + ")";
                    }
                    throw new CaptchaSolverException("Error getting result: " + errorDesc);
                }

                // Check status
                String status = parseJsonString(resultResponse, "status");
                if ("ready".equals(status)) {
                    // Extract solution object as substring
                    String solutionJson = extractJsonObject(resultResponse, "solution");
                    if (solutionJson == null) {
                        throw new CaptchaSolverException("No solution in response: " + resultResponse);
                    }
                    return extractToken(solutionJson);
                }
                // If not ready, continue polling
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
        String url = baseUrl() + "getBalance";
        String body = "{\"clientKey\":" + escapeJsonString(apiKey) + "}";
        String response = sendPostJson(url, body);

        int errorId = parseJsonInt(response, "errorId");
        if (errorId != 0) {
            String errorDesc = parseJsonString(response, "errorDescription");
            if (errorDesc == null) {
                errorDesc = "Unknown error (errorId=" + errorId + ")";
            }
            throw new Exception("Error getting balance: " + errorDesc);
        }

        double balance = parseJsonDouble(response, "balance");
        return BigDecimal.valueOf(balance);
    }

    @Override
    public boolean isValidKeyFormat(String apiKey) {
        return apiKey != null && !apiKey.isEmpty()
                && apiKey.length() >= 10 && apiKey.matches("[a-zA-Z0-9]+");
    }

    /**
     * Send a POST request with JSON body using Burp's Montoya API.
     *
     * @param url      the target URL
     * @param jsonBody the JSON body string
     * @return the response body as a string
     * @throws Exception if the request fails
     */
    private String sendPostJson(String url, String jsonBody) throws Exception {
        HttpRequest request = HttpRequest.httpRequestFromUrl(url)
                .withMethod("POST")
                .withHeader("Content-Type", "application/json")
                .withBody(jsonBody);

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

    // ---- JSON building helpers (no org.json dependency) ----

    /**
     * Build a JSON object string from a map of key-value pairs.
     * Supports String, Number, Boolean, and nested Map values.
     *
     * @param fields the key-value pairs
     * @return a JSON object string
     */
    private String buildJsonObject(Map<String, Object> fields) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append(escapeJsonString(entry.getKey()));
            sb.append(":");
            sb.append(toJsonValue(entry.getValue()));
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert a Java value to its JSON representation.
     *
     * @param value the value to convert
     * @return JSON representation
     */
    @SuppressWarnings("unchecked")
    private String toJsonValue(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof String) {
            return escapeJsonString((String) value);
        } else if (value instanceof Number) {
            return value.toString();
        } else if (value instanceof Boolean) {
            return value.toString();
        } else if (value instanceof Map) {
            return buildJsonObject((Map<String, Object>) value);
        } else {
            return escapeJsonString(value.toString());
        }
    }

    /**
     * Escape a string for JSON output, wrapping in double quotes.
     *
     * @param s the string to escape
     * @return the escaped and quoted string
     */
    private String escapeJsonString(String s) {
        if (s == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append("\"");
        return sb.toString();
    }

    // ---- JSON parsing helpers (no org.json dependency) ----

    /**
     * Parse a string value from JSON by key.
     *
     * @param json the JSON string
     * @param key  the key to find
     * @return the string value, or null if not found
     */
    private String parseJsonString(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) {
            return null;
        }

        // Find the colon after the key
        int colonIndex = json.indexOf(':', keyIndex + searchKey.length());
        if (colonIndex == -1) {
            return null;
        }

        // Skip whitespace after colon
        int valueStart = colonIndex + 1;
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }

        if (valueStart >= json.length()) {
            return null;
        }

        // Check if value is a quoted string
        if (json.charAt(valueStart) == '"') {
            // Find the end of the string, handling escaped quotes
            int valueEnd = valueStart + 1;
            while (valueEnd < json.length()) {
                char c = json.charAt(valueEnd);
                if (c == '\\') {
                    valueEnd += 2; // skip escaped character
                    continue;
                }
                if (c == '"') {
                    break;
                }
                valueEnd++;
            }
            if (valueEnd >= json.length()) {
                return null;
            }
            String raw = json.substring(valueStart + 1, valueEnd);
            // Unescape basic sequences
            return raw.replace("\\\"", "\"")
                       .replace("\\\\", "\\")
                       .replace("\\n", "\n")
                       .replace("\\r", "\r")
                       .replace("\\t", "\t");
        }

        // Check for null literal
        if (json.startsWith("null", valueStart)) {
            return null;
        }

        return null;
    }

    /**
     * Parse an integer value from JSON by key.
     *
     * @param json the JSON string
     * @param key  the key to find
     * @return the integer value, or 0 if not found
     */
    private int parseJsonInt(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) {
            return 0;
        }

        int colonIndex = json.indexOf(':', keyIndex + searchKey.length());
        if (colonIndex == -1) {
            return 0;
        }

        int valueStart = colonIndex + 1;
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }

        // Read digits (and optional leading minus)
        int valueEnd = valueStart;
        if (valueEnd < json.length() && json.charAt(valueEnd) == '-') {
            valueEnd++;
        }
        while (valueEnd < json.length() && Character.isDigit(json.charAt(valueEnd))) {
            valueEnd++;
        }

        if (valueEnd == valueStart) {
            return 0;
        }

        try {
            return Integer.parseInt(json.substring(valueStart, valueEnd));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Parse a double value from JSON by key.
     *
     * @param json the JSON string
     * @param key  the key to find
     * @return the double value, or 0.0 if not found
     */
    private double parseJsonDouble(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) {
            return 0.0;
        }

        int colonIndex = json.indexOf(':', keyIndex + searchKey.length());
        if (colonIndex == -1) {
            return 0.0;
        }

        int valueStart = colonIndex + 1;
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }

        // Read number characters (digits, minus, dot, e, E, +)
        int valueEnd = valueStart;
        while (valueEnd < json.length()) {
            char c = json.charAt(valueEnd);
            if (Character.isDigit(c) || c == '.' || c == '-' || c == '+' || c == 'e' || c == 'E') {
                valueEnd++;
            } else {
                break;
            }
        }

        if (valueEnd == valueStart) {
            return 0.0;
        }

        try {
            return Double.parseDouble(json.substring(valueStart, valueEnd));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * Extract a nested JSON object as a raw string by key.
     *
     * @param json the JSON string
     * @param key  the key whose value is a JSON object
     * @return the nested object as a JSON string, or null if not found
     */
    private String extractJsonObject(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) {
            return null;
        }

        int colonIndex = json.indexOf(':', keyIndex + searchKey.length());
        if (colonIndex == -1) {
            return null;
        }

        int objStart = json.indexOf('{', colonIndex);
        if (objStart == -1) {
            return null;
        }

        // Find the matching closing brace, accounting for nesting and strings
        int depth = 0;
        boolean inString = false;
        int i = objStart;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (inString) {
                if (c == '\\') {
                    i += 2; // skip escaped character
                    continue;
                } else if (c == '"') {
                    inString = false;
                }
            } else {
                if (c == '"') {
                    inString = true;
                } else if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        return json.substring(objStart, i + 1);
                    }
                }
            }
            i++;
        }

        return null;
    }
}
