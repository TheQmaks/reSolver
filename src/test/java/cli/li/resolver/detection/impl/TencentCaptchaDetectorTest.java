package cli.li.resolver.detection.impl;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import cli.li.resolver.detection.DetectedCaptcha;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TencentCaptchaDetector")
class TencentCaptchaDetectorTest {

    private TencentCaptchaDetector detector;

    @BeforeEach
    void setUp() {
        detector = new TencentCaptchaDetector();
    }

    @Test
    @DisplayName("detects Tencent Captcha via new TencentCaptcha() with app ID")
    void detectsViaInitCall() {
        String html = "<script>var captcha = new TencentCaptcha('2090816888', function(res){});</script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("tencent");
        assertThat(results.get(0).siteKey()).isEqualTo("2090816888");
    }

    @Test
    @DisplayName("detects Tencent Captcha with unknown app ID via TCaptcha.js script")
    void detectsViaTCaptchaScript() {
        String html = "<script src=\"https://ssl.captcha.qq.com/TCaptcha.js\"></script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("tencent");
        assertThat(results.get(0).siteKey()).isEqualTo("unknown");
    }

    @Test
    @DisplayName("detects Tencent Captcha via captcha.gtimg.com domain")
    void detectsViaGtimgDomain() {
        String html = "<script src=\"https://captcha.gtimg.com/1/TDC.js\"></script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("tencent");
        assertThat(results.get(0).siteKey()).isEqualTo("unknown");
    }

    @Test
    @DisplayName("prefers app ID from TencentCaptcha constructor over script fallback")
    void prefersAppIdOverScript() {
        String html = "<script src=\"https://ssl.captcha.qq.com/TCaptcha.js\"></script>"
                + "<script>new TencentCaptcha('12345', cb);</script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).siteKey()).isEqualTo("12345");
    }

    @Test
    @DisplayName("returns empty list when HTML contains no Tencent Captcha")
    void returnsEmptyForCleanHtml() {
        String html = "<html><body><p>No captcha here</p></body></html>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("populates evidence field")
    void populatesEvidenceField() {
        String html = "<script>new TencentCaptcha('999', cb);</script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).evidence()).isNotNull();
        assertThat(results.get(0).evidence()).isNotEmpty();
    }
}
