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
 * Detects MTCaptcha instances in HTTP response bodies.
 * Covers mtcaptcha.min.js script, mtcaptchaConfig sitekey,
 * and mtcaptcha widget class/id.
 */
public class MtCaptchaDetector implements CaptchaDetector {

    // JS config: mtcaptchaConfig = { sitekey: '...' }
    private static final Pattern CONFIG_SITEKEY_PATTERN = Pattern.compile(
            "mtcaptchaConfig[^}]*?sitekey\\s*:\\s*['\"]([^'\"]+)",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE
    );

    // Script URL: service.mtcaptcha.com
    private static final Pattern SCRIPT_PATTERN = Pattern.compile(
            "service\\.mtcaptcha\\.com/mtcv1/client/mtcaptcha",
            Pattern.CASE_INSENSITIVE
    );

    // Widget class or id
    private static final Pattern WIDGET_PATTERN = Pattern.compile(
            "(?:class|id)=[\"']mtcaptcha[\"']",
            Pattern.CASE_INSENSITIVE
    );

    private static final int MAX_EVIDENCE_LENGTH = 120;

    @Override
    public List<DetectedCaptcha> detect(String url, String responseBody) {
        List<DetectedCaptcha> results = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();
        Instant now = Instant.now();

        // Detect via mtcaptchaConfig sitekey
        Matcher configMatcher = CONFIG_SITEKEY_PATTERN.matcher(responseBody);
        while (configMatcher.find()) {
            String siteKey = configMatcher.group(1);
            if (seenKeys.add(siteKey)) {
                String evidence = extractEvidence(responseBody, configMatcher.start(), configMatcher.end());
                results.add(new DetectedCaptcha(url, "mtcaptcha", siteKey, now, evidence));
            }
        }

        // Fallback: script URL or widget element present but no sitekey found
        if (results.isEmpty()) {
            Pattern[] fallbacks = {SCRIPT_PATTERN, WIDGET_PATTERN};
            for (Pattern pattern : fallbacks) {
                Matcher matcher = pattern.matcher(responseBody);
                if (matcher.find()) {
                    String evidence = extractEvidence(responseBody, matcher.start(), matcher.end());
                    results.add(new DetectedCaptcha(url, "mtcaptcha", "unknown", now, evidence));
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
