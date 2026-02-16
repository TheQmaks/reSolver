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
 * Detects Tencent Captcha (TCaptcha) instances in HTTP response bodies.
 * Covers ssl.captcha.qq.com script URL and new TencentCaptcha() JS calls.
 */
public class TencentCaptchaDetector implements CaptchaDetector {

    // JS init: new TencentCaptcha('APP_ID', ...)
    private static final Pattern INIT_PATTERN = Pattern.compile(
            "new\\s+TencentCaptcha\\s*\\(\\s*['\"]([^'\"]+)",
            Pattern.CASE_INSENSITIVE
    );

    // Script URL: ssl.captcha.qq.com/TCaptcha.js
    private static final Pattern SCRIPT_PATTERN = Pattern.compile(
            "ssl\\.captcha\\.qq\\.com/TCaptcha\\.js",
            Pattern.CASE_INSENSITIVE
    );

    // Alternative script domain
    private static final Pattern ALT_SCRIPT_PATTERN = Pattern.compile(
            "captcha\\.gtimg\\.com",
            Pattern.CASE_INSENSITIVE
    );

    private static final int MAX_EVIDENCE_LENGTH = 120;

    @Override
    public List<DetectedCaptcha> detect(String url, String responseBody) {
        List<DetectedCaptcha> results = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();
        Instant now = Instant.now();

        // Detect via new TencentCaptcha('APP_ID', ...) call
        Matcher initMatcher = INIT_PATTERN.matcher(responseBody);
        while (initMatcher.find()) {
            String appId = initMatcher.group(1);
            if (seenKeys.add(appId)) {
                String evidence = extractEvidence(responseBody, initMatcher.start(), initMatcher.end());
                results.add(new DetectedCaptcha(url, "tencent", appId, now, evidence));
            }
        }

        // Fallback: script URL present but no app ID found
        if (results.isEmpty()) {
            Pattern[] fallbacks = {SCRIPT_PATTERN, ALT_SCRIPT_PATTERN};
            for (Pattern pattern : fallbacks) {
                Matcher matcher = pattern.matcher(responseBody);
                if (matcher.find()) {
                    String evidence = extractEvidence(responseBody, matcher.start(), matcher.end());
                    results.add(new DetectedCaptcha(url, "tencent", "unknown", now, evidence));
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
