package cli.li.resolver.provider;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Registry for CAPTCHA providers with ServiceLoader discovery.
 */
public class ProviderRegistry {
    private final Map<String, CaptchaProvider> providers = new LinkedHashMap<>();

    public void discoverProviders() {
        ServiceLoader.load(CaptchaProvider.class).forEach(this::register);
    }

    public void register(CaptchaProvider provider) {
        providers.put(provider.id(), provider);
    }

    public Optional<CaptchaProvider> get(String id) {
        return Optional.ofNullable(providers.get(id));
    }

    public List<CaptchaProvider> getAll() {
        return List.copyOf(providers.values());
    }

    public int size() {
        return providers.size();
    }
}
