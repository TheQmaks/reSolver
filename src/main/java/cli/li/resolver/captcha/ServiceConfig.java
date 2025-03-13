package cli.li.resolver.captcha;

/**
 * Configuration for a CAPTCHA service
 */
public class ServiceConfig {
    private String apiKey;
    private boolean enabled;
    private int priority;

    public ServiceConfig(String apiKey, boolean enabled, int priority) {
        this.apiKey = apiKey;
        this.enabled = enabled;
        this.priority = priority;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }
}