package com.enterprise.qa.core.config;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Singleton configuration manager for loading and accessing test configuration.
 * Supports environment-specific configurations (qa, staging, prod) and system property overrides.
 */
@Slf4j
public class ConfigManager {

    private static ConfigManager instance;
    private final Properties properties;
    private final Environment environment;

    private ConfigManager() {
        this.properties = new Properties();
        this.environment = determineEnvironment();
        loadProperties();
    }

    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    private Environment determineEnvironment() {
        String envName = System.getProperty("env", System.getenv().getOrDefault("TEST_ENV", "qa"));
        try {
            return Environment.valueOf(envName.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown environment '{}', defaulting to QA", envName);
            return Environment.QA;
        }
    }

    private void loadProperties() {
        // Load default config
        loadPropertiesFile("config.properties");

        // Load environment-specific config (overrides defaults)
        String envConfigFile = String.format("config-%s.properties", environment.name().toLowerCase());
        loadPropertiesFile(envConfigFile);

        log.info("Configuration loaded for environment: {}", environment);
    }

    private void loadPropertiesFile(String fileName) {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(fileName)) {
            if (input != null) {
                properties.load(input);
                log.debug("Loaded configuration from: {}", fileName);
            } else {
                log.debug("Configuration file not found: {}", fileName);
            }
        } catch (IOException e) {
            log.error("Error loading configuration file: {}", fileName, e);
        }
    }

    /**
     * Gets a configuration value. Checks system properties first, then loaded properties.
     *
     * @param key the configuration key
     * @return the value, or null if not found
     */
    public String get(String key) {
        // System properties take precedence
        String systemValue = System.getProperty(key);
        if (systemValue != null) {
            return systemValue;
        }
        return properties.getProperty(key);
    }

    /**
     * Gets a configuration value with a default fallback.
     *
     * @param key          the configuration key
     * @param defaultValue the default value if key is not found
     * @return the value, or defaultValue if not found
     */
    public String get(String key, String defaultValue) {
        String value = get(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Gets a configuration value as an integer.
     *
     * @param key          the configuration key
     * @param defaultValue the default value if key is not found or cannot be parsed
     * @return the integer value
     */
    public int getInt(String key, int defaultValue) {
        String value = get(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                log.warn("Cannot parse '{}' as integer for key '{}'", value, key);
            }
        }
        return defaultValue;
    }

    /**
     * Gets a configuration value as a long.
     *
     * @param key          the configuration key
     * @param defaultValue the default value if key is not found or cannot be parsed
     * @return the long value
     */
    public long getLong(String key, long defaultValue) {
        String value = get(key);
        if (value != null) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                log.warn("Cannot parse '{}' as long for key '{}'", value, key);
            }
        }
        return defaultValue;
    }

    /**
     * Gets a configuration value as a boolean.
     *
     * @param key          the configuration key
     * @param defaultValue the default value if key is not found
     * @return the boolean value
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        return defaultValue;
    }

    /**
     * Gets the current environment.
     *
     * @return the current environment
     */
    public Environment getEnvironment() {
        return environment;
    }

    /**
     * Gets the base URL for the current environment.
     *
     * @return the base URL
     */
    public String getBaseUrl() {
        return get("base.url", "http://localhost");
    }

    /**
     * Gets the API base URL for the current environment.
     *
     * @return the API base URL
     */
    public String getApiBaseUrl() {
        return get("api.base.url", getBaseUrl() + "/api");
    }

    /**
     * Gets the browser to use for UI tests.
     *
     * @return the browser name (chrome, firefox, edge)
     */
    public String getBrowser() {
        return get("browser", "chrome").toLowerCase();
    }

    /**
     * Checks if headless mode is enabled.
     *
     * @return true if headless mode is enabled
     */
    public boolean isHeadless() {
        return getBoolean("headless", false);
    }

    /**
     * Gets the implicit wait timeout in seconds.
     *
     * @return the implicit wait timeout
     */
    public int getImplicitWait() {
        return getInt("implicit.wait.seconds", 10);
    }

    /**
     * Gets the explicit wait timeout in seconds.
     *
     * @return the explicit wait timeout
     */
    public int getExplicitWait() {
        return getInt("explicit.wait.seconds", 30);
    }

    /**
     * Gets the page load timeout in seconds.
     *
     * @return the page load timeout
     */
    public int getPageLoadTimeout() {
        return getInt("page.load.timeout.seconds", 60);
    }

    /**
     * Gets the Selenium Grid URL if configured.
     *
     * @return the grid URL, or null if not using grid
     */
    public String getGridUrl() {
        return get("grid.url");
    }

    /**
     * Checks if Selenium Grid should be used.
     *
     * @return true if grid URL is configured
     */
    public boolean useGrid() {
        return getGridUrl() != null && !getGridUrl().isEmpty();
    }

    /**
     * Gets the OpenAI API key.
     *
     * @return the API key, or null if not configured
     */
    public String getOpenAiApiKey() {
        String key = get("openai.api.key");
        if (key == null || key.isEmpty()) {
            key = System.getenv("OPENAI_API_KEY");
        }
        return key;
    }

    /**
     * Gets the Report Portal endpoint.
     *
     * @return the RP endpoint
     */
    public String getReportPortalEndpoint() {
        String endpoint = get("rp.endpoint");
        if (endpoint == null || endpoint.isEmpty()) {
            endpoint = System.getenv("RP_ENDPOINT");
        }
        return endpoint;
    }

    /**
     * Gets the Report Portal API key.
     *
     * @return the RP API key
     */
    public String getReportPortalApiKey() {
        String key = get("rp.api.key");
        if (key == null || key.isEmpty()) {
            key = System.getenv("RP_API_KEY");
        }
        return key;
    }

    /**
     * Gets the Report Portal project name.
     *
     * @return the RP project name
     */
    public String getReportPortalProject() {
        String project = get("rp.project");
        if (project == null || project.isEmpty()) {
            project = System.getenv("RP_PROJECT");
        }
        return project != null ? project : "qa-enterprise-suite";
    }

    /**
     * Checks if self-healing is enabled.
     *
     * @return true if self-healing is enabled
     */
    public boolean isSelfHealingEnabled() {
        return getBoolean("selfhealing.enabled", true);
    }

    /**
     * Checks if AI test data generation is enabled.
     *
     * @return true if AI data generation is enabled
     */
    public boolean isAiDataGenerationEnabled() {
        return getBoolean("ai.datagen.enabled", false) && getOpenAiApiKey() != null;
    }

    /**
     * Gets the screenshot directory path.
     *
     * @return the screenshot directory
     */
    public String getScreenshotDir() {
        return get("screenshot.dir", "target/screenshots");
    }

    /**
     * Resets the singleton instance (useful for testing).
     */
    public static synchronized void reset() {
        instance = null;
    }
}
