import { Page, Locator, ElementHandle } from '@playwright/test';

/**
 * Helper class providing utility methods for element interactions.
 * Includes advanced operations and convenience methods.
 */
export class ElementHelper {
  private readonly page: Page;

  constructor(page: Page) {
    this.page = page;
  }

  /**
   * Waits for an element to be stable (no longer animating).
   * @param locator - The element locator
   * @param timeout - Optional timeout in ms
   */
  async waitForStable(locator: Locator, timeout = 5000): Promise<void> {
    const startTime = Date.now();
    let previousBox = await locator.boundingBox();

    while (Date.now() - startTime < timeout) {
      await this.page.waitForTimeout(100);
      const currentBox = await locator.boundingBox();

      if (
        previousBox &&
        currentBox &&
        previousBox.x === currentBox.x &&
        previousBox.y === currentBox.y &&
        previousBox.width === currentBox.width &&
        previousBox.height === currentBox.height
      ) {
        return;
      }

      previousBox = currentBox;
    }
  }

  /**
   * Highlights an element for debugging purposes.
   * @param locator - The element locator
   * @param duration - How long to highlight in ms
   */
  async highlight(locator: Locator, duration = 2000): Promise<void> {
    await locator.evaluate(
      (el, dur) => {
        const originalStyle = el.getAttribute('style') || '';
        el.setAttribute('style', `${originalStyle}; border: 3px solid red !important; background: rgba(255,0,0,0.1) !important;`);
        setTimeout(() => {
          el.setAttribute('style', originalStyle);
        }, dur);
      },
      duration
    );
  }

  /**
   * Gets the bounding box of an element.
   * @param locator - The element locator
   */
  async getBoundingBox(locator: Locator): Promise<{ x: number; y: number; width: number; height: number } | null> {
    return await locator.boundingBox();
  }

  /**
   * Checks if an element is in the viewport.
   * @param locator - The element locator
   */
  async isInViewport(locator: Locator): Promise<boolean> {
    return await locator.evaluate((el) => {
      const rect = el.getBoundingClientRect();
      return (
        rect.top >= 0 &&
        rect.left >= 0 &&
        rect.bottom <= (window.innerHeight || document.documentElement.clientHeight) &&
        rect.right <= (window.innerWidth || document.documentElement.clientWidth)
      );
    });
  }

  /**
   * Scrolls to an element with offset.
   * @param locator - The element locator
   * @param offset - Offset from the element
   */
  async scrollTo(locator: Locator, offset = { x: 0, y: -100 }): Promise<void> {
    await locator.evaluate(
      (el, off) => {
        const rect = el.getBoundingClientRect();
        window.scrollTo({
          top: window.scrollY + rect.top + off.y,
          left: window.scrollX + rect.left + off.x,
          behavior: 'smooth',
        });
      },
      offset
    );
  }

  /**
   * Gets all classes of an element.
   * @param locator - The element locator
   */
  async getClasses(locator: Locator): Promise<string[]> {
    const classList = await locator.getAttribute('class');
    return classList ? classList.split(/\s+/).filter(Boolean) : [];
  }

  /**
   * Checks if an element has a specific class.
   * @param locator - The element locator
   * @param className - The class to check
   */
  async hasClass(locator: Locator, className: string): Promise<boolean> {
    const classes = await this.getClasses(locator);
    return classes.includes(className);
  }

  /**
   * Gets the computed style of an element.
   * @param locator - The element locator
   * @param property - The CSS property
   */
  async getComputedStyle(locator: Locator, property: string): Promise<string> {
    return await locator.evaluate(
      (el, prop) => window.getComputedStyle(el).getPropertyValue(prop),
      property
    );
  }

  /**
   * Waits for an element to have specific text.
   * @param locator - The element locator
   * @param text - The expected text
   * @param timeout - Optional timeout
   */
  async waitForText(locator: Locator, text: string | RegExp, timeout = 30000): Promise<void> {
    const startTime = Date.now();

    while (Date.now() - startTime < timeout) {
      const currentText = await locator.textContent();

      if (typeof text === 'string') {
        if (currentText?.includes(text)) return;
      } else {
        if (currentText && text.test(currentText)) return;
      }

      await this.page.waitForTimeout(100);
    }

    throw new Error(`Text "${text}" not found in element after ${timeout}ms`);
  }

