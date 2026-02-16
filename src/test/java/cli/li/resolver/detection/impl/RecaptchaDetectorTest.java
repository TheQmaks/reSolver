package cli.li.resolver.detection.impl;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import cli.li.resolver.detection.DetectedCaptcha;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RecaptchaDetector")
class RecaptchaDetectorTest {

    private RecaptchaDetector detector;

    @BeforeEach
    void setUp() {
        detector = new RecaptchaDetector();
    }

    // --- v2 widget detection ---

    @Test
    @DisplayName("detects reCAPTCHA v2 via widget class with data-sitekey attribute")
    void detectsV2ViaWidgetClass() {
        String html = "<div class=\"g-recaptcha\" data-sitekey=\"6LeIxAcTAAAAAN...\"></div>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("recaptchav2");
        assertThat(results.get(0).siteKey()).isEqualTo("6LeIxAcTAAAAAN...");
    }

    @Test
    @DisplayName("detects reCAPTCHA v2 when data-sitekey appears before class attribute")
    void detectsV2WithReversedAttributes() {
        String html = "<div data-sitekey=\"REVERSED_KEY\" class=\"g-recaptcha\" data-callback=\"onSubmit\"></div>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("recaptchav2");
        assertThat(results.get(0).siteKey()).isEqualTo("REVERSED_KEY");
    }

    @Test
    @DisplayName("detects reCAPTCHA v2 via grecaptcha.render() call")
    void detectsV2ViaRenderCall() {
        String html = "<script>grecaptcha.render(container, {sitekey: 'RENDER_SITE_KEY_V2'});</script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("recaptchav2");
        assertThat(results.get(0).siteKey()).isEqualTo("RENDER_SITE_KEY_V2");
    }

    // --- v3 script URL detection ---

    @Test
    @DisplayName("detects reCAPTCHA v3 via render parameter in script URL")
    void detectsV3ViaRenderParam() {
        String html = "<script src=\"https://www.google.com/recaptcha/api.js?render=6LdV3cYqAAAAABCDEFGHIJKL\"></script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("recaptchav3");
        assertThat(results.get(0).siteKey()).isEqualTo("6LdV3cYqAAAAABCDEFGHIJKL");
    }

    @Test
    @DisplayName("detects reCAPTCHA v3 via enterprise.js with render parameter")
    void detectsV3ViaEnterpriseJs() {
        String html = "<script src=\"https://www.google.com/recaptcha/enterprise.js?render=6LdEnterprise_KEY_HERE\"></script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("recaptchav3");
        assertThat(results.get(0).siteKey()).isEqualTo("6LdEnterprise_KEY_HERE");
    }

    @Test
    @DisplayName("detects reCAPTCHA v3 via recaptcha.net domain")
    void detectsV3ViaRecaptchaNetDomain() {
        String html = "<script src=\"https://www.recaptcha.net/recaptcha/api.js?render=6LdRecaptchaNet_KEY_X\"></script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("recaptchav3");
        assertThat(results.get(0).siteKey()).isEqualTo("6LdRecaptchaNet_KEY_X");
    }

    @Test
    @DisplayName("does not detect v3 when render=explicit (that indicates v2)")
    void doesNotDetectV3WhenRenderExplicit() {
        String html = "<script src=\"https://www.google.com/recaptcha/api.js?render=explicit\"></script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).isEmpty();
    }

    // --- v3 execute detection ---

    @Test
    @DisplayName("detects reCAPTCHA v3 via grecaptcha.execute() call")
    void detectsV3ViaExecuteCall() {
        String html = "<script>grecaptcha.execute('EXECUTE_SITE_KEY_V3', {action: 'homepage'});</script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("recaptchav3");
        assertThat(results.get(0).siteKey()).isEqualTo("EXECUTE_SITE_KEY_V3");
    }

    @Test
    @DisplayName("detects reCAPTCHA v3 via grecaptcha.enterprise.execute() call")
    void detectsV3ViaEnterpriseExecuteCall() {
        String html = "<script>grecaptcha.enterprise.execute('ENTERPRISE_KEY_V3', {action: 'login'});</script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("recaptchav3");
        assertThat(results.get(0).siteKey()).isEqualTo("ENTERPRISE_KEY_V3");
    }

    // --- Edge cases ---

    @Test
    @DisplayName("returns empty list when HTML contains no reCAPTCHA")
    void returnsEmptyForCleanHtml() {
        String html = "<html><body><p>No captcha here</p></body></html>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("deduplicates same sitekey found by multiple patterns")
    void deduplicatesSameSitekey() {
        String html = "<div class=\"g-recaptcha\" data-sitekey=\"DUPLICATE_KEY\"></div>"
                + "<script>grecaptcha.render(el, {sitekey: 'DUPLICATE_KEY'});</script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).siteKey()).isEqualTo("DUPLICATE_KEY");
    }

    @Test
    @DisplayName("detects multiple distinct sitekeys in the same page")
    void detectsMultipleDistinctSitekeys() {
        String html = "<div class=\"g-recaptcha\" data-sitekey=\"KEY_ONE\"></div>"
                + "<div class=\"g-recaptcha\" data-sitekey=\"KEY_TWO\"></div>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(2);
        assertThat(results).extracting(DetectedCaptcha::siteKey)
                .containsExactlyInAnyOrder("KEY_ONE", "KEY_TWO");
    }

    @Test
    @DisplayName("populates evidence field with snippet around match")
    void populatesEvidenceField() {
        String html = "<div class=\"g-recaptcha\" data-sitekey=\"EVIDENCE_KEY\"></div>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).evidence()).isNotNull();
        assertThat(results.get(0).evidence()).isNotEmpty();
    }
}
