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
 * Detects GeeTest v3 and v4 CAPTCHA instances in HTTP response bodies.
 * Covers initGeetest/initGeetest4 JS calls, script URLs,
 * and captcha_id/captchaId parameters.
 */
public class GeeTestDetector implements CaptchaDetector {

    // --- GeeTest v3 ---

    // initGeetest({ gt: "..." }) — negative lookahead prevents matching initGeetest4
    private static final Pattern V3_INIT_PATTERN = Pattern.compile(
            "initGeetest(?!4)\\s*\\([^)]*?gt\\s*:\\s*['\"]([^'\"]+)",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE
    );

    // Script URL: static.geetest.com (v3 scripts: gt.js, geetest.*.js)
    private static final Pattern V3_SCRIPT_PATTERN = Pattern.compile(
            "static\\.geetest\\.com/static/(?:tools/gt|js/(?:geetest|gt))[^\"']*\\.js",
            Pattern.CASE_INSENSITIVE
    );

    // --- GeeTest v4 ---

    // initGeetest4({ captchaId: '...' }) — camelCase parameter name
    private static final Pattern V4_INIT_PATTERN = Pattern.compile(
            "initGeetest4\\s*\\([^)]*?captchaId\\s*:\\s*['\"]([^'\"]+)",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE
    );

    // captcha_id parameter with 32-char hex value (URL params, JSON, JS objects)
    private static final Pattern V4_CAPTCHA_ID_PATTERN = Pattern.compile(
            "captcha_id[\"'\\s:=]+['\"]?([0-9a-f]{32})",
            Pattern.CASE_INSENSITIVE
    );

    // Script URL: gcaptcha4.geetest.com or gt4.js
    private static final Pattern V4_SCRIPT_PATTERN = Pattern.compile(
            "(?:gcaptcha4\\.geetest\\.com|static\\.geetest\\.com/v4/gt4\\.js)",
            Pattern.CASE_INSENSITIVE
    );

    private static final int MAX_EVIDENCE_LENGTH = 120;

    @Override
    public List<DetectedCaptcha> detect(String url, String responseBody) {
        List<DetectedCaptcha> results = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();
        Instant now = Instant.now();

        // Detect GeeTest v3 via initGeetest() call
        addMatches(results, seenKeys, V3_INIT_PATTERN, responseBody, url, "geetest", now);

        // Fallback: v3 script URL present but no gt key found
        if (results.isEmpty()) {
            Matcher v3ScriptMatcher = V3_SCRIPT_PATTERN.matcher(responseBody);
            if (v3ScriptMatcher.find()) {
                String evidence = extractEvidence(responseBody, v3ScriptMatcher.start(), v3ScriptMatcher.end());
                results.add(new DetectedCaptcha(url, "geetest", "unknown", now, evidence));
            }
        }

        // Detect GeeTest v4 via initGeetest4() call
        addMatches(results, seenKeys, V4_INIT_PATTERN, responseBody, url, "geetestv4", now);

        // Detect GeeTest v4 via captcha_id parameter
        addMatches(results, seenKeys, V4_CAPTCHA_ID_PATTERN, responseBody, url, "geetestv4", now);

        // Fallback: v4 script URL present but no captcha_id found
        boolean hasV4Results = results.stream().anyMatch(r -> "geetestv4".equals(r.type()));
        if (!hasV4Results) {
            Matcher v4ScriptMatcher = V4_SCRIPT_PATTERN.matcher(responseBody);
            if (v4ScriptMatcher.find()) {
                String evidence = extractEvidence(responseBody, v4ScriptMatcher.start(), v4ScriptMatcher.end());
                results.add(new DetectedCaptcha(url, "geetestv4", "unknown", now, evidence));
            }
        }

        return results;
    }

    private static void addMatches(List<DetectedCaptcha> results, Set<String> seenKeys,
                                   Pattern pattern, String body, String url, String type, Instant now) {
        Matcher matcher = pattern.matcher(body);
        while (matcher.find()) {
            String key = matcher.group(1);
            if (seenKeys.add(key)) {
                String evidence = extractEvidence(body, matcher.start(), matcher.end());
                results.add(new DetectedCaptcha(url, type, key, now, evidence));
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
