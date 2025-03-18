package cli.li.resolver.captcha.solver;

import cli.li.resolver.captcha.model.CaptchaType;
import cli.li.resolver.captcha.model.CaptchaRequest;
import cli.li.resolver.captcha.exception.CaptchaSolverException;

/**
 * Interface for CAPTCHA solvers
 */
public interface ICaptchaSolver {
    /**
     * Solves a CAPTCHA
     *
     * @param request CAPTCHA request with parameters
     * @return The solved CAPTCHA token/response
     * @throws CaptchaSolverException If an error occurs during solving
     */
    String solve(CaptchaRequest request) throws CaptchaSolverException;
    
    /**
     * Get supported CAPTCHA type
     * @return CAPTCHA type
     */
    CaptchaType getSupportedType();
}
