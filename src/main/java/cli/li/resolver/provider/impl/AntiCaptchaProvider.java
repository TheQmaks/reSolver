package cli.li.resolver.provider.impl;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import cli.li.resolver.provider.SolveRequest;
import cli.li.resolver.provider.base.JsonProtocolProvider;

/**
 * CAPTCHA provider implementation for Anti-Captcha.
 * Uses the JSON createTask/getTaskResult protocol.
 */
public class AntiCaptchaProvider extends JsonProtocolProvider {

    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "recaptchav2", "recaptchav3", "hcaptcha", "turnstile",
            "funcaptcha", "geetest", "geetestv4"
    );

    @Override
    public String id() {
        return "anticaptcha";
    }

    @Override
    public String displayName() {
        return "Anti-Captcha";
    }

    @Override
    public Set<String> supportedTypes() {
        return SUPPORTED_TYPES;
    }

    @Override
    protected String baseUrl() {
        return "https://api.anti-captcha.com/";
    }

    @Override
    protected Map<String, Object> buildTaskObject(SolveRequest request) {
        Map<String, Object> task = new LinkedHashMap<>();

        String type = request.type();
        Map<String, String> extra = request.params();

        switch (type) {
            case "recaptchav2":
                task.put("type", "NoCaptchaTaskProxyless");
                task.put("websiteURL", request.pageUrl());
                task.put("websiteKey", request.siteKey());
                if (extra != null && extra.containsKey("invisible")) {
                    task.put("isInvisible", Boolean.TRUE);
                }
                break;

            case "recaptchav3":
                task.put("type", "RecaptchaV3TaskProxyless");
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
                task.put("type", "TurnstileTaskProxyless");
                task.put("websiteURL", request.pageUrl());
                task.put("websiteKey", request.siteKey());
                break;

            case "funcaptcha":
                task.put("type", "FunCaptchaTaskProxyless");
                task.put("websiteURL", request.pageUrl());
                task.put("websitePublicKey", request.siteKey());
                break;

            case "geetest":
                task.put("type", "GeeTestTaskProxyless");
                task.put("websiteURL", request.pageUrl());
                task.put("gt", request.siteKey());
                if (extra != null && extra.containsKey("challenge")) {
                    task.put("challenge", extra.get("challenge"));
                }
                break;

            case "geetestv4":
                task.put("type", "GeeTestTaskProxyless");
                task.put("websiteURL", request.pageUrl());
                task.put("gt", request.siteKey());
                task.put("version", Integer.valueOf(4));
                Map<String, Object> initParameters = new LinkedHashMap<>();
                initParameters.put("captcha_id", request.siteKey());
                task.put("initParameters", initParameters);
                break;

            default:
                break;
        }

        return task;
    }
}
