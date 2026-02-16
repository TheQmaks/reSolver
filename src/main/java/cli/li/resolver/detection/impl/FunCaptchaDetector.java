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
 * Detects FunCaptcha (Arkose Labs) instances in HTTP response bodies.
 * Covers data-pkey attributes, ArkoseEnforcement JS calls,
 * and script URLs from arkoselabs.com and funcaptcha.com domains.
 */
public class FunCaptchaDetector implements CaptchaDetector {

    // Script URL patterns (multiple domains and paths)
    private static final Pattern SCRIPT_PATTERN = Pattern.compile(
            "(?:arkoselabs\\.com|funcaptcha\\.com)/(?:v2/[^/\"']+/api\\.js|fc/api/)",
            Pattern.CASE_INSENSITIVE
    );

    // data-pkey attribute
    private static final Pattern PKEY_PATTERN = Pattern.compile(
            "data-pkey=\"([^\"]+)\"",
            Pattern.CASE_INSENSITIVE
    );

    // JS initialization: new ArkoseEnforcement({ public_key: "..." })
    private static final Pattern ARKOSE_ENFORCEMENT_PATTERN = Pattern.compile(
            "ArkoseEnforcement\\(\\{[^}]*?public_key\\s*:\\s*['\"]([^'\"]+)",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE
    );

    private static final int MAX_EVIDENCE_LENGTH = 120;

    @Override
    public List<DetectedCaptcha> detect(String url, String responseBody) {
        List<DetectedCaptcha> results = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();
        Instant now = Instant.now();

        // Detect via data-pkey attribute
        addMatches(results, seenKeys, PKEY_PATTERN, responseBody, url, now);

        // Detect via ArkoseEnforcement JS object
        addMatches(results, seenKeys, ARKOSE_ENFORCEMENT_PATTERN, responseBody, url, now);

        // Fallback: script tag present but no public key found
        if (results.isEmpty()) {
            Matcher scriptMatcher = SCRIPT_PATTERN.matcher(responseBody);
            if (scriptMatcher.find()) {
                String evidence = extractEvidence(responseBody, scriptMatcher.start(), scriptMatcher.end());
                results.add(new DetectedCaptcha(url, "funcaptcha", "unknown", now, evidence));
            }
        }

        return results;
    }

    private static void addMatches(List<DetectedCaptcha> results, Set<String> seenKeys,
                                   Pattern pattern, String body, String url, Instant now) {
        Matcher matcher = pattern.matcher(body);
        while (matcher.find()) {
            String publicKey = matcher.group(1);
            if (seenKeys.add(publicKey)) {
                String evidence = extractEvidence(body, matcher.start(), matcher.end());
                results.add(new DetectedCaptcha(url, "funcaptcha", publicKey, now, evidence));
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
