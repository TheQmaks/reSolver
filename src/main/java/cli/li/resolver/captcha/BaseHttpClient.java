package cli.li.resolver.captcha;

import java.util.Map;

/**
 * Base class for HTTP client
 */
public abstract class BaseHttpClient {
    protected final String apiKey;

    public BaseHttpClient(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * Performs a POST request
     * @param url URL for the request
     * @param params Request parameters
     * @return Server response
     * @throws Exception In case of an error
     */
    public String post(String url, Map<String, String> params) throws Exception {
        return executePost(url, params);
    }

    /**
     * Performs a GET request
     * @param url URL for the request
     * @param params Request parameters
     * @return Server response
     * @throws Exception In case of an error
     */
    public String get(String url, Map<String, String> params) throws Exception {
        return executeGet(url, params);
    }

    /**
     * Internal implementation of the POST request
     */
    protected abstract String executePost(String url, Map<String, String> params) throws Exception;

    /**
     * Internal implementation of the GET request
     */
    protected abstract String executeGet(String url, Map<String, String> params) throws Exception;
}