package cli.li.resolver.provider.impl;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import cli.li.resolver.provider.SolveRequest;
import cli.li.resolver.provider.base.QueryParamProvider;

/**
 * CAPTCHA provider implementation for 2Captcha.
 * Uses the query-parameter (form-encoded) in.php/res.php protocol.
 */
public class TwoCaptchaProvider extends QueryParamProvider {

    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "recaptchav2", "recaptchav3", "hcaptcha", "turnstile",
            "funcaptcha", "geetest", "geetestv4", "awswaf"
    );

    @Override
    public String id() {
        return "2captcha";
    }

    @Override
    public String displayName() {
        return "2Captcha";
    }

    @Override
    public Set<String> supportedTypes() {
        return SUPPORTED_TYPES;
    }

    @Override
    protected String baseUrl() {
        return "https://2captcha.com/";
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

            case "turnstile":
                params.put("method", "turnstile");
                params.put("sitekey", request.siteKey());
                params.put("pageurl", request.pageUrl());
                break;

            case "funcaptcha":
                params.put("method", "funcaptcha");
                params.put("publickey", request.siteKey());
                params.put("pageurl", request.pageUrl());
                break;

            case "geetest":
                params.put("method", "geetest");
                params.put("gt", request.siteKey());
                params.put("pageurl", request.pageUrl());
                if (extra != null && extra.containsKey("challenge")) {
                    params.put("challenge", extra.get("challenge"));
                }
                break;

            case "geetestv4":
                params.put("method", "geetest_v4");
                params.put("captcha_id", request.siteKey());
                params.put("pageurl", request.pageUrl());
                break;

            case "awswaf":
                params.put("method", "amazon_waf");
                params.put("sitekey", request.siteKey());
                params.put("pageurl", request.pageUrl());
                if (extra != null && extra.containsKey("iv")) {
                    params.put("iv", extra.get("iv"));
                }
                if (extra != null && extra.containsKey("context")) {
                    params.put("context", extra.get("context"));
                }
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
