package cli.li.resolver.detection.impl;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import cli.li.resolver.detection.DetectedCaptcha;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FunCaptchaDetector")
class FunCaptchaDetectorTest {

    private FunCaptchaDetector detector;

    @BeforeEach
    void setUp() {
        detector = new FunCaptchaDetector();
    }

    // --- data-pkey detection ---

    @Test
    @DisplayName("detects FunCaptcha via data-pkey attribute")
    void detectsViaPkeyAttribute() {
        String html = "<div id=\"captcha\" data-pkey=\"PUBLIC_KEY_12345\"></div>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("funcaptcha");
        assertThat(results.get(0).siteKey()).isEqualTo("PUBLIC_KEY_12345");
        assertThat(results.get(0).pageUrl()).isEqualTo("https://example.com");
    }

    // --- ArkoseEnforcement JS detection ---

    @Test
    @DisplayName("detects FunCaptcha via ArkoseEnforcement JS object")
    void detectsViaArkoseEnforcement() {
        String html = "<script>new ArkoseEnforcement({public_key: 'ARKOSE_PK_123'});</script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("funcaptcha");
        assertThat(results.get(0).siteKey()).isEqualTo("ARKOSE_PK_123");
    }

    // --- Script URL detection ---

    @Test
    @DisplayName("detects FunCaptcha with unknown sitekey via arkoselabs.com script")
    void detectsScriptOnlyViaArkoselabs() {
        String html = "<script src=\"https://client-api.arkoselabs.com/v2/ABCDEF1234/api.js\"></script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("funcaptcha");
        assertThat(results.get(0).siteKey()).isEqualTo("unknown");
    }

    @Test
    @DisplayName("detects FunCaptcha with unknown sitekey via funcaptcha.com script")
    void detectsScriptOnlyViaFuncaptchaDomain() {
        String html = "<script src=\"https://client-api.funcaptcha.com/fc/api/something\"></script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("funcaptcha");
        assertThat(results.get(0).siteKey()).isEqualTo("unknown");
    }

    @Test
    @DisplayName("prefers data-pkey over script-only detection")
    void prefersPkeyOverScriptOnly() {
        String html = "<script src=\"https://client-api.arkoselabs.com/v2/ABCDEF/api.js\"></script>"
                + "<div data-pkey=\"MY_PUBLIC_KEY\"></div>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).siteKey()).isEqualTo("MY_PUBLIC_KEY");
    }

    // --- Edge cases ---

    @Test
    @DisplayName("returns empty list when HTML contains no FunCaptcha")
    void returnsEmptyForCleanHtml() {
        String html = "<html><body><div>Hello world</div></body></html>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("deduplicates same public key found by multiple patterns")
    void deduplicatesSameKey() {
        String html = "<div data-pkey=\"SAME_KEY\"></div>"
                + "<script>new ArkoseEnforcement({public_key: 'SAME_KEY'});</script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).siteKey()).isEqualTo("SAME_KEY");
    }

    @Test
    @DisplayName("detects multiple distinct public keys in the same page")
    void detectsMultipleDistinctKeys() {
        String html = "<div data-pkey=\"KEY_ONE\"></div>"
                + "<div data-pkey=\"KEY_TWO\"></div>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(2);
        assertThat(results).extracting(DetectedCaptcha::siteKey)
                .containsExactlyInAnyOrder("KEY_ONE", "KEY_TWO");
    }

    @Test
    @DisplayName("populates evidence field with snippet around match")
    void populatesEvidenceField() {
        String html = "<div data-pkey=\"EVIDENCE_KEY\"></div>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).evidence()).isNotNull();
        assertThat(results.get(0).evidence()).isNotEmpty();
    }
}
