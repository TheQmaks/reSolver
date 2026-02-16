package cli.li.resolver.detection.impl;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import cli.li.resolver.detection.DetectedCaptcha;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FriendlyCaptchaDetector")
class FriendlyCaptchaDetectorTest {

    private FriendlyCaptchaDetector detector;

    @BeforeEach
    void setUp() {
        detector = new FriendlyCaptchaDetector();
    }

    @Test
    @DisplayName("detects Friendly Captcha via frc-captcha widget with data-sitekey")
    void detectsViaWidgetClass() {
        String html = "<div class=\"frc-captcha\" data-sitekey=\"FCMN5BRVKH123456\"></div>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("friendlycaptcha");
        assertThat(results.get(0).siteKey()).isEqualTo("FCMN5BRVKH123456");
    }

    @Test
    @DisplayName("detects Friendly Captcha with reversed attribute order")
    void detectsWithReversedAttributes() {
        String html = "<div data-sitekey=\"REVERSED_FC_KEY\" class=\"frc-captcha\"></div>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).siteKey()).isEqualTo("REVERSED_FC_KEY");
    }

    @Test
    @DisplayName("detects Friendly Captcha via createWidget JS call")
    void detectsViaCreateWidget() {
        String html = "<script>sdk.createWidget({element: el, sitekey: 'SDK_KEY_123'});</script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).siteKey()).isEqualTo("SDK_KEY_123");
    }

    @Test
    @DisplayName("detects Friendly Captcha with unknown sitekey via script URL")
    void detectsViaScriptUrl() {
        String html = "<script src=\"https://cdn.jsdelivr.net/npm/friendly-challenge@0.9.12/widget.min.js\"></script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("friendlycaptcha");
        assertThat(results.get(0).siteKey()).isEqualTo("unknown");
    }

    @Test
    @DisplayName("detects Friendly Captcha v2 SDK script")
    void detectsViaV2SdkScript() {
        String html = "<script src=\"https://cdn.jsdelivr.net/npm/@friendlycaptcha/sdk@0.1.0/site.min.js\"></script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).siteKey()).isEqualTo("unknown");
    }

    @Test
    @DisplayName("returns empty list when HTML contains no Friendly Captcha")
    void returnsEmptyForCleanHtml() {
        String html = "<html><body><p>No captcha here</p></body></html>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("populates evidence field")
    void populatesEvidenceField() {
        String html = "<div class=\"frc-captcha\" data-sitekey=\"EV_KEY\"></div>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).evidence()).isNotNull();
        assertThat(results.get(0).evidence()).isNotEmpty();
    }
}
