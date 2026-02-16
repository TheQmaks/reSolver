package cli.li.resolver.detection.impl;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import cli.li.resolver.detection.DetectedCaptcha;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LeminDetector")
class LeminDetectorTest {

    private LeminDetector detector;

    @BeforeEach
    void setUp() {
        detector = new LeminDetector();
    }

    // --- Script URL with captcha ID ---

    @Test
    @DisplayName("detects Lemin via script URL with embedded captcha ID")
    void detectsViaScriptUrlWithId() {
        String html = "<script src=\"https://api.leminnow.com/captcha/v1/cropped/"
                + "CROPPED_3dfdd5c_d1872b526b794d83ba3b365eb15a200b/js\"></script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("lemin");
        assertThat(results.get(0).siteKey()).isEqualTo("CROPPED_3dfdd5c_d1872b526b794d83ba3b365eb15a200b");
        assertThat(results.get(0).pageUrl()).isEqualTo("https://example.com");
    }

    // --- Widget element detection ---

    @Test
    @DisplayName("detects Lemin via lemin-cropped-captcha element ID")
    void detectsViaWidgetId() {
        String html = "<div id=\"lemin-cropped-captcha\"></div>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("lemin");
        assertThat(results.get(0).siteKey()).isEqualTo("unknown");
    }

    // --- General script URL fallback ---

    @Test
    @DisplayName("detects Lemin via api.leminnow.com domain without captcha ID")
    void detectsViaGeneralScriptUrl() {
        String html = "<script src=\"https://api.leminnow.com/captcha/sdk.js\"></script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("lemin");
        assertThat(results.get(0).siteKey()).isEqualTo("unknown");
    }

    // --- Priority ---

    @Test
    @DisplayName("prefers captcha ID from script URL over fallback")
    void prefersCaptchaIdOverFallback() {
        String html = "<script src=\"https://api.leminnow.com/captcha/v1/cropped/MY_CAPTCHA_ID/js\"></script>"
                + "<div id=\"lemin-cropped-captcha\"></div>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).siteKey()).isEqualTo("MY_CAPTCHA_ID");
    }

    // --- Edge cases ---

    @Test
    @DisplayName("returns empty list when HTML contains no Lemin")
    void returnsEmptyForCleanHtml() {
        String html = "<html><body><p>No captcha here</p></body></html>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("populates evidence field with snippet around match")
    void populatesEvidenceField() {
        String html = "<div id=\"lemin-cropped-captcha\"></div>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).evidence()).isNotNull();
        assertThat(results.get(0).evidence()).isNotEmpty();
    }
}
