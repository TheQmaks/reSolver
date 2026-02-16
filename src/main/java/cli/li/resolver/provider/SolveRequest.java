package cli.li.resolver.provider;

import java.util.Map;

/**
 * Represents a request to solve a CAPTCHA.
 */
public record SolveRequest(
    String apiKey,
    String type,
    String siteKey,
    String pageUrl,
    Map<String, String> params
) {}
