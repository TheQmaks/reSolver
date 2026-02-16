package cli.li.resolver.detection;

import java.time.Instant;

/**
 * Represents a CAPTCHA instance detected in an HTTP response.
 *
 * @param pageUrl    the URL of the page where the CAPTCHA was found
 * @param type       the CAPTCHA type code (e.g. "recaptchav2", "hcaptcha")
 * @param siteKey    the site key extracted from the page
 * @param detectedAt the time the CAPTCHA was detected
 * @param evidence   a short snippet of the matching HTML
 */
public record DetectedCaptcha(
    String pageUrl,
    String type,
    String siteKey,
    Instant detectedAt,
    String evidence
) {
    /**
     * Generate a placeholder string for this detected CAPTCHA.
     * The placeholder can be used in requests to mark where the CAPTCHA
     * solution should be substituted.
     *
     * @return placeholder string in the format {{CAPTCHA:type:siteKey:pageUrl}}
     */
    public String toPlaceholder() {
        return "{{CAPTCHA[:]" + type + "[:]" + siteKey + "[:]" + pageUrl + "}}";
    }
}
