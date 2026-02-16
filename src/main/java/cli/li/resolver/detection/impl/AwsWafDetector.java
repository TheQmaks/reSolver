package cli.li.resolver.detection.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cli.li.resolver.detection.CaptchaDetector;
import cli.li.resolver.detection.DetectedCaptcha;

/**
 * Detects AWS WAF CAPTCHA instances in HTTP response bodies.
 * Covers captcha-sdk.awswaf.com, token.awswaf.com,
 * AwsWafCaptcha/AwsWafIntegration JS objects, and aws-waf-token references.
 */
public class AwsWafDetector implements CaptchaDetector {

    // Strong signals: AWS WAF-specific domains
    private static final Pattern CAPTCHA_SDK_PATTERN = Pattern.compile(
            "captcha-sdk\\.awswaf\\.com",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern TOKEN_DOMAIN_PATTERN = Pattern.compile(
            "token\\.awswaf\\.com",
            Pattern.CASE_INSENSITIVE
    );

    // JS API objects
    private static final Pattern AWS_WAF_CAPTCHA_PATTERN = Pattern.compile(
            "AwsWafCaptcha\\.renderCaptcha",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern AWS_WAF_INTEGRATION_PATTERN = Pattern.compile(
            "AwsWafIntegration\\.",
            Pattern.CASE_INSENSITIVE
    );

    // Cookie/token reference
    private static final Pattern WAF_TOKEN_PATTERN = Pattern.compile(
            "aws-waf-token",
            Pattern.CASE_INSENSITIVE
    );

    private static final int MAX_EVIDENCE_LENGTH = 120;

    @Override
    public List<DetectedCaptcha> detect(String url, String responseBody) {
        List<DetectedCaptcha> results = new ArrayList<>();
        Instant now = Instant.now();

        // Check strong signals first (most specific → least specific)
        Pattern[] patterns = {
                CAPTCHA_SDK_PATTERN,
                TOKEN_DOMAIN_PATTERN,
                AWS_WAF_CAPTCHA_PATTERN,
                AWS_WAF_INTEGRATION_PATTERN,
                WAF_TOKEN_PATTERN
        };

        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(responseBody);
            if (matcher.find()) {
                String evidence = extractEvidence(responseBody, matcher.start(), matcher.end());
                results.add(new DetectedCaptcha(url, "awswaf", "unknown", now, evidence));
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
