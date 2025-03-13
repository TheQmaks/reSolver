package cli.li.resolver.http;

import java.util.Map;

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
     * Performs a GET request
     * @param url URL for the request
     * @param params Request parameters
     * @return Server response
     * @throws Exception In case of an error
     */
    String get(String url, Map<String, String> params) throws Exception;
}
