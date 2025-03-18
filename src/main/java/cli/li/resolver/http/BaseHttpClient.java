package cli.li.resolver.http;

import java.util.Map;
import java.time.Duration;
import org.json.JSONObject;

/**
 * Base interface for HTTP client implementations
 */
public interface BaseHttpClient {
    /**
     * Performs a POST request
     * @param url URL for the request
     * @param params Request parameters
     * @return Server response
     * @throws Exception In case of an error
     */
    String post(String url, Map<String, String> params) throws Exception;

    /**
     * Performs a POST request with custom timeout
     * @param url URL for the request
     * @param params Request parameters
     * @param timeout Custom timeout duration
     * @return Server response
     * @throws Exception In case of an error
     */
    String post(String url, Map<String, String> params, Duration timeout) throws Exception;

    /**
     * Performs a POST request with a JSON body
     * @param url URL for the request
     * @param jsonBody Request body as JSON string
     * @return Server response
     * @throws Exception In case of an error
     */
    String postJson(String url, String jsonBody) throws Exception;

    /**
     * Performs a POST request with a JSON body and custom timeout
     * @param url URL for the request
     * @param jsonBody Request body as JSON string
     * @param timeout Custom timeout duration
     * @return Server response
     * @throws Exception In case of an error
     */
    String postJson(String url, String jsonBody, Duration timeout) throws Exception;

    /**
     * Performs a POST request with a JSON object
     * @param url URL for the request
     * @param jsonObject Request body as JSONObject
     * @return Server response
     * @throws Exception In case of an error
     */
    String postJson(String url, JSONObject jsonObject) throws Exception;

    /**
     * Performs a POST request with a JSON object and custom timeout
     * @param url URL for the request
     * @param jsonObject Request body as JSONObject
     * @param timeout Custom timeout duration
     * @return Server response
     * @throws Exception In case of an error
     */
    String postJson(String url, JSONObject jsonObject, Duration timeout) throws Exception;

    /**
     * Performs a GET request
     * @param url URL for the request
     * @param params Request parameters
     * @return Server response
     * @throws Exception In case of an error
     */
    String get(String url, Map<String, String> params) throws Exception;
    
    /**
     * Performs a GET request with custom timeout
     * @param url URL for the request
     * @param params Request parameters
     * @param timeout Custom timeout duration
     * @return Server response
     * @throws Exception In case of an error
     */
    String get(String url, Map<String, String> params, Duration timeout) throws Exception;
}
