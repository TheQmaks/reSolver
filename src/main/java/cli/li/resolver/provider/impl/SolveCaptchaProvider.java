package cli.li.resolver.provider.impl;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import cli.li.resolver.provider.SolveRequest;
import cli.li.resolver.provider.base.QueryParamProvider;

/**
 * CAPTCHA provider implementation for SolveCaptcha.
 * Uses the 2Captcha-compatible query-parameter protocol with limited type support.
 */
public class SolveCaptchaProvider extends QueryParamProvider {

    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "recaptchav2", "recaptchav3", "hcaptcha"
    );

    @Override
    public String id() {
        return "solvecaptcha";
    }

    @Override
    public String displayName() {
        return "SolveCaptcha";
    }

    @Override
    public Set<String> supportedTypes() {
        return SUPPORTED_TYPES;
    }

    @Override
    protected String baseUrl() {
        return "https://api.solvecaptcha.com/";
    }

    @Override
    protected Map<String, String> buildSubmitParams(SolveRequest request) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("key", request.apiKey());

        String type = request.type();
        Map<String, String> extra = request.params();

        switch (type) {
            case "recaptchav2":
                params.put("method", "userrecaptcha");
                params.put("googlekey", request.siteKey());
                params.put("pageurl", request.pageUrl());
                if (extra != null && extra.containsKey("invisible")) {
                    params.put("invisible", "1");
                }
                if (extra != null && extra.containsKey("enterprise")) {
                    params.put("enterprise", "1");
                }
                break;

            case "recaptchav3":
                params.put("method", "userrecaptcha");
                params.put("googlekey", request.siteKey());
                params.put("pageurl", request.pageUrl());
                params.put("version", "v3");
                params.put("action", extra != null && extra.containsKey("action")
                        ? extra.get("action") : "verify");
                if (extra != null && extra.containsKey("min_score")) {
                    params.put("min_score", extra.get("min_score"));
                }
                break;

            case "hcaptcha":
                params.put("method", "hcaptcha");
                params.put("sitekey", request.siteKey());
                params.put("pageurl", request.pageUrl());
                break;

            default:
                break;
        }

        return params;
    }

    @Override
    protected Map<String, String> buildResultParams(String apiKey, String taskId) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("key", apiKey);
        params.put("action", "get");
        params.put("id", taskId);
        return params;
    }

    @Override
    protected Map<String, String> buildBalanceParams(String apiKey) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("key", apiKey);
        params.put("action", "getbalance");
        return params;
    }
}
