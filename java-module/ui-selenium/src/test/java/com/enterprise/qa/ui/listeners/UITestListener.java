package com.enterprise.qa.ui.listeners;

import com.enterprise.qa.core.listeners.TestListener;
import lombok.extern.slf4j.Slf4j;
import org.testng.ITestResult;

/**
 * Extended test listener for UI-specific functionality.
 * Inherits base listener behavior and adds UI-specific handling.
 */
@Slf4j
public class UITestListener extends TestListener {

    @Override
    public void onTestStart(ITestResult result) {
        super.onTestStart(result);

        // Log browser info if available
        log.debug("Starting UI test in browser context");
    }

    @Override
    public void onTestFailure(ITestResult result) {
        super.onTestFailure(result);

        // Additional UI-specific failure handling
        log.debug("UI test failed - screenshot should be captured by parent listener");
    }
}
