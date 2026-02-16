package cli.li.resolver.provider.selection;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import cli.li.resolver.provider.CaptchaProvider;
import cli.li.resolver.provider.ProviderService;
import cli.li.resolver.provider.SolveRequest;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProviderSelector")
class ProviderSelectorTest {

    private ProviderSelector selector;

    @BeforeEach
    void setUp() {
        selector = new ProviderSelector();
    }

    @Test
    @DisplayName("empty list returns empty result")
    void emptyListReturnsEmpty() {
        List<ProviderService> result = selector.selectOrdered("recaptchav2", List.of());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("disabled provider is filtered out")
    void disabledProviderIsFilteredOut() {
        ProviderService ps = createProviderService("provider1", Set.of("recaptchav2"), 1);
        ps.setEnabled(false);
        ps.setApiKey("valid-key");
        setCachedBalance(ps, BigDecimal.TEN);

        List<ProviderService> result = selector.selectOrdered("recaptchav2", List.of(ps));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("provider without API key is filtered out")
    void providerWithoutApiKeyIsFilteredOut() {
        ProviderService ps = createProviderService("provider1", Set.of("recaptchav2"), 1);
        ps.setEnabled(true);
        // apiKey defaults to empty string, which fails isValidKeyFormat
        setCachedBalance(ps, BigDecimal.TEN);

        List<ProviderService> result = selector.selectOrdered("recaptchav2", List.of(ps));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("provider that does not support the captcha type is filtered out")
    void providerNotSupportingTypeIsFilteredOut() {
        ProviderService ps = createProviderService("provider1", Set.of("hcaptcha"), 1);
        ps.setEnabled(true);
        ps.setApiKey("valid-key");
        setCachedBalance(ps, BigDecimal.TEN);

        List<ProviderService> result = selector.selectOrdered("recaptchav2", List.of(ps));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("provider with zero balance is filtered out")
    void providerWithZeroBalanceIsFilteredOut() {
        ProviderService ps = createProviderService("provider1", Set.of("recaptchav2"), 1);
        ps.setEnabled(true);
        ps.setApiKey("valid-key");
        setCachedBalance(ps, BigDecimal.ZERO);

        List<ProviderService> result = selector.selectOrdered("recaptchav2", List.of(ps));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("provider with open circuit breaker is filtered out")
    void providerWithOpenCircuitBreakerIsFilteredOut() {
        ProviderService ps = createProviderService("provider1", Set.of("recaptchav2"), 1);
        ps.setEnabled(true);
        ps.setApiKey("valid-key");
        setCachedBalance(ps, BigDecimal.TEN);

        CircuitBreaker cb = selector.getCircuitBreaker("provider1");
        for (int i = 0; i < 5; i++) {
            cb.recordFailure();
        }

        List<ProviderService> result = selector.selectOrdered("recaptchav2", List.of(ps));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("eligible provider is included in results")
    void eligibleProviderIsIncluded() {
        ProviderService ps = createEligibleProvider("provider1", Set.of("recaptchav2"), 1);

        List<ProviderService> result = selector.selectOrdered("recaptchav2", List.of(ps));

        assertThat(result).hasSize(1).containsExactly(ps);
    }

    @Test
    @DisplayName("multiple eligible providers are sorted by score (higher priority first)")
    void multipleEligibleProvidersSortedByScore() {
        // Lower priority number = higher priority = higher score
        ProviderService highPriority = createEligibleProvider("high", Set.of("recaptchav2"), 0);
        ProviderService lowPriority = createEligibleProvider("low", Set.of("recaptchav2"), 10);

        List<ProviderService> result = selector.selectOrdered(
                "recaptchav2", List.of(lowPriority, highPriority));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo("high");
        assertThat(result.get(1).getId()).isEqualTo("low");
    }

    @Test
    @DisplayName("provider with better success rate ranks higher when priorities are equal")
    void betterSuccessRateRanksHigher() {
        ProviderService goodProvider = createEligibleProvider("good", Set.of("recaptchav2"), 1);
        goodProvider.getStatistics().recordSuccess(1000);
        goodProvider.getStatistics().recordSuccess(1000);

        ProviderService badProvider = createEligibleProvider("bad", Set.of("recaptchav2"), 1);
        badProvider.getStatistics().recordFailure();
        badProvider.getStatistics().recordFailure();

        List<ProviderService> result = selector.selectOrdered(
                "recaptchav2", List.of(badProvider, goodProvider));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo("good");
        assertThat(result.get(1).getId()).isEqualTo("bad");
    }

    @Test
    @DisplayName("getCircuitBreaker returns the same instance for the same provider id")
    void getCircuitBreakerReturnsSameInstance() {
        CircuitBreaker cb1 = selector.getCircuitBreaker("provider1");
        CircuitBreaker cb2 = selector.getCircuitBreaker("provider1");

        assertThat(cb1).isSameAs(cb2);
    }

    @Test
    @DisplayName("getCircuitBreaker returns different instances for different provider ids")
    void getCircuitBreakerReturnsDifferentInstances() {
        CircuitBreaker cb1 = selector.getCircuitBreaker("provider1");
        CircuitBreaker cb2 = selector.getCircuitBreaker("provider2");

        assertThat(cb1).isNotSameAs(cb2);
    }

    @Test
    @DisplayName("mix of eligible and ineligible providers returns only eligible ones")
    void mixOfEligibleAndIneligibleProviders() {
        ProviderService eligible = createEligibleProvider("eligible", Set.of("recaptchav2"), 1);

        ProviderService disabled = createProviderService("disabled", Set.of("recaptchav2"), 1);
        disabled.setEnabled(false);
        disabled.setApiKey("valid-key");
        setCachedBalance(disabled, BigDecimal.TEN);

        ProviderService wrongType = createProviderService("wrongType", Set.of("hcaptcha"), 1);
        wrongType.setEnabled(true);
        wrongType.setApiKey("valid-key");
        setCachedBalance(wrongType, BigDecimal.TEN);

        List<ProviderService> result = selector.selectOrdered(
                "recaptchav2", List.of(eligible, disabled, wrongType));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("eligible");
    }

    // ---- Helper methods ----

    private ProviderService createProviderService(String id, Set<String> supportedTypes, int priority) {
        CaptchaProvider provider = new TestProvider(id, supportedTypes);
        return new ProviderService(provider, priority);
    }

    private ProviderService createEligibleProvider(String id, Set<String> supportedTypes, int priority) {
        ProviderService ps = createProviderService(id, supportedTypes, priority);
        ps.setEnabled(true);
        ps.setApiKey("valid-key");
        setCachedBalance(ps, BigDecimal.TEN);
        return ps;
    }

    private static void setCachedBalance(ProviderService ps, BigDecimal balance) {
        try {
            Field field = ProviderService.class.getDeclaredField("cachedBalance");
            field.setAccessible(true);
            field.set(ps, balance);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to set cachedBalance via reflection", e);
        }
    }

    private static class TestProvider implements CaptchaProvider {
        private final String id;
        private final Set<String> supportedTypes;

        TestProvider(String id, Set<String> supportedTypes) {
            this.id = id;
            this.supportedTypes = supportedTypes;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public String displayName() {
            return "Test " + id;
        }

        @Override
        public Set<String> supportedTypes() {
            return supportedTypes;
        }

        @Override
        public String solve(SolveRequest request) {
            return "token";
        }

        @Override
        public BigDecimal fetchBalance(String apiKey) {
            return BigDecimal.TEN;
        }

        @Override
        public boolean isValidKeyFormat(String apiKey) {
            return apiKey != null && !apiKey.isEmpty();
        }
    }
}
