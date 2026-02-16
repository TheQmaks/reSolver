package cli.li.resolver.detection.impl;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import cli.li.resolver.detection.DetectedCaptcha;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TurnstileDetector")
class TurnstileDetectorTest {

    private TurnstileDetector detector;

    @BeforeEach
    void setUp() {
        detector = new TurnstileDetector();
    }

    // --- Widget detection ---

    @Test
    @DisplayName("detects Turnstile via widget class with data-sitekey attribute")
    void detectsViaWidgetClass() {
        String html = "<div class=\"cf-turnstile\" data-sitekey=\"0x4AAAAAAAB...\"></div>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("turnstile");
        assertThat(results.get(0).siteKey()).isEqualTo("0x4AAAAAAAB...");
        assertThat(results.get(0).pageUrl()).isEqualTo("https://example.com");
    }

    @Test
    @DisplayName("detects Turnstile when data-sitekey appears before class attribute")
    void detectsWithReversedAttributes() {
        String html = "<div data-sitekey=\"REVERSED_KEY\" class=\"cf-turnstile\" data-callback=\"onDone\"></div>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("turnstile");
        assertThat(results.get(0).siteKey()).isEqualTo("REVERSED_KEY");
    }

    // --- JS render detection ---

    @Test
    @DisplayName("detects Turnstile via turnstile.render() JS call")
    void detectsViaRenderCall() {
        String html = "<script>turnstile.render('#widget', {sitekey: 'RENDER_KEY'});</script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("turnstile");
        assertThat(results.get(0).siteKey()).isEqualTo("RENDER_KEY");
    }

    // --- Script-only fallback ---

    @Test
    @DisplayName("detects Turnstile with unknown sitekey when only script tag is present")
    void detectsScriptOnlyWithUnknownSitekey() {
        String html = "<script src=\"https://challenges.cloudflare.com/turnstile/v0/api.js\"></script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo("turnstile");
        assertThat(results.get(0).siteKey()).isEqualTo("unknown");
    }

    @Test
    @DisplayName("prefers widget sitekey over script-only detection")
    void prefersWidgetOverScriptOnly() {
        String html = "<script src=\"https://challenges.cloudflare.com/turnstile/v0/api.js\"></script>"
                + "<div class=\"cf-turnstile\" data-sitekey=\"WIDGET_KEY\"></div>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).siteKey()).isEqualTo("WIDGET_KEY");
    }

    // --- Edge cases ---

    @Test
    @DisplayName("returns empty list when HTML contains no Turnstile")
    void returnsEmptyForCleanHtml() {
        String html = "<html><body><p>Just a regular page</p></body></html>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("deduplicates same sitekey found by multiple patterns")
    void deduplicatesSameSitekey() {
        String html = "<div class=\"cf-turnstile\" data-sitekey=\"DUP_KEY\"></div>"
                + "<script>turnstile.render(el, {sitekey: 'DUP_KEY'});</script>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).siteKey()).isEqualTo("DUP_KEY");
    }

    @Test
    @DisplayName("detects multiple distinct sitekeys in the same page")
    void detectsMultipleDistinctSitekeys() {
        String html = "<div class=\"cf-turnstile\" data-sitekey=\"KEY_ONE\"></div>"
                + "<div class=\"cf-turnstile\" data-sitekey=\"KEY_TWO\"></div>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(2);
        assertThat(results).extracting(DetectedCaptcha::siteKey)
                .containsExactlyInAnyOrder("KEY_ONE", "KEY_TWO");
    }

    @Test
    @DisplayName("populates evidence field with snippet around match")
    void populatesEvidenceField() {
        String html = "<div class=\"cf-turnstile\" data-sitekey=\"EVIDENCE_KEY\"></div>";

        List<DetectedCaptcha> results = detector.detect("https://example.com", html);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).evidence()).isNotNull();
        assertThat(results.get(0).evidence()).isNotEmpty();
    }
}
