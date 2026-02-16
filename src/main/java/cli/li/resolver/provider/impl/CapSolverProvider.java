package cli.li.resolver.provider.impl;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import cli.li.resolver.provider.SolveRequest;
import cli.li.resolver.provider.base.JsonProtocolProvider;

/**
 * CAPTCHA provider implementation for CapSolver.
 * Uses the JSON createTask/getTaskResult protocol with CapSolver-specific task type names.
 */
public class CapSolverProvider extends JsonProtocolProvider {

    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "recaptchav2", "recaptchav3", "hcaptcha", "turnstile",
            "funcaptcha", "geetest", "geetestv4", "awswaf"
    );

    @Override
    public String id() {
        return "capsolver";
    }

    @Override
    public String displayName() {
        return "CapSolver";
    }

    @Override
    public Set<String> supportedTypes() {
        return SUPPORTED_TYPES;
    }

    @Override
    protected String baseUrl() {
        return "https://api.capsolver.com/";
    }

    @Override
    protected Map<String, Object> buildTaskObject(SolveRequest request) {
        Map<String, Object> task = new LinkedHashMap<>();

        String type = request.type();
        Map<String, String> extra = request.params();

        switch (type) {
            case "recaptchav2":
                task.put("type", "ReCaptchaV2TaskProxyLess");
                task.put("websiteURL", request.pageUrl());
                task.put("websiteKey", request.siteKey());
                break;

            case "recaptchav3":
                task.put("type", "ReCaptchaV3TaskProxyLess");
                task.put("websiteURL", request.pageUrl());
                task.put("websiteKey", request.siteKey());
                task.put("pageAction", extra != null && extra.containsKey("action")
                        ? extra.get("action") : "verify");
                if (extra != null && extra.containsKey("min_score")) {
                    task.put("minScore", Double.parseDouble(extra.get("min_score")));
                }
                break;

            case "hcaptcha":
                task.put("type", "HCaptchaTaskProxyless");
                task.put("websiteURL", request.pageUrl());
                task.put("websiteKey", request.siteKey());
                break;

            case "turnstile":
                task.put("type", "AntiTurnstileTaskProxyLess");
                task.put("websiteURL", request.pageUrl());
                task.put("websiteKey", request.siteKey());
                break;

            case "funcaptcha":
                task.put("type", "FunCaptchaTaskProxyLess");
                task.put("websiteURL", request.pageUrl());
                task.put("websitePublicKey", request.siteKey());
                break;

            case "geetest":
                task.put("type", "GeeTestTaskProxyless");
                task.put("websiteURL", request.pageUrl());
                task.put("gt", request.siteKey());
                break;

            case "geetestv4":
                task.put("type", "GeeTestTaskProxyless");
                task.put("websiteURL", request.pageUrl());
                task.put("captchaId", request.siteKey());
                if (extra != null && extra.containsKey("geetestApiServerSubdomain")) {
                    task.put("geetestApiServerSubdomain", extra.get("geetestApiServerSubdomain"));
                }
                break;

            case "awswaf":
                task.put("type", "AntiAwsWafTaskProxyLess");
                task.put("websiteURL", request.pageUrl());
                task.put("awsKey", request.siteKey());
                break;

            default:
                break;
        }

        return task;
    }
}
