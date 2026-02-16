package cli.li.resolver.provider;

import java.math.BigDecimal;
import java.util.Set;

import cli.li.resolver.captcha.exception.CaptchaSolverException;

/**
 * Interface for CAPTCHA solving service providers.
 * Each provider implements this to support a specific API protocol.
 */
public interface CaptchaProvider {
    String id();
    String displayName();
    Set<String> supportedTypes();
    String solve(SolveRequest request) throws CaptchaSolverException;
    BigDecimal fetchBalance(String apiKey) throws Exception;
    boolean isValidKeyFormat(String apiKey);
}
