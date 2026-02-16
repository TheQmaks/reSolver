package cli.li.resolver.detection.impl;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import cli.li.resolver.detection.DetectedCaptcha;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AwsWafDetector")
class AwsWafDetectorTest {

    private AwsWafDetector detector;

    @BeforeEach
    void setUp() {
        detector = new AwsWafDetector();
    }

    // --- Strong signal detection ---

    @Test
    @DisplayName("detects AWS WAF via captcha-sdk.awswaf.com domain")
    void detectsViaCaptchaSdkDomain() {
        String html = "<script src=\"https://captcha-sdk.awswaf.com/captcha.js\"></script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("awswaf");
        assertThat(results.get(0).siteKey()).isEqualTo("unknown");
        assertThat(results.get(0).pageUrl()).isEqualTo("https://example.com");
    }

    @Test
    @DisplayName("detects AWS WAF via token.awswaf.com domain")
    void detectsViaTokenDomain() {
        String html = "<script src=\"https://token.awswaf.com/token-verify.js\"></script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("awswaf");
        assertThat(results.get(0).siteKey()).isEqualTo("unknown");
    }

    @Test
    @DisplayName("detects AWS WAF via AwsWafCaptcha.renderCaptcha JS call")
    void detectsViaRenderCaptcha() {
        String html = "<script>AwsWafCaptcha.renderCaptcha(container, {apiKey: 'xxx'});</script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("awswaf");
    }

    @Test
    @DisplayName("detects AWS WAF via AwsWafIntegration JS object")
    void detectsViaAwsWafIntegration() {
        String html = "<script>AwsWafIntegration.getToken();</script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("awswaf");
    }

    @Test
    @DisplayName("detects AWS WAF via aws-waf-token reference")
    void detectsViaWafToken() {
        String html = "<input type=\"hidden\" name=\"aws-waf-token\" value=\"abc123\" />";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("awswaf");
        assertThat(results.get(0).siteKey()).isEqualTo("unknown");
    }

    // --- Priority and deduplication ---

    @Test
    @DisplayName("returns only one detection even when multiple indicators are present")
    void returnsOneDetectionForMultipleIndicators() {
        String html = "<script src=\"https://captcha-sdk.awswaf.com/captcha.js\"></script>"
                + "<input name=\"aws-waf-token\" />"
                + "<script>AwsWafCaptcha.renderCaptcha(el, {});</script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("awswaf");
    }

    // --- Edge cases ---

    @Test
    @DisplayName("returns empty list when HTML contains no AWS WAF indicators")
    void returnsEmptyForCleanHtml() {
        String html = "<html><body><p>Clean page with no WAF</p></body></html>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("does not false-positive on generic challenge.js or awswaf.js strings")
    void doesNotFalsePositiveOnGenericScripts() {
        String html = "<script src=\"/challenge.js\"></script><script src=\"/awswaf.js\"></script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        // These generic filenames are no longer matched — only specific AWS WAF domains and APIs
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("populates evidence field with snippet around match")
    void populatesEvidenceField() {
        String html = "<script src=\"https://captcha-sdk.awswaf.com/captcha.js\"></script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).evidence()).isNotNull();
        assertThat(results.get(0).evidence()).isNotEmpty();
    }
}
