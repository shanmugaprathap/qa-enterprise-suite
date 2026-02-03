package com.enterprise.qa.ui.builders;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Builder class for creating customized Chrome browser profiles.
 * Supports various configurations for different testing scenarios.
 */
@Slf4j
public class ChromeProfileBuilder {

    private final ChromeOptions options;
    private final Map<String, Object> prefs;

    public ChromeProfileBuilder() {
        this.options = new ChromeOptions();
        this.prefs = new HashMap<>();
        applyDefaults();
    }

    /**
     * Applies default Chrome options suitable for testing.
     */
    private void applyDefaults() {
        options.addArguments("--start-maximized");
        options.addArguments("--disable-infobars");
        options.addArguments("--disable-extensions");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--remote-allow-origins=*");

        // Disable password manager
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
    }

    /**
     * Enables headless mode.
     *
     * @return the builder
     */
    public ChromeProfileBuilder headless() {
        options.addArguments("--headless=new");
        options.addArguments("--window-size=1920,1080");
        log.debug("Headless mode enabled");
        return this;
    }

    /**
     * Sets a custom window size.
     *
     * @param width  the width in pixels
     * @param height the height in pixels
     * @return the builder
     */
    public ChromeProfileBuilder windowSize(int width, int height) {
        options.addArguments("--window-size=" + width + "," + height);
        log.debug("Window size set to {}x{}", width, height);
        return this;
    }

    /**
     * Enables mobile emulation.
     *
     * @param deviceName the device name (e.g., "iPhone X", "Pixel 5")
     * @return the builder
     */
    public ChromeProfileBuilder mobileEmulation(String deviceName) {
        Map<String, String> mobileEmulation = new HashMap<>();
        mobileEmulation.put("deviceName", deviceName);
        options.setExperimentalOption("mobileEmulation", mobileEmulation);
        log.debug("Mobile emulation enabled for: {}", deviceName);
        return this;
    }

    /**
     * Enables mobile emulation with custom dimensions.
     *
     * @param width      the viewport width
     * @param height     the viewport height
     * @param pixelRatio the device pixel ratio
     * @param userAgent  the user agent string
     * @return the builder
     */
    public ChromeProfileBuilder mobileEmulation(int width, int height, double pixelRatio, String userAgent) {
        Map<String, Object> deviceMetrics = new HashMap<>();
        deviceMetrics.put("width", width);
        deviceMetrics.put("height", height);
        deviceMetrics.put("pixelRatio", pixelRatio);

        Map<String, Object> mobileEmulation = new HashMap<>();
        mobileEmulation.put("deviceMetrics", deviceMetrics);
        if (userAgent != null) {
            mobileEmulation.put("userAgent", userAgent);
        }

        options.setExperimentalOption("mobileEmulation", mobileEmulation);
        log.debug("Custom mobile emulation enabled: {}x{} @ {}x", width, height, pixelRatio);
        return this;
    }

    /**
     * Disables notifications.
     *
     * @return the builder
     */
    public ChromeProfileBuilder disableNotifications() {
        prefs.put("profile.default_content_setting_values.notifications", 2);
        log.debug("Notifications disabled");
        return this;
    }

    /**
     * Disables geolocation prompts.
     *
     * @return the builder
     */
    public ChromeProfileBuilder disableGeolocation() {
        prefs.put("profile.default_content_setting_values.geolocation", 2);
        log.debug("Geolocation prompts disabled");
        return this;
    }

    /**
     * Sets a custom download directory.
     *
     * @param downloadPath the download directory path
     * @return the builder
     */
    public ChromeProfileBuilder downloadDirectory(String downloadPath) {
        File downloadDir = new File(downloadPath);
        if (!downloadDir.exists()) {
            downloadDir.mkdirs();
        }

        prefs.put("download.default_directory", downloadDir.getAbsolutePath());
        prefs.put("download.prompt_for_download", false);
        prefs.put("download.directory_upgrade", true);
        prefs.put("safebrowsing.enabled", true);

        log.debug("Download directory set to: {}", downloadPath);
        return this;
    }

    /**
     * Sets a proxy server.
     *
     * @param proxyAddress the proxy address (e.g., "localhost:8080")
     * @return the builder
     */
    public ChromeProfileBuilder proxy(String proxyAddress) {
        options.addArguments("--proxy-server=" + proxyAddress);
        log.debug("Proxy set to: {}", proxyAddress);
        return this;
    }

