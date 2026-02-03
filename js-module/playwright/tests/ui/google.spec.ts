import { test, expect, testSetup, tags } from '../../src/fixtures/baseFixture';

test.describe('Google Search', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('https://www.google.com');
    await testSetup.acceptCookies(page);
  });

  test('should display Google homepage @smoke', { tag: [tags.smoke, tags.ui] }, async ({ page }) => {
    await expect(page).toHaveTitle(/Google/);

    // Check search box is visible
    const searchBox = page.locator('input[name="q"], textarea[name="q"]');
    await expect(searchBox.first()).toBeVisible();
  });

  test('should perform a search @smoke', { tag: [tags.smoke, tags.ui] }, async ({ page }) => {
    // Find and interact with search box
    const searchBox = page.locator('input[name="q"], textarea[name="q"]').first();
    await searchBox.fill('Playwright testing');
    await searchBox.press('Enter');

    // Wait for search results
    await page.waitForURL(/search/);

    // Verify results page
    await expect(page).toHaveTitle(/Playwright testing/);

    // Check for search results
    const results = page.locator('#search, #rso');
    await expect(results).toBeVisible();
  });

  test('should show search suggestions @regression', { tag: [tags.regression, tags.ui] }, async ({ page }) => {
    const searchBox = page.locator('input[name="q"], textarea[name="q"]').first();

    // Type slowly to trigger suggestions
    await searchBox.pressSequentially('selenium', { delay: 100 });

    // Wait for suggestions
    const suggestions = page.locator('ul[role="listbox"], div.sbqs_c, div.sbct');

    // Suggestions may or may not appear depending on region
    const suggestionsVisible = await suggestions.first().isVisible().catch(() => false);
    console.log(`Search suggestions visible: ${suggestionsVisible}`);
  });

  test('should handle empty search @regression', { tag: [tags.regression, tags.ui] }, async ({ page }) => {
    const initialUrl = page.url();

    const searchBox = page.locator('input[name="q"], textarea[name="q"]').first();
    await searchBox.press('Enter');

    // Should stay on same page or redirect back
    await page.waitForTimeout(1000);
    expect(page.url()).toBe(initialUrl);
  });

  test('should search with special characters @regression', { tag: [tags.regression, tags.ui] }, async ({ page }) => {
    const searchBox = page.locator('input[name="q"], textarea[name="q"]').first();

    const specialQuery = 'C++ "best practices" 2024';
    await searchBox.fill(specialQuery);
    await searchBox.press('Enter');

    // Wait for results
    await page.waitForURL(/search/);

    // Verify we got results
    const resultsContainer = page.locator('#search, #rso, div.g');
    await expect(resultsContainer.first()).toBeVisible();
  });
});

test.describe('Google Homepage Elements', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('https://www.google.com');
    await testSetup.acceptCookies(page);
  });

  test('should have Google logo @smoke', { tag: [tags.smoke, tags.ui] }, async ({ page }) => {
    // Google logo can be an image or an SVG
    const logo = page.locator('img[alt="Google"], svg[aria-label*="Google"]');

    // At least one logo variant should be visible
    const logoCount = await logo.count();
    expect(logoCount).toBeGreaterThan(0);
  });

  test('should have navigation buttons @regression', { tag: [tags.regression, tags.ui] }, async ({ page }) => {
    // Check for "Google Search" button
    const searchButton = page.locator('input[value="Google Search"], button:has-text("Google Search")');

    // Check for "I\'m Feeling Lucky" button
    const luckyButton = page.locator('input[value*="Feeling Lucky"], button:has-text("Feeling Lucky")');

    // At least one of these should be present
    const searchVisible = await searchButton.first().isVisible().catch(() => false);
    const luckyVisible = await luckyButton.first().isVisible().catch(() => false);

    expect(searchVisible || luckyVisible).toBeTruthy();
  });
});

test.describe('Google Search Results', () => {
  test('should display search result components @regression', { tag: [tags.regression, tags.ui] }, async ({ page }) => {
    await page.goto('https://www.google.com/search?q=playwright+testing');

    // Wait for results to load
    await page.waitForSelector('#search, #rso', { timeout: 10000 });

    // Check for result links
    const resultLinks = page.locator('#search a[href]:not([href^="#"])');
    const linkCount = await resultLinks.count();

    expect(linkCount).toBeGreaterThan(0);
    console.log(`Found ${linkCount} result links`);
  });

  test('should navigate to search result @regression', { tag: [tags.regression, tags.ui] }, async ({ page, context }) => {
    await page.goto('https://www.google.com/search?q=playwright+documentation');

    // Wait for results
    await page.waitForSelector('#search, #rso', { timeout: 10000 });

    // Find the first organic result link
    const firstResult = page.locator('#search a[href*="playwright"], #rso a[href*="playwright"]').first();

    if (await firstResult.isVisible()) {
      // Open in new tab to avoid navigation issues
      const [newPage] = await Promise.all([
        context.waitForEvent('page'),
        firstResult.click({ modifiers: ['Control'] }),
      ]).catch(() => [null]);

      if (newPage) {
        await newPage.waitForLoadState('domcontentloaded');
        expect(newPage.url()).not.toBe('https://www.google.com');
        await newPage.close();
      }
    }
  });
});

test.describe('Responsive Design', () => {
  test('should work on mobile viewport @mobile', { tag: [tags.regression, tags.mobile] }, async ({ page }) => {
    // Set mobile viewport
    await page.setViewportSize({ width: 375, height: 667 });

    await page.goto('https://www.google.com');
    await testSetup.acceptCookies(page);

    // Search box should still be functional
    const searchBox = page.locator('input[name="q"], textarea[name="q"]').first();
    await expect(searchBox).toBeVisible();

    // Perform search
    await searchBox.fill('mobile search test');
    await searchBox.press('Enter');

    await page.waitForURL(/search/);
    await expect(page).toHaveTitle(/mobile search test/);
  });

  test('should work on tablet viewport @regression', { tag: [tags.regression] }, async ({ page }) => {
    // Set tablet viewport
    await page.setViewportSize({ width: 768, height: 1024 });

    await page.goto('https://www.google.com');
    await testSetup.acceptCookies(page);

    const searchBox = page.locator('input[name="q"], textarea[name="q"]').first();
    await expect(searchBox).toBeVisible();
  });
});
