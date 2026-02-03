# Mobile Testing Guide

This guide covers mobile application testing using Appium for iOS and Android platforms.

## Overview

Mobile testing verifies applications work correctly on mobile devices, covering native apps, hybrid apps, and mobile web browsers.

## Prerequisites

### Software Requirements

| Component | Version | Purpose |
|-----------|---------|---------|
| Node.js | 18+ | Appium runtime |
| Appium | 2.x | Mobile automation |
| Java | 17+ | Test framework |
| Xcode | 15+ | iOS development |
| Android Studio | Latest | Android development |

### Installation

```bash
# Install Appium
npm install -g appium

# Install drivers
appium driver install uiautomator2  # Android
appium driver install xcuitest      # iOS

# Install Appium Doctor (verification)
npm install -g appium-doctor

# Verify setup
appium-doctor --android
appium-doctor --ios
```

## Environment Setup

### Android Setup

1. **Install Android SDK**
   ```bash
   # Set environment variables
   export ANDROID_HOME=$HOME/Library/Android/sdk
   export PATH=$PATH:$ANDROID_HOME/tools
   export PATH=$PATH:$ANDROID_HOME/platform-tools
   ```

2. **Create Emulator**
   ```bash
   # List available system images
   sdkmanager --list

   # Install system image
   sdkmanager "system-images;android-34;google_apis;arm64-v8a"

   # Create AVD
   avdmanager create avd -n Pixel_7_API_34 \
     -k "system-images;android-34;google_apis;arm64-v8a" \
     -d "pixel_7"

   # Start emulator
   emulator -avd Pixel_7_API_34
   ```

3. **Connect Real Device**
   ```bash
   # Enable USB debugging on device
   # Verify connection
   adb devices
   ```

### iOS Setup (macOS only)

1. **Install Xcode**
   - Download from App Store
   - Install command line tools: `xcode-select --install`

2. **Create Simulator**
   ```bash
   # List available simulators
   xcrun simctl list devices

   # Create simulator
   xcrun simctl create "iPhone 15" com.apple.CoreSimulator.SimDeviceType.iPhone-15

   # Boot simulator
   xcrun simctl boot "iPhone 15"
   ```

3. **Configure WebDriverAgent**
   ```bash
   # Build WDA
   cd ~/.appium/node_modules/appium-xcuitest-driver/node_modules/appium-webdriveragent

   # Open in Xcode
   open WebDriverAgent.xcodeproj

   # Configure signing team and build
   ```

## Appium Capabilities

### Android Capabilities

```java
UiAutomator2Options options = new UiAutomator2Options()
    .setDeviceName("Pixel_7_API_34")
    .setPlatformVersion("14")
    .setAutomationName("UiAutomator2")
    .setApp("/path/to/app.apk")
    .setAutoGrantPermissions(true)
    .setNoReset(false)
    .setFullReset(false)
    .setNewCommandTimeout(Duration.ofSeconds(300));
```

### iOS Capabilities

```java
XCUITestOptions options = new XCUITestOptions()
    .setDeviceName("iPhone 15")
    .setPlatformVersion("17.0")
    .setAutomationName("XCUITest")
    .setApp("/path/to/app.app")
    .setAutoAcceptAlerts(true)
    .setNoReset(false);
```

### Mobile Browser Testing

```java
// Android Chrome
UiAutomator2Options options = new UiAutomator2Options()
    .setDeviceName("Pixel_7")
    .withBrowserName("Chrome");

// iOS Safari
XCUITestOptions options = new XCUITestOptions()
    .setDeviceName("iPhone 15")
    .withBrowserName("Safari");
```

## Writing Mobile Tests

### Page Object Pattern

```java
public class LoginPage {
    private AppiumDriver driver;

    // Locators
    private By usernameField = AppiumBy.accessibilityId("username");
    private By passwordField = AppiumBy.accessibilityId("password");
    private By loginButton = AppiumBy.accessibilityId("login-btn");

    public LoginPage(AppiumDriver driver) {
        this.driver = driver;
    }

    public void login(String username, String password) {
        driver.findElement(usernameField).sendKeys(username);
        driver.findElement(passwordField).sendKeys(password);
        driver.findElement(loginButton).click();
    }
}
```

### Mobile-Specific Actions

```java
// Scroll
driver.findElement(AppiumBy.androidUIAutomator(
    "new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(" +
    "new UiSelector().text(\"Target Text\"))"));

// Swipe
PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
Sequence swipe = new Sequence(finger, 0);
swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), 500, 1500));
swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
swipe.addAction(finger.createPointerMove(Duration.ofMillis(500), PointerInput.Origin.viewport(), 500, 500));
swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
driver.perform(Arrays.asList(swipe));

// Long Press
WebElement element = driver.findElement(locator);
Actions actions = new Actions(driver);
actions.clickAndHold(element).pause(Duration.ofSeconds(2)).release().perform();

// Tap by coordinates
new TouchAction(driver)
    .tap(PointOption.point(x, y))
    .perform();
```

