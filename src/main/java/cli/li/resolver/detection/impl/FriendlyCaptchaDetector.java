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
 * Detects Friendly Captcha instances in HTTP response bodies.
 * Covers frc-captcha widget class, data-sitekey attribute,
 * and friendly-challenge / @friendlycaptcha/sdk script URLs.
 */
public class FriendlyCaptchaDetector implements CaptchaDetector {

    // Widget: class="frc-captcha" ... data-sitekey="..."
    private static final Pattern WIDGET_PATTERN = Pattern.compile(
            "class=\"frc-captcha\"[^>]*?data-sitekey=\"([^\"]+)\"",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern WIDGET_REVERSED_PATTERN = Pattern.compile(
            "data-sitekey=\"([^\"]+)\"[^>]*?class=\"frc-captcha\"",
            Pattern.CASE_INSENSITIVE
    );

    // JS SDK v2: createWidget({ ..., sitekey: '...' })
    private static final Pattern CREATE_WIDGET_PATTERN = Pattern.compile(
            "createWidget\\([^)]*?sitekey\\s*:\\s*['\"]([^'\"]+)",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE
    );

    // Script URL: friendly-challenge or @friendlycaptcha/sdk on CDN
    private static final Pattern SCRIPT_PATTERN = Pattern.compile(
            "friendly-challenge|friendlycaptcha/sdk",
            Pattern.CASE_INSENSITIVE
    );

    private static final int MAX_EVIDENCE_LENGTH = 120;

    @Override
    public List<DetectedCaptcha> detect(String url, String responseBody) {
        List<DetectedCaptcha> results = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();
        Instant now = Instant.now();

        addMatches(results, seenKeys, WIDGET_PATTERN, responseBody, url, now);
        addMatches(results, seenKeys, WIDGET_REVERSED_PATTERN, responseBody, url, now);
        addMatches(results, seenKeys, CREATE_WIDGET_PATTERN, responseBody, url, now);

        if (results.isEmpty()) {
            Matcher scriptMatcher = SCRIPT_PATTERN.matcher(responseBody);
            if (scriptMatcher.find()) {
                String evidence = extractEvidence(responseBody, scriptMatcher.start(), scriptMatcher.end());
                results.add(new DetectedCaptcha(url, "friendlycaptcha", "unknown", now, evidence));
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
                results.add(new DetectedCaptcha(url, "friendlycaptcha", siteKey, now, evidence));
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
