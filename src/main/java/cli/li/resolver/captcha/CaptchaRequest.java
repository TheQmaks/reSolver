package cli.li.resolver.captcha;

import java.util.Map;

/**
 * Class representing a CAPTCHA solving request
 */
public record CaptchaRequest(CaptchaType captchaType, String siteKey, String url,
                             Map<String, String> additionalParams) {
}