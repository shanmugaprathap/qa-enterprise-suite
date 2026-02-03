package com.enterprise.qa.junit5;

import com.enterprise.qa.core.drivers.DriverManager;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sample JUnit 5 tests demonstrating modern testing patterns.
 */
@Epic("JUnit 5 Testing")
@Feature("Google Search")
@DisplayName("Google Search Tests - JUnit 5")
class SampleJUnit5Test {

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeEach
    void setUp() {
        DriverManager.initDriver();
        driver = DriverManager.getDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        driver.get("https://www.google.com");
        acceptCookies();
    }

    @AfterEach
    void tearDown() {
        DriverManager.quitDriver();
    }

    @Test
    @DisplayName("Should display Google homepage")
    @Story("Homepage")
    @Severity(SeverityLevel.CRITICAL)
    @Tag("smoke")
    void shouldDisplayGoogleHomepage() {
        assertThat(driver.getTitle()).containsIgnoringCase("Google");

        WebElement searchBox = findSearchBox();
        assertThat(searchBox.isDisplayed()).isTrue();
    }

    @Test
    @DisplayName("Should perform a search")
    @Story("Search")
    @Severity(SeverityLevel.CRITICAL)
    @Tag("smoke")
    void shouldPerformSearch() {
        WebElement searchBox = findSearchBox();
        searchBox.sendKeys("JUnit 5 testing");
        searchBox.sendKeys(Keys.ENTER);

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("search")));

        assertThat(driver.getTitle()).contains("JUnit 5 testing");
    }

    @ParameterizedTest(name = "Search for: {0}")
    @DisplayName("Should search for different terms")
    @Story("Parameterized Search")
    @ValueSource(strings = {"Selenium", "Playwright", "Cypress"})
    @Tag("regression")
    void shouldSearchForDifferentTerms(String searchTerm) {
        WebElement searchBox = findSearchBox();
        searchBox.sendKeys(searchTerm);
        searchBox.sendKeys(Keys.ENTER);

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("search")));

        assertThat(driver.getTitle()).containsIgnoringCase(searchTerm);
    }

    @ParameterizedTest(name = "Search: {0} should find results about {1}")
    @DisplayName("Should find relevant results")
    @Story("Search Results")
    @CsvSource({
            "Java testing, Java",
            "Python automation, Python",
            "JavaScript frameworks, JavaScript"
    })
    @Tag("regression")
    void shouldFindRelevantResults(String searchQuery, String expectedTopic) {
        WebElement searchBox = findSearchBox();
        searchBox.sendKeys(searchQuery);
        searchBox.sendKeys(Keys.ENTER);

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("search")));

        String pageTitle = driver.getTitle();
        assertThat(pageTitle.toLowerCase()).contains(expectedTopic.toLowerCase());
    }

    @Test
    @DisplayName("Should handle empty search")
    @Story("Edge Cases")
    @Severity(SeverityLevel.MINOR)
    @Tag("regression")
    void shouldHandleEmptySearch() {
        String initialUrl = driver.getCurrentUrl();

        WebElement searchBox = findSearchBox();
        searchBox.sendKeys(Keys.ENTER);

        // Wait a moment
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertThat(driver.getCurrentUrl()).isEqualTo(initialUrl);
    }

    @Nested
    @DisplayName("Search Suggestions Tests")
    class SearchSuggestionsTests {

        @Test
        @DisplayName("Should show suggestions while typing")
        @Story("Search Suggestions")
        @Tag("regression")
        void shouldShowSuggestionsWhileTyping() {
            WebElement searchBox = findSearchBox();

            // Type slowly to trigger suggestions
            for (char c : "selenium".toCharArray()) {
                searchBox.sendKeys(String.valueOf(c));
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // Check for suggestions (may not always appear)
            boolean suggestionsPresent = driver.findElements(
                    By.cssSelector("ul[role='listbox'], div.sbqs_c")
            ).size() > 0;

            // Just log the result - suggestions depend on region/settings
            System.out.println("Suggestions displayed: " + suggestionsPresent);
        }
    }

    @Nested
    @DisplayName("Disabled/Skipped Tests")
    class DisabledTests {

        @Test
        @Disabled("Demonstrating disabled test")
        @DisplayName("This test is disabled")
        void disabledTest() {
            // This test won't run
        }
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
        throw new RuntimeException("Search box not found");
    }

    private void acceptCookies() {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(3));
            WebElement acceptButton = shortWait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(text(),'Accept') or contains(text(),'I agree')]")
            ));
            acceptButton.click();
        } catch (Exception e) {
            // No cookies dialog
        }
    }
}
