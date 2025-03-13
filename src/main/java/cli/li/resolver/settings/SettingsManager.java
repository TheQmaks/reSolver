package cli.li.resolver.settings;

import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

import org.json.JSONObject;
import org.json.JSONException;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.persistence.PersistedObject;

import cli.li.resolver.captcha.ServiceConfig;

/**
 * Manager for extension settings
 */
public class SettingsManager {
    private static final String EXTENSION_PREFIX = "resolver.";
    private static final String SERVICE_CONFIGS_KEY = EXTENSION_PREFIX + "services.config";
    private static final String THREAD_POOL_SIZE_KEY = EXTENSION_PREFIX + "threadPoolSize";
    private static final String QUEUE_SIZE_KEY = EXTENSION_PREFIX + "queueSize";
    private static final String HIGH_LOAD_THRESHOLD_KEY = EXTENSION_PREFIX + "highLoadThreshold";

    private final MontoyaApi api;

    public SettingsManager(MontoyaApi api) {
        this.api = api;

        // Initialize default settings if they don't exist
        initializeDefaultSettings();
    }

    /**
     * Initialize default settings
     */
    private void initializeDefaultSettings() {
        PersistedObject extensionData = api.persistence().extensionData();

        // Default thread pool size
        if (!extensionData.childObjectKeys().contains(THREAD_POOL_SIZE_KEY)) {
            extensionData.setInteger(THREAD_POOL_SIZE_KEY, 10);
        }

        // Default queue size
        if (!extensionData.childObjectKeys().contains(QUEUE_SIZE_KEY)) {
            extensionData.setInteger(QUEUE_SIZE_KEY, 100);
        }

        // Default high load threshold
        if (!extensionData.childObjectKeys().contains(HIGH_LOAD_THRESHOLD_KEY)) {
            extensionData.setInteger(HIGH_LOAD_THRESHOLD_KEY, 50);
        }
    }

    /**
     * Load service configurations
     * @return Map of service ID to service configuration
     */
    public Map<String, ServiceConfig> loadServiceConfigs() {
        Map<String, ServiceConfig> configs = new HashMap<>();
        PersistedObject data = api.persistence().extensionData();

        // Load service configs from JSON string
        String configsJson = data.getString(SERVICE_CONFIGS_KEY);
        api.logging().logToOutput("Loading service configs from persistence: " + configsJson);

        if (configsJson != null && !configsJson.isEmpty()) {
            try {
                JSONObject jsonConfigs = new JSONObject(configsJson);

                for (String serviceId : jsonConfigs.keySet()) {
                    JSONObject serviceConfig = jsonConfigs.getJSONObject(serviceId);

                    String apiKey = serviceConfig.getString("apiKey");
                    boolean enabled = serviceConfig.getBoolean("enabled");
                    int priority = serviceConfig.getInt("priority");

                    configs.put(serviceId, new ServiceConfig(apiKey, enabled, priority));
                    api.logging().logToOutput("Loaded config for service: " + serviceId + ", API key: " + apiKey);
                }
            } catch (JSONException e) {
                api.logging().logToError("Error parsing service configs: " + e.getMessage());
            }
        } else {
            api.logging().logToOutput("No saved service configurations found");
        }

        return configs;
    }

    /**
     * Save service configurations
     * @param configs Map of service ID to service configuration
     */
    public void saveServiceConfigs(Map<String, ServiceConfig> configs) {
        PersistedObject data = api.persistence().extensionData();

        // Serialize configs to JSON
        JSONObject jsonConfigs = new JSONObject();

        for (Map.Entry<String, ServiceConfig> entry : configs.entrySet()) {
            String serviceId = entry.getKey();
            ServiceConfig config = entry.getValue();

            try {
                JSONObject serviceConfig = new JSONObject();
                serviceConfig.put("apiKey", config.getApiKey());
                serviceConfig.put("enabled", config.isEnabled());
                serviceConfig.put("priority", config.getPriority());

                jsonConfigs.put(serviceId, serviceConfig);
                api.logging().logToOutput("Saving config for service: " + serviceId + ", API key: " + config.getApiKey());
            } catch (JSONException e) {
                api.logging().logToError("Error serializing config for service " + serviceId + ": " + e.getMessage());
            }
        }

        // Save as a single JSON string
        String configsJson = jsonConfigs.toString();
        data.setString(SERVICE_CONFIGS_KEY, configsJson);
        api.logging().logToOutput("Saved service configs to persistence: " + configsJson);
    }

    /**
     * Get the thread pool size
     * @return Thread pool size
     */
    public int getThreadPoolSize() {
        return Optional.ofNullable(api.persistence().extensionData().getInteger(THREAD_POOL_SIZE_KEY))
                .orElse(10);
    }

    /**
     * Set the thread pool size
     * @param size Thread pool size
     */
    public void setThreadPoolSize(int size) {
        api.persistence().extensionData().setInteger(THREAD_POOL_SIZE_KEY, size);
    }

    /**
     * Get the queue size
     * @return Queue size
     */
    public int getQueueSize() {
        return Optional.ofNullable(api.persistence().extensionData().getInteger(QUEUE_SIZE_KEY))
                .orElse(100);
    }

    /**
     * Set the queue size
     * @param size Queue size
     */
    public void setQueueSize(int size) {
        api.persistence().extensionData().setInteger(QUEUE_SIZE_KEY, size);
    }

    /**
     * Get the high load threshold
     * @return High load threshold
     */
    public int getHighLoadThreshold() {
        return Optional.ofNullable(api.persistence().extensionData().getInteger(HIGH_LOAD_THRESHOLD_KEY))
                .orElse(50);
    }

    /**
     * Set the high load threshold
     * @param threshold High load threshold
     */
    public void setHighLoadThreshold(int threshold) {
        api.persistence().extensionData().setInteger(HIGH_LOAD_THRESHOLD_KEY, threshold);
    }
}