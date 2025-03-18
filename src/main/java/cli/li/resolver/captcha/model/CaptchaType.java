package cli.li.resolver.captcha.model;

/**
 * Enum representing different types of CAPTCHAs
 */
public enum CaptchaType {
    RECAPTCHA_V2("recaptchav2"),
    RECAPTCHA_V3("recaptchav3");

    private final String code;

    CaptchaType(String code) {
        this.code = code;
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
        switch (this) {
            case RECAPTCHA_V2:
                return "reCAPTCHA v2";
            case RECAPTCHA_V3:
                return "reCAPTCHA v3";
            default:
                return code;
        }
    }

    /**
     * Get CAPTCHA type from code string
     * @param code Code string (case-insensitive)
     * @return Matching CAPTCHA type or throws IllegalArgumentException if no match
     * @throws IllegalArgumentException If the code doesn't match any known CAPTCHA type
     */
    public static CaptchaType fromCode(String code) {
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("CAPTCHA type code cannot be null or empty");
        }
        
        String normalizedCode = code.toLowerCase().trim();
        
        for (CaptchaType type : values()) {
            if (type.code.equals(normalizedCode)) {
                return type;
            }
        }
        
        // Handle some common alternative forms
        if (normalizedCode.equals("recaptcha2") || normalizedCode.equals("recaptcha_v2")) {
            return RECAPTCHA_V2;
        } else if (normalizedCode.equals("recaptcha3") || normalizedCode.equals("recaptcha_v3")) {
            return RECAPTCHA_V3;
        }
        
        throw new IllegalArgumentException("Unknown CAPTCHA type: " + code);
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
