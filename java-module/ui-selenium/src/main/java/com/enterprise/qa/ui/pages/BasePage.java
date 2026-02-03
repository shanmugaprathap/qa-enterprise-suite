package com.enterprise.qa.ui.pages;

import com.enterprise.qa.core.ai.selfhealing.SelfHealingLocator;
import com.enterprise.qa.core.config.ConfigManager;
import com.enterprise.qa.core.drivers.DriverManager;
import com.enterprise.qa.ui.helpers.ElementHelper;
import com.enterprise.qa.ui.helpers.SyncHelper;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.Select;

import java.util.List;

/**
 * Base page object class providing common functionality for all page objects.
 * Includes self-healing locator support and comprehensive element interaction methods.
 */
@Slf4j
public abstract class BasePage {

    protected final WebDriver driver;
    protected final SelfHealingLocator selfHealingLocator;
    protected final ElementHelper elementHelper;
    protected final SyncHelper syncHelper;
    protected final ConfigManager config;

    public BasePage() {
        this.driver = DriverManager.getDriver();
        this.selfHealingLocator = new SelfHealingLocator(driver);
        this.elementHelper = new ElementHelper(driver);
        this.syncHelper = new SyncHelper(driver);
        this.config = ConfigManager.getInstance();
        PageFactory.initElements(driver, this);
    }

    /**
     * Gets the page title.
     *
     * @return the current page title
     */
    public String getPageTitle() {
        return driver.getTitle();
    }

    /**
     * Gets the current URL.
     *
     * @return the current URL
     */
    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    /**
     * Navigates to a URL.
     *
     * @param url the URL to navigate to
     */
    @Step("Navigate to: {url}")
    public void navigateTo(String url) {
        log.info("Navigating to: {}", url);
        driver.get(url);
        waitForPageLoad();
    }

    /**
     * Refreshes the current page.
     */
    @Step("Refresh page")
    public void refreshPage() {
        log.debug("Refreshing page");
        driver.navigate().refresh();
        waitForPageLoad();
    }

    /**
     * Navigates back to the previous page.
     */
    @Step("Navigate back")
    public void navigateBack() {
        log.debug("Navigating back");
        driver.navigate().back();
    }

    /**
     * Navigates forward to the next page.
     */
    @Step("Navigate forward")
    public void navigateForward() {
        log.debug("Navigating forward");
        driver.navigate().forward();
    }

    /**
     * Finds an element with self-healing capability.
     *
     * @param locator     the element locator
     * @param elementName a descriptive name for the element
     * @return the found element
     */
    protected WebElement findElement(By locator, String elementName) {
        return selfHealingLocator.findElement(locator, elementName);
    }

    /**
     * Finds multiple elements with self-healing capability.
     *
     * @param locator     the element locator
     * @param elementName a descriptive name for the elements
     * @return list of found elements
     */
    protected List<WebElement> findElements(By locator, String elementName) {
        return selfHealingLocator.findElements(locator, elementName);
    }

    /**
     * Clicks on an element.
     *
     * @param element     the element to click
     * @param elementName a descriptive name for the element
     */
    @Step("Click on: {elementName}")
    protected void click(WebElement element, String elementName) {
        syncHelper.waitForElementClickable(element);
        log.debug("Clicking on: {}", elementName);
        element.click();
    }

    /**
     * Clicks on an element by locator.
     *
     * @param locator     the element locator
     * @param elementName a descriptive name for the element
     */
    @Step("Click on: {elementName}")
    protected void click(By locator, String elementName) {
        WebElement element = findElement(locator, elementName);
        click(element, elementName);
    }

    /**
     * Double-clicks on an element.
     *
     * @param element     the element to double-click
     * @param elementName a descriptive name for the element
     */
    @Step("Double-click on: {elementName}")
    protected void doubleClick(WebElement element, String elementName) {
        syncHelper.waitForElementClickable(element);
        log.debug("Double-clicking on: {}", elementName);
        new Actions(driver).doubleClick(element).perform();
    }

    /**
     * Right-clicks (context clicks) on an element.
     *
     * @param element     the element to right-click
     * @param elementName a descriptive name for the element
     */
    @Step("Right-click on: {elementName}")
    protected void rightClick(WebElement element, String elementName) {
        syncHelper.waitForElementClickable(element);
        log.debug("Right-clicking on: {}", elementName);
        new Actions(driver).contextClick(element).perform();
    }

