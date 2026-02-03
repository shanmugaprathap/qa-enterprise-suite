import { test as base, Page } from '@playwright/test';
import { BasePage } from '../pages/BasePage';
import { ElementHelper } from '../helpers/ElementHelper';
import { SelfHealingLocator } from '../ai/SelfHealingLocator';

/**
 * Extended test fixture with common utilities.
 */
interface TestFixtures {
  elementHelper: ElementHelper;
  selfHealingLocator: SelfHealingLocator;
}

/**
 * Extended page fixtures.
 */
interface PageFixtures {
  basePage: TestBasePage;
}

/**
 * Simple test page implementation.
 */
class TestBasePage extends BasePage {
  constructor(page: Page) {
    super(page);
  }
}

/**
 * Extended test with common fixtures.
 */
export const test = base.extend<TestFixtures & PageFixtures>({
  // Element helper fixture
  elementHelper: async ({ page }, use) => {
    const helper = new ElementHelper(page);
    await use(helper);
  },

  // Self-healing locator fixture
  selfHealingLocator: async ({ page }, use) => {
    const healer = new SelfHealingLocator(page);
    await use(healer);
    // Cleanup
    healer.clearCache();
  },

  // Base page fixture
  basePage: async ({ page }, use) => {
    const basePage = new TestBasePage(page);
    await use(basePage);
  },
});

export { expect } from '@playwright/test';

/**
 * Custom annotations for tests.
 */
export const annotations = {
  smoke: { type: 'smoke', description: 'Smoke test' },
  regression: { type: 'regression', description: 'Regression test' },
  critical: { type: 'critical', description: 'Critical path test' },
  flaky: { type: 'flaky', description: 'Known flaky test' },
};

/**
 * Helper to add test tags.
 * Usage: test('my test', { tag: ['@smoke', '@critical'] }, async ({ page }) => { ... });
 */
export const tags = {
  smoke: '@smoke',
  regression: '@regression',
  critical: '@critical',
  api: '@api',
  ui: '@ui',
  mobile: '@mobile',
};

/**
 * Common test setup functions.
 */
export const testSetup = {
  /**
   * Accepts cookies if dialog appears.
   */
  acceptCookies: async (page: Page): Promise<void> => {
    const cookieButtons = [
      page.locator('button:has-text("Accept")'),
      page.locator('button:has-text("I agree")'),
      page.locator('[id*="accept"]'),
      page.locator('[class*="cookie"] button'),
    ];

    for (const button of cookieButtons) {
      try {
        if ((await button.count()) > 0 && (await button.first().isVisible())) {
          await button.first().click({ timeout: 3000 });
          return;
        }
      } catch {
        // Try next
      }
    }
  },

  /**
   * Waits for page to be fully loaded.
   */
  waitForFullLoad: async (page: Page): Promise<void> => {
    await page.waitForLoadState('domcontentloaded');
    try {
      await page.waitForLoadState('networkidle', { timeout: 10000 });
    } catch {
      // Network idle may timeout
    }
  },

  /**
   * Clears browser storage.
   */
  clearStorage: async (page: Page): Promise<void> => {
    await page.evaluate(() => {
      localStorage.clear();
      sessionStorage.clear();
    });
  },

  /**
   * Sets up request interception for API mocking.
   */
  setupMocking: async (page: Page, mocks: Map<string, object>): Promise<void> => {
    for (const [url, response] of mocks) {
      await page.route(url, (route) => {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(response),
        });
      });
    }
  },
};

/**
 * Common assertions.
 */
export const assertions = {
  /**
   * Asserts page has no console errors.
   */
  noConsoleErrors: async (page: Page): Promise<void> => {
    const errors: string[] = [];
    page.on('console', (msg) => {
      if (msg.type() === 'error') {
        errors.push(msg.text());
      }
    });

    // Return a checker function
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        if (errors.length > 0) {
          reject(new Error(`Console errors found: ${errors.join(', ')}`));
        } else {
          resolve();
        }
      }, 1000);
    });
  },

  /**
   * Asserts no broken images on page.
   */
  noBrokenImages: async (page: Page): Promise<void> => {
    const brokenImages = await page.evaluate(() => {
      const images = document.querySelectorAll('img');
      return Array.from(images)
        .filter((img) => !img.complete || img.naturalWidth === 0)
        .map((img) => img.src);
    });

    if (brokenImages.length > 0) {
      throw new Error(`Broken images found: ${brokenImages.join(', ')}`);
    }
  },

  /**
   * Asserts page accessibility.
   */
  accessible: async (page: Page): Promise<void> => {
    // Basic accessibility checks
    const issues = await page.evaluate(() => {
      const problems: string[] = [];

      // Check for images without alt
      const imagesWithoutAlt = document.querySelectorAll('img:not([alt])');
      if (imagesWithoutAlt.length > 0) {
        problems.push(`${imagesWithoutAlt.length} images without alt attribute`);
      }

      // Check for form inputs without labels
      const inputsWithoutLabels = document.querySelectorAll(
        'input:not([type="hidden"]):not([aria-label]):not([aria-labelledby])'
      );
      const orphanInputs = Array.from(inputsWithoutLabels).filter((input) => {
        const id = input.id;
        if (!id) return true;
        return !document.querySelector(`label[for="${id}"]`);
      });
      if (orphanInputs.length > 0) {
        problems.push(`${orphanInputs.length} inputs without labels`);
      }

      // Check for missing lang attribute
      if (!document.documentElement.lang) {
        problems.push('Missing lang attribute on html element');
      }

      return problems;
    });

    if (issues.length > 0) {
      throw new Error(`Accessibility issues: ${issues.join('; ')}`);
    }
  },
};
