package cli.li.resolver.captcha.exception;

/**
 * Exception thrown when an error occurs during CAPTCHA solving
 */
public class CaptchaSolverException extends Exception {
    
    /**
     * Constructor with message
     * @param message Error message
     */
    public CaptchaSolverException(String message) {
        super(message);
    }
    
    /**
     * Constructor with message and cause
     * @param message Error message
     * @param cause The cause of the exception
     */
    public CaptchaSolverException(String message, Throwable cause) {
        super(message, cause);
    }
}
