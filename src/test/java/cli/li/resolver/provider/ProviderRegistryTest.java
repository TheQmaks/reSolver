package cli.li.resolver.provider;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProviderRegistry")
class ProviderRegistryTest {

    private ProviderRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ProviderRegistry();
    }

    @Test
    @DisplayName("register and retrieve a provider by id")
    void registerAndRetrieveById() {
        TestProvider provider = new TestProvider("twocaptcha");

        registry.register(provider);

        assertThat(registry.get("twocaptcha")).isPresent().contains(provider);
    }

    @Test
    @DisplayName("get() returns empty Optional for unknown id")
    void getReturnsEmptyForUnknownId() {
        assertThat(registry.get("nonexistent")).isEmpty();
    }

    @Test
    @DisplayName("getAll() returns all registered providers")
    void getAllReturnsAllRegisteredProviders() {
        TestProvider provider1 = new TestProvider("twocaptcha");
        TestProvider provider2 = new TestProvider("anticaptcha");
        TestProvider provider3 = new TestProvider("capsolver");

        registry.register(provider1);
        registry.register(provider2);
        registry.register(provider3);

        assertThat(registry.getAll())
                .hasSize(3)
                .containsExactly(provider1, provider2, provider3);
    }

    @Test
    @DisplayName("size() returns correct count")
    void sizeReturnsCorrectCount() {
        assertThat(registry.size()).isZero();

        registry.register(new TestProvider("twocaptcha"));
        assertThat(registry.size()).isEqualTo(1);

        registry.register(new TestProvider("anticaptcha"));
        assertThat(registry.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("registering a provider with the same id replaces the previous one")
    void registerWithSameIdReplacesPrevious() {
        TestProvider original = new TestProvider("twocaptcha");
        TestProvider replacement = new TestProvider("twocaptcha");

        registry.register(original);
        registry.register(replacement);

        assertThat(registry.size()).isEqualTo(1);
        assertThat(registry.get("twocaptcha")).isPresent().containsSame(replacement);
    }

    @Test
    @DisplayName("getAll() returns empty list when no providers registered")
    void getAllReturnsEmptyListWhenEmpty() {
        assertThat(registry.getAll()).isEmpty();
    }

    private static class TestProvider implements CaptchaProvider {
        private final String id;

        TestProvider(String id) {
            this.id = id;
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
            return Set.of("recaptchav2");
        }

        @Override
        public String solve(SolveRequest request) {
            return "token";
        }

        @Override
        public BigDecimal fetchBalance(String apiKey) {
            return BigDecimal.ONE;
        }

        @Override
        public boolean isValidKeyFormat(String apiKey) {
            return apiKey != null && !apiKey.isEmpty();
        }
    }
}
