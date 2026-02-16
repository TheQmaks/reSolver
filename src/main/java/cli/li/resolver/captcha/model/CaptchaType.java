package cli.li.resolver.captcha.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Enum representing different types of CAPTCHAs
 */
public enum CaptchaType {
    RECAPTCHA_V2("recaptchav2", "reCAPTCHA v2"),
    RECAPTCHA_V3("recaptchav3", "reCAPTCHA v3"),
    HCAPTCHA("hcaptcha", "hCaptcha"),
    TURNSTILE("turnstile", "Cloudflare Turnstile"),
    FUNCAPTCHA("funcaptcha", "FunCaptcha"),
    GEETEST("geetest", "GeeTest"),
    GEETEST_V4("geetestv4", "GeeTest v4"),
    AWS_WAF("awswaf", "Amazon WAF"),
    MTCAPTCHA("mtcaptcha", "MTCaptcha"),
    LEMIN("lemin", "Lemin Cropped"),
    KEYCAPTCHA("keycaptcha", "KeyCaptcha"),
    FRIENDLY_CAPTCHA("friendlycaptcha", "Friendly Captcha"),
    YANDEX("yandex", "Yandex SmartCaptcha"),
    TENCENT("tencent", "Tencent Captcha"),
    CAPTCHAFOX("captchafox", "CaptchaFox"),
    PROCAPTCHA("procaptcha", "Procaptcha");

    private final String code;
    private final String displayName;

    private static final Map<String, CaptchaType> BY_CODE;

    static {
        Map<String, CaptchaType> map = new HashMap<>();
        for (CaptchaType type : values()) {
            map.put(type.code, type);
        }
        // Aliases
        map.put("recaptcha2", RECAPTCHA_V2);
        map.put("recaptcha_v2", RECAPTCHA_V2);
        map.put("recaptcha3", RECAPTCHA_V3);
        map.put("recaptcha_v3", RECAPTCHA_V3);
        BY_CODE = Collections.unmodifiableMap(map);
    }

    CaptchaType(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    /**
     * Get code value used in placeholders
     * @return Code string for this CAPTCHA type
     */
    public String getCode() {
        return code;
    }

    /**
     * Get display name for UI
     * @return Human-readable name of the CAPTCHA type
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get CAPTCHA type from code string
     * @param code Code string (case-insensitive)
     * @return Matching CAPTCHA type
     * @throws IllegalArgumentException If the code doesn't match any known CAPTCHA type
     */
    public static CaptchaType fromCode(String code) {
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("CAPTCHA type code cannot be null or empty");
        }
        CaptchaType type = BY_CODE.get(code.toLowerCase().trim());
        if (type == null) {
            throw new IllegalArgumentException("Unknown CAPTCHA type: " + code);
        }
        return type;
    }

    /**
     * Get CAPTCHA type from code string, returning Optional.empty() if not found
     * @param code Code string (case-insensitive)
     * @return Optional containing the matching CAPTCHA type, or empty if not found
     */
    public static Optional<CaptchaType> fromCodeOptional(String code) {
        if (code == null || code.isEmpty()) return Optional.empty();
        return Optional.ofNullable(BY_CODE.get(code.toLowerCase().trim()));
    }

    @Override
    public String toString() {
        return displayName;
    }
}
