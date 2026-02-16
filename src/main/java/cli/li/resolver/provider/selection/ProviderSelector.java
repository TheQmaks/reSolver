package cli.li.resolver.provider.selection;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import cli.li.resolver.provider.ProviderService;

/**
 * Selects and orders providers for a CAPTCHA solve request based on
 * priority, success rate, speed, and circuit breaker state.
 */
public class ProviderSelector {

    private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();

    /**
     * Get or create a circuit breaker for a provider.
     *
     * @param providerId the provider ID
     * @return the circuit breaker for this provider
     */
    public CircuitBreaker getCircuitBreaker(String providerId) {
        return circuitBreakers.computeIfAbsent(providerId, k -> new CircuitBreaker());
    }

    /**
     * Select and order providers that can handle the given CAPTCHA type.
     * Filters out disabled, unconfigured, zero-balance, and circuit-broken providers.
     * Remaining providers are scored and sorted by descending score.
     *
     * @param captchaType the CAPTCHA type code (e.g. "recaptchav2")
     * @param available   all available provider services
     * @return ordered list of eligible providers (best first)
     */
    public List<ProviderService> selectOrdered(String captchaType, List<ProviderService> available) {
        return available.stream()
                .filter(p -> p.isEnabled() && p.getProvider().isValidKeyFormat(p.getApiKey()))
                .filter(p -> p.getProvider().supportedTypes().contains(captchaType))
                .filter(p -> p.getCachedBalance() == null || p.getCachedBalance().compareTo(BigDecimal.ZERO) > 0)
                .filter(p -> !getCircuitBreaker(p.getId()).isOpen())
                .sorted(Comparator.comparingDouble(this::score).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Calculate a score for a provider based on priority, success rate, and speed.
     *
     * @param p the provider service
     * @return a score (higher is better)
     */
    private double score(ProviderService p) {
        double priorityScore = 1.0 / (1 + p.getPriority());
        double successRate = p.getStatistics().getSuccessRate() / 100.0;
        double speed = 1.0 / (1 + p.getStatistics().getAvgSolveTimeMs() / 1000.0);
        return priorityScore * 0.4 + successRate * 0.4 + speed * 0.2;
    }
}
