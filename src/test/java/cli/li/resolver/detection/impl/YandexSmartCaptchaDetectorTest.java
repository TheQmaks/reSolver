package cli.li.resolver.detection.impl;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import cli.li.resolver.detection.DetectedCaptcha;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("YandexSmartCaptchaDetector")
class YandexSmartCaptchaDetectorTest {

    private YandexSmartCaptchaDetector detector;

    @BeforeEach
    void setUp() {
        detector = new YandexSmartCaptchaDetector();
    }

    @Test
    @DisplayName("detects Yandex SmartCaptcha via smart-captcha widget with data-sitekey")
    void detectsViaWidgetClass() {
        String html = "<div class=\"smart-captcha\" data-sitekey=\"ysc1_ABCDEFGHIJKLMNOP\"></div>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("yandex");
        assertThat(results.get(0).siteKey()).isEqualTo("ysc1_ABCDEFGHIJKLMNOP");
    }

    @Test
    @DisplayName("detects Yandex SmartCaptcha with reversed attribute order")
    void detectsWithReversedAttributes() {
        String html = "<div data-sitekey=\"REVERSED_YSC_KEY\" class=\"smart-captcha\"></div>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).siteKey()).isEqualTo("REVERSED_YSC_KEY");
    }

    @Test
    @DisplayName("detects Yandex SmartCaptcha via smartCaptcha.render() JS call")
    void detectsViaRenderCall() {
        String html = "<script>window.smartCaptcha.render(container, {sitekey: 'RENDER_YSC_KEY'});</script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).siteKey()).isEqualTo("RENDER_YSC_KEY");
    }

    @Test
    @DisplayName("detects Yandex SmartCaptcha via yandexcloud.net script URL")
    void detectsViaYandexCloudScript() {
        String html = "<script src=\"https://smartcaptcha.yandexcloud.net/captcha.js\"></script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("yandex");
        assertThat(results.get(0).siteKey()).isEqualTo("unknown");
    }

    @Test
    @DisplayName("detects Yandex SmartCaptcha via older cloud.yandex.ru script URL")
    void detectsViaOlderYandexScript() {
        String html = "<script src=\"https://smartcaptcha.cloud.yandex.ru/captcha.js\"></script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).siteKey()).isEqualTo("unknown");
    }

    @Test
    @DisplayName("returns empty list when HTML contains no Yandex SmartCaptcha")
    void returnsEmptyForCleanHtml() {
        String html = "<html><body><p>No captcha here</p></body></html>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("populates evidence field")
    void populatesEvidenceField() {
        String html = "<div class=\"smart-captcha\" data-sitekey=\"EV_KEY\"></div>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).evidence()).isNotNull();
        assertThat(results.get(0).evidence()).isNotEmpty();
    }
}