  /**
   * Gets the shadow root of an element.
   * @param locator - The element locator
   */
  async getShadowRoot(locator: Locator): Promise<Locator> {
    return locator.locator('>> *');
  }

  /**
   * Clicks at specific coordinates relative to an element.
   * @param locator - The element locator
   * @param position - The position within the element
   */
  async clickAtPosition(locator: Locator, position: { x: number; y: number }): Promise<void> {
    await locator.click({ position });
  }

  /**
   * Triple-clicks an element (select all text).
   * @param locator - The element locator
   */
  async tripleClick(locator: Locator): Promise<void> {
    await locator.click({ clickCount: 3 });
  }

  /**
   * Gets the parent element.
   * @param locator - The element locator
   */
  getParent(locator: Locator): Locator {
    return locator.locator('..');
  }

  /**
   * Gets sibling elements.
   * @param locator - The element locator
   */
  getSiblings(locator: Locator): Locator {
    return locator.locator('xpath=following-sibling::* | preceding-sibling::*');
  }

  /**
   * Waits for network to be idle.
   * @param timeout - Optional timeout
   */
  async waitForNetworkIdle(timeout = 5000): Promise<void> {
    await this.page.waitForLoadState('networkidle', { timeout });
  }

  /**
   * Waits for all images to load.
   */
  async waitForImagesLoaded(): Promise<void> {
    await this.page.evaluate(async () => {
      const images = document.querySelectorAll('img');
      await Promise.all(
        Array.from(images).map((img) => {
          if (img.complete) return Promise.resolve();
          return new Promise((resolve) => {
            img.addEventListener('load', resolve);
            img.addEventListener('error', resolve);
          });
        })
      );
    });
  }

  /**
   * Gets table data as a 2D array.
   * @param tableLocator - The table element locator
   */
  async getTableData(tableLocator: Locator): Promise<string[][]> {
    return await tableLocator.evaluate((table: HTMLTableElement) => {
      const rows = table.querySelectorAll('tr');
      return Array.from(rows).map((row) => {
        const cells = row.querySelectorAll('td, th');
        return Array.from(cells).map((cell) => cell.textContent?.trim() || '');
      });
    });
  }

  /**
   * Blocks specific resource types (images, fonts, etc.).
   * @param resourceTypes - Resource types to block
   */
  async blockResources(resourceTypes: string[]): Promise<void> {
    await this.page.route('**/*', (route) => {
      if (resourceTypes.includes(route.request().resourceType())) {
        route.abort();
      } else {
        route.continue();
      }
    });
  }

  /**
   * Mocks a network request.
   * @param urlPattern - URL pattern to match
   * @param response - The mock response
   */
  async mockRequest(
    urlPattern: string | RegExp,
    response: { status?: number; body?: string | object; headers?: Record<string, string> }
  ): Promise<void> {
    await this.page.route(urlPattern, async (route) => {
      await route.fulfill({
        status: response.status || 200,
        body: typeof response.body === 'object' ? JSON.stringify(response.body) : response.body,
        headers: response.headers || { 'Content-Type': 'application/json' },
      });
    });
  }

  /**
   * Gets browser console logs.
   */
  getConsoleLogs(): { type: string; text: string }[] {
    const logs: { type: string; text: string }[] = [];
    this.page.on('console', (msg) => {
      logs.push({ type: msg.type(), text: msg.text() });
    });
    return logs;
  }

  /**
   * Sets the viewport size.
   * @param width - Viewport width
   * @param height - Viewport height
   */
  async setViewport(width: number, height: number): Promise<void> {
    await this.page.setViewportSize({ width, height });
  }

  /**
   * Emulates a device.
   * @param device - Device descriptor
   */
  async emulateDevice(device: { viewport: { width: number; height: number }; userAgent: string }): Promise<void> {
    await this.page.setViewportSize(device.viewport);
    // Note: User agent should be set when creating the browser context
  }
}
