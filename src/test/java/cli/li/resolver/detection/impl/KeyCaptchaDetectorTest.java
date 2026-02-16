package cli.li.resolver.detection.impl;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import cli.li.resolver.detection.DetectedCaptcha;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("KeyCaptchaDetector")
class KeyCaptchaDetectorTest {

    private KeyCaptchaDetector detector;

    @BeforeEach
    void setUp() {
        detector = new KeyCaptchaDetector();
    }

    // --- JS variable detection ---

    @Test
    @DisplayName("detects KeyCaptcha via s_s_c_user_id JS variable")
    void detectsViaUserIdVariable() {
        String html = "<script>var s_s_c_user_id = 184015;</script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("keycaptcha");
        assertThat(results.get(0).siteKey()).isEqualTo("184015");
        assertThat(results.get(0).pageUrl()).isEqualTo("https://example.com");
    }

    @Test
    @DisplayName("detects KeyCaptcha via s_s_c_user_id with string value")
    void detectsViaUserIdString() {
        String html = "<script>var s_s_c_user_id = '99999';</script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).siteKey()).isEqualTo("99999");
    }

    // --- Script URL detection ---

    @Test
    @DisplayName("detects KeyCaptcha via keycaptcha.com script URL")
    void detectsViaScriptUrl() {
        String html = "<script src=\"https://backs.keycaptcha.com/swfs/cap.js\"></script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("keycaptcha");
        assertThat(results.get(0).siteKey()).isEqualTo("unknown");
    }

    // --- Widget element detection ---

    @Test
    @DisplayName("detects KeyCaptcha via div_for_keycaptcha element ID")
    void detectsViaWidgetId() {
        String html = "<div id=\"div_for_keycaptcha\"></div>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("keycaptcha");
        assertThat(results.get(0).siteKey()).isEqualTo("unknown");
    }

    // --- Priority ---

    @Test
    @DisplayName("prefers s_s_c_user_id over script-only detection")
    void prefersUserIdOverScript() {
        String html = "<script src=\"https://backs.keycaptcha.com/swfs/cap.js\"></script>"
                + "<script>var s_s_c_user_id = 12345;</script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).siteKey()).isEqualTo("12345");
    }

    // --- Edge cases ---

    @Test
    @DisplayName("returns empty list when HTML contains no KeyCaptcha")
    void returnsEmptyForCleanHtml() {
        String html = "<html><body><p>No captcha here</p></body></html>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("populates evidence field with snippet around match")
    void populatesEvidenceField() {
        String html = "<script>var s_s_c_user_id = 184015;</script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).evidence()).isNotNull();
        assertThat(results.get(0).evidence()).isNotEmpty();
    }
}