    /**
     * Sets a custom user agent.
     *
     * @param userAgent the user agent string
     * @return the builder
     */
    public ChromeProfileBuilder userAgent(String userAgent) {
        options.addArguments("--user-agent=" + userAgent);
        log.debug("User agent set to: {}", userAgent);
        return this;
    }

    /**
     * Uses an existing Chrome profile.
     *
     * @param profilePath the path to the Chrome profile
     * @return the builder
     */
    public ChromeProfileBuilder useProfile(String profilePath) {
        options.addArguments("--user-data-dir=" + profilePath);
        log.debug("Using Chrome profile: {}", profilePath);
        return this;
    }

    /**
     * Disables GPU acceleration.
     *
     * @return the builder
     */
    public ChromeProfileBuilder disableGpu() {
        options.addArguments("--disable-gpu");
        log.debug("GPU disabled");
        return this;
    }

    /**
     * Enables incognito mode.
     *
     * @return the builder
     */
    public ChromeProfileBuilder incognito() {
        options.addArguments("--incognito");
        log.debug("Incognito mode enabled");
        return this;
    }

    /**
     * Enables performance logging.
     *
     * @return the builder
     */
    public ChromeProfileBuilder enablePerformanceLogging() {
        Map<String, Object> perfLogPrefs = new HashMap<>();
        perfLogPrefs.put("enableNetwork", true);
        perfLogPrefs.put("enablePage", true);

        options.setExperimentalOption("perfLoggingPrefs", perfLogPrefs);
        options.setCapability("goog:loggingPrefs", Map.of("performance", "ALL"));

        log.debug("Performance logging enabled");
        return this;
    }

    /**
     * Ignores certificate errors.
     *
     * @return the builder
     */
    public ChromeProfileBuilder ignoreCertificateErrors() {
        options.addArguments("--ignore-certificate-errors");
        options.setAcceptInsecureCerts(true);
        log.debug("Certificate errors ignored");
        return this;
    }

    /**
     * Adds a Chrome extension.
     *
     * @param extensionPath the path to the .crx extension file
     * @return the builder
     */
    public ChromeProfileBuilder addExtension(String extensionPath) {
        File extension = new File(extensionPath);
        if (extension.exists()) {
            options.addExtensions(extension);
            log.debug("Extension added: {}", extensionPath);
        } else {
            log.warn("Extension file not found: {}", extensionPath);
        }
        return this;
    }

    /**
     * Adds a custom argument.
     *
     * @param argument the Chrome argument
     * @return the builder
     */
    public ChromeProfileBuilder addArgument(String argument) {
        options.addArguments(argument);
        log.debug("Added argument: {}", argument);
        return this;
    }

    /**
     * Sets a custom preference.
     *
     * @param key   the preference key
     * @param value the preference value
     * @return the builder
     */
    public ChromeProfileBuilder setPreference(String key, Object value) {
        prefs.put(key, value);
        log.debug("Set preference: {} = {}", key, value);
        return this;
    }

    /**
     * Disables automation detection.
     *
     * @return the builder
     */
    public ChromeProfileBuilder disableAutomationDetection() {
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);
        log.debug("Automation detection disabled");
        return this;
    }

    /**
     * Configures for CI/CD environment.
     *
     * @return the builder
     */
    public ChromeProfileBuilder forCI() {
        headless();
        disableGpu();
        disableNotifications();
        ignoreCertificateErrors();
        disableAutomationDetection();

        options.addArguments("--disable-software-rasterizer");
        options.addArguments("--disable-background-networking");
        options.addArguments("--disable-background-timer-throttling");
        options.addArguments("--disable-client-side-phishing-detection");
        options.addArguments("--disable-default-apps");
        options.addArguments("--disable-hang-monitor");
        options.addArguments("--disable-popup-blocking");
        options.addArguments("--disable-prompt-on-repost");
        options.addArguments("--disable-sync");
        options.addArguments("--disable-translate");
        options.addArguments("--metrics-recording-only");
        options.addArguments("--no-first-run");

        log.debug("Configured for CI/CD environment");
        return this;
    }

    /**
     * Builds and returns the configured ChromeOptions.
     *
     * @return the ChromeOptions
     */
    public ChromeOptions build() {
        options.setExperimentalOption("prefs", prefs);
        log.info("Chrome profile built with {} preferences and {} arguments",
                prefs.size(), options.asMap().size());
        return options;
    }
}