    /**
     * Types text into an element.
     *
     * @param element     the element to type into
     * @param text        the text to type
     * @param elementName a descriptive name for the element
     */
    @Step("Type '{text}' into: {elementName}")
    protected void type(WebElement element, String text, String elementName) {
        syncHelper.waitForElementVisible(element);
        log.debug("Typing '{}' into: {}", text, elementName);
        element.clear();
        element.sendKeys(text);
    }

    /**
     * Types text into an element by locator.
     *
     * @param locator     the element locator
     * @param text        the text to type
     * @param elementName a descriptive name for the element
     */
    @Step("Type '{text}' into: {elementName}")
    protected void type(By locator, String text, String elementName) {
        WebElement element = findElement(locator, elementName);
        type(element, text, elementName);
    }

    /**
     * Clears an input field.
     *
     * @param element     the element to clear
     * @param elementName a descriptive name for the element
     */
    @Step("Clear: {elementName}")
    protected void clear(WebElement element, String elementName) {
        syncHelper.waitForElementVisible(element);
        log.debug("Clearing: {}", elementName);
        element.clear();
    }

    /**
     * Gets the text of an element.
     *
     * @param element     the element
     * @param elementName a descriptive name for the element
     * @return the element's text
     */
    protected String getText(WebElement element, String elementName) {
        syncHelper.waitForElementVisible(element);
        String text = element.getText();
        log.debug("Got text '{}' from: {}", text, elementName);
        return text;
    }

    /**
     * Gets an attribute value of an element.
     *
     * @param element     the element
     * @param attribute   the attribute name
     * @param elementName a descriptive name for the element
     * @return the attribute value
     */
    protected String getAttribute(WebElement element, String attribute, String elementName) {
        syncHelper.waitForElementVisible(element);
        return element.getAttribute(attribute);
    }

    /**
     * Checks if an element is displayed.
     *
     * @param element the element to check
     * @return true if displayed
     */
    protected boolean isDisplayed(WebElement element) {
        try {
            return element.isDisplayed();
        } catch (NoSuchElementException | StaleElementReferenceException e) {
            return false;
        }
    }

    /**
     * Checks if an element is enabled.
     *
     * @param element the element to check
     * @return true if enabled
     */
    protected boolean isEnabled(WebElement element) {
        try {
            return element.isEnabled();
        } catch (NoSuchElementException | StaleElementReferenceException e) {
            return false;
        }
    }

    /**
     * Checks if a checkbox or radio button is selected.
     *
     * @param element the element to check
     * @return true if selected
     */
    protected boolean isSelected(WebElement element) {
        try {
            return element.isSelected();
        } catch (NoSuchElementException | StaleElementReferenceException e) {
            return false;
        }
    }

    /**
     * Selects an option from a dropdown by visible text.
     *
     * @param element     the select element
     * @param text        the option text
     * @param elementName a descriptive name for the element
     */
    @Step("Select '{text}' from: {elementName}")
    protected void selectByVisibleText(WebElement element, String text, String elementName) {
        syncHelper.waitForElementVisible(element);
        log.debug("Selecting '{}' from: {}", text, elementName);
        new Select(element).selectByVisibleText(text);
    }

    /**
     * Selects an option from a dropdown by value.
     *
     * @param element     the select element
     * @param value       the option value
     * @param elementName a descriptive name for the element
     */
    @Step("Select value '{value}' from: {elementName}")
    protected void selectByValue(WebElement element, String value, String elementName) {
        syncHelper.waitForElementVisible(element);
        log.debug("Selecting value '{}' from: {}", value, elementName);
        new Select(element).selectByValue(value);
    }

    /**
     * Selects an option from a dropdown by index.
     *
     * @param element     the select element
     * @param index       the option index
     * @param elementName a descriptive name for the element
     */
    @Step("Select index {index} from: {elementName}")
    protected void selectByIndex(WebElement element, int index, String elementName) {
        syncHelper.waitForElementVisible(element);
        log.debug("Selecting index {} from: {}", index, elementName);
        new Select(element).selectByIndex(index);
    }

