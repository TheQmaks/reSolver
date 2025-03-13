package cli.li.resolver.captcha;

/**
 * Enumeration of supported CAPTCHA types
 */
public enum CaptchaType {
    RECAPTCHA_V2("recaptcha2"),
    RECAPTCHA_V3("recaptcha3");

    private final String code;

    CaptchaType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static CaptchaType fromCode(String code) {
        for (CaptchaType type : values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown CAPTCHA type: " + code);
    }
}