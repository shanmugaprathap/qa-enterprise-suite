package com.enterprise.qa.core.listeners;

import com.enterprise.qa.core.config.ConfigManager;
import com.enterprise.qa.core.reporting.ReportPortalClient;
import lombok.extern.slf4j.Slf4j;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * TestNG listener for Report Portal integration.
 * Sends test execution events to Report Portal for centralized reporting.
 */
@Slf4j
public class ReportPortalListener implements ITestListener {

    private final ReportPortalClient rpClient;
    private final boolean enabled;
    private String launchId;

    public ReportPortalListener() {
        ConfigManager config = ConfigManager.getInstance();
        this.enabled = config.getReportPortalEndpoint() != null &&
                       config.getReportPortalApiKey() != null;

        if (enabled) {
            this.rpClient = new ReportPortalClient();
            log.info("Report Portal integration enabled");
        } else {
            this.rpClient = null;
            log.debug("Report Portal integration disabled (endpoint or API key not configured)");
        }
    }

    @Override
    public void onStart(ITestContext context) {
        if (!enabled || rpClient == null) {
            return;
        }

        try {
            // Start a new launch
            launchId = rpClient.startLaunch(
                    context.getName(),
                    "Automated test execution: " + context.getName()
            );
            log.info("Report Portal launch started: {}", launchId);

        } catch (Exception e) {
            log.error("Failed to start Report Portal launch: {}", e.getMessage());
        }
    }

    @Override
    public void onFinish(ITestContext context) {
        if (!enabled || rpClient == null || launchId == null) {
            return;
        }

        try {
            rpClient.finishLaunch(launchId);
            log.info("Report Portal launch finished: {}", launchId);

        } catch (Exception e) {
            log.error("Failed to finish Report Portal launch: {}", e.getMessage());
        }
    }

    @Override
    public void onTestStart(ITestResult result) {
        if (!enabled || rpClient == null || launchId == null) {
            return;
        }

        try {
            String testName = getTestName(result);
            String itemId = rpClient.startTestItem(launchId, testName, "test");

            // Store item ID in test context for later use
            result.setAttribute("rpItemId", itemId);

            log.debug("Report Portal test item started: {}", testName);

        } catch (Exception e) {
            log.error("Failed to start Report Portal test item: {}", e.getMessage());
        }
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        finishTestItem(result, "PASSED", null);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        String errorMessage = result.getThrowable() != null ?
                result.getThrowable().getMessage() : "Test failed";
        finishTestItem(result, "FAILED", errorMessage);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        String skipReason = result.getThrowable() != null ?
                result.getThrowable().getMessage() : "Test skipped";
        finishTestItem(result, "SKIPPED", skipReason);
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        finishTestItem(result, "PASSED", "Passed within success percentage");
    }

    /**
     * Finishes a test item in Report Portal.
     */
    private void finishTestItem(ITestResult result, String status, String message) {
        if (!enabled || rpClient == null) {
            return;
        }

        String itemId = (String) result.getAttribute("rpItemId");
        if (itemId == null) {
            return;
        }

        try {
            // Log message if present
            if (message != null) {
                rpClient.logMessage(itemId, message,
                        "FAILED".equals(status) ? "ERROR" : "INFO");
            }

            // Finish the item
            rpClient.finishTestItem(itemId, status);

            log.debug("Report Portal test item finished: {} - {}",
                    getTestName(result), status);

        } catch (Exception e) {
            log.error("Failed to finish Report Portal test item: {}", e.getMessage());
        }
    }

    /**
     * Gets a formatted test name.
     */
    private String getTestName(ITestResult result) {
        return result.getTestClass().getRealClass().getSimpleName() +
               "." + result.getMethod().getMethodName();
    }
}
