package cli.li.resolver.service.captcha.impl;

import cli.li.resolver.service.captcha.AntiCaptchaService;
import cli.li.resolver.captcha.model.CaptchaRequest;
import cli.li.resolver.captcha.exception.CaptchaSolverException;
import cli.li.resolver.captcha.model.CaptchaType;
import cli.li.resolver.captcha.solver.ICaptchaSolver;

import java.util.HashMap;
import java.util.Map;

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
        // For reCAPTCHA v3, the "action" parameter is required
        Map<String, String> additionalParams = request.additionalParams();
        
        // Default action value if not provided
        String action = additionalParams.getOrDefault("action", "verify");
        
        // Minimum score (0.3 by default for Anti-Captcha)
        Double minScore = null;
        if (additionalParams.containsKey("min_score")) {
            try {
                minScore = Double.parseDouble(additionalParams.get("min_score"));
            } catch (NumberFormatException e) {
                // Ignore invalid score
            }
        }
        
        // Create a map for extra parameters
        Map<String, String> extraParams = new HashMap<>();
        for (Map.Entry<String, String> entry : additionalParams.entrySet()) {
            String key = entry.getKey();
            if (!"action".equals(key) && !"min_score".equals(key)) {
                extraParams.put(key, entry.getValue());
            }
        }
        
        return service.solveRecaptchaV3(request.siteKey(), request.url(), action, minScore, extraParams);
    }

    @Override
    public CaptchaType getSupportedType() {
        return CaptchaType.RECAPTCHA_V3;
    }
}
