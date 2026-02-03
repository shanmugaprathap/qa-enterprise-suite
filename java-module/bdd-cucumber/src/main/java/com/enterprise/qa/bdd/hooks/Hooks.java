package com.enterprise.qa.bdd.hooks;

import com.enterprise.qa.core.drivers.DriverManager;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import lombok.extern.slf4j.Slf4j;

/**
 * Cucumber hooks for setup and teardown.
 */
@Slf4j
public class Hooks {

    @Before
    public void setUp(Scenario scenario) {
        log.info("Starting scenario: {}", scenario.getName());

        // Initialize the driver
        DriverManager.initDriver();
    }

    @After
    public void tearDown(Scenario scenario) {
        log.info("Finishing scenario: {} - Status: {}",
                scenario.getName(), scenario.getStatus());

        // Take screenshot on failure
        if (scenario.isFailed()) {
            try {
                byte[] screenshot = DriverManager.takeScreenshot();
                scenario.attach(screenshot, "image/png", "Screenshot on Failure");
                log.info("Screenshot captured for failed scenario");
            } catch (Exception e) {
                log.error("Failed to capture screenshot: {}", e.getMessage());
            }
        }

        // Quit the driver
        DriverManager.quitDriver();
    }
}
