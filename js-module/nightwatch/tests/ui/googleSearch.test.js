/**
 * Google Search UI Tests - Nightwatch.js
 * Demonstrates comprehensive UI testing with Page Objects
 */

describe('Google Search Tests', function () {
  // Use page object
  let googlePage;

  before(function (browser) {
    googlePage = browser.page.googlePage();
  });

  beforeEach(function (browser) {
    googlePage.navigate();
  });

  after(function (browser) {
    browser.end();
  });

  it('should display Google homepage correctly', function (browser) {
    googlePage
      .waitForElementVisible('@searchBox')
      .assert.visible('@searchBox')
      .assert.visible('@searchButton')
      .assert.titleContains('Google');
  });

  it('should perform a search and display results', function (browser) {
    googlePage
      .search('Nightwatch.js testing')
      .waitForElementVisible('@searchResults')
      .assert.visible('@searchResults')
      .assert.urlContains('search');
  });

  it('should show search suggestions', function (browser) {
    googlePage
      .setValue('@searchBox', 'selenium')
      .waitForElementVisible('@suggestions')
      .assert.visible('@suggestions');
  });

  it('should clear search box', function (browser) {
    googlePage
      .setValue('@searchBox', 'test query')
      .clearValue('@searchBox')
      .assert.value('@searchBox', '');
  });

  it('should navigate using keyboard', function (browser) {
    googlePage
      .setValue('@searchBox', 'Nightwatch automation')
      .sendKeys('@searchBox', browser.Keys.ENTER)
      .waitForElementVisible('@searchResults')
      .assert.urlContains('search');
  });
});

describe('Google Search - Advanced Scenarios', function () {
  let googlePage;

  before(function (browser) {
    googlePage = browser.page.googlePage();
  });

  beforeEach(function (browser) {
    googlePage.navigate();
  });

  after(function (browser) {
    browser.end();
  });

  it('should handle special characters in search', function (browser) {
    googlePage
      .search('test@example.com')
      .waitForElementVisible('@searchResults')
      .assert.visible('@searchResults');
  });

  it('should handle empty search gracefully', function (browser) {
    googlePage
      .click('@searchBox')
      .sendKeys('@searchBox', browser.Keys.ENTER)
      // Should stay on same page or show validation
      .assert.urlContains('google');
  });

  it('should have responsive layout', function (browser) {
    browser
      .windowSize('current', 375, 667) // iPhone SE size
      .pause(500);

    googlePage
      .waitForElementVisible('@searchBox')
      .assert.visible('@searchBox');

    // Reset to desktop size
    browser.windowSize('current', 1920, 1080);
  });

  it('should capture screenshot on test actions', function (browser) {
    googlePage
      .waitForElementVisible('@searchBox')
      .saveScreenshot('reports/screenshots/google-homepage.png');
  });
});

describe('Google Search - Performance Checks', function () {
  let googlePage;
  let startTime;

  before(function (browser) {
    googlePage = browser.page.googlePage();
  });

  beforeEach(function (browser, done) {
    startTime = Date.now();
    googlePage.navigate();
    done();
  });

  after(function (browser) {
    browser.end();
  });

  it('should load within acceptable time', function (browser) {
    googlePage.waitForElementVisible('@searchBox', 5000);

    const loadTime = Date.now() - startTime;
    console.log(`Page load time: ${loadTime}ms`);

    browser.assert.ok(loadTime < 5000, `Page loaded in ${loadTime}ms (expected < 5000ms)`);
  });

  it('should return search results quickly', function (browser) {
    googlePage.setValue('@searchBox', 'performance test');

    const searchStart = Date.now();
    googlePage
      .sendKeys('@searchBox', browser.Keys.ENTER)
      .waitForElementVisible('@searchResults', 10000);

    const searchTime = Date.now() - searchStart;
    console.log(`Search response time: ${searchTime}ms`);

    browser.assert.ok(searchTime < 3000, `Search completed in ${searchTime}ms (expected < 3000ms)`);
  });
});
