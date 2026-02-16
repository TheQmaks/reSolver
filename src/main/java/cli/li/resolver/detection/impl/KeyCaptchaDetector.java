package cli.li.resolver.detection.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cli.li.resolver.detection.CaptchaDetector;
import cli.li.resolver.detection.DetectedCaptcha;

/**
 * Detects KeyCaptcha instances in HTTP response bodies.
 * Covers keycaptcha.com script URLs, s_s_c_user_id JS variable,
 * and div_for_keycaptcha element ID.
 */
public class KeyCaptchaDetector implements CaptchaDetector {

    // JS variable: s_s_c_user_id = 12345
    private static final Pattern USER_ID_PATTERN = Pattern.compile(
            "s_s_c_user_id\\s*=\\s*['\"]?(\\d+)",
            Pattern.CASE_INSENSITIVE
    );

    // Script URL: keycaptcha.com
    private static final Pattern SCRIPT_PATTERN = Pattern.compile(
            "keycaptcha\\.com/swfs/cap\\.js",
            Pattern.CASE_INSENSITIVE
    );

    // Widget element: id="div_for_keycaptcha"
    private static final Pattern WIDGET_PATTERN = Pattern.compile(
            "id=[\"']div_for_keycaptcha[\"']",
            Pattern.CASE_INSENSITIVE
    );

    private static final int MAX_EVIDENCE_LENGTH = 120;

    @Override
    public List<DetectedCaptcha> detect(String url, String responseBody) {
        List<DetectedCaptcha> results = new ArrayList<>();
        Instant now = Instant.now();

        // Detect via s_s_c_user_id variable (most specific — gives the user ID)
        Matcher userIdMatcher = USER_ID_PATTERN.matcher(responseBody);
        if (userIdMatcher.find()) {
            String userId = userIdMatcher.group(1);
            String evidence = extractEvidence(responseBody, userIdMatcher.start(), userIdMatcher.end());
            results.add(new DetectedCaptcha(url, "keycaptcha", userId, now, evidence));
            return results;
        }

        // Fallback: script URL or widget element
        Pattern[] fallbacks = {SCRIPT_PATTERN, WIDGET_PATTERN};
        for (Pattern pattern : fallbacks) {
            Matcher matcher = pattern.matcher(responseBody);
            if (matcher.find()) {
                String evidence = extractEvidence(responseBody, matcher.start(), matcher.end());
                results.add(new DetectedCaptcha(url, "keycaptcha", "unknown", now, evidence));
                return results;
            }
        }

        return results;
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
