package com.enterprise.qa.ui.base;

import com.enterprise.qa.core.config.ConfigManager;
import com.enterprise.qa.core.drivers.DriverManager;
import com.enterprise.qa.core.listeners.RetryAnalyzer;
import com.enterprise.qa.core.listeners.TestListener;
import io.qameta.allure.Allure;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;
import org.testng.annotations.*;

import java.io.ByteArrayInputStream;

/**
 * Base class for all UI tests.
 * Handles driver initialization, cleanup, and common test setup.
 */
@Slf4j
@Listeners({TestListener.class})
public abstract class BaseUITest {

    protected WebDriver driver;
    protected ConfigManager config;

    @BeforeClass(alwaysRun = true)
    public void setUpClass() {
        config = ConfigManager.getInstance();
        log.info("Test class setup: {}", this.getClass().getSimpleName());
    }

    @BeforeMethod(alwaysRun = true)
    public void setUp(ITestResult result) {
        log.info("Setting up test: {}", result.getMethod().getMethodName());

        // Initialize the driver
        DriverManager.initDriver();
        driver = DriverManager.getDriver();

        // Navigate to base URL if configured
        String baseUrl = config.getBaseUrl();
        if (baseUrl != null && !baseUrl.isEmpty()) {
            driver.get(baseUrl);
            log.info("Navigated to base URL: {}", baseUrl);
        }
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        log.info("Tearing down test: {} - Status: {}",
                result.getMethod().getMethodName(),
                getStatusName(result.getStatus()));

        // Capture screenshot on failure
        if (result.getStatus() == ITestResult.FAILURE) {
            captureScreenshotToAllure("Screenshot on Failure");
        }

        // Quit the driver
        DriverManager.quitDriver();
    }

    @AfterClass(alwaysRun = true)
    public void tearDownClass() {
        log.info("Test class teardown: {}", this.getClass().getSimpleName());
    }

    /**
     * Gets the WebDriver instance.
     *
     * @return the WebDriver
     */
    protected WebDriver getDriver() {
        return driver;
    }

    /**
     * Navigates to a URL.
     *
     * @param url the URL to navigate to
     */
    protected void navigateTo(String url) {
        driver.get(url);
        log.debug("Navigated to: {}", url);
    }

    /**
     * Navigates to a path relative to the base URL.
     *
     * @param path the relative path
     */
    protected void navigateToPath(String path) {
        String baseUrl = config.getBaseUrl();
        String fullUrl = baseUrl.endsWith("/") ? baseUrl + path : baseUrl + "/" + path;
        navigateTo(fullUrl);
    }

    /**
     * Gets the current URL.
     *
     * @return the current URL
     */
    protected String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    /**
     * Gets the page title.
     *
     * @return the page title
     */
    protected String getPageTitle() {
        return driver.getTitle();
    }

    /**
     * Captures a screenshot and attaches it to Allure report.
     *
     * @param name the attachment name
     */
    protected void captureScreenshotToAllure(String name) {
        byte[] screenshot = DriverManager.takeScreenshot();
        if (screenshot.length > 0) {
            Allure.addAttachment(name, "image/png",
                    new ByteArrayInputStream(screenshot), "png");
        }
    }

    /**
     * Attaches text to the Allure report.
     *
     * @param name    the attachment name
     * @param content the text content
     */
    protected void attachTextToAllure(String name, String content) {
        Allure.addAttachment(name, "text/plain", content);
    }

    /**
     * Converts a test status code to a name.
     *
     * @param status the status code
     * @return the status name
     */
    private String getStatusName(int status) {
        return switch (status) {
            case ITestResult.SUCCESS -> "PASSED";
            case ITestResult.FAILURE -> "FAILED";
            case ITestResult.SKIP -> "SKIPPED";
            default -> "UNKNOWN";
        };
    }

    /**
     * Gets the retry analyzer for flaky tests.
     *
     * @return the RetryAnalyzer class
     */
    protected Class<? extends RetryAnalyzer> getRetryAnalyzer() {
        return RetryAnalyzer.class;
    }
}
