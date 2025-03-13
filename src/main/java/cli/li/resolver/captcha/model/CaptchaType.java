package cli.li.resolver.captcha.model;

/**
 * Enum representing different types of CAPTCHAs
 */
public enum CaptchaType {
    RECAPTCHA_V2("recaptcha2"),
    RECAPTCHA_V3("recaptcha3");

    private final String displayName;

    CaptchaType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Get display name
     * @return Human-readable name of the CAPTCHA type
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get CAPTCHA type from code string
     * @param code Code string (case-insensitive)
     * @return Matching CAPTCHA type or default (RECAPTCHA_V2)
     */
    public static CaptchaType fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return RECAPTCHA_V2;
        }
        
        String upperCode = code.toUpperCase();
        
        if (upperCode.contains("RECAPTCHA") && upperCode.contains("V3")) {
            return RECAPTCHA_V3;
        } else if (upperCode.contains("RECAPTCHA")) {
            return RECAPTCHA_V2;
        }
        
        return RECAPTCHA_V2; // Default
    }

    @Override
    public String toString() {
        return displayName;
    }
}
