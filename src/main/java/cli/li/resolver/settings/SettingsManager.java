package cli.li.resolver.settings;

import java.util.Map;
import java.util.HashMap;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.IOException;

import org.json.JSONObject;
import org.json.JSONException;

import cli.li.resolver.logger.LoggerService;
import cli.li.resolver.captcha.model.ServiceConfig;

/**
 * Manager for extension settings
 */
public class SettingsManager {
    // File storage constants
    private static final String CONFIG_DIRECTORY = ".resolver";
    private static final String SERVICE_CONFIG_FILENAME = "services.json";
    private static final String SETTINGS_FILENAME = "settings.json";
    
    private final LoggerService logger;
    private final Path configDirectory;
    private final Path serviceConfigFile;
    private final Path settingsFile;

    public SettingsManager() {
        this.logger = LoggerService.getInstance();

        logger.info("SettingsManager", "Initializing settings manager");
        
        // Setup configuration directory in user's home directory
        String userHome = System.getProperty("user.home");
        configDirectory = Paths.get(userHome, CONFIG_DIRECTORY);
        serviceConfigFile = configDirectory.resolve(SERVICE_CONFIG_FILENAME);
        settingsFile = configDirectory.resolve(SETTINGS_FILENAME);
        
        // Ensure config directory exists
        createConfigDirectory();

        // Initialize default settings if they don't exist
        initializeDefaultSettings();
    }
    
    /**
     * Create configuration directory if it doesn't exist
     */
    private void createConfigDirectory() {
        try {
            if (!Files.exists(configDirectory)) {
                Files.createDirectory(configDirectory);
                logger.info("SettingsManager", "Created configuration directory: " + configDirectory);
            }
        } catch (IOException e) {
            logger.error("SettingsManager", "Failed to create configuration directory: " + e.getMessage(), e);
        }
    }

    /**
     * Initialize default settings
     */
    private void initializeDefaultSettings() {
        JSONObject settings = loadSettingsFromFile();

        // Default thread pool size
        if (!settings.has("threadPoolSize")) {
            settings.put("threadPoolSize", 10);
            logger.info("SettingsManager", "Initialized default thread pool size: 10");
        } else {
            logger.debug("SettingsManager", "Thread pool size already set: " + settings.getInt("threadPoolSize"));
        }

        // Default queue size
        if (!settings.has("queueSize")) {
            settings.put("queueSize", 100);
            logger.info("SettingsManager", "Initialized default queue size: 100");
        } else {
            logger.debug("SettingsManager", "Queue size already set: " + settings.getInt("queueSize"));
        }

        // Default high load threshold
        if (!settings.has("highLoadThreshold")) {
            settings.put("highLoadThreshold", 50);
            logger.info("SettingsManager", "Initialized default high load threshold: 50");
        } else {
            logger.debug("SettingsManager", "High load threshold already set: " + settings.getInt("highLoadThreshold"));
        }
        
        // Save settings to file
        saveSettingsToFile(settings);
    }
    
    /**
     * Load settings from file
     * @return JSONObject containing settings
     */
    private JSONObject loadSettingsFromFile() {
        if (!Files.exists(settingsFile)) {
            return new JSONObject();
        }
        
        try (FileReader reader = new FileReader(settingsFile.toFile())) {
            StringBuilder content = new StringBuilder();
            char[] buffer = new char[1024];
            int bytesRead;
            
            while ((bytesRead = reader.read(buffer)) != -1) {
                content.append(buffer, 0, bytesRead);
            }
            
            if (content.length() > 0) {
                return new JSONObject(content.toString());
            }
        } catch (IOException | JSONException e) {
            logger.error("SettingsManager", "Error loading settings from file: " + e.getMessage(), e);
        }
        
        return new JSONObject();
    }
    
    /**
     * Save settings to file
     * @param settings JSONObject containing settings
     */
    private void saveSettingsToFile(JSONObject settings) {
        try (FileWriter writer = new FileWriter(settingsFile.toFile())) {
            writer.write(settings.toString(2)); // Pretty print with 2-space indentation
            logger.info("SettingsManager", "Settings saved to file: " + settingsFile);
        } catch (IOException e) {
            logger.error("SettingsManager", "Error saving settings to file: " + e.getMessage(), e);
        }
    }

