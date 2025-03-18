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
        
        if (additionalParams != null) {
            // Handle invisible reCAPTCHA
            if (additionalParams.containsKey("invisible")) {
                extraParams.put("invisible", "true");
            }
            
            // Handle enterprise reCAPTCHA
            if (additionalParams.containsKey("enterprise")) {
                extraParams.put("enterprise", "true");
            }
            
            // Copy any other parameters
            for (Map.Entry<String, String> entry : additionalParams.entrySet()) {
                if (!entry.getKey().equals("invisible") && !entry.getKey().equals("enterprise")) {
                    extraParams.put(entry.getKey(), entry.getValue());
                }
            }
        }
        
        return service.solveRecaptchaV2(request.siteKey(), request.url(), extraParams);
    }

    @Override
    public CaptchaType getSupportedType() {
        return CaptchaType.RECAPTCHA_V2;
    }
}
