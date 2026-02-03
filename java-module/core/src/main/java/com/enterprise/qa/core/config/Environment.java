package com.enterprise.qa.core.config;

/**
 * Enumeration of supported test environments.
 */
public enum Environment {
    /**
     * Local development environment
     */
    LOCAL("local", "http://localhost:8080"),

    /**
     * QA/Test environment
     */
    QA("qa", "https://qa.example.com"),

    /**
     * Staging/Pre-production environment
     */
    STAGING("staging", "https://staging.example.com"),

    /**
     * Production environment
     */
    PROD("prod", "https://www.example.com");

    private final String name;
    private final String defaultBaseUrl;

    Environment(String name, String defaultBaseUrl) {
        this.name = name;
        this.defaultBaseUrl = defaultBaseUrl;
    }

    /**
     * Gets the environment name.
     *
     * @return the environment name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the default base URL for this environment.
     *
     * @return the default base URL
     */
    public String getDefaultBaseUrl() {
        return defaultBaseUrl;
    }

    /**
     * Checks if this is a production environment.
     *
     * @return true if this is the production environment
     */
    public boolean isProduction() {
        return this == PROD;
    }

    /**
     * Checks if this is a local development environment.
     *
     * @return true if this is the local environment
     */
    public boolean isLocal() {
        return this == LOCAL;
    }

    @Override
    public String toString() {
        return name;
    }
}