    /**
     * Load service configurations
     * @return Map of service ID to service configuration
     */
    public Map<String, ServiceConfig> loadServiceConfigs() {
        Map<String, ServiceConfig> configs = new HashMap<>();
        
        if (Files.exists(serviceConfigFile)) {
            try (FileReader reader = new FileReader(serviceConfigFile.toFile())) {
                StringBuilder content = new StringBuilder();
                char[] buffer = new char[1024];
                int bytesRead;
                
                while ((bytesRead = reader.read(buffer)) != -1) {
                    content.append(buffer, 0, bytesRead);
                }
                
                String configsJson = content.toString();
                if (!configsJson.isEmpty()) {
                    parseServiceConfigsJson(configsJson, configs);
                    logger.info("SettingsManager", "Loaded service configs from file: " + serviceConfigFile);
                }
            } catch (IOException e) {
                logger.error("SettingsManager", "Error loading service configs from file: " + e.getMessage(), e);
            }
        } else {
            logger.info("SettingsManager", "No saved service configurations found");
        }

        return configs;
    }
    
    /**
     * Parse service configs JSON string
     * @param configsJson JSON string containing service configs
     * @param configs Map to populate with parsed configs
     */
    private void parseServiceConfigsJson(String configsJson, Map<String, ServiceConfig> configs) {
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
    }

    /**
     * Save service configurations
     * @param configs Map of service ID to service configuration
     */
    public void saveServiceConfigs(Map<String, ServiceConfig> configs) {
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

        // Save as a JSON string to file
        String configsJson = jsonConfigs.toString(2); // Pretty print with 2-space indentation
        
        try (FileWriter writer = new FileWriter(serviceConfigFile.toFile())) {
            writer.write(configsJson);
            logger.info("SettingsManager", "Service configurations saved to file successfully");
        } catch (IOException e) {
            logger.error("SettingsManager", "Error saving service configurations to file: " + e.getMessage(), e);
        }
    }

    /**
     * Get the thread pool size
     * @return Thread pool size
     */
    public int getThreadPoolSize() {
        JSONObject settings = loadSettingsFromFile();
        if (settings.has("threadPoolSize")) {
            return settings.getInt("threadPoolSize");
        }
        
        // Default value if not found
        int defaultSize = 10;
        logger.warning("SettingsManager", "Thread pool size not found, using default: " + defaultSize);
        return defaultSize;
    }

    /**
     * Set the thread pool size
     * @param size Thread pool size
     */
    public void setThreadPoolSize(int size) {
        JSONObject settings = loadSettingsFromFile();
        settings.put("threadPoolSize", size);
        saveSettingsToFile(settings);
        logger.info("SettingsManager", "Thread pool size updated to: " + size);
    }

    /**
     * Get the queue size
     * @return Queue size
     */
    public int getQueueSize() {
        JSONObject settings = loadSettingsFromFile();
        if (settings.has("queueSize")) {
            return settings.getInt("queueSize");
        }
        
        // Default value if not found
        int defaultSize = 100;
        logger.warning("SettingsManager", "Queue size not found, using default: " + defaultSize);
        return defaultSize;
    }

    /**
     * Set the queue size
     * @param size Queue size
     */
    public void setQueueSize(int size) {
        JSONObject settings = loadSettingsFromFile();
        settings.put("queueSize", size);
        saveSettingsToFile(settings);
        logger.info("SettingsManager", "Queue size updated to: " + size);
    }

    /**
     * Get the high load threshold
     * @return High load threshold
     */
    public int getHighLoadThreshold() {
        JSONObject settings = loadSettingsFromFile();
        if (settings.has("highLoadThreshold")) {
            return settings.getInt("highLoadThreshold");
        }
        
        // Default value if not found
        int defaultThreshold = 50;
        logger.warning("SettingsManager", "High load threshold not found, using default: " + defaultThreshold);
        return defaultThreshold;
    }

    /**
     * Set the high load threshold
     * @param threshold High load threshold
     */
    public void setHighLoadThreshold(int threshold) {
        JSONObject settings = loadSettingsFromFile();
        settings.put("highLoadThreshold", threshold);
        saveSettingsToFile(settings);
        logger.info("SettingsManager", "High load threshold updated to: " + threshold);
    }
}