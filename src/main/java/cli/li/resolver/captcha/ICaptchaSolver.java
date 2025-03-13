package cli.li.resolver.captcha;

/**
 * Interface for CAPTCHA solvers
 */
public interface ICaptchaSolver {
    /**
     * Solve a CAPTCHA
     * @param request CAPTCHA request
     * @return Solved CAPTCHA token
     * @throws CaptchaSolverException If solving fails
     */
    String solve(CaptchaRequest request) throws CaptchaSolverException;

    /**
     * Get supported CAPTCHA type
     * @return CAPTCHA type
     */
    CaptchaType getSupportedType();
}