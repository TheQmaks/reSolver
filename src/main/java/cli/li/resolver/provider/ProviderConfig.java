package cli.li.resolver.provider;

/**
 * Configuration for a CAPTCHA provider.
 */
public record ProviderConfig(
    String apiKey,
    boolean enabled,
    int priority
) {}
