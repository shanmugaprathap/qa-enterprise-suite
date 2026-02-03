import { Page, Locator, expect } from '@playwright/test';
import { SelfHealingLocator } from '../ai/SelfHealingLocator';

/**
 * Base page object class providing common functionality for all page objects.
 * Includes self-healing locator support and comprehensive element interaction methods.
 */
export abstract class BasePage {
  readonly page: Page;
  readonly selfHealingLocator: SelfHealingLocator;

  constructor(page: Page) {
    this.page = page;
    this.selfHealingLocator = new SelfHealingLocator(page);
  }

  /**
   * Gets the page title.
   */
  async getPageTitle(): Promise<string> {
    return await this.page.title();
  }

  /**
   * Gets the current URL.
   */
  getCurrentUrl(): string {
    return this.page.url();
  }

  /**
   * Navigates to a URL.
   * @param url - The URL to navigate to
   */
  async navigateTo(url: string): Promise<void> {
    await this.page.goto(url);
    await this.waitForPageLoad();
  }

  /**
   * Navigates to a path relative to the base URL.
   * @param path - The relative path
   */
  async navigateToPath(path: string): Promise<void> {
    await this.page.goto(path);
    await this.waitForPageLoad();
  }

  /**
   * Refreshes the current page.
   */
  async refreshPage(): Promise<void> {
    await this.page.reload();
    await this.waitForPageLoad();
  }

  /**
   * Navigates back.
   */
  async goBack(): Promise<void> {
    await this.page.goBack();
  }

  /**
   * Navigates forward.
   */
  async goForward(): Promise<void> {
    await this.page.goForward();
  }

  /**
   * Waits for the page to fully load.
   */
  async waitForPageLoad(): Promise<void> {
    await this.page.waitForLoadState('domcontentloaded');
    await this.page.waitForLoadState('networkidle').catch(() => {
      // Network idle may timeout on some pages
    });
  }

  /**
   * Clicks on an element.
   * @param locator - The element locator
   */
  async click(locator: Locator): Promise<void> {
    await locator.click();
  }

  /**
   * Double-clicks on an element.
   * @param locator - The element locator
   */
  async doubleClick(locator: Locator): Promise<void> {
    await locator.dblclick();
  }

  /**
   * Right-clicks on an element.
   * @param locator - The element locator
   */
  async rightClick(locator: Locator): Promise<void> {
    await locator.click({ button: 'right' });
  }

  /**
   * Types text into an element.
   * @param locator - The element locator
   * @param text - The text to type
   * @param options - Optional typing options
   */
  async type(
    locator: Locator,
    text: string,
    options?: { clear?: boolean; delay?: number }
  ): Promise<void> {
    if (options?.clear !== false) {
      await locator.clear();
    }
    await locator.fill(text);
  }

  /**
   * Types text character by character (simulates real typing).
   * @param locator - The element locator
   * @param text - The text to type
   * @param delay - Delay between keystrokes in ms
   */
  async typeSlowly(locator: Locator, text: string, delay = 50): Promise<void> {
    await locator.clear();
    await locator.pressSequentially(text, { delay });
  }

  /**
   * Gets the text content of an element.
   * @param locator - The element locator
   */
  async getText(locator: Locator): Promise<string> {
    return (await locator.textContent()) || '';
  }

  /**
   * Gets the inner text of an element.
   * @param locator - The element locator
   */
  async getInnerText(locator: Locator): Promise<string> {
    return await locator.innerText();
  }

  /**
   * Gets the value of an input element.
   * @param locator - The element locator
   */
  async getValue(locator: Locator): Promise<string> {
    return await locator.inputValue();
  }

  /**
   * Gets an attribute value of an element.
   * @param locator - The element locator
   * @param attribute - The attribute name
   */
  async getAttribute(locator: Locator, attribute: string): Promise<string | null> {
    return await locator.getAttribute(attribute);
  }

  /**
   * Checks if an element is visible.
   * @param locator - The element locator
   */
  async isVisible(locator: Locator): Promise<boolean> {
    return await locator.isVisible();
  }

  /**
   * Checks if an element is enabled.
   * @param locator - The element locator
   */
  async isEnabled(locator: Locator): Promise<boolean> {
    return await locator.isEnabled();
  }

  /**
   * Checks if a checkbox/radio is checked.
   * @param locator - The element locator
   */
  async isChecked(locator: Locator): Promise<boolean> {
    return await locator.isChecked();
  }

  /**
   * Selects an option from a dropdown by value.
   * @param locator - The select element locator
   * @param value - The option value
   */
  async selectByValue(locator: Locator, value: string): Promise<void> {
    await locator.selectOption({ value });
  }

  /**
   * Selects an option from a dropdown by label.
   * @param locator - The select element locator
   * @param label - The option label
   */
  async selectByLabel(locator: Locator, label: string): Promise<void> {
    await locator.selectOption({ label });
  }

  /**
   * Selects an option from a dropdown by index.
   * @param locator - The select element locator
   * @param index - The option index
   */
  async selectByIndex(locator: Locator, index: number): Promise<void> {
    await locator.selectOption({ index });
  }

