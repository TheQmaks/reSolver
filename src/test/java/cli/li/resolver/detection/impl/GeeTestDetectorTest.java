package cli.li.resolver.detection.impl;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import cli.li.resolver.detection.DetectedCaptcha;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GeeTestDetector")
class GeeTestDetectorTest {

    private GeeTestDetector detector;

    @BeforeEach
    void setUp() {
        detector = new GeeTestDetector();
    }

    // --- GeeTest v3 ---

    @Test
    @DisplayName("detects GeeTest v3 via initGeetest() call with gt parameter")
    void detectsV3ViaInitGeetest() {
        String html = "<script>initGeetest({gt: 'GT_KEY_ABC123', challenge: 'xxx'});</script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("geetest");
        assertThat(results.get(0).siteKey()).isEqualTo("GT_KEY_ABC123");
    }

    @Test
    @DisplayName("detects GeeTest v3 script URL as fallback with unknown sitekey")
    void detectsV3ViaScriptUrl() {
        String html = "<script src=\"https://static.geetest.com/static/tools/gt.js\"></script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("geetest");
        assertThat(results.get(0).siteKey()).isEqualTo("unknown");
    }

    @Test
    @DisplayName("does not use v3 script fallback when initGeetest found a key")
    void v3ScriptFallbackNotUsedWhenKeyFound() {
        String html = "<script src=\"https://static.geetest.com/static/tools/gt.js\"></script>"
                + "<script>initGeetest({gt: 'FOUND_KEY'});</script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).siteKey()).isEqualTo("FOUND_KEY");
    }

    @Test
    @DisplayName("initGeetest does not match initGeetest4")
    void v3DoesNotMatchV4Init() {
        String html = "<script>initGeetest4({captchaId: 'a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4'});</script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        // Should detect as v4, not v3
        assertThat(results).allMatch(r -> !"geetest".equals(r.type()) || "unknown".equals(r.siteKey())
                || r.type().equals("geetestv4"));
    }

    // --- GeeTest v4 ---

    @Test
    @DisplayName("detects GeeTest v4 via initGeetest4() with captchaId parameter")
    void detectsV4ViaInitGeetest4() {
        String html = "<script>initGeetest4({captchaId: 'a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4', product: 'bind'});</script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).anyMatch(r -> "geetestv4".equals(r.type())
                && "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4".equals(r.siteKey()));
    }

    @Test
    @DisplayName("detects GeeTest v4 via captcha_id with 32-char hex value")
    void detectsV4ViaCaptchaId() {
        String html = "<script>captcha_id: 'a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4'</script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("geetestv4");
        assertThat(results.get(0).siteKey()).isEqualTo("a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4");
    }

    @Test
    @DisplayName("detects GeeTest v4 via gcaptcha4.geetest.com script URL")
    void detectsV4ViaGcaptcha4Script() {
        String html = "<script src=\"https://gcaptcha4.geetest.com/load?captcha_id=abc&callback=fn\"></script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).anyMatch(r -> "geetestv4".equals(r.type()));
    }

    @Test
    @DisplayName("detects GeeTest v4 via gt4.js script URL")
    void detectsV4ViaGt4Script() {
        String html = "<script src=\"https://static.geetest.com/v4/gt4.js\"></script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("geetestv4");
        assertThat(results.get(0).siteKey()).isEqualTo("unknown");
    }

    // --- Edge cases ---

    @Test
    @DisplayName("returns empty list when HTML contains no GeeTest")
    void returnsEmptyForCleanHtml() {
        String html = "<html><body><p>No captcha here</p></body></html>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("detects both v3 and v4 in the same page")
    void detectsBothV3AndV4() {
        String html = "<script>initGeetest({gt: 'V3_GT_KEY'});</script>"
                + "<script>initGeetest4({captchaId: 'aabbccddaabbccddaabbccddaabbccdd'});</script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(2);
        assertThat(results).extracting(DetectedCaptcha::type)
                .containsExactlyInAnyOrder("geetest", "geetestv4");
    }

    @Test
    @DisplayName("populates evidence field with snippet around match")
    void populatesEvidenceField() {
        String html = "<script>initGeetest({gt: 'EVIDENCE_GT_KEY'});</script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).evidence()).isNotNull();
        assertThat(results.get(0).evidence()).isNotEmpty();
    }
}
