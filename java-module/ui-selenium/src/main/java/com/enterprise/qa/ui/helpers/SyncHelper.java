package com.enterprise.qa.ui.helpers;

import com.enterprise.qa.core.config.ConfigManager;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

/**
 * Helper class for synchronization and waiting operations.
 * Provides various wait conditions for reliable test execution.
 */
@Slf4j
public class SyncHelper {

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final WebDriverWait shortWait;
    private final int explicitWaitSeconds;

    public SyncHelper(WebDriver driver) {
        this.driver = driver;
        this.explicitWaitSeconds = ConfigManager.getInstance().getExplicitWait();
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(explicitWaitSeconds));
        this.shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));
    }

    /**
     * Waits for an element to be visible.
     *
     * @param element the element to wait for
     * @return the element
     */
    public WebElement waitForElementVisible(WebElement element) {
        return wait.until(ExpectedConditions.visibilityOf(element));
    }

    /**
     * Waits for an element to be visible by locator.
     *
     * @param locator the element locator
     * @return the element
     */
    public WebElement waitForElementVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /**
     * Waits for an element to be visible with custom timeout.
     *
     * @param locator        the element locator
     * @param timeoutSeconds the timeout in seconds
     * @return the element
     */
    public WebElement waitForElementVisible(By locator, int timeoutSeconds) {
        WebDriverWait customWait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        return customWait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /**
     * Waits for an element to be clickable.
     *
     * @param element the element to wait for
     * @return the element
     */
    public WebElement waitForElementClickable(WebElement element) {
        return wait.until(ExpectedConditions.elementToBeClickable(element));
    }

    /**
     * Waits for an element to be clickable by locator.
     *
     * @param locator the element locator
     * @return the element
     */
    public WebElement waitForElementClickable(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    /**
     * Waits for an element to be present in the DOM.
     *
     * @param locator the element locator
     * @return the element
     */
    public WebElement waitForElementPresent(By locator) {
        return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    /**
     * Waits for all elements matching a locator to be visible.
     *
     * @param locator the element locator
     * @return list of elements
     */
    public List<WebElement> waitForAllElementsVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
    }

    /**
     * Waits for an element to be invisible.
     *
     * @param element the element to wait for
     * @return true if the element became invisible
     */
    public boolean waitForElementInvisible(WebElement element) {
        try {
            return wait.until(ExpectedConditions.invisibilityOf(element));
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * Waits for an element to be invisible by locator.
     *
     * @param locator the element locator
     * @return true if the element became invisible
     */
    public boolean waitForElementInvisible(By locator) {
        try {
            return wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * Waits for an alert to be present.
     *
     * @return the alert
     */
    public Alert waitForAlert() {
        return wait.until(ExpectedConditions.alertIsPresent());
    }

    /**
     * Waits for the page to fully load.
     */
    public void waitForPageLoad() {
        wait.until((ExpectedCondition<Boolean>) d -> {
            String readyState = ((JavascriptExecutor) d)
                    .executeScript("return document.readyState").toString();
            return "complete".equals(readyState);
        });
    }

    /**
     * Waits for all AJAX calls to complete (jQuery).
     */
    public void waitForAjax() {
        try {
            wait.until((ExpectedCondition<Boolean>) d -> {
                JavascriptExecutor js = (JavascriptExecutor) d;
                return (Boolean) js.executeScript(
                        "return (typeof jQuery === 'undefined') || (jQuery.active === 0)"
                );
            });
        } catch (Exception e) {
            log.debug("jQuery not present or AJAX wait failed: {}", e.getMessage());
        }
    }

    /**
     * Waits for Angular to finish loading.
     */
    public void waitForAngular() {
        try {
            String angularReadyScript =
                    "return (window.angular === undefined) || " +
                    "(angular.element(document.body).injector() === undefined) || " +
                    "(angular.element(document.body).injector().get('$http').pendingRequests.length === 0)";

            wait.until((ExpectedCondition<Boolean>) d ->
                    (Boolean) ((JavascriptExecutor) d).executeScript(angularReadyScript)
            );
        } catch (Exception e) {
            log.debug("Angular not present or wait failed: {}", e.getMessage());
        }
    }

    /**
     * Waits for a frame to be available and switches to it.
     *
     * @param locator the frame locator
     * @return the driver focused on the frame
     */
    public WebDriver waitForFrameAndSwitch(By locator) {
        return wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(locator));
    }

    /**
     * Waits for a frame to be available by index and switches to it.
     *
     * @param index the frame index
     * @return the driver focused on the frame
     */
    public WebDriver waitForFrameAndSwitch(int index) {
        return wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(index));
    }

    /**
     * Waits for the URL to contain a specific string.
     *
     * @param urlPart the expected URL part
     * @return true if the URL contains the string
     */
    public boolean waitForUrlContains(String urlPart) {
        try {
            return wait.until(ExpectedConditions.urlContains(urlPart));
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * Waits for the URL to match exactly.
     *
     * @param url the expected URL
     * @return true if the URL matches
     */
    public boolean waitForUrlToBe(String url) {
        try {
            return wait.until(ExpectedConditions.urlToBe(url));
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * Waits for the title to contain a specific string.
     *
     * @param titlePart the expected title part
     * @return true if the title contains the string
     */
    public boolean waitForTitleContains(String titlePart) {
        try {
            return wait.until(ExpectedConditions.titleContains(titlePart));
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * Waits for the title to match exactly.
     *
     * @param title the expected title
     * @return true if the title matches
     */
    public boolean waitForTitleToBe(String title) {
        try {
            return wait.until(ExpectedConditions.titleIs(title));
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * Waits for a specific number of windows to be open.
     *
     * @param count the expected number of windows
     * @return true if the condition is met
     */
    public boolean waitForWindowCount(int count) {
        try {
            return wait.until(ExpectedConditions.numberOfWindowsToBe(count));
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * Waits for a custom condition.
     *
     * @param condition the condition to wait for
     * @param <T>       the return type
     * @return the result of the condition
     */
    public <T> T waitFor(ExpectedCondition<T> condition) {
        return wait.until(condition);
    }

    /**
     * Waits for a custom condition with custom timeout.
     *
     * @param condition      the condition to wait for
     * @param timeoutSeconds the timeout in seconds
     * @param <T>            the return type
     * @return the result of the condition
     */
    public <T> T waitFor(ExpectedCondition<T> condition, int timeoutSeconds) {
        WebDriverWait customWait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        return customWait.until(condition);
    }

    /**
     * Waits for a custom function to return a non-null value.
     *
     * @param function the function to evaluate
     * @param <T>      the return type
     * @return the result of the function
     */
    public <T> T waitFor(Function<WebDriver, T> function) {
        return wait.until(function);
    }

    /**
     * Pauses execution for a specified time. Use sparingly - prefer explicit waits.
     *
     * @param milliseconds the time to wait in milliseconds
     */
    public void hardWait(long milliseconds) {
        log.debug("Hard wait for {} ms", milliseconds);
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Hard wait interrupted");
        }
    }

    /**
     * Checks if an element is visible without waiting.
     *
     * @param locator the element locator
     * @return true if the element is visible
     */
    public boolean isElementVisibleNow(By locator) {
        try {
            return shortWait.until(ExpectedConditions.visibilityOfElementLocated(locator)) != null;
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * Checks if an element is present in the DOM without waiting.
     *
     * @param locator the element locator
     * @return true if the element is present
     */
    public boolean isElementPresentNow(By locator) {
        return !driver.findElements(locator).isEmpty();
    }

    /**
     * Waits for a JavaScript variable to be defined.
     *
     * @param variableName the variable name
     * @return true if the variable is defined
     */
    public boolean waitForJsVariable(String variableName) {
        try {
            return wait.until((ExpectedCondition<Boolean>) d -> {
                Object result = ((JavascriptExecutor) d).executeScript(
                        "return typeof " + variableName + " !== 'undefined'"
                );
                return Boolean.TRUE.equals(result);
            });
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * Waits for a loading spinner to disappear.
     *
     * @param spinnerLocator the spinner element locator
     */
    public void waitForSpinnerToDisappear(By spinnerLocator) {
        // First wait for spinner to appear (briefly)
        try {
            shortWait.until(ExpectedConditions.visibilityOfElementLocated(spinnerLocator));
        } catch (TimeoutException e) {
            // Spinner may have already disappeared
        }

        // Then wait for it to disappear
        waitForElementInvisible(spinnerLocator);
    }

    /**
     * Waits with retry for a condition that may be flaky.
     *
     * @param condition   the condition to wait for
     * @param maxRetries  the maximum number of retries
     * @param retryDelay  the delay between retries in milliseconds
     * @param <T>         the return type
     * @return the result of the condition
     */
    public <T> T waitWithRetry(ExpectedCondition<T> condition, int maxRetries, long retryDelay) {
        int attempts = 0;
        while (attempts < maxRetries) {
            try {
                return shortWait.until(condition);
            } catch (TimeoutException e) {
                attempts++;
                if (attempts >= maxRetries) {
                    throw e;
                }
                log.debug("Retry {} of {} for condition", attempts, maxRetries);
                hardWait(retryDelay);
            }
        }
        throw new TimeoutException("Condition not met after " + maxRetries + " retries");
    }
}
