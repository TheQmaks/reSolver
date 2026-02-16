package cli.li.resolver.detection.impl;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import cli.li.resolver.detection.DetectedCaptcha;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HCaptchaDetector")
class HCaptchaDetectorTest {

    private HCaptchaDetector detector;

    @BeforeEach
    void setUp() {
        detector = new HCaptchaDetector();
    }

    // --- Widget detection ---

    @Test
    @DisplayName("detects hCaptcha via widget class with data-sitekey attribute")
    void detectsViaWidgetClass() {
        String html = "<div class=\"h-captcha\" data-sitekey=\"10000000-ffff-ffff-ffff-000000000001\"></div>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("hcaptcha");
        assertThat(results.get(0).siteKey()).isEqualTo("10000000-ffff-ffff-ffff-000000000001");
        assertThat(results.get(0).pageUrl()).isEqualTo("https://example.com");
    }

    @Test
    @DisplayName("detects hCaptcha when data-sitekey appears before class attribute")
    void detectsWithReversedAttributes() {
        String html = "<div data-sitekey=\"REVERSED_KEY\" class=\"h-captcha\"></div>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("hcaptcha");
        assertThat(results.get(0).siteKey()).isEqualTo("REVERSED_KEY");
    }

    // --- JS render detection ---

    @Test
    @DisplayName("detects hCaptcha via hcaptcha.render() JS call")
    void detectsViaRenderCall() {
        String html = "<script>hcaptcha.render(container, {sitekey: 'RENDER_KEY'});</script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("hcaptcha");
        assertThat(results.get(0).siteKey()).isEqualTo("RENDER_KEY");
    }

    // --- Script-only fallback ---

    @Test
    @DisplayName("detects hCaptcha with unknown sitekey when only script tag is present")
    void detectsScriptOnlyWithUnknownSitekey() {
        String html = "<script src=\"https://hcaptcha.com/1/api.js\" async defer></script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("hcaptcha");
        assertThat(results.get(0).siteKey()).isEqualTo("unknown");
    }

    @Test
    @DisplayName("prefers widget sitekey over script-only detection")
    void prefersWidgetOverScriptOnly() {
        String html = "<script src=\"https://hcaptcha.com/1/api.js\"></script>"
                + "<div class=\"h-captcha\" data-sitekey=\"WIDGET_KEY\"></div>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).siteKey()).isEqualTo("WIDGET_KEY");
    }

    // --- Edge cases ---

    @Test
    @DisplayName("returns empty list when HTML contains no hCaptcha")
    void returnsEmptyForCleanHtml() {
        String html = "<html><body><form><input type=\"text\" /></form></body></html>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("deduplicates same sitekey found by multiple patterns")
    void deduplicatesSameSitekey() {
        String html = "<div class=\"h-captcha\" data-sitekey=\"DUP_KEY\"></div>"
                + "<script>hcaptcha.render(el, {sitekey: 'DUP_KEY'});</script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).siteKey()).isEqualTo("DUP_KEY");
    }

    @Test
    @DisplayName("detects multiple distinct sitekeys in the same page")
    void detectsMultipleDistinctSitekeys() {
        String html = "<div class=\"h-captcha\" data-sitekey=\"KEY_ONE\"></div>"
                + "<div class=\"h-captcha\" data-sitekey=\"KEY_TWO\"></div>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(2);
        assertThat(results).extracting(DetectedCaptcha::siteKey)
                .containsExactlyInAnyOrder("KEY_ONE", "KEY_TWO");
    }

    @Test
    @DisplayName("populates evidence field with snippet around match")
    void populatesEvidenceField() {
        String html = "<div class=\"h-captcha\" data-sitekey=\"EVIDENCE_KEY\"></div>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).evidence()).isNotNull();
        assertThat(results.get(0).evidence()).isNotEmpty();
    }
}
