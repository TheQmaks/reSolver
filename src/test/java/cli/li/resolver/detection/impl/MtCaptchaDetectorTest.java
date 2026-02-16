package cli.li.resolver.detection.impl;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import cli.li.resolver.detection.DetectedCaptcha;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MtCaptchaDetector")
class MtCaptchaDetectorTest {

    private MtCaptchaDetector detector;

    @BeforeEach
    void setUp() {
        detector = new MtCaptchaDetector();
    }

    // --- Config sitekey detection ---

    @Test
    @DisplayName("detects MTCaptcha via mtcaptchaConfig sitekey")
    void detectsViaConfigSitekey() {
        String html = "<script>var mtcaptchaConfig = {sitekey: 'MTPublic-KzqLY1cKH'};</script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("mtcaptcha");
        assertThat(results.get(0).siteKey()).isEqualTo("MTPublic-KzqLY1cKH");
        assertThat(results.get(0).pageUrl()).isEqualTo("https://example.com");
    }

    @Test
    @DisplayName("detects MTCaptcha via mtcaptchaConfig with single quotes")
    void detectsViaConfigSingleQuotes() {
        String html = "<script>var mtcaptchaConfig = {\n  sitekey: 'MTPublic-ABC123'\n};</script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).siteKey()).isEqualTo("MTPublic-ABC123");
    }

    // --- Script URL detection ---

    @Test
    @DisplayName("detects MTCaptcha with unknown sitekey via script URL")
    void detectsViaScriptUrl() {
        String html = "<script src=\"https://service.mtcaptcha.com/mtcv1/client/mtcaptcha.min.js\"></script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("mtcaptcha");
        assertThat(results.get(0).siteKey()).isEqualTo("unknown");
    }

    @Test
    @DisplayName("detects MTCaptcha via mtcaptcha2.min.js script URL")
    void detectsViaMtcaptcha2Script() {
        String html = "<script src=\"https://service.mtcaptcha.com/mtcv1/client/mtcaptcha2.min.js\"></script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("mtcaptcha");
        assertThat(results.get(0).siteKey()).isEqualTo("unknown");
    }

    // --- Widget class/id detection ---

    @Test
    @DisplayName("detects MTCaptcha via widget class")
    void detectsViaWidgetClass() {
        String html = "<div class=\"mtcaptcha\"></div>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("mtcaptcha");
        assertThat(results.get(0).siteKey()).isEqualTo("unknown");
    }

    // --- Priority ---

    @Test
    @DisplayName("prefers config sitekey over script-only detection")
    void prefersConfigOverScript() {
        String html = "<script src=\"https://service.mtcaptcha.com/mtcv1/client/mtcaptcha.min.js\"></script>"
                + "<script>var mtcaptchaConfig = {sitekey: 'MTPublic-FOUND'};</script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).siteKey()).isEqualTo("MTPublic-FOUND");
    }

    // --- Edge cases ---

    @Test
    @DisplayName("returns empty list when HTML contains no MTCaptcha")
    void returnsEmptyForCleanHtml() {
        String html = "<html><body><p>No captcha here</p></body></html>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("populates evidence field with snippet around match")
    void populatesEvidenceField() {
        String html = "<script>var mtcaptchaConfig = {sitekey: 'MTPublic-EV'};</script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).evidence()).isNotNull();
        assertThat(results.get(0).evidence()).isNotEmpty();
    }
}
