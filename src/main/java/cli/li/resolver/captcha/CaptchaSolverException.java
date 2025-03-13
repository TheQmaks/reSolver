package cli.li.resolver.captcha;

/**
 * Exception thrown when CAPTCHA solving fails
 */
public class CaptchaSolverException extends Exception {
    public CaptchaSolverException(String message) {
        super(message);
    }

    public CaptchaSolverException(String message, Throwable cause) {
        super(message, cause);
    }
}