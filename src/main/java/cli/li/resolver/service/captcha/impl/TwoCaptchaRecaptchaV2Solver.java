package cli.li.resolver.service.captcha.impl;

import cli.li.resolver.service.captcha.TwoCaptchaService;
import cli.li.resolver.captcha.model.CaptchaRequest;
import cli.li.resolver.captcha.exception.CaptchaSolverException;
import cli.li.resolver.captcha.model.CaptchaType;
import cli.li.resolver.captcha.solver.ICaptchaSolver;

import java.util.HashMap;
import java.util.Map;

/**
 * reCAPTCHA v2 solver implementation for 2Captcha service
 */
public class TwoCaptchaRecaptchaV2Solver implements ICaptchaSolver {
    private final TwoCaptchaService service;

    public TwoCaptchaRecaptchaV2Solver(TwoCaptchaService service) {
        this.service = service;
    }

    @Override
    public String solve(CaptchaRequest request) throws CaptchaSolverException {
        Map<String, String> extraParams = new HashMap<>();
        
        // Handle additional parameters
        Map<String, String> additionalParams = request.additionalParams();
        if (additionalParams.containsKey("invisible")) {
            extraParams.put("invisible", "1");
        }
        
        if (additionalParams.containsKey("enterprise")) {
            extraParams.put("enterprise", "1");
        }
        
        // Copy any other parameters
        if (additionalParams != null) {
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
