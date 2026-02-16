package cli.li.resolver.detection.impl;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import cli.li.resolver.detection.DetectedCaptcha;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProcaptchaDetector")
class ProcaptchaDetectorTest {

    private ProcaptchaDetector detector;

    @BeforeEach
    void setUp() {
        detector = new ProcaptchaDetector();
    }

    @Test
    @DisplayName("detects Procaptcha via widget class with data-sitekey")
    void detectsViaWidgetClass() {
        String html = "<div class=\"procaptcha\" data-sitekey=\"prosopo_site_key_123\"></div>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("procaptcha");
        assertThat(results.get(0).siteKey()).isEqualTo("prosopo_site_key_123");
    }

    @Test
    @DisplayName("detects Procaptcha with reversed attribute order")
    void detectsWithReversedAttributes() {
        String html = "<div data-sitekey=\"REVERSED_PRO_KEY\" class=\"procaptcha\"></div>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).siteKey()).isEqualTo("REVERSED_PRO_KEY");
    }

    @Test
    @DisplayName("detects Procaptcha via procaptcha.render() JS call")
    void detectsViaRenderCall() {
        String html = "<script>window.procaptcha.render(el, {siteKey: 'RENDER_PRO_KEY'});</script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).siteKey()).isEqualTo("RENDER_PRO_KEY");
    }

    @Test
    @DisplayName("detects Procaptcha with unknown sitekey via prosopo.io script URL")
    void detectsViaProsopoScript() {
        String html = "<script src=\"https://js.prosopo.io/js/procaptcha.bundle.js\"></script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("procaptcha");
        assertThat(results.get(0).siteKey()).isEqualTo("unknown");
    }

    @Test
    @DisplayName("returns empty list when HTML contains no Procaptcha")
    void returnsEmptyForCleanHtml() {
        String html = "<html><body><p>No captcha here</p></body></html>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("populates evidence field")
    void populatesEvidenceField() {
        String html = "<div class=\"procaptcha\" data-sitekey=\"EV_KEY\"></div>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).evidence()).isNotNull();
        assertThat(results.get(0).evidence()).isNotEmpty();
    }
}
