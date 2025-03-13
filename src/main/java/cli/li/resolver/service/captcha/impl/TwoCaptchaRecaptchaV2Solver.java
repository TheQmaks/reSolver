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
        
        // Add additional parameters if present
        Map<String, String> additionalParams = request.additionalParams();
        if (additionalParams.containsKey("invisible")) {
            extraParams.put("invisible", additionalParams.get("invisible"));
        }

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
