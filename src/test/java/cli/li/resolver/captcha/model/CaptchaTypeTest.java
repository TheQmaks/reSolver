package cli.li.resolver.captcha.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CaptchaType")
class CaptchaTypeTest {

    @ParameterizedTest(name = "fromCode(\"{0}\") returns {1}")
    @CsvSource({
            "recaptchav2, RECAPTCHA_V2",
            "recaptchav3, RECAPTCHA_V3",
            "hcaptcha, HCAPTCHA",
            "turnstile, TURNSTILE",
            "funcaptcha, FUNCAPTCHA",
            "geetest, GEETEST",
            "geetestv4, GEETEST_V4",
            "awswaf, AWS_WAF"
    })
    @DisplayName("fromCode returns correct enum for each standard code")
    void fromCodeReturnsCorrectEnumForStandardCodes(String code, CaptchaType expected) {
        assertThat(CaptchaType.fromCode(code)).isEqualTo(expected);
    }

    @Test
    @DisplayName("fromCode resolves alias 'recaptcha2' to RECAPTCHA_V2")
    void fromCodeResolvesRecaptcha2Alias() {
        assertThat(CaptchaType.fromCode("recaptcha2")).isEqualTo(CaptchaType.RECAPTCHA_V2);
    }

    @Test
    @DisplayName("fromCode resolves alias 'recaptcha_v2' to RECAPTCHA_V2")
    void fromCodeResolvesRecaptchaUnderscoreV2Alias() {
        assertThat(CaptchaType.fromCode("recaptcha_v2")).isEqualTo(CaptchaType.RECAPTCHA_V2);
    }

    @Test
    @DisplayName("fromCode resolves alias 'recaptcha3' to RECAPTCHA_V3")
    void fromCodeResolvesRecaptcha3Alias() {
        assertThat(CaptchaType.fromCode("recaptcha3")).isEqualTo(CaptchaType.RECAPTCHA_V3);
    }

    @Test
    @DisplayName("fromCode resolves alias 'recaptcha_v3' to RECAPTCHA_V3")
    void fromCodeResolvesRecaptchaUnderscoreV3Alias() {
        assertThat(CaptchaType.fromCode("recaptcha_v3")).isEqualTo(CaptchaType.RECAPTCHA_V3);
    }

    @ParameterizedTest(name = "fromCode(\"{0}\") is case insensitive")
    @ValueSource(strings = {"RECAPTCHAV2", "Recaptchav2", "HCAPTCHA", "Turnstile", "AWSWAF"})
    @DisplayName("fromCode is case insensitive")
    void fromCodeIsCaseInsensitive(String code) {
        CaptchaType result = CaptchaType.fromCode(code);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("fromCode with unknown code throws IllegalArgumentException")
    void fromCodeWithUnknownCodeThrows() {
        assertThatThrownBy(() -> CaptchaType.fromCode("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown CAPTCHA type");
    }

    @Test
    @DisplayName("fromCode with null throws IllegalArgumentException")
    void fromCodeWithNullThrows() {
        assertThatThrownBy(() -> CaptchaType.fromCode(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
    }

    @Test
    @DisplayName("fromCode with empty string throws IllegalArgumentException")
    void fromCodeWithEmptyStringThrows() {
        assertThatThrownBy(() -> CaptchaType.fromCode(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
    }

    @Test
    @DisplayName("fromCodeOptional with valid code returns present Optional")
    void fromCodeOptionalWithValidCodeReturnsPresent() {
        assertThat(CaptchaType.fromCodeOptional("recaptchav2"))
                .isPresent()
                .contains(CaptchaType.RECAPTCHA_V2);
    }

    @Test
    @DisplayName("fromCodeOptional with unknown code returns empty Optional")
    void fromCodeOptionalWithUnknownCodeReturnsEmpty() {
        assertThat(CaptchaType.fromCodeOptional("unknown")).isEmpty();
    }

    @Test
    @DisplayName("fromCodeOptional with null returns empty Optional")
    void fromCodeOptionalWithNullReturnsEmpty() {
        assertThat(CaptchaType.fromCodeOptional(null)).isEmpty();
    }

    @Test
    @DisplayName("fromCodeOptional with empty string returns empty Optional")
    void fromCodeOptionalWithEmptyStringReturnsEmpty() {
        assertThat(CaptchaType.fromCodeOptional("")).isEmpty();
    }

    @Test
    @DisplayName("getCode returns the correct code string for each type")
    void getCodeReturnsCorrectCode() {
        assertThat(CaptchaType.RECAPTCHA_V2.getCode()).isEqualTo("recaptchav2");
        assertThat(CaptchaType.RECAPTCHA_V3.getCode()).isEqualTo("recaptchav3");
        assertThat(CaptchaType.HCAPTCHA.getCode()).isEqualTo("hcaptcha");
        assertThat(CaptchaType.TURNSTILE.getCode()).isEqualTo("turnstile");
        assertThat(CaptchaType.FUNCAPTCHA.getCode()).isEqualTo("funcaptcha");
        assertThat(CaptchaType.GEETEST.getCode()).isEqualTo("geetest");
        assertThat(CaptchaType.GEETEST_V4.getCode()).isEqualTo("geetestv4");
        assertThat(CaptchaType.AWS_WAF.getCode()).isEqualTo("awswaf");
    }

    @Test
    @DisplayName("getDisplayName returns the correct display name for each type")
    void getDisplayNameReturnsCorrectName() {
        assertThat(CaptchaType.RECAPTCHA_V2.getDisplayName()).isEqualTo("reCAPTCHA v2");
        assertThat(CaptchaType.RECAPTCHA_V3.getDisplayName()).isEqualTo("reCAPTCHA v3");
        assertThat(CaptchaType.HCAPTCHA.getDisplayName()).isEqualTo("hCaptcha");
        assertThat(CaptchaType.TURNSTILE.getDisplayName()).isEqualTo("Cloudflare Turnstile");
        assertThat(CaptchaType.FUNCAPTCHA.getDisplayName()).isEqualTo("FunCaptcha");
        assertThat(CaptchaType.GEETEST.getDisplayName()).isEqualTo("GeeTest");
        assertThat(CaptchaType.GEETEST_V4.getDisplayName()).isEqualTo("GeeTest v4");
        assertThat(CaptchaType.AWS_WAF.getDisplayName()).isEqualTo("Amazon WAF");
    }
}
