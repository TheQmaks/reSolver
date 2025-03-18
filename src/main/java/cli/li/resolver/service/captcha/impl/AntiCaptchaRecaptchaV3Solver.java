package cli.li.resolver.service.captcha.impl;

import java.util.Map;
import java.util.HashMap;

import cli.li.resolver.captcha.model.CaptchaType;
import cli.li.resolver.captcha.model.CaptchaRequest;
import cli.li.resolver.captcha.solver.ICaptchaSolver;
import cli.li.resolver.service.captcha.AntiCaptchaService;
import cli.li.resolver.captcha.exception.CaptchaSolverException;

/**
 * reCAPTCHA v3 solver implementation for Anti-Captcha service
 */
public class AntiCaptchaRecaptchaV3Solver implements ICaptchaSolver {
    private final AntiCaptchaService service;

    public AntiCaptchaRecaptchaV3Solver(AntiCaptchaService service) {
        this.service = service;
    }

    @Override
    public String solve(CaptchaRequest request) throws CaptchaSolverException {
        Map<String, String> extraParams = new HashMap<>();
        
        // Handle additional parameters
        Map<String, String> additionalParams = request.additionalParams();
        
        // For v3, get action and min_score if provided
        String action = additionalParams != null && additionalParams.containsKey("action") ? 
                additionalParams.get("action") : "verify";
        
        Double minScore = null;
        if (additionalParams != null && additionalParams.containsKey("min_score")) {
            try {
                minScore = Double.parseDouble(additionalParams.get("min_score"));
            } catch (NumberFormatException e) {
                // Use default value if parsing fails
            }
        }
        
        if (additionalParams != null && additionalParams.containsKey("enterprise")) {
            extraParams.put("isEnterprise", "true");
        }
        
        // Copy any other parameters
        if (additionalParams != null) {
            for (Map.Entry<String, String> entry : additionalParams.entrySet()) {
                if (!entry.getKey().equals("action") && !entry.getKey().equals("min_score") && 
                        !entry.getKey().equals("enterprise")) {
                    extraParams.put(entry.getKey(), entry.getValue());
                }
            }
        }
        
        return service.solveRecaptchaV3(request.siteKey(), request.url(), action, minScore, extraParams);
    }

    @Override
    public CaptchaType getSupportedType() {
        return CaptchaType.RECAPTCHA_V3;
    }
}
