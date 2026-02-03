package com.enterprise.qa.mobile.drivers;

import com.enterprise.qa.core.config.ConfigManager;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;
import lombok.extern.slf4j.Slf4j;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

/**
 * Thread-safe mobile driver manager for Appium testing.
 * Supports both Android and iOS platforms.
 */
@Slf4j
public class MobileDriverManager {

    private static final ThreadLocal<AppiumDriver> driverThreadLocal = new ThreadLocal<>();
    private static final ConfigManager config = ConfigManager.getInstance();

    private static final String DEFAULT_APPIUM_URL = "http://localhost:4723";

    private MobileDriverManager() {
        // Private constructor
    }

    /**
     * Gets the AppiumDriver instance for the current thread.
     *
     * @return the AppiumDriver
     */
    public static AppiumDriver getDriver() {
        if (driverThreadLocal.get() == null) {
            throw new IllegalStateException("Driver not initialized. Call initDriver() first.");
        }
        return driverThreadLocal.get();
    }

    /**
     * Initializes an Android driver.
     *
     * @param appPath     path to the APK
     * @param deviceName  the device name
     * @param platformVersion the Android version
     */
    public static void initAndroidDriver(String appPath, String deviceName, String platformVersion) {
        log.info("Initializing Android driver for device: {}", deviceName);

        UiAutomator2Options options = new UiAutomator2Options()
                .setDeviceName(deviceName)
                .setPlatformVersion(platformVersion)
                .setAutomationName("UiAutomator2")
                .setApp(appPath)
                .setAutoGrantPermissions(true)
                .setNoReset(false);

        try {
            URL appiumUrl = new URL(getAppiumUrl());
            AndroidDriver driver = new AndroidDriver(appiumUrl, options);
            configureDriver(driver);
            driverThreadLocal.set(driver);
            log.info("Android driver initialized successfully");
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid Appium URL", e);
        }
    }

    /**
     * Initializes an iOS driver.
     *
     * @param appPath        path to the IPA or app bundle
     * @param deviceName     the device name
     * @param platformVersion the iOS version
     */
    public static void initIOSDriver(String appPath, String deviceName, String platformVersion) {
        log.info("Initializing iOS driver for device: {}", deviceName);

        XCUITestOptions options = new XCUITestOptions()
                .setDeviceName(deviceName)
                .setPlatformVersion(platformVersion)
                .setAutomationName("XCUITest")
                .setApp(appPath)
                .setAutoAcceptAlerts(true)
                .setNoReset(false);

        try {
            URL appiumUrl = new URL(getAppiumUrl());
            IOSDriver driver = new IOSDriver(appiumUrl, options);
            configureDriver(driver);
            driverThreadLocal.set(driver);
            log.info("iOS driver initialized successfully");
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid Appium URL", e);
        }
    }

    /**
     * Initializes an Android Chrome browser driver.
     *
     * @param deviceName      the device name
     * @param platformVersion the Android version
     */
    public static void initAndroidBrowserDriver(String deviceName, String platformVersion) {
        log.info("Initializing Android Chrome driver for device: {}", deviceName);

        UiAutomator2Options options = new UiAutomator2Options()
                .setDeviceName(deviceName)
                .setPlatformVersion(platformVersion)
                .setAutomationName("UiAutomator2")
                .withBrowserName("Chrome");

        try {
            URL appiumUrl = new URL(getAppiumUrl());
            AndroidDriver driver = new AndroidDriver(appiumUrl, options);
            configureDriver(driver);
            driverThreadLocal.set(driver);
            log.info("Android Chrome driver initialized successfully");
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid Appium URL", e);
        }
    }

    /**
     * Initializes an iOS Safari browser driver.
     *
     * @param deviceName      the device name
     * @param platformVersion the iOS version
     */
    public static void initIOSBrowserDriver(String deviceName, String platformVersion) {
        log.info("Initializing iOS Safari driver for device: {}", deviceName);

        XCUITestOptions options = new XCUITestOptions()
                .setDeviceName(deviceName)
                .setPlatformVersion(platformVersion)
                .setAutomationName("XCUITest")
                .withBrowserName("Safari");

        try {
            URL appiumUrl = new URL(getAppiumUrl());
            IOSDriver driver = new IOSDriver(appiumUrl, options);
            configureDriver(driver);
            driverThreadLocal.set(driver);
            log.info("iOS Safari driver initialized successfully");
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid Appium URL", e);
        }
    }

    /**
     * Configures the driver with standard timeouts.
     */
    private static void configureDriver(AppiumDriver driver) {
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(config.getImplicitWait()));
    }

    /**
     * Gets the Appium server URL.
     */
    private static String getAppiumUrl() {
        return config.get("appium.url", DEFAULT_APPIUM_URL);
    }

    /**
     * Quits the driver and removes it from the thread local.
     */
    public static void quitDriver() {
        AppiumDriver driver = driverThreadLocal.get();
        if (driver != null) {
            try {
                log.info("Quitting mobile driver");
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
     */
    public static boolean hasDriver() {
        return driverThreadLocal.get() != null;
    }

    /**
     * Takes a screenshot of the current screen.
     *
     * @return the screenshot as bytes
     */
    public static byte[] takeScreenshot() {
        if (hasDriver()) {
            return getDriver().getScreenshotAs(org.openqa.selenium.OutputType.BYTES);
        }
        return new byte[0];
    }
}
