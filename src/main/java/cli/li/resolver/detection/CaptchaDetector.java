package cli.li.resolver.detection;

import java.util.List;

/**
 * Interface for detecting CAPTCHA instances in HTTP responses.
 */
public interface CaptchaDetector {
    List<DetectedCaptcha> detect(String url, String responseBody);
}
