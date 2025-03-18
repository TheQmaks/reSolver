package cli.li.resolver.captcha.model;

import java.util.Collections;
import java.util.Map;

/**
 * Represents a CAPTCHA solving request
 */
public class CaptchaRequest {
    private final String siteKey;
    private final String url;
    private final Map<String, String> additionalParams;
    private final CaptchaType captchaType;
    
    /**
     * Additional parameter keys
     */
    public static final String PARAM_TIMEOUT_SECONDS = "timeout_seconds";

    /**
     * Constructor for CAPTCHA request
     * @param siteKey CAPTCHA site key
     * @param url URL where the CAPTCHA is located
     * @param additionalParams Additional parameters for the request
     * @param captchaType Type of CAPTCHA
     */
    public CaptchaRequest(String siteKey, String url, Map<String, String> additionalParams, CaptchaType captchaType) {
        this.siteKey = siteKey;
        this.url = url;
        this.additionalParams = additionalParams != null ? 
                Collections.unmodifiableMap(additionalParams) : Collections.emptyMap();
        this.captchaType = captchaType;
    }

    /**
     * Get site key
     * @return CAPTCHA site key
     */
    public String siteKey() {
        return siteKey;
    }

    /**
     * Get URL
     * @return URL where the CAPTCHA is located
     */
    public String url() {
        return url;
    }

    /**
     * Get additional parameters
     * @return Map of additional parameters
     */
    public Map<String, String> additionalParams() {
        return additionalParams;
    }
    
    /**
     * Get CAPTCHA type
     * @return Type of CAPTCHA
     */
    public CaptchaType captchaType() {
        return captchaType;
    }
}
