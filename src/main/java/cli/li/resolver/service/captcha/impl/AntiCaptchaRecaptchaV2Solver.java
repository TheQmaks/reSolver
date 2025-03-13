package cli.li.resolver.service.captcha.impl;

import cli.li.resolver.service.captcha.AntiCaptchaService;
import cli.li.resolver.captcha.model.CaptchaRequest;
import cli.li.resolver.captcha.exception.CaptchaSolverException;
import cli.li.resolver.captcha.model.CaptchaType;
import cli.li.resolver.captcha.solver.ICaptchaSolver;

import java.util.HashMap;
import java.util.Map;

/**
 * reCAPTCHA v2 solver implementation for Anti-Captcha service
 */
public class AntiCaptchaRecaptchaV2Solver implements ICaptchaSolver {
    private final AntiCaptchaService service;

    public AntiCaptchaRecaptchaV2Solver(AntiCaptchaService service) {
        this.service = service;
    }

    @Override
    public String solve(CaptchaRequest request) throws CaptchaSolverException {
        Map<String, String> extraParams = new HashMap<>();
        
        // Handle additional parameters
        Map<String, String> additionalParams = request.additionalParams();
        if (additionalParams.containsKey("is_invisible") && "true".equals(additionalParams.get("is_invisible"))) {
            extraParams.put("isInvisible", "true");
        }
        
        // Add proxy if specified
        if (additionalParams.containsKey("proxy")) {
            extraParams.put("proxy", additionalParams.get("proxy"));
        }
        
        return service.solveRecaptchaV2(request.siteKey(), request.url(), extraParams);
    }

    @Override
    public CaptchaType getSupportedType() {
        return CaptchaType.RECAPTCHA_V2;
    }
}
