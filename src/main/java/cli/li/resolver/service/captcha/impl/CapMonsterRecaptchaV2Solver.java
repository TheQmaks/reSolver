package cli.li.resolver.service.captcha.impl;

import cli.li.resolver.service.captcha.CapMonsterService;
import cli.li.resolver.captcha.model.CaptchaRequest;
import cli.li.resolver.captcha.exception.CaptchaSolverException;
import cli.li.resolver.captcha.model.CaptchaType;
import cli.li.resolver.captcha.solver.ICaptchaSolver;

import java.util.HashMap;
import java.util.Map;

/**
 * reCAPTCHA v2 solver implementation for CapMonster service
 */
public class CapMonsterRecaptchaV2Solver implements ICaptchaSolver {
    private final CapMonsterService service;

    public CapMonsterRecaptchaV2Solver(CapMonsterService service) {
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
        
        try {
            return service.solveRecaptchaV2(request.siteKey(), request.url(), extraParams);
        } catch (Exception e) {
            throw new CaptchaSolverException("Error solving reCAPTCHA v2: " + e.getMessage(), e);
        }
    }

    @Override
    public CaptchaType getSupportedType() {
        return CaptchaType.RECAPTCHA_V2;
    }
}
