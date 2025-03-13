package cli.li.resolver.http;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.requests.HttpRequest;

import cli.li.resolver.captcha.CaptchaType;
import cli.li.resolver.captcha.CaptchaRequest;

/**
 * Parser for CAPTCHA placeholders in HTTP requests
 */
public class PlaceholderParser {
    // Regex pattern for CAPTCHA placeholder: {{CAPTCHA[:]TYPE[:]SITEKEY[:]URL[:][OPTIONAL_PARAMS]}}
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile(
            "\\{\\{CAPTCHA\\[:\\]([^\\[:\\]]+)\\[:\\]([^\\[:\\]]+)\\[:\\]([^\\[\\]\\}]+)(?:\\[:\\]([^\\}]+))?\\}\\}"
    );

    /**
     * Find all placeholders in a request
     * @param request HTTP request
     * @return List of placeholder locations with their parsed parameters
     */
    public List<PlaceholderLocation> findPlaceholders(HttpRequest request) {
        List<PlaceholderLocation> locations = new ArrayList<>();

        // Check in request body
        String body = request.bodyToString();
        if (body != null && !body.isEmpty()) {
            Matcher matcher = PLACEHOLDER_PATTERN.matcher(body);
            while (matcher.find()) {
                locations.add(parsePlaceholder(matcher, PlaceholderLocationType.BODY));
            }
        }

        // Check in URL
        String url = request.url();
        Matcher urlMatcher = PLACEHOLDER_PATTERN.matcher(url);
        while (urlMatcher.find()) {
            locations.add(parsePlaceholder(urlMatcher, PlaceholderLocationType.URL));
        }

        // Check in headers
        for (HttpHeader header : request.headers()) {
            Matcher headerMatcher = PLACEHOLDER_PATTERN.matcher(header.value());
            while (headerMatcher.find()) {
                locations.add(parsePlaceholder(headerMatcher, PlaceholderLocationType.HEADER, header.name()));
            }
        }

        return locations;
    }

    /**
     * Parse a placeholder from a matcher
     * @param matcher Regex matcher with a match
     * @param locationType Location type
     * @return Placeholder location
     */
    private PlaceholderLocation parsePlaceholder(Matcher matcher, PlaceholderLocationType locationType) {
        return parsePlaceholder(matcher, locationType, null);
    }

    /**
     * Parse a placeholder from a matcher
     * @param matcher Regex matcher with a match
     * @param locationType Location type
     * @param headerName Header name (if location type is HEADER)
     * @return Placeholder location
     */
    private PlaceholderLocation parsePlaceholder(Matcher matcher, PlaceholderLocationType locationType, String headerName) {
        String fullMatch = matcher.group(0);
        String captchaTypeStr = matcher.group(1);
        String siteKey = matcher.group(2);
        String url = matcher.group(3);
        String optionalParams = matcher.group(4);

        // Parse CAPTCHA type
        CaptchaType captchaType;
        try {
            captchaType = CaptchaType.fromCode(captchaTypeStr);
        } catch (IllegalArgumentException e) {
            // Unknown CAPTCHA type, use default
            captchaType = CaptchaType.RECAPTCHA_V2;
        }

        // Parse optional parameters
        Map<String, String> additionalParams = new HashMap<>();
        if (optionalParams != null && !optionalParams.isEmpty()) {
            String[] params = optionalParams.split(",");
            for (String param : params) {
                String[] keyValue = param.split("=", 2);
                if (keyValue.length == 2) {
                    additionalParams.put(keyValue[0].trim(), keyValue[1].trim());
                }
            }
        }

        // Create CAPTCHA request
        CaptchaRequest captchaRequest = new CaptchaRequest(captchaType, siteKey, url, additionalParams);

        return new PlaceholderLocation(
                fullMatch,
                captchaRequest,
                locationType,
                headerName,
                matcher.start(),
                matcher.end()
        );
    }
}