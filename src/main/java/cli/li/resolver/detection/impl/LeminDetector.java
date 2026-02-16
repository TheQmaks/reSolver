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
 * Detects Lemin Cropped CAPTCHA instances in HTTP response bodies.
 * Covers api.leminnow.com script URLs with captcha ID,
 * and lemin-cropped-captcha element ID.
 */
public class LeminDetector implements CaptchaDetector {

    // Script URL with captcha ID: api.leminnow.com/captcha/v1/cropped/{CAPTCHA_ID}/js
    private static final Pattern SCRIPT_WITH_ID_PATTERN = Pattern.compile(
            "api\\.leminnow\\.com/captcha/v1/cropped/([^/\"']+)/js",
            Pattern.CASE_INSENSITIVE
    );

    // General script URL fallback
    private static final Pattern SCRIPT_PATTERN = Pattern.compile(
            "api\\.leminnow\\.com",
            Pattern.CASE_INSENSITIVE
    );

    // Widget element: id="lemin-cropped-captcha"
    private static final Pattern WIDGET_PATTERN = Pattern.compile(
            "id=[\"']lemin-cropped-captcha[\"']",
            Pattern.CASE_INSENSITIVE
    );

    private static final int MAX_EVIDENCE_LENGTH = 120;

    @Override
    public List<DetectedCaptcha> detect(String url, String responseBody) {
        List<DetectedCaptcha> results = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();
        Instant now = Instant.now();

        // Detect via script URL with embedded captcha ID
        Matcher scriptIdMatcher = SCRIPT_WITH_ID_PATTERN.matcher(responseBody);
        while (scriptIdMatcher.find()) {
            String captchaId = scriptIdMatcher.group(1);
            if (seenKeys.add(captchaId)) {
                String evidence = extractEvidence(responseBody, scriptIdMatcher.start(), scriptIdMatcher.end());
                results.add(new DetectedCaptcha(url, "lemin", captchaId, now, evidence));
            }
        }

        // Fallback: general script URL or widget element
        if (results.isEmpty()) {
            Pattern[] fallbacks = {SCRIPT_PATTERN, WIDGET_PATTERN};
            for (Pattern pattern : fallbacks) {
                Matcher matcher = pattern.matcher(responseBody);
                if (matcher.find()) {
                    String evidence = extractEvidence(responseBody, matcher.start(), matcher.end());
                    results.add(new DetectedCaptcha(url, "lemin", "unknown", now, evidence));
                    return results;
                }
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
