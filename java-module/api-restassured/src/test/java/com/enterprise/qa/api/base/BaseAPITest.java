package com.enterprise.qa.api.base;

import com.enterprise.qa.api.client.ApiClient;
import com.enterprise.qa.core.config.ConfigManager;
import com.enterprise.qa.core.listeners.TestListener;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.*;

/**
 * Base class for all API tests.
 * Provides common setup, teardown, and utilities for API testing.
 */
@Slf4j
@Listeners({TestListener.class})
public abstract class BaseAPITest {

    protected ApiClient apiClient;
    protected ConfigManager config;

    @BeforeClass(alwaysRun = true)
    public void setUpClass() {
        config = ConfigManager.getInstance();

        // Configure REST Assured
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());

        // Initialize API client
        apiClient = new ApiClient();

        log.info("API Test class setup complete: {}", this.getClass().getSimpleName());
        log.info("API Base URL: {}", config.getApiBaseUrl());
    }

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        // Reset any test-specific configurations
        log.debug("API Test setup complete");
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        // Cleanup after each test if needed
        log.debug("API Test teardown complete");
    }

    @AfterClass(alwaysRun = true)
    public void tearDownClass() {
        log.info("API Test class teardown: {}", this.getClass().getSimpleName());
    }

    /**
     * Creates a new API client with the default base URL.
     *
     * @return a new ApiClient
     */
    protected ApiClient createClient() {
        return new ApiClient();
    }

    /**
     * Creates a new API client with a custom base URL.
     *
     * @param baseUrl the custom base URL
     * @return a new ApiClient
     */
    protected ApiClient createClient(String baseUrl) {
        return new ApiClient(baseUrl);
    }

    /**
     * Creates an authenticated API client.
     *
     * @param token the bearer token
     * @return an authenticated ApiClient
     */
    protected ApiClient createAuthenticatedClient(String token) {
        return apiClient.withBearerToken(token);
    }
}
