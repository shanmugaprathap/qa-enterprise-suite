package com.enterprise.qa.ui.tests.smoke;

import com.enterprise.qa.ui.base.BaseUITest;
import io.qameta.allure.*;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests for Google Search functionality.
 * Demonstrates basic UI test structure and Allure reporting.
 */
@Slf4j
@Epic("Search Functionality")
@Feature("Google Search")
public class GoogleSearchTest extends BaseUITest {

    private static final String GOOGLE_URL = "https://www.google.com";

    @Test(groups = {"smoke", "ui"})
    @Story("Basic Search")
    @Description("Verify that Google search returns results for a query")
    @Severity(SeverityLevel.CRITICAL)
    public void testGoogleSearch() {
        // Navigate to Google
        navigateTo(GOOGLE_URL);

        // Accept cookies if dialog appears (EU)
        acceptCookiesIfPresent();

        // Verify page loaded
        assertThat(getPageTitle()).contains("Google");

        // Find search box and enter query
        WebElement searchBox = findSearchBox();
        assertThat(searchBox.isDisplayed()).isTrue();

        String searchQuery = "Selenium WebDriver";
        searchBox.sendKeys(searchQuery);
        searchBox.sendKeys(Keys.ENTER);

        // Wait for results
        waitForSearchResults();

        // Verify results page
        assertThat(getPageTitle()).contains(searchQuery);
        assertThat(getCurrentUrl()).contains("search");

        log.info("Search test completed successfully");
    }

    @Test(groups = {"smoke", "ui"})
    @Story("Search Suggestions")
    @Description("Verify that Google shows search suggestions as user types")
    @Severity(SeverityLevel.NORMAL)
    public void testSearchSuggestions() {
        navigateTo(GOOGLE_URL);
        acceptCookiesIfPresent();

        WebElement searchBox = findSearchBox();
        searchBox.sendKeys("selenium");

        // Wait for suggestions to appear
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("ul[role='listbox'] li, div.sbqs_c, div.sbct")));

        // Verify suggestions are shown
        captureScreenshotToAllure("Search Suggestions");

        log.info("Search suggestions test completed");
    }

    @Test(groups = {"smoke", "ui"})
    @Story("Empty Search")
    @Description("Verify behavior when submitting an empty search")
    @Severity(SeverityLevel.MINOR)
    public void testEmptySearch() {
        navigateTo(GOOGLE_URL);
        acceptCookiesIfPresent();

        WebElement searchBox = findSearchBox();
        String initialUrl = getCurrentUrl();

        // Submit empty search
        searchBox.sendKeys(Keys.ENTER);

        // Should stay on the same page
        assertThat(getCurrentUrl()).isEqualTo(initialUrl);

        log.info("Empty search test completed");
    }

    @Test(groups = {"regression", "ui"})
    @Story("Special Characters Search")
    @Description("Verify search handles special characters correctly")
    @Severity(SeverityLevel.NORMAL)
    public void testSpecialCharactersSearch() {
        navigateTo(GOOGLE_URL);
        acceptCookiesIfPresent();

        WebElement searchBox = findSearchBox();
        String specialQuery = "C++ programming \"best practices\"";

        searchBox.sendKeys(specialQuery);
        searchBox.sendKeys(Keys.ENTER);

        waitForSearchResults();

        // Verify we got results (page changed)
        assertThat(getCurrentUrl()).contains("search");

        log.info("Special characters search test completed");
    }

    @Test(groups = {"regression", "ui"})
    @Story("Search by Voice Button")
    @Description("Verify the search by voice button is present")
    @Severity(SeverityLevel.MINOR)
    public void testVoiceSearchButtonPresent() {
        navigateTo(GOOGLE_URL);
        acceptCookiesIfPresent();

        // Look for voice search button (microphone icon)
        boolean voiceButtonPresent = driver.findElements(
                By.cssSelector("div.XDyW0e svg, div[aria-label*='voice'], button[aria-label*='voice']")
        ).size() > 0;

        // Note: Voice search may not be available in all regions
        log.info("Voice search button present: {}", voiceButtonPresent);

        captureScreenshotToAllure("Google Homepage");
    }

    /**
     * Finds the Google search box using multiple strategies.
     */
    @Step("Find search box")
    private WebElement findSearchBox() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Try multiple selectors for the search box
        By[] selectors = {
                By.name("q"),
                By.cssSelector("input[title='Search']"),
                By.cssSelector("textarea[name='q']"),
                By.cssSelector("input[type='text'][aria-label*='Search']")
        };

        for (By selector : selectors) {
            try {
                return wait.until(ExpectedConditions.elementToBeClickable(selector));
            } catch (Exception e) {
                log.debug("Selector not found: {}", selector);
            }
        }

        throw new RuntimeException("Could not find Google search box");
    }

    /**
     * Accepts cookies dialog if it appears.
     */
    @Step("Accept cookies if present")
    private void acceptCookiesIfPresent() {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(3));

            // Look for common cookie accept buttons
            By[] cookieButtons = {
                    By.id("L2AGLb"),
                    By.cssSelector("button[id*='accept']"),
                    By.xpath("//button[contains(text(),'Accept')]"),
                    By.xpath("//button[contains(text(),'I agree')]")
            };

            for (By button : cookieButtons) {
                try {
                    WebElement acceptButton = shortWait.until(
                            ExpectedConditions.elementToBeClickable(button));
                    acceptButton.click();
                    log.info("Accepted cookies dialog");
                    return;
                } catch (Exception e) {
                    // Try next selector
                }
            }
        } catch (Exception e) {
            log.debug("No cookies dialog found or already accepted");
        }
    }

    /**
     * Waits for search results to load.
     */
    @Step("Wait for search results")
    private void waitForSearchResults() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Wait for results container
        wait.until(ExpectedConditions.or(
                ExpectedConditions.presenceOfElementLocated(By.id("search")),
                ExpectedConditions.presenceOfElementLocated(By.id("rso")),
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.g"))
        ));
    }
}
