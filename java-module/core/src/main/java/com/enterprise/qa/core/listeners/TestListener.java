package com.enterprise.qa.core.listeners;

import com.enterprise.qa.core.config.ConfigManager;
import com.enterprise.qa.core.drivers.DriverManager;
import io.qameta.allure.Allure;
import lombok.extern.slf4j.Slf4j;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * TestNG listener for test execution events.
 * Handles screenshot capture on failure, Allure attachments, and logging.
 */
@Slf4j
public class TestListener implements ITestListener {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Override
    public void onStart(ITestContext context) {
        log.info("========================================");
        log.info("Starting test suite: {}", context.getName());
        log.info("Environment: {}", ConfigManager.getInstance().getEnvironment());
        log.info("Browser: {}", ConfigManager.getInstance().getBrowser());
        log.info("Headless: {}", ConfigManager.getInstance().isHeadless());
        log.info("========================================");

        // Create screenshot directory if needed
        createScreenshotDirectory();
    }

    @Override
    public void onFinish(ITestContext context) {
        log.info("========================================");
        log.info("Finished test suite: {}", context.getName());
        log.info("Passed: {}", context.getPassedTests().size());
        log.info("Failed: {}", context.getFailedTests().size());
        log.info("Skipped: {}", context.getSkippedTests().size());
        log.info("========================================");
    }

    @Override
    public void onTestStart(ITestResult result) {
        String testName = getTestName(result);
        log.info(">>> Starting test: {}", testName);

        // Add test name to Allure
        Allure.getLifecycle().updateTestCase(testCase ->
                testCase.setName(testName));
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        log.info("<<< PASSED: {} ({}ms)",
                getTestName(result),
                result.getEndMillis() - result.getStartMillis());
    }

    @Override
    public void onTestFailure(ITestResult result) {
        String testName = getTestName(result);
        log.error("<<< FAILED: {} - {}",
                testName,
                result.getThrowable().getMessage());

        // Capture screenshot on failure
        captureScreenshotOnFailure(result);

        // Log stack trace
        log.debug("Stack trace:", result.getThrowable());
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        String testName = getTestName(result);
        log.warn("<<< SKIPPED: {}", testName);

        if (result.getThrowable() != null) {
            log.warn("Skip reason: {}", result.getThrowable().getMessage());
        }
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        log.warn("<<< PARTIALLY PASSED: {} (within success percentage)",
                getTestName(result));
    }

    /**
     * Captures a screenshot when a test fails.
     */
    private void captureScreenshotOnFailure(ITestResult result) {
        if (!DriverManager.hasDriver()) {
            log.debug("No driver available for screenshot");
            return;
        }

        try {
            byte[] screenshot = DriverManager.takeScreenshot();
            if (screenshot.length > 0) {
                // Save to file
                String fileName = generateScreenshotFileName(result);
                Path screenshotPath = saveScreenshot(screenshot, fileName);
                log.info("Screenshot saved: {}", screenshotPath);

                // Attach to Allure report
                Allure.addAttachment(
                        "Screenshot on Failure",
                        "image/png",
                        new ByteArrayInputStream(screenshot),
                        "png"
                );

                // Attach page source for debugging
                attachPageSource();
            }
        } catch (Exception e) {
            log.error("Failed to capture screenshot: {}", e.getMessage());
        }
    }

    /**
     * Attaches the current page source to Allure.
     */
    private void attachPageSource() {
        try {
            String pageSource = DriverManager.getDriver().getPageSource();
            Allure.addAttachment(
                    "Page Source",
                    "text/html",
                    pageSource
            );
        } catch (Exception e) {
            log.debug("Could not attach page source: {}", e.getMessage());
        }
    }

    /**
     * Saves a screenshot to the configured directory.
     */
    private Path saveScreenshot(byte[] screenshot, String fileName) throws IOException {
        String screenshotDir = ConfigManager.getInstance().getScreenshotDir();
        Path filePath = Paths.get(screenshotDir, fileName);
        Files.write(filePath, screenshot);
        return filePath;
    }

    /**
     * Generates a unique screenshot file name.
     */
    private String generateScreenshotFileName(ITestResult result) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String testName = result.getMethod().getMethodName();
        return String.format("%s_%s.png", testName, timestamp);
    }

    /**
     * Creates the screenshot directory if it doesn't exist.
     */
    private void createScreenshotDirectory() {
        String screenshotDir = ConfigManager.getInstance().getScreenshotDir();
        File dir = new File(screenshotDir);
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                log.debug("Created screenshot directory: {}", screenshotDir);
            }
        }
    }

    /**
     * Gets a formatted test name including class and method.
     */
    private String getTestName(ITestResult result) {
        String className = result.getTestClass().getRealClass().getSimpleName();
        String methodName = result.getMethod().getMethodName();

        // Include parameters if any
        Object[] params = result.getParameters();
        if (params != null && params.length > 0) {
            StringBuilder sb = new StringBuilder(className)
                    .append(".")
                    .append(methodName)
                    .append("(");
            for (int i = 0; i < params.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(params[i]);
            }
            sb.append(")");
            return sb.toString();
        }

        return className + "." + methodName;
    }
}
