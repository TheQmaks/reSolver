package cli.li.resolver.detection;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DetectedCaptcha")
class DetectedCaptchaTest {

    @Test
    @DisplayName("toPlaceholder() returns correctly formatted placeholder string")
    void toPlaceholderFormat() {
        Instant now = Instant.now();
        DetectedCaptcha captcha = new DetectedCaptcha(
                "https://example.com/login", "recaptchav2", "6LeIxAcTAAAA", now, "evidence");

        String placeholder = captcha.toPlaceholder();

        assertThat(placeholder).isEqualTo(
                "{{CAPTCHA[:]recaptchav2[:]6LeIxAcTAAAA[:]https://example.com/login}}");
    }

    @Test
    @DisplayName("pageUrl() accessor returns the page URL")
    void pageUrlAccessor() {
        DetectedCaptcha captcha = new DetectedCaptcha(
                "https://example.com", "hcaptcha", "KEY", Instant.now(), "evidence");

        assertThat(captcha.pageUrl()).isEqualTo("https://example.com");
    }

    @Test
    @DisplayName("type() accessor returns the CAPTCHA type")
    void typeAccessor() {
        DetectedCaptcha captcha = new DetectedCaptcha(
                "https://example.com", "turnstile", "KEY", Instant.now(), "evidence");

        assertThat(captcha.type()).isEqualTo("turnstile");
    }

    @Test
    @DisplayName("siteKey() accessor returns the site key")
    void siteKeyAccessor() {
        DetectedCaptcha captcha = new DetectedCaptcha(
                "https://example.com", "recaptchav3", "MY_SITE_KEY", Instant.now(), "evidence");

        assertThat(captcha.siteKey()).isEqualTo("MY_SITE_KEY");
    }

    @Test
    @DisplayName("detectedAt() accessor returns the detection timestamp")
    void detectedAtAccessor() {
        Instant timestamp = Instant.parse("2024-01-15T10:30:00Z");
        DetectedCaptcha captcha = new DetectedCaptcha(
                "https://example.com", "funcaptcha", "KEY", timestamp, "evidence");

        assertThat(captcha.detectedAt()).isEqualTo(timestamp);
    }

    @Test
    @DisplayName("evidence() accessor returns the evidence snippet")
    void evidenceAccessor() {
        DetectedCaptcha captcha = new DetectedCaptcha(
                "https://example.com", "awswaf", "unknown", Instant.now(), "snippet of html");

        assertThat(captcha.evidence()).isEqualTo("snippet of html");
    }

    @Test
    @DisplayName("two records with same fields are equal")
    void equalityForSameFields() {
        Instant now = Instant.parse("2024-06-01T12:00:00Z");
        DetectedCaptcha a = new DetectedCaptcha(
                "https://example.com", "recaptchav2", "KEY", now, "evidence");
        DetectedCaptcha b = new DetectedCaptcha(
                "https://example.com", "recaptchav2", "KEY", now, "evidence");

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    @DisplayName("two records with different fields are not equal")
    void inequalityForDifferentFields() {
        Instant now = Instant.now();
        DetectedCaptcha a = new DetectedCaptcha(
                "https://example.com", "recaptchav2", "KEY_A", now, "evidence");
        DetectedCaptcha b = new DetectedCaptcha(
                "https://example.com", "recaptchav2", "KEY_B", now, "evidence");

        assertThat(a).isNotEqualTo(b);
    }
}
