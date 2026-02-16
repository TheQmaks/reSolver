package cli.li.resolver.settings;

import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.IOException;

import cli.li.resolver.logger.LoggerService;
import cli.li.resolver.provider.ProviderConfig;

/**
 * Manager for extension settings.
 * Uses in-memory cache - loads settings once in constructor.
 * Getters read from memory, setters update memory and file.
 * Uses manual JSON building/parsing (no org.json).
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

    // In-memory cache of settings JSON
    private volatile String cachedSettings;

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

        // Load settings into memory cache
        cachedSettings = loadRawFromFile(settingsFile);

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
     * Load raw file content as string
     * @param path Path to the file
     * @return File content or empty string if file does not exist or an error occurs
     */
    private String loadRawFromFile(Path path) {
        if (!Files.exists(path)) {
            return "";
        }
        try {
            return Files.readString(path);
        } catch (IOException e) {
            logger.error("SettingsManager", "Error reading file " + path + ": " + e.getMessage(), e);
            return "";
        }
    }

    /**
     * Write raw content to a file
     * @param path Path to the file
     * @param content Content to write
     */
    private void writeRawToFile(Path path, String content) {
        try {
            Files.writeString(path, content);
        } catch (IOException e) {
            logger.error("SettingsManager", "Error writing file " + path + ": " + e.getMessage(), e);
        }
    }

    /**
     * Initialize default settings if they don't exist in the cached settings
     */
    private void initializeDefaultSettings() {
        boolean modified = false;

        if (getJsonIntValue(cachedSettings, "threadPoolSize") == null) {
            cachedSettings = setJsonIntValue(cachedSettings, "threadPoolSize", 10);
            modified = true;
            logger.info("SettingsManager", "Initialized default thread pool size: 10");
        } else {
            logger.debug("SettingsManager", "Thread pool size already set: " +
                    getJsonIntValue(cachedSettings, "threadPoolSize"));
        }

        if (getJsonIntValue(cachedSettings, "highLoadThreshold") == null) {
            cachedSettings = setJsonIntValue(cachedSettings, "highLoadThreshold", 50);
            modified = true;
            logger.info("SettingsManager", "Initialized default high load threshold: 50");
        } else {
            logger.debug("SettingsManager", "High load threshold already set: " +
                    getJsonIntValue(cachedSettings, "highLoadThreshold"));
        }

        if (getJsonIntValue(cachedSettings, "solveTimeout") == null) {
            cachedSettings = setJsonIntValue(cachedSettings, "solveTimeout", 120);
            modified = true;
            logger.info("SettingsManager", "Initialized default solve timeout: 120");
        }

        if (getJsonIntValue(cachedSettings, "maxRetries") == null) {
            cachedSettings = setJsonIntValue(cachedSettings, "maxRetries", 2);
            modified = true;
            logger.info("SettingsManager", "Initialized default max retries: 2");
        }

        if (getJsonStringValue(cachedSettings, "autoDetectionEnabled") == null &&
                !cachedSettings.contains("\"autoDetectionEnabled\"")) {
            cachedSettings = setJsonBoolValue(cachedSettings, "autoDetectionEnabled", true);
            modified = true;
            logger.info("SettingsManager", "Initialized default auto detection: enabled");
        }

        if (getJsonStringValue(cachedSettings, "logLevel") == null) {
            cachedSettings = setJsonStringValue(cachedSettings, "logLevel", "INFO");
            modified = true;
            logger.info("SettingsManager", "Initialized default log level: INFO");
        }

        if (modified) {
            writeRawToFile(settingsFile, cachedSettings);
            logger.info("SettingsManager", "Settings saved to file: " + settingsFile);
        }
    }

    // ---- Manual JSON parsing helpers ----

    /**
     * Get an integer value from a JSON string by key
     * @param json JSON string
     * @param key Key to look up
     * @return Integer value or null if not found
     */
    private Integer getJsonIntValue(String json, String key) {
        if (json == null || json.isEmpty()) return null;
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex < 0) return null;

        int colonIndex = json.indexOf(':', keyIndex + searchKey.length());
        if (colonIndex < 0) return null;

        // Find the start of the value (skip whitespace)
        int valueStart = colonIndex + 1;
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }
        if (valueStart >= json.length()) return null;

        // Find the end of the value (number ends at comma, }, or whitespace)
        int valueEnd = valueStart;
        while (valueEnd < json.length()) {
            char c = json.charAt(valueEnd);
            if (c == ',' || c == '}' || c == ']' || Character.isWhitespace(c)) break;
            valueEnd++;
        }

        String valueStr = json.substring(valueStart, valueEnd).trim();
        try {
            return Integer.parseInt(valueStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Get a string value from a JSON string by key
     * @param json JSON string
     * @param key Key to look up
     * @return String value or null if not found
     */
    private String getJsonStringValue(String json, String key) {
        if (json == null || json.isEmpty()) return null;
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex < 0) return null;

        int colonIndex = json.indexOf(':', keyIndex + searchKey.length());
        if (colonIndex < 0) return null;

        int quoteStart = json.indexOf('"', colonIndex + 1);
        if (quoteStart < 0) return null;

        int quoteEnd = findClosingQuote(json, quoteStart + 1);
        if (quoteEnd < 0) return null;

        return unescapeJsonString(json.substring(quoteStart + 1, quoteEnd));
    }

    /**
     * Get a boolean value from a JSON string by key
     * @param json JSON string
     * @param key Key to look up
     * @return Boolean value or null if not found
     */
    private boolean getJsonBoolValue(String json, String key, boolean defaultValue) {
        if (json == null || json.isEmpty()) return defaultValue;
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex < 0) return defaultValue;

        int colonIndex = json.indexOf(':', keyIndex + searchKey.length());
        if (colonIndex < 0) return defaultValue;

        int valueStart = colonIndex + 1;
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }

        if (json.startsWith("true", valueStart)) return true;
        if (json.startsWith("false", valueStart)) return false;
        return defaultValue;
    }

    /**
     * Find the closing quote, respecting escaped quotes
     */
    private int findClosingQuote(String json, int startFrom) {
        for (int i = startFrom; i < json.length(); i++) {
            if (json.charAt(i) == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Unescape a JSON string (handle \\, \", \n, \t, etc.)
     */
    private String unescapeJsonString(String s) {
        if (s == null || !s.contains("\\")) return s;
        StringBuilder sb = new StringBuilder(s.length());
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                switch (next) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    default -> sb.append(c);
                }
                i += (next == '"' || next == '\\' || next == 'n' || next == 't' || next == 'r') ? 2 : 1;
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    /**
     * Escape a string for JSON value
     */
    private String escapeJsonString(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Set an integer value in a JSON string, creating the JSON object if needed
     * @param json Current JSON string (may be empty)
     * @param key Key to set
     * @param value Value to set
     * @return Updated JSON string
     */
    private String setJsonIntValue(String json, String key, int value) {
        Map<String, String> entries = parseJsonToRawEntries(json);
        entries.put(key, String.valueOf(value));
        return buildJsonFromRawEntries(entries);
    }

    /**
     * Set a string value in a JSON string, creating the JSON object if needed
     * @param json Current JSON string (may be empty)
     * @param key Key to set
     * @param value Value to set
     * @return Updated JSON string
     */
    private String setJsonStringValue(String json, String key, String value) {
        Map<String, String> entries = parseJsonToRawEntries(json);
        entries.put(key, "\"" + escapeJsonString(value) + "\"");
        return buildJsonFromRawEntries(entries);
    }

    /**
     * Set a boolean value in a JSON string, creating the JSON object if needed
     * @param json Current JSON string (may be empty)
     * @param key Key to set
     * @param value Value to set
     * @return Updated JSON string
     */
    private String setJsonBoolValue(String json, String key, boolean value) {
        Map<String, String> entries = parseJsonToRawEntries(json);
        entries.put(key, String.valueOf(value));
        return buildJsonFromRawEntries(entries);
    }

    /**
     * Parse a flat JSON object into a map of key -> raw value strings
     * (e.g., "10", "true", "\"hello\"")
     */
    private Map<String, String> parseJsonToRawEntries(String json) {
        Map<String, String> map = new LinkedHashMap<>();
        if (json == null || json.trim().isEmpty()) return map;

        String trimmed = json.trim();
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return map;

        // Remove outer braces
        String inner = trimmed.substring(1, trimmed.length() - 1).trim();
        if (inner.isEmpty()) return map;

        // Parse key-value pairs
        int i = 0;
        while (i < inner.length()) {
            // Skip whitespace
            while (i < inner.length() && Character.isWhitespace(inner.charAt(i))) i++;
            if (i >= inner.length()) break;

            // Expect opening quote for key
            if (inner.charAt(i) != '"') break;
            int keyStart = i + 1;
            int keyEnd = findClosingQuote(inner, keyStart);
            if (keyEnd < 0) break;
            String entryKey = inner.substring(keyStart, keyEnd);
            i = keyEnd + 1;

            // Skip whitespace and colon
            while (i < inner.length() && Character.isWhitespace(inner.charAt(i))) i++;
            if (i >= inner.length() || inner.charAt(i) != ':') break;
            i++;
            while (i < inner.length() && Character.isWhitespace(inner.charAt(i))) i++;
            if (i >= inner.length()) break;

            // Parse value
            String rawValue;
            if (inner.charAt(i) == '"') {
                // String value
                int valStart = i;
                int valEnd = findClosingQuote(inner, i + 1);
                if (valEnd < 0) break;
                rawValue = inner.substring(valStart, valEnd + 1);
                i = valEnd + 1;
            } else if (inner.charAt(i) == '{') {
                // Nested object - find matching closing brace
                int depth = 1;
                int valStart = i;
                i++;
                while (i < inner.length() && depth > 0) {
                    if (inner.charAt(i) == '{') {
                        depth++;
                    } else if (inner.charAt(i) == '}') {
                        depth--;
                    }
                    if (depth > 0) {
                        i++;
                    }
                }
                rawValue = inner.substring(valStart, i + 1);
                i++;
            } else {
                // Number, boolean, null
                int valStart = i;
                while (i < inner.length() && inner.charAt(i) != ',' &&
                       inner.charAt(i) != '}' && !Character.isWhitespace(inner.charAt(i))) {
                    i++;
                }
                rawValue = inner.substring(valStart, i);
            }

            map.put(entryKey, rawValue.trim());

            // Skip comma
            while (i < inner.length() && (Character.isWhitespace(inner.charAt(i)) || inner.charAt(i) == ',')) i++;
        }

        return map;
    }

    /**
     * Build a JSON string from raw entries map
     */
    private String buildJsonFromRawEntries(Map<String, String> entries) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        int count = 0;
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            sb.append("  \"").append(escapeJsonString(entry.getKey())).append("\": ")
              .append(entry.getValue());
            count++;
            if (count < entries.size()) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("}");
        return sb.toString();
    }

    // ---- Service Config Load/Save ----

    /**
     * Load service configurations
     * @return Map of service ID to provider configuration
     */
    public Map<String, ProviderConfig> loadServiceConfigs() {
        Map<String, ProviderConfig> configs = new HashMap<>();

        String raw = loadRawFromFile(serviceConfigFile);
        if (raw == null || raw.trim().isEmpty()) {
            logger.info("SettingsManager", "No saved service configurations found");
            return configs;
        }

        // Parse top-level JSON object: each key is a service ID, each value is an object
        Map<String, String> topLevel = parseJsonToRawEntries(raw);
        logger.debug("SettingsManager", "Found config for " + topLevel.size() + " services");

        for (Map.Entry<String, String> entry : topLevel.entrySet()) {
            String serviceId = entry.getKey();
            String serviceJson = entry.getValue();

            try {
                String apiKey = getJsonStringValue(serviceJson, "apiKey");
                if (apiKey == null) apiKey = "";
                boolean enabled = getJsonBoolValue(serviceJson, "enabled", false);
                Integer priority = getJsonIntValue(serviceJson, "priority");
                if (priority == null) priority = 0;

                configs.put(serviceId, new ProviderConfig(apiKey, enabled, priority));

                // Mask API key for logging
                String maskedApiKey = apiKey.isEmpty() ? "(empty)" :
                        apiKey.length() > 8 ?
                                apiKey.substring(0, 4) + "..." + apiKey.substring(apiKey.length() - 4) :
                                "****";

                logger.info("SettingsManager", "Loaded config for service: " + serviceId +
                        ", API key: " + maskedApiKey +
                        ", enabled: " + enabled +
                        ", priority: " + priority);
            } catch (Exception e) {
                logger.error("SettingsManager", "Error parsing config for service " +
                        serviceId + ": " + e.getMessage(), e);
            }
        }

        logger.info("SettingsManager", "Loaded service configs from file: " + serviceConfigFile);
        return configs;
    }

    /**
     * Save service configurations
     * @param configs Map of service ID to provider configuration
     */
    public void saveServiceConfigs(Map<String, ProviderConfig> configs) {
        logger.info("SettingsManager", "Saving service configurations for " + configs.size() + " services");

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        int count = 0;
        for (Map.Entry<String, ProviderConfig> entry : configs.entrySet()) {
            String serviceId = entry.getKey();
            ProviderConfig config = entry.getValue();

            sb.append("  \"").append(escapeJsonString(serviceId)).append("\": {\n");
            sb.append("    \"apiKey\": \"").append(escapeJsonString(config.apiKey())).append("\",\n");
            sb.append("    \"enabled\": ").append(config.enabled()).append(",\n");
            sb.append("    \"priority\": ").append(config.priority()).append("\n");
            sb.append("  }");

            count++;
            if (count < configs.size()) {
                sb.append(",");
            }
            sb.append("\n");

            // Mask API key for logging
            String maskedApiKey = config.apiKey().isEmpty() ? "(empty)" :
                    config.apiKey().length() > 8 ?
                            config.apiKey().substring(0, 4) + "..." +
                                    config.apiKey().substring(config.apiKey().length() - 4) :
                            "****";

            logger.info("SettingsManager", "Saving config for service: " + serviceId +
                    ", API key: " + maskedApiKey +
                    ", enabled: " + config.enabled() +
                    ", priority: " + config.priority());
        }
        sb.append("}");

        writeRawToFile(serviceConfigFile, sb.toString());
        logger.info("SettingsManager", "Service configurations saved to file successfully");
    }

    // ---- Settings Getters/Setters ----

    /**
     * Get the thread pool size
     * @return Thread pool size
     */
    public int getThreadPoolSize() {
        Integer value = getJsonIntValue(cachedSettings, "threadPoolSize");
        if (value != null) {
            return value;
        }
        int defaultSize = 10;
        logger.warning("SettingsManager", "Thread pool size not found, using default: " + defaultSize);
        return defaultSize;
    }

    /**
     * Set the thread pool size
     * @param size Thread pool size
     */
    public void setThreadPoolSize(int size) {
        cachedSettings = setJsonIntValue(cachedSettings, "threadPoolSize", size);
        writeRawToFile(settingsFile, cachedSettings);
        logger.info("SettingsManager", "Thread pool size updated to: " + size);
    }

    /**
     * Get the high load threshold
     * @return High load threshold
     */
    public int getHighLoadThreshold() {
        Integer value = getJsonIntValue(cachedSettings, "highLoadThreshold");
        if (value != null) {
            return value;
        }
        int defaultThreshold = 50;
        logger.warning("SettingsManager", "High load threshold not found, using default: " + defaultThreshold);
        return defaultThreshold;
    }

    /**
     * Set the high load threshold
     * @param threshold High load threshold
     */
    public void setHighLoadThreshold(int threshold) {
        cachedSettings = setJsonIntValue(cachedSettings, "highLoadThreshold", threshold);
        writeRawToFile(settingsFile, cachedSettings);
        logger.info("SettingsManager", "High load threshold updated to: " + threshold);
    }

    /**
     * Get the solve timeout in seconds
     * @return Solve timeout
     */
    public int getSolveTimeout() {
        Integer value = getJsonIntValue(cachedSettings, "solveTimeout");
        return value != null ? value : 120;
    }

    /**
     * Set the solve timeout in seconds
     * @param timeout Solve timeout
     */
    public void setSolveTimeout(int timeout) {
        cachedSettings = setJsonIntValue(cachedSettings, "solveTimeout", timeout);
        writeRawToFile(settingsFile, cachedSettings);
        logger.info("SettingsManager", "Solve timeout updated to: " + timeout);
    }

    /**
     * Get the max retries count
     * @return Max retries
     */
    public int getMaxRetries() {
        Integer value = getJsonIntValue(cachedSettings, "maxRetries");
        return value != null ? value : 2;
    }

    /**
     * Set the max retries count
     * @param retries Max retries
     */
    public void setMaxRetries(int retries) {
        cachedSettings = setJsonIntValue(cachedSettings, "maxRetries", retries);
        writeRawToFile(settingsFile, cachedSettings);
        logger.info("SettingsManager", "Max retries updated to: " + retries);
    }

    /**
     * Get whether auto-detection is enabled
     * @return true if auto-detection is enabled
     */
    public boolean isAutoDetectionEnabled() {
        return getJsonBoolValue(cachedSettings, "autoDetectionEnabled", true);
    }

    /**
     * Set whether auto-detection is enabled
     * @param enabled true to enable auto-detection
     */
    public void setAutoDetectionEnabled(boolean enabled) {
        cachedSettings = setJsonBoolValue(cachedSettings, "autoDetectionEnabled", enabled);
        writeRawToFile(settingsFile, cachedSettings);
        logger.info("SettingsManager", "Auto detection updated to: " + enabled);
    }

    /**
     * Get the log level
     * @return Log level string
     */
    public String getLogLevel() {
        String value = getJsonStringValue(cachedSettings, "logLevel");
        return value != null ? value : "INFO";
    }

    /**
     * Set the log level
     * @param level Log level string
     */
    public void setLogLevel(String level) {
        cachedSettings = setJsonStringValue(cachedSettings, "logLevel", level);
        writeRawToFile(settingsFile, cachedSettings);
        logger.info("SettingsManager", "Log level updated to: " + level);
    }
}
