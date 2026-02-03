package com.enterprise.qa.core.drivers;

import com.enterprise.qa.core.config.ConfigManager;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Thread-safe WebDriver manager supporting multiple browsers and Selenium Grid.
 * Uses ThreadLocal for parallel test execution support.
 */
@Slf4j
public class DriverManager {

    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();
    private static final ConfigManager config = ConfigManager.getInstance();

    private DriverManager() {
        // Private constructor to prevent instantiation
    }

    /**
     * Gets the WebDriver instance for the current thread.
     * Creates a new driver if one doesn't exist.
     *
     * @return the WebDriver instance
     */
    public static WebDriver getDriver() {
        if (driverThreadLocal.get() == null) {
            initDriver();
        }
        return driverThreadLocal.get();
    }

    /**
     * Initializes the WebDriver based on configuration.
     */
    public static void initDriver() {
        String browser = config.getBrowser();
        boolean headless = config.isHeadless();
        boolean useGrid = config.useGrid();

        log.info("Initializing {} driver (headless: {}, grid: {})", browser, headless, useGrid);

        WebDriver driver;
        if (useGrid) {
            driver = createRemoteDriver(browser, headless);
        } else {
            driver = createLocalDriver(browser, headless);
        }

        configureDriver(driver);
        driverThreadLocal.set(driver);

        log.info("Driver initialized successfully for thread: {}", Thread.currentThread().getId());
    }

    /**
     * Creates a local WebDriver instance.
     *
     * @param browser  the browser type
     * @param headless whether to run in headless mode
     * @return the WebDriver instance
     */
    private static WebDriver createLocalDriver(String browser, boolean headless) {
        return switch (browser.toLowerCase()) {
            case "firefox" -> createFirefoxDriver(headless);
            case "edge" -> createEdgeDriver(headless);
            default -> createChromeDriver(headless);
        };
    }

    /**
     * Creates a remote WebDriver instance for Selenium Grid.
     *
     * @param browser  the browser type
     * @param headless whether to run in headless mode
     * @return the RemoteWebDriver instance
     */
    private static WebDriver createRemoteDriver(String browser, boolean headless) {
        try {
            URL gridUrl = new URL(config.getGridUrl());

            return switch (browser.toLowerCase()) {
                case "firefox" -> new RemoteWebDriver(gridUrl, getFirefoxOptions(headless));
                case "edge" -> new RemoteWebDriver(gridUrl, getEdgeOptions(headless));
                default -> new RemoteWebDriver(gridUrl, getChromeOptions(headless));
            };
        } catch (MalformedURLException e) {
            log.error("Invalid Grid URL: {}", config.getGridUrl(), e);
            throw new RuntimeException("Failed to create remote driver", e);
        }
    }

    /**
     * Creates a Chrome WebDriver instance.
     *
     * @param headless whether to run in headless mode
     * @return the ChromeDriver instance
     */
    private static WebDriver createChromeDriver(boolean headless) {
        WebDriverManager.chromedriver().setup();
        return new ChromeDriver(getChromeOptions(headless));
    }

    /**
     * Gets Chrome options configured for testing.
     *
     * @param headless whether to run in headless mode
     * @return the ChromeOptions
     */
    public static ChromeOptions getChromeOptions(boolean headless) {
        ChromeOptions options = new ChromeOptions();

        // Basic options
        options.addArguments("--start-maximized");
        options.addArguments("--disable-infobars");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--remote-allow-origins=*");

        // Headless mode
        if (headless) {
            options.addArguments("--headless=new");
            options.addArguments("--window-size=1920,1080");
        }

        // Preferences
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        prefs.put("profile.default_content_setting_values.notifications", 2);
        options.setExperimentalOption("prefs", prefs);

        // Exclude automation switches to avoid detection
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);

        return options;
    }

    /**
     * Creates a Firefox WebDriver instance.
     *
     * @param headless whether to run in headless mode
     * @return the FirefoxDriver instance
     */
    private static WebDriver createFirefoxDriver(boolean headless) {
        WebDriverManager.firefoxdriver().setup();
        return new FirefoxDriver(getFirefoxOptions(headless));
    }

    /**
     * Gets Firefox options configured for testing.
     *
     * @param headless whether to run in headless mode
     * @return the FirefoxOptions
     */
    public static FirefoxOptions getFirefoxOptions(boolean headless) {
        FirefoxOptions options = new FirefoxOptions();

        if (headless) {
            options.addArguments("-headless");
        }

        // Disable notifications
        options.addPreference("dom.webnotifications.enabled", false);
        options.addPreference("dom.push.enabled", false);

        return options;
    }

    /**
     * Creates an Edge WebDriver instance.
     *
     * @param headless whether to run in headless mode
     * @return the EdgeDriver instance
     */
    private static WebDriver createEdgeDriver(boolean headless) {
        WebDriverManager.edgedriver().setup();
        return new EdgeDriver(getEdgeOptions(headless));
    }

    /**
     * Gets Edge options configured for testing.
     *
     * @param headless whether to run in headless mode
     * @return the EdgeOptions
     */
    public static EdgeOptions getEdgeOptions(boolean headless) {
        EdgeOptions options = new EdgeOptions();

        options.addArguments("--start-maximized");
        options.addArguments("--disable-infobars");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");

        if (headless) {
            options.addArguments("--headless=new");
            options.addArguments("--window-size=1920,1080");
        }

        return options;
    }

    /**
     * Configures the WebDriver with standard settings.
     *
     * @param driver the WebDriver to configure
     */
    private static void configureDriver(WebDriver driver) {
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(config.getImplicitWait()));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(config.getPageLoadTimeout()));
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(config.getExplicitWait()));

        if (!config.isHeadless()) {
            driver.manage().window().maximize();
        }
    }

    /**
     * Quits the WebDriver and removes it from the thread local.
     */
    public static void quitDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver != null) {
            try {
                log.info("Quitting driver for thread: {}", Thread.currentThread().getId());
                driver.quit();
            } catch (Exception e) {
                log.error("Error quitting driver", e);
            } finally {
                driverThreadLocal.remove();
            }
        }
    }

    /**
     * Checks if a driver exists for the current thread.
     *
     * @return true if a driver exists
     */
    public static boolean hasDriver() {
        return driverThreadLocal.get() != null;
    }

    /**
     * Refreshes the current page.
     */
    public static void refresh() {
        getDriver().navigate().refresh();
    }

    /**
     * Navigates to the specified URL.
     *
     * @param url the URL to navigate to
     */
    public static void navigateTo(String url) {
        log.debug("Navigating to: {}", url);
        getDriver().get(url);
    }

    /**
     * Gets the current URL.
     *
     * @return the current URL
     */
    public static String getCurrentUrl() {
        return getDriver().getCurrentUrl();
    }

    /**
     * Gets the page title.
     *
     * @return the page title
     */
    public static String getTitle() {
        return getDriver().getTitle();
    }

    /**
     * Takes a screenshot of the current page.
     *
     * @return the screenshot as bytes
     */
    public static byte[] takeScreenshot() {
        if (getDriver() instanceof org.openqa.selenium.TakesScreenshot) {
            return ((org.openqa.selenium.TakesScreenshot) getDriver())
                    .getScreenshotAs(org.openqa.selenium.OutputType.BYTES);
        }
        return new byte[0];
    }
}
