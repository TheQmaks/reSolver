package cli.li.resolver.detection.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cli.li.resolver.detection.CaptchaDetector;
import cli.li.resolver.detection.DetectedCaptcha;

/**
 * Detects reCAPTCHA v2 and v3 instances in HTTP response bodies.
 * Covers google.com, recaptcha.net, and enterprise.js variants.
 */
public class RecaptchaDetector implements CaptchaDetector {

    // --- v2 widget patterns (both attribute orders) ---
    private static final Pattern WIDGET_PATTERN = Pattern.compile(
            "class=\"g-recaptcha\"[^>]*?data-sitekey=\"([^\"]+)\"",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern WIDGET_REVERSED_PATTERN = Pattern.compile(
            "data-sitekey=\"([^\"]+)\"[^>]*?class=\"g-recaptcha\"",
            Pattern.CASE_INSENSITIVE
    );

    // --- v2 via grecaptcha.render() call ---
    private static final Pattern RENDER_CALL_PATTERN = Pattern.compile(
            "grecaptcha\\.render\\([^,]+,\\s*\\{[^}]*?sitekey\\s*:\\s*['\"]([^'\"]+)",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE
    );

    // --- v3 via render= parameter in script URL (api.js or enterprise.js) ---
    private static final Pattern RENDER_PARAM_PATTERN = Pattern.compile(
            "recaptcha/(?:api|enterprise)\\.js\\?[^\"']*?render=([A-Za-z0-9_-]{20,})",
            Pattern.CASE_INSENSITIVE
    );

    // --- v3 via grecaptcha.execute() or grecaptcha.enterprise.execute() ---
    private static final Pattern EXECUTE_PATTERN = Pattern.compile(
            "grecaptcha(?:\\.enterprise)?\\.execute\\(['\"]([A-Za-z0-9_-]+)",
            Pattern.CASE_INSENSITIVE
    );

    private static final int MAX_EVIDENCE_LENGTH = 120;

    @Override
    public List<DetectedCaptcha> detect(String url, String responseBody) {
        List<DetectedCaptcha> results = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();
        Instant now = Instant.now();

        // Detect v2 via widget class attribute (class before data-sitekey)
        addMatches(results, seenKeys, WIDGET_PATTERN, responseBody, url, "recaptchav2", now);

        // Detect v2 via widget class attribute (data-sitekey before class)
        addMatches(results, seenKeys, WIDGET_REVERSED_PATTERN, responseBody, url, "recaptchav2", now);

        // Detect v2 via grecaptcha.render() call
        addMatches(results, seenKeys, RENDER_CALL_PATTERN, responseBody, url, "recaptchav2", now);

        // Detect v3 via render= parameter in script URL (not render=explicit)
        Matcher renderParamMatcher = RENDER_PARAM_PATTERN.matcher(responseBody);
        while (renderParamMatcher.find()) {
            String siteKey = renderParamMatcher.group(1);
            if (!siteKey.equalsIgnoreCase("explicit") && seenKeys.add(siteKey)) {
                String evidence = extractEvidence(responseBody, renderParamMatcher.start(), renderParamMatcher.end());
                results.add(new DetectedCaptcha(url, "recaptchav3", siteKey, now, evidence));
            }
        }

        // Detect v3 via grecaptcha.execute() or grecaptcha.enterprise.execute()
        addMatches(results, seenKeys, EXECUTE_PATTERN, responseBody, url, "recaptchav3", now);

        return results;
    }

    private static void addMatches(List<DetectedCaptcha> results, Set<String> seenKeys,
                                   Pattern pattern, String body, String url, String type, Instant now) {
        Matcher matcher = pattern.matcher(body);
        while (matcher.find()) {
            String siteKey = matcher.group(1);
            if (seenKeys.add(siteKey)) {
                String evidence = extractEvidence(body, matcher.start(), matcher.end());
                results.add(new DetectedCaptcha(url, type, siteKey, now, evidence));
            }
        }
    }

    private static String extractEvidence(String body, int matchStart, int matchEnd) {
        int start = Math.max(0, matchStart - 10);
        int end = Math.min(body.length(), matchEnd + 10);
        String snippet = body.substring(start, end);
        if (snippet.length() > MAX_EVIDENCE_LENGTH) {
            snippet = snippet.substring(0, MAX_EVIDENCE_LENGTH) + "...";
        }
        return snippet;
    }
}
