package com.enterprise.qa.ui.helpers;

import com.enterprise.qa.core.config.ConfigManager;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * Helper class providing utility methods for element interactions.
 * Includes advanced operations like file upload, iframe handling, and JavaScript execution.
 */
@Slf4j
public class ElementHelper {

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final int explicitWait;

    public ElementHelper(WebDriver driver) {
        this.driver = driver;
        this.explicitWait = ConfigManager.getInstance().getExplicitWait();
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(explicitWait));
    }

    /**
     * Finds a child element within a parent element.
     *
     * @param parent  the parent element
     * @param locator the child locator
     * @return the child element
     */
    public WebElement findChildElement(WebElement parent, By locator) {
        return parent.findElement(locator);
    }

    /**
     * Finds all child elements within a parent element.
     *
     * @param parent  the parent element
     * @param locator the child locator
     * @return list of child elements
     */
    public List<WebElement> findChildElements(WebElement parent, By locator) {
        return parent.findElements(locator);
    }

    /**
     * Gets the count of elements matching a locator.
     *
     * @param locator the element locator
     * @return the count of matching elements
     */
    public int getElementCount(By locator) {
        return driver.findElements(locator).size();
    }

    /**
     * Checks if an element exists on the page.
     *
     * @param locator the element locator
     * @return true if the element exists
     */
    public boolean elementExists(By locator) {
        return !driver.findElements(locator).isEmpty();
    }

    /**
     * Checks if an element is visible.
     *
     * @param locator the element locator
     * @return true if the element is visible
     */
    public boolean isElementVisible(By locator) {
        try {
            return driver.findElement(locator).isDisplayed();
        } catch (NoSuchElementException | StaleElementReferenceException e) {
            return false;
        }
    }

    /**
     * Gets all options from a select element.
     *
     * @param selectElement the select element
     * @return list of option elements
     */
    public List<WebElement> getAllOptions(WebElement selectElement) {
        return new Select(selectElement).getOptions();
    }

    /**
     * Gets all selected options from a select element.
     *
     * @param selectElement the select element
     * @return list of selected option elements
     */
    public List<WebElement> getSelectedOptions(WebElement selectElement) {
        return new Select(selectElement).getAllSelectedOptions();
    }

    /**
     * Deselects all options in a multi-select element.
     *
     * @param selectElement the select element
     */
    public void deselectAll(WebElement selectElement) {
        new Select(selectElement).deselectAll();
    }

    /**
     * Uploads a file to a file input element.
     *
     * @param fileInput the file input element
     * @param filePath  the path to the file
     */
    public void uploadFile(WebElement fileInput, String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IllegalArgumentException("File not found: " + filePath);
        }
        fileInput.sendKeys(file.getAbsolutePath());
        log.info("Uploaded file: {}", filePath);
    }

    /**
     * Highlights an element for debugging purposes.
     *
     * @param element the element to highlight
     */
    public void highlightElement(WebElement element) {
        String originalStyle = element.getAttribute("style");
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].setAttribute('style', arguments[1]);",
                element,
                "border: 3px solid red; background: yellow;"
        );

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].setAttribute('style', arguments[1]);",
                element,
                originalStyle != null ? originalStyle : ""
        );
    }

    /**
     * Gets the value of a CSS property.
     *
     * @param element  the element
     * @param property the CSS property name
     * @return the property value
     */
    public String getCssValue(WebElement element, String property) {
        return element.getCssValue(property);
    }

    /**
     * Gets the location of an element.
     *
     * @param element the element
     * @return the element's location
     */
    public Point getElementLocation(WebElement element) {
        return element.getLocation();
    }

    /**
     * Gets the size of an element.
     *
     * @param element the element
     * @return the element's size
     */
    public Dimension getElementSize(WebElement element) {
        return element.getSize();
    }

    /**
     * Scrolls the page by specified offset.
     *
     * @param xOffset horizontal offset
     * @param yOffset vertical offset
     */
    public void scrollBy(int xOffset, int yOffset) {
        ((JavascriptExecutor) driver).executeScript(
                "window.scrollBy(arguments[0], arguments[1]);",
                xOffset, yOffset
        );
    }

    /**
     * Scrolls to the top of the page.
     */
    public void scrollToTop() {
        ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0);");
    }

    /**
     * Scrolls to the bottom of the page.
     */
    public void scrollToBottom() {
        ((JavascriptExecutor) driver).executeScript(
                "window.scrollTo(0, document.body.scrollHeight);"
        );
    }

    /**
     * Executes JavaScript and returns the result.
     *
     * @param script the JavaScript to execute
     * @param args   arguments to pass to the script
     * @return the script result
     */
    public Object executeScript(String script, Object... args) {
        return ((JavascriptExecutor) driver).executeScript(script, args);
    }

    /**
     * Executes async JavaScript and returns the result.
     *
     * @param script the JavaScript to execute
     * @param args   arguments to pass to the script
     * @return the script result
     */
    public Object executeAsyncScript(String script, Object... args) {
        return ((JavascriptExecutor) driver).executeAsyncScript(script, args);
    }

    /**
     * Sets the value of an input using JavaScript.
     *
     * @param element the input element
     * @param value   the value to set
     */
    public void setValueByJS(WebElement element, String value) {
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = arguments[1];",
                element, value
        );
    }

    /**
     * Gets the inner HTML of an element.
     *
     * @param element the element
     * @return the inner HTML
     */
    public String getInnerHTML(WebElement element) {
        return element.getAttribute("innerHTML");
    }

    /**
     * Gets the outer HTML of an element.
     *
     * @param element the element
     * @return the outer HTML
     */
    public String getOuterHTML(WebElement element) {
        return element.getAttribute("outerHTML");
    }

    /**
     * Gets all window handles.
     *
     * @return set of window handles
     */
    public Set<String> getWindowHandles() {
        return driver.getWindowHandles();
    }

    /**
     * Gets the current window handle.
     *
     * @return the current window handle
     */
    public String getWindowHandle() {
        return driver.getWindowHandle();
    }

    /**
     * Closes the current window.
     */
    public void closeWindow() {
        driver.close();
    }

    /**
     * Maximizes the browser window.
     */
    public void maximizeWindow() {
        driver.manage().window().maximize();
    }

    /**
     * Sets the browser window size.
     *
     * @param width  the width in pixels
     * @param height the height in pixels
     */
    public void setWindowSize(int width, int height) {
        driver.manage().window().setSize(new Dimension(width, height));
    }

    /**
     * Takes a screenshot of a specific element.
     *
     * @param element the element to screenshot
     * @return the screenshot as a File
     */
    public File takeElementScreenshot(WebElement element) {
        return element.getScreenshotAs(OutputType.FILE);
    }

    /**
     * Performs keyboard actions.
     *
     * @param keys the keys to press
     */
    public void pressKeys(CharSequence... keys) {
        new Actions(driver).sendKeys(keys).perform();
    }

    /**
     * Performs a key down action.
     *
     * @param key the key to press down
     */
    public void keyDown(Keys key) {
        new Actions(driver).keyDown(key).perform();
    }

    /**
     * Performs a key up action.
     *
     * @param key the key to release
     */
    public void keyUp(Keys key) {
        new Actions(driver).keyUp(key).perform();
    }

    /**
     * Waits for an element to be stale (removed from DOM).
     *
     * @param element the element
     * @return true if the element became stale
     */
    public boolean waitForStaleness(WebElement element) {
        try {
            wait.until(ExpectedConditions.stalenessOf(element));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * Waits for text to be present in an element.
     *
     * @param element the element
     * @param text    the expected text
     * @return true if the text is present
     */
    public boolean waitForTextPresent(WebElement element, String text) {
        try {
            wait.until(ExpectedConditions.textToBePresentInElement(element, text));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * Waits for an attribute to have a specific value.
     *
     * @param element   the element
     * @param attribute the attribute name
     * @param value     the expected value
     * @return true if the attribute has the value
     */
    public boolean waitForAttributeValue(WebElement element, String attribute, String value) {
        try {
            wait.until(ExpectedConditions.attributeToBe(element, attribute, value));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * Creates a fluent wait with custom settings.
     *
     * @param timeoutSeconds the timeout in seconds
     * @param pollingMillis  the polling interval in milliseconds
     * @return the fluent wait instance
     */
    public FluentWait<WebDriver> createFluentWait(int timeoutSeconds, long pollingMillis) {
        return new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(timeoutSeconds))
                .pollingEvery(Duration.ofMillis(pollingMillis))
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class);
    }
}
