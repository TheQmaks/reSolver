package cli.li.resolver.http;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.requests.HttpRequest;

import cli.li.resolver.captcha.CaptchaType;
import cli.li.resolver.captcha.CaptchaRequest;
import cli.li.resolver.logger.BurpLoggerAdapter;

/**
 * Parser for CAPTCHA placeholders in HTTP requests
 */
public class PlaceholderParser {
    private final BurpLoggerAdapter logger;

    // Regex pattern for CAPTCHA placeholder: {{CAPTCHA[:]TYPE[:]SITEKEY[:]URL[:][OPTIONAL_PARAMS]}}
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile(
            "\\{\\{CAPTCHA\\[:\\]([^\\[:\\]]+)\\[:\\]([^\\[:\\]]+)\\[:\\]([^\\[\\]\\}]+)(?:\\[:\\]([^\\}]+))?\\}\\}"
    );

    public PlaceholderParser() {
        this.logger = BurpLoggerAdapter.getInstance();
        logger.info("PlaceholderParser", "Placeholder parser initialized");
    }

    /**
     * Find all placeholders in a request
     * @param request HTTP request
     * @return List of placeholder locations with their parsed parameters
     */
    public List<PlaceholderLocation> findPlaceholders(HttpRequest request) {
        List<PlaceholderLocation> locations = new ArrayList<>();
        String url = request.url();
        String truncatedUrl = url.length() > 100 ? url.substring(0, 100) + "..." : url;

        logger.debug("PlaceholderParser", "Searching for CAPTCHA placeholders in request to: " + truncatedUrl);

        // Check in request body
        String body = request.bodyToString();
        if (body != null && !body.isEmpty()) {
            Matcher matcher = PLACEHOLDER_PATTERN.matcher(body);
            while (matcher.find()) {
                PlaceholderLocation location = parsePlaceholder(matcher, PlaceholderLocationType.BODY);
                locations.add(location);
                logger.info("PlaceholderParser", "Found CAPTCHA placeholder in body: " + location.captchaRequest().captchaType() +
                        " for siteKey: " + location.captchaRequest().siteKey());
            }
        }

        // Check in URL
        Matcher urlMatcher = PLACEHOLDER_PATTERN.matcher(url);
        while (urlMatcher.find()) {
            PlaceholderLocation location = parsePlaceholder(urlMatcher, PlaceholderLocationType.URL);
            locations.add(location);
            logger.info("PlaceholderParser", "Found CAPTCHA placeholder in URL: " + location.captchaRequest().captchaType() +
                    " for siteKey: " + location.captchaRequest().siteKey());
        }

        // Check in headers
        for (HttpHeader header : request.headers()) {
            Matcher headerMatcher = PLACEHOLDER_PATTERN.matcher(header.value());
            while (headerMatcher.find()) {
                PlaceholderLocation location = parsePlaceholder(headerMatcher, PlaceholderLocationType.HEADER, header.name());
                locations.add(location);
                logger.info("PlaceholderParser", "Found CAPTCHA placeholder in header '" + header.name() + "': " +
                        location.captchaRequest().captchaType() + " for siteKey: " + location.captchaRequest().siteKey());
            }
        }

        if (locations.size() > 0) {
            logger.info("PlaceholderParser", "Found " + locations.size() + " CAPTCHA placeholder(s) in request");
        } else {
            logger.debug("PlaceholderParser", "No CAPTCHA placeholders found in request");
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

        logger.debug("PlaceholderParser", "Parsing CAPTCHA placeholder: type=" + captchaTypeStr +
                ", siteKey=" + siteKey + ", url=" + url +
                (optionalParams != null ? ", params=" + optionalParams : ""));

        // Parse CAPTCHA type
        CaptchaType captchaType;
        try {
            captchaType = CaptchaType.fromCode(captchaTypeStr);
        } catch (IllegalArgumentException e) {
            // Unknown CAPTCHA type, use default
            logger.warning("PlaceholderParser", "Unknown CAPTCHA type: " + captchaTypeStr +
                    ", falling back to RECAPTCHA_V2");
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
                    logger.debug("PlaceholderParser", "Added parameter: " + keyValue[0].trim() + "=" + keyValue[1].trim());
                } else {
                    logger.warning("PlaceholderParser", "Invalid parameter format in placeholder: " + param);
                }
            }
        }

        // Create CAPTCHA request
        CaptchaRequest captchaRequest = new CaptchaRequest(captchaType, siteKey, url, additionalParams);

        PlaceholderLocation location = new PlaceholderLocation(
                fullMatch,
                captchaRequest,
                locationType,
                headerName,
                matcher.start(),
                matcher.end()
        );

        logger.debug("PlaceholderParser", "Placeholder parsed successfully: " +
                captchaType + " in " + locationType +
                (headerName != null ? " (header: " + headerName + ")" : ""));

        return location;
    }
}