    /**
     * Hovers over an element.
     *
     * @param element     the element to hover over
     * @param elementName a descriptive name for the element
     */
    @Step("Hover over: {elementName}")
    protected void hover(WebElement element, String elementName) {
        syncHelper.waitForElementVisible(element);
        log.debug("Hovering over: {}", elementName);
        new Actions(driver).moveToElement(element).perform();
    }

    /**
     * Drags an element and drops it on another element.
     *
     * @param source     the source element
     * @param target     the target element
     * @param sourceName the source element name
     * @param targetName the target element name
     */
    @Step("Drag {sourceName} to {targetName}")
    protected void dragAndDrop(WebElement source, WebElement target,
                               String sourceName, String targetName) {
        syncHelper.waitForElementVisible(source);
        syncHelper.waitForElementVisible(target);
        log.debug("Dragging {} to {}", sourceName, targetName);
        new Actions(driver).dragAndDrop(source, target).perform();
    }

    /**
     * Scrolls to an element.
     *
     * @param element     the element to scroll to
     * @param elementName a descriptive name for the element
     */
    @Step("Scroll to: {elementName}")
    protected void scrollToElement(WebElement element, String elementName) {
        log.debug("Scrolling to: {}", elementName);
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});",
                element
        );
    }

    /**
     * Clicks an element using JavaScript.
     *
     * @param element     the element to click
     * @param elementName a descriptive name for the element
     */
    @Step("JS Click on: {elementName}")
    protected void jsClick(WebElement element, String elementName) {
        log.debug("JS clicking on: {}", elementName);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }

    /**
     * Waits for the page to fully load.
     */
    protected void waitForPageLoad() {
        syncHelper.waitForPageLoad();
    }

    /**
     * Takes a screenshot of the current page.
     *
     * @return the screenshot as bytes
     */
    protected byte[] takeScreenshot() {
        return DriverManager.takeScreenshot();
    }

    /**
     * Switches to a frame by index.
     *
     * @param index the frame index
     */
    @Step("Switch to frame {index}")
    protected void switchToFrame(int index) {
        log.debug("Switching to frame: {}", index);
        driver.switchTo().frame(index);
    }

    /**
     * Switches to a frame by name or ID.
     *
     * @param nameOrId the frame name or ID
     */
    @Step("Switch to frame: {nameOrId}")
    protected void switchToFrame(String nameOrId) {
        log.debug("Switching to frame: {}", nameOrId);
        driver.switchTo().frame(nameOrId);
    }

    /**
     * Switches to a frame by element.
     *
     * @param element the frame element
     */
    protected void switchToFrame(WebElement element) {
        log.debug("Switching to frame element");
        driver.switchTo().frame(element);
    }

    /**
     * Switches back to the default content.
     */
    @Step("Switch to default content")
    protected void switchToDefaultContent() {
        log.debug("Switching to default content");
        driver.switchTo().defaultContent();
    }

    /**
     * Switches to a new window/tab.
     *
     * @return the original window handle
     */
    protected String switchToNewWindow() {
        String originalWindow = driver.getWindowHandle();
        for (String windowHandle : driver.getWindowHandles()) {
            if (!windowHandle.equals(originalWindow)) {
                driver.switchTo().window(windowHandle);
                break;
            }
        }
        return originalWindow;
    }

    /**
     * Switches back to the original window.
     *
     * @param windowHandle the window handle to switch to
     */
    protected void switchToWindow(String windowHandle) {
        driver.switchTo().window(windowHandle);
    }

    /**
     * Accepts an alert.
     */
    @Step("Accept alert")
    protected void acceptAlert() {
        syncHelper.waitForAlert();
        driver.switchTo().alert().accept();
    }

    /**
     * Dismisses an alert.
     */
    @Step("Dismiss alert")
    protected void dismissAlert() {
        syncHelper.waitForAlert();
        driver.switchTo().alert().dismiss();
    }

    /**
     * Gets the text of an alert.
     *
     * @return the alert text
     */
    protected String getAlertText() {
        syncHelper.waitForAlert();
        return driver.switchTo().alert().getText();
    }

    /**
     * Sends keys to an alert prompt.
     *
     * @param text the text to send
     */
    @Step("Type '{text}' into alert")
    protected void sendKeysToAlert(String text) {
        syncHelper.waitForAlert();
        driver.switchTo().alert().sendKeys(text);
    }
}