### Handling Alerts

```java
// Android
driver.findElement(AppiumBy.id("android:id/button1")).click();

// iOS (auto-accept configured)
// Or handle manually:
driver.switchTo().alert().accept();
```

## Locator Strategies

### Priority Order

1. **Accessibility ID** (Recommended)
   ```java
   AppiumBy.accessibilityId("login-button")
   ```

2. **ID (resource-id for Android)**
   ```java
   AppiumBy.id("com.app:id/login_button")
   ```

3. **XPath** (Last resort)
   ```java
   AppiumBy.xpath("//XCUIElementTypeButton[@name='Login']")
   ```

### Platform-Specific Locators

```java
// Android UIAutomator
AppiumBy.androidUIAutomator("new UiSelector().text(\"Login\")")

// iOS Class Chain
AppiumBy.iOSClassChain("**/XCUIElementTypeButton[`label == \"Login\"`]")

// iOS Predicate
AppiumBy.iOSNsPredicateString("label == 'Login'")
```

## Device Farm Integration

### AWS Device Farm

```java
// Configure for AWS Device Farm
DesiredCapabilities caps = new DesiredCapabilities();
caps.setCapability("testobject_api_key", System.getenv("DEVICE_FARM_API_KEY"));
caps.setCapability("testobject_app_id", "1");
caps.setCapability("deviceName", "Samsung Galaxy S21");

driver = new AndroidDriver(
    new URL("https://us-west-2.devicefarm.amazonaws.com/wd/hub"),
    caps
);
```

### BrowserStack

```java
MutableCapabilities caps = new MutableCapabilities();
caps.setCapability("browserstack.user", System.getenv("BROWSERSTACK_USER"));
caps.setCapability("browserstack.key", System.getenv("BROWSERSTACK_KEY"));
caps.setCapability("app", "bs://app_hashed_id");
caps.setCapability("device", "Samsung Galaxy S23");
caps.setCapability("os_version", "13.0");

driver = new AndroidDriver(
    new URL("https://hub.browserstack.com/wd/hub"),
    caps
);
```

## CI/CD Integration

### GitHub Actions

```yaml
jobs:
  mobile-tests:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v4

      - name: Setup JDK
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '18'

      - name: Install Appium
        run: |
          npm install -g appium
          appium driver install uiautomator2
          appium driver install xcuitest

      - name: Start Android Emulator
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 34
          target: google_apis
          arch: x86_64
          script: |
            mvn test -pl mobile-appium -Dplatform=android
```

## Best Practices

### 1. Test Isolation

- Reset app state between tests (`noReset: false`)
- Clear app data for critical tests (`fullReset: true`)

### 2. Wait Strategies

```java
// Explicit wait
WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
wait.until(ExpectedConditions.visibilityOfElementLocated(locator));

// Fluent wait
Wait<AppiumDriver> fluentWait = new FluentWait<>(driver)
    .withTimeout(Duration.ofSeconds(30))
    .pollingEvery(Duration.ofSeconds(2))
    .ignoring(NoSuchElementException.class);
```

### 3. Screenshot on Failure

```java
@AfterMethod
public void captureScreenshot(ITestResult result) {
    if (result.getStatus() == ITestResult.FAILURE) {
        File screenshot = driver.getScreenshotAs(OutputType.FILE);
        Files.copy(screenshot.toPath(),
            Paths.get("screenshots/" + result.getName() + ".png"));
    }
}
```

### 4. Parallel Execution

```xml
<!-- testng.xml -->
<suite name="Mobile Suite" parallel="tests" thread-count="2">
    <test name="Android Tests">
        <parameter name="platform" value="android"/>
        <classes>
            <class name="com.enterprise.qa.mobile.tests.LoginTest"/>
        </classes>
    </test>
    <test name="iOS Tests">
        <parameter name="platform" value="ios"/>
        <classes>
            <class name="com.enterprise.qa.mobile.tests.LoginTest"/>
        </classes>
    </test>
</suite>
```

## Troubleshooting

### Common Issues

| Issue | Solution |
|-------|----------|
| Session not created | Check Appium server is running |
| Element not found | Verify locator strategy and wait |
| App not installed | Check APK/IPA path and signing |
| Connection refused | Check device/emulator connection |

### Debug Commands

```bash
# Android
adb devices
adb logcat
adb shell dumpsys activity

# iOS
xcrun simctl list
xcrun simctl logverbose booted
```

### Appium Inspector

Use Appium Inspector for element inspection:
1. Download from: https://github.com/appium/appium-inspector
2. Connect to Appium server
3. Start session with capabilities
4. Inspect elements and generate locators
