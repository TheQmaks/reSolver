package cli.li.resolver.settings;

import java.util.Map;
import java.util.HashMap;

import cli.li.resolver.logger.LoggerService;
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
    private final LoggerService logger;

    public SettingsManager(MontoyaApi api) {
        this.api = api;
        this.logger = LoggerService.getInstance();

        logger.info("SettingsManager", "Initializing settings manager");

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
            logger.info("SettingsManager", "Initialized default thread pool size: 10");
        } else {
            logger.debug("SettingsManager", "Thread pool size already set: " + extensionData.getInteger(THREAD_POOL_SIZE_KEY));
        }

        // Default queue size
        if (!extensionData.childObjectKeys().contains(QUEUE_SIZE_KEY)) {
            extensionData.setInteger(QUEUE_SIZE_KEY, 100);
            logger.info("SettingsManager", "Initialized default queue size: 100");
        } else {
            logger.debug("SettingsManager", "Queue size already set: " + extensionData.getInteger(QUEUE_SIZE_KEY));
        }

        // Default high load threshold
        if (!extensionData.childObjectKeys().contains(HIGH_LOAD_THRESHOLD_KEY)) {
            extensionData.setInteger(HIGH_LOAD_THRESHOLD_KEY, 50);
            logger.info("SettingsManager", "Initialized default high load threshold: 50");
        } else {
            logger.debug("SettingsManager", "High load threshold already set: " + extensionData.getInteger(HIGH_LOAD_THRESHOLD_KEY));
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
        logger.info("SettingsManager", "Loading service configs from persistence");

        if (configsJson != null && !configsJson.isEmpty()) {
            try {
                JSONObject jsonConfigs = new JSONObject(configsJson);
                logger.debug("SettingsManager", "Found config for " + jsonConfigs.length() + " services");

                for (String serviceId : jsonConfigs.keySet()) {
                    JSONObject serviceConfig = jsonConfigs.getJSONObject(serviceId);

                    String apiKey = serviceConfig.getString("apiKey");
                    boolean enabled = serviceConfig.getBoolean("enabled");
                    int priority = serviceConfig.getInt("priority");

                    configs.put(serviceId, new ServiceConfig(apiKey, enabled, priority));

                    // Mask API key for logging
                    String maskedApiKey = apiKey.isEmpty() ? "(empty)" :
                            apiKey.length() > 8 ?
                                    apiKey.substring(0, 4) + "..." + apiKey.substring(apiKey.length() - 4) :
                                    "****";

                    logger.info("SettingsManager", "Loaded config for service: " + serviceId +
                            ", API key: " + maskedApiKey +
                            ", enabled: " + enabled +
                            ", priority: " + priority);
                }
            } catch (JSONException e) {
                logger.error("SettingsManager", "Error parsing service configs: " + e.getMessage(), e);
            }
        } else {
            logger.info("SettingsManager", "No saved service configurations found");
        }

        return configs;
    }

    /**
     * Save service configurations
     * @param configs Map of service ID to service configuration
     */
    public void saveServiceConfigs(Map<String, ServiceConfig> configs) {
        PersistedObject data = api.persistence().extensionData();
        logger.info("SettingsManager", "Saving service configurations for " + configs.size() + " services");

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

                // Mask API key for logging
                String maskedApiKey = config.getApiKey().isEmpty() ? "(empty)" :
                        config.getApiKey().length() > 8 ?
                                config.getApiKey().substring(0, 4) + "..." +
                                        config.getApiKey().substring(config.getApiKey().length() - 4) :
                                "****";

                logger.info("SettingsManager", "Saving config for service: " + serviceId +
                        ", API key: " + maskedApiKey +
                        ", enabled: " + config.isEnabled() +
                        ", priority: " + config.getPriority());
            } catch (JSONException e) {
                logger.error("SettingsManager", "Error serializing config for service " + serviceId + ": " + e.getMessage(), e);
            }
        }

        // Save as a single JSON string
        String configsJson = jsonConfigs.toString();
        data.setString(SERVICE_CONFIGS_KEY, configsJson);
        logger.info("SettingsManager", "Service configurations saved successfully");
    }

    /**
     * Get the thread pool size
     * @return Thread pool size
     */
    public int getThreadPoolSize() {
        Integer size = api.persistence().extensionData().getInteger(THREAD_POOL_SIZE_KEY);
        int defaultSize = 10;
        if (size == null) {
            logger.warning("SettingsManager", "Thread pool size not found, using default: " + defaultSize);
            return defaultSize;
        }
        return size;
    }

    /**
     * Set the thread pool size
     * @param size Thread pool size
     */
    public void setThreadPoolSize(int size) {
        api.persistence().extensionData().setInteger(THREAD_POOL_SIZE_KEY, size);
        logger.info("SettingsManager", "Thread pool size updated to: " + size);
    }

    /**
     * Get the queue size
     * @return Queue size
     */
    public int getQueueSize() {
        Integer size = api.persistence().extensionData().getInteger(QUEUE_SIZE_KEY);
        int defaultSize = 100;
        if (size == null) {
            logger.warning("SettingsManager", "Queue size not found, using default: " + defaultSize);
            return defaultSize;
        }
        return size;
    }

    /**
     * Set the queue size
     * @param size Queue size
     */
    public void setQueueSize(int size) {
        api.persistence().extensionData().setInteger(QUEUE_SIZE_KEY, size);
        logger.info("SettingsManager", "Queue size updated to: " + size);
    }

    /**
     * Get the high load threshold
     * @return High load threshold
     */
    public int getHighLoadThreshold() {
        Integer threshold = api.persistence().extensionData().getInteger(HIGH_LOAD_THRESHOLD_KEY);
        int defaultThreshold = 50;
        if (threshold == null) {
            logger.warning("SettingsManager", "High load threshold not found, using default: " + defaultThreshold);
            return defaultThreshold;
        }
        return threshold;
    }

    /**
     * Set the high load threshold
     * @param threshold High load threshold
     */
    public void setHighLoadThreshold(int threshold) {
        api.persistence().extensionData().setInteger(HIGH_LOAD_THRESHOLD_KEY, threshold);
        logger.info("SettingsManager", "High load threshold updated to: " + threshold);
    }
}