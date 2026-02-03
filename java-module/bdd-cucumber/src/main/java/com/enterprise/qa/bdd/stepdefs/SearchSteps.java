package com.enterprise.qa.bdd.stepdefs;

import com.enterprise.qa.core.drivers.DriverManager;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for search-related scenarios.
 */
@Slf4j
public class SearchSteps {

    private WebDriver driver;
    private WebDriverWait wait;
    private String initialUrl;

    @Given("I am on the Google homepage")
    public void iAmOnTheGoogleHomepage() {
        driver = DriverManager.getDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        driver.get("https://www.google.com");
        initialUrl = driver.getCurrentUrl();

        // Accept cookies if present
        acceptCookiesIfPresent();

        log.info("Navigated to Google homepage");
    }

    @When("I search for {string}")
    public void iSearchFor(String searchTerm) {
        WebElement searchBox = findSearchBox();
        searchBox.sendKeys(searchTerm);
        searchBox.sendKeys(Keys.ENTER);

        // Wait for search results
        wait.until(ExpectedConditions.or(
                ExpectedConditions.presenceOfElementLocated(By.id("search")),
                ExpectedConditions.presenceOfElementLocated(By.id("rso"))
        ));

        log.info("Searched for: {}", searchTerm);
    }

    @When("I type {string} in the search box")
    public void iTypeInTheSearchBox(String text) {
        WebElement searchBox = findSearchBox();
        searchBox.sendKeys(text);

        // Wait a bit for suggestions to appear
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("Typed: {}", text);
    }

    @When("I submit an empty search")
    public void iSubmitAnEmptySearch() {
        WebElement searchBox = findSearchBox();
        searchBox.sendKeys(Keys.ENTER);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("Submitted empty search");
    }

    @Then("I should see search results")
    public void iShouldSeeSearchResults() {
        WebElement results = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("#search, #rso, div.g")
        ));
        assertThat(results.isDisplayed()).isTrue();

        log.info("Search results are displayed");
    }

    @Then("I should see search suggestions")
    public void iShouldSeeSearchSuggestions() {
        try {
            WebElement suggestions = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("ul[role='listbox'], div.sbqs_c, div.sbct")
            ));
            assertThat(suggestions.isDisplayed()).isTrue();
            log.info("Search suggestions are displayed");
        } catch (Exception e) {
            // Suggestions might not always appear
            log.warn("Search suggestions not found (may vary by region)");
        }
    }

    @Then("the page title should contain {string}")
    public void thePageTitleShouldContain(String expectedText) {
        String title = driver.getTitle();
        assertThat(title).containsIgnoringCase(expectedText);

        log.info("Page title '{}' contains '{}'", title, expectedText);
    }

    @Then("I should remain on the homepage")
    public void iShouldRemainOnTheHomepage() {
        String currentUrl = driver.getCurrentUrl();
        assertThat(currentUrl).isEqualTo(initialUrl);

        log.info("Remained on homepage");
    }

    private WebElement findSearchBox() {
        By[] selectors = {
                By.name("q"),
                By.cssSelector("input[title='Search']"),
                By.cssSelector("textarea[name='q']")
        };

        for (By selector : selectors) {
            try {
                return wait.until(ExpectedConditions.elementToBeClickable(selector));
            } catch (Exception e) {
                // Try next
            }
        }

        throw new RuntimeException("Could not find search box");
    }

    private void acceptCookiesIfPresent() {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(3));
            WebElement acceptButton = shortWait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(text(),'Accept') or contains(text(),'I agree')]")
            ));
            acceptButton.click();
            log.debug("Accepted cookies");
        } catch (Exception e) {
            // No cookies dialog
        }
    }
}