  /**
   * Checks a checkbox.
   * @param locator - The checkbox locator
   */
  async check(locator: Locator): Promise<void> {
    await locator.check();
  }

  /**
   * Unchecks a checkbox.
   * @param locator - The checkbox locator
   */
  async uncheck(locator: Locator): Promise<void> {
    await locator.uncheck();
  }

  /**
   * Hovers over an element.
   * @param locator - The element locator
   */
  async hover(locator: Locator): Promise<void> {
    await locator.hover();
  }

  /**
   * Drags an element and drops it on another.
   * @param source - The source element locator
   * @param target - The target element locator
   */
  async dragAndDrop(source: Locator, target: Locator): Promise<void> {
    await source.dragTo(target);
  }

  /**
   * Scrolls an element into view.
   * @param locator - The element locator
   */
  async scrollIntoView(locator: Locator): Promise<void> {
    await locator.scrollIntoViewIfNeeded();
  }

  /**
   * Waits for an element to be visible.
   * @param locator - The element locator
   * @param timeout - Optional timeout in ms
   */
  async waitForVisible(locator: Locator, timeout?: number): Promise<void> {
    await locator.waitFor({ state: 'visible', timeout });
  }

  /**
   * Waits for an element to be hidden.
   * @param locator - The element locator
   * @param timeout - Optional timeout in ms
   */
  async waitForHidden(locator: Locator, timeout?: number): Promise<void> {
    await locator.waitFor({ state: 'hidden', timeout });
  }

  /**
   * Takes a screenshot of the page.
   * @param name - Optional screenshot name
   */
  async takeScreenshot(name?: string): Promise<Buffer> {
    return await this.page.screenshot({
      fullPage: true,
      path: name ? `screenshots/${name}.png` : undefined,
    });
  }

  /**
   * Takes a screenshot of a specific element.
   * @param locator - The element locator
   * @param name - Optional screenshot name
   */
  async takeElementScreenshot(locator: Locator, name?: string): Promise<Buffer> {
    return await locator.screenshot({
      path: name ? `screenshots/${name}.png` : undefined,
    });
  }

  /**
   * Presses a key or key combination.
   * @param key - The key to press (e.g., 'Enter', 'Control+A')
   */
  async pressKey(key: string): Promise<void> {
    await this.page.keyboard.press(key);
  }

  /**
   * Waits for a specific URL pattern.
   * @param urlPattern - The URL pattern to wait for
   * @param timeout - Optional timeout in ms
   */
  async waitForUrl(urlPattern: string | RegExp, timeout?: number): Promise<void> {
    await this.page.waitForURL(urlPattern, { timeout });
  }

  /**
   * Waits for a network request to complete.
   * @param urlPattern - The URL pattern to match
   */
  async waitForRequest(urlPattern: string | RegExp): Promise<void> {
    await this.page.waitForRequest(urlPattern);
  }

  /**
   * Waits for a network response.
   * @param urlPattern - The URL pattern to match
   */
  async waitForResponse(urlPattern: string | RegExp): Promise<void> {
    await this.page.waitForResponse(urlPattern);
  }

  /**
   * Evaluates JavaScript in the page context.
   * @param pageFunction - The function to evaluate
   */
  async evaluate<T>(pageFunction: () => T): Promise<T> {
    return await this.page.evaluate(pageFunction);
  }

  /**
   * Gets the count of elements matching a locator.
   * @param locator - The element locator
   */
  async getElementCount(locator: Locator): Promise<number> {
    return await locator.count();
  }

  /**
   * Gets all texts from elements matching a locator.
   * @param locator - The element locator
   */
  async getAllTexts(locator: Locator): Promise<string[]> {
    return await locator.allTextContents();
  }

  /**
   * Uploads a file to an input element.
   * @param locator - The file input locator
   * @param filePath - The path to the file
   */
  async uploadFile(locator: Locator, filePath: string | string[]): Promise<void> {
    await locator.setInputFiles(filePath);
  }

  /**
   * Handles a dialog (alert, confirm, prompt).
   * @param action - The action to take ('accept' or 'dismiss')
   * @param promptText - Optional text to enter for prompts
   */
  async handleDialog(action: 'accept' | 'dismiss', promptText?: string): Promise<void> {
    this.page.once('dialog', async (dialog) => {
      if (action === 'accept') {
        await dialog.accept(promptText);
      } else {
        await dialog.dismiss();
      }
    });
  }

  /**
   * Asserts that an element is visible.
   * @param locator - The element locator
   */
  async assertVisible(locator: Locator): Promise<void> {
    await expect(locator).toBeVisible();
  }

  /**
   * Asserts that an element contains specific text.
   * @param locator - The element locator
   * @param text - The expected text
   */
  async assertContainsText(locator: Locator, text: string): Promise<void> {
    await expect(locator).toContainText(text);
  }

  /**
   * Asserts that the page title matches.
   * @param title - The expected title
   */
  async assertTitle(title: string | RegExp): Promise<void> {
    await expect(this.page).toHaveTitle(title);
  }

  /**
   * Asserts that the URL matches.
   * @param url - The expected URL
   */
  async assertUrl(url: string | RegExp): Promise<void> {
    await expect(this.page).toHaveURL(url);
  }
}
