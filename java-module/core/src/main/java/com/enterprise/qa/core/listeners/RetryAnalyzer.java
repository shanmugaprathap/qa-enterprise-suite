package com.enterprise.qa.core.listeners;

import com.enterprise.qa.core.config.ConfigManager;
import lombok.extern.slf4j.Slf4j;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Retry analyzer for automatically retrying failed tests.
 * Useful for handling flaky tests caused by timing issues or external dependencies.
 */
@Slf4j
public class RetryAnalyzer implements IRetryAnalyzer {

    private static final ConcurrentHashMap<String, AtomicInteger> retryCountMap = new ConcurrentHashMap<>();
    private final int maxRetryCount;

    public RetryAnalyzer() {
        this.maxRetryCount = ConfigManager.getInstance().getInt("retry.max.attempts", 2);
    }

    @Override
    public boolean retry(ITestResult result) {
        String testId = getTestId(result);

        AtomicInteger retryCount = retryCountMap.computeIfAbsent(testId, k -> new AtomicInteger(0));
        int currentCount = retryCount.get();

        if (currentCount < maxRetryCount) {
            retryCount.incrementAndGet();
            log.warn("Retrying test '{}' - attempt {} of {}",
                    getTestName(result),
                    currentCount + 1,
                    maxRetryCount);
            return true;
        }

        log.error("Test '{}' failed after {} retry attempts",
                getTestName(result),
                maxRetryCount);
        return false;
    }

    /**
     * Gets a unique identifier for the test method.
     */
    private String getTestId(ITestResult result) {
        return result.getTestClass().getName() + "." + result.getMethod().getMethodName();
    }

    /**
     * Gets a human-readable test name.
     */
    private String getTestName(ITestResult result) {
        return result.getTestClass().getRealClass().getSimpleName() +
               "." + result.getMethod().getMethodName();
    }

    /**
     * Resets the retry count for all tests.
     * Call this at the start of a test suite if needed.
     */
    public static void resetRetryCounts() {
        retryCountMap.clear();
    }

    /**
     * Gets the current retry count for a specific test.
     *
     * @param testClass  the test class name
     * @param testMethod the test method name
     * @return the current retry count
     */
    public static int getRetryCount(String testClass, String testMethod) {
        String testId = testClass + "." + testMethod;
        AtomicInteger count = retryCountMap.get(testId);
        return count != null ? count.get() : 0;
    }
}
