package cli.li.resolver.detection.impl;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import cli.li.resolver.detection.DetectedCaptcha;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CaptchaFoxDetector")
class CaptchaFoxDetectorTest {

    private CaptchaFoxDetector detector;

    @BeforeEach
    void setUp() {
        detector = new CaptchaFoxDetector();
    }

    @Test
    @DisplayName("detects CaptchaFox via widget class with data-sitekey")
    void detectsViaWidgetClass() {
        String html = "<div class=\"captchafox\" data-sitekey=\"sk_CF_abc123\"></div>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("captchafox");
        assertThat(results.get(0).siteKey()).isEqualTo("sk_CF_abc123");
    }

    @Test
    @DisplayName("detects CaptchaFox with reversed attribute order")
    void detectsWithReversedAttributes() {
        String html = "<div data-sitekey=\"REVERSED_CF_KEY\" class=\"captchafox\"></div>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).siteKey()).isEqualTo("REVERSED_CF_KEY");
    }

    @Test
    @DisplayName("detects CaptchaFox via captchafox.render() JS call")
    void detectsViaRenderCall() {
        String html = "<script>captchafox.render(el, {sitekey: 'RENDER_CF_KEY'});</script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).siteKey()).isEqualTo("RENDER_CF_KEY");
    }

    @Test
    @DisplayName("detects CaptchaFox with unknown sitekey via CDN script URL")
    void detectsViaCdnScript() {
        String html = "<script src=\"https://cdn.captchafox.com/api.js\" async defer></script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("captchafox");
        assertThat(results.get(0).siteKey()).isEqualTo("unknown");
    }

    @Test
    @DisplayName("returns empty list when HTML contains no CaptchaFox")
    void returnsEmptyForCleanHtml() {
        String html = "<html><body><p>No captcha here</p></body></html>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("populates evidence field")
    void populatesEvidenceField() {
        String html = "<div class=\"captchafox\" data-sitekey=\"EV_KEY\"></div>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).evidence()).isNotNull();
        assertThat(results.get(0).evidence()).isNotEmpty();
    }
}
