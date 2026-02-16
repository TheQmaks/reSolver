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
 * Detects Cloudflare Turnstile CAPTCHA instances in HTTP response bodies.
 * Covers widget div, explicit rendering via JS, and script-only detection.
 */
public class TurnstileDetector implements CaptchaDetector {

    // Widget patterns (both attribute orders)
    private static final Pattern WIDGET_PATTERN = Pattern.compile(
            "class=\"cf-turnstile\"[^>]*?data-sitekey=\"([^\"]+)\"",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern WIDGET_REVERSED_PATTERN = Pattern.compile(
            "data-sitekey=\"([^\"]+)\"[^>]*?class=\"cf-turnstile\"",
            Pattern.CASE_INSENSITIVE
    );

    // Explicit rendering via JS: turnstile.render(el, { sitekey: '...' })
    private static final Pattern RENDER_CALL_PATTERN = Pattern.compile(
            "turnstile\\.render\\([^,]*,\\s*\\{[^}]*?sitekey\\s*:\\s*['\"]([^'\"]+)",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE
    );

    // Script presence (fallback when sitekey is not found)
    private static final Pattern SCRIPT_PATTERN = Pattern.compile(
            "challenges\\.cloudflare\\.com/turnstile/",
            Pattern.CASE_INSENSITIVE
    );

    private static final int MAX_EVIDENCE_LENGTH = 120;

    @Override
    public List<DetectedCaptcha> detect(String url, String responseBody) {
        List<DetectedCaptcha> results = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();
        Instant now = Instant.now();

        // Detect via widget class with data-sitekey (both attribute orders)
        addMatches(results, seenKeys, WIDGET_PATTERN, responseBody, url, now);
        addMatches(results, seenKeys, WIDGET_REVERSED_PATTERN, responseBody, url, now);

        // Detect via turnstile.render() JS call
        addMatches(results, seenKeys, RENDER_CALL_PATTERN, responseBody, url, now);

        // Fallback: script tag present but no sitekey found
        if (results.isEmpty()) {
            Matcher scriptMatcher = SCRIPT_PATTERN.matcher(responseBody);
            if (scriptMatcher.find()) {
                String evidence = extractEvidence(responseBody, scriptMatcher.start(), scriptMatcher.end());
                results.add(new DetectedCaptcha(url, "turnstile", "unknown", now, evidence));
            }
        }

        return results;
    }

    private static void addMatches(List<DetectedCaptcha> results, Set<String> seenKeys,
                                   Pattern pattern, String body, String url, Instant now) {
        Matcher matcher = pattern.matcher(body);
        while (matcher.find()) {
            String siteKey = matcher.group(1);
            if (seenKeys.add(siteKey)) {
                String evidence = extractEvidence(body, matcher.start(), matcher.end());
                results.add(new DetectedCaptcha(url, "turnstile", siteKey, now, evidence));
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
