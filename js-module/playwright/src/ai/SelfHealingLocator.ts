import { Page, Locator } from '@playwright/test';

/**
 * Element snapshot for self-healing.
 */
interface ElementSnapshot {
  selector: string;
  tagName: string;
  id?: string;
  name?: string;
  className?: string;
  text?: string;
  type?: string;
  placeholder?: string;
  ariaLabel?: string;
  dataTestId?: string;
  href?: string;
  x: number;
  y: number;
  width: number;
  height: number;
  capturedAt: number;
}

/**
 * Self-healing locator that automatically recovers from broken selectors.
 * Uses multiple fallback strategies to find elements when primary locators fail.
 */
export class SelfHealingLocator {
  private readonly page: Page;
  private readonly snapshotCache: Map<string, ElementSnapshot>;
  private readonly enabled: boolean;

  constructor(page: Page, enabled = true) {
    this.page = page;
    this.snapshotCache = new Map();
    this.enabled = enabled;
  }

  /**
   * Finds an element with self-healing capability.
   * @param selector - The primary selector
   * @param elementName - A descriptive name for the element
   * @returns The found locator
   */
  async find(selector: string, elementName: string): Promise<Locator> {
    // Try primary selector first
    const primaryLocator = this.page.locator(selector);

    try {
      if ((await primaryLocator.count()) > 0 && (await primaryLocator.first().isVisible())) {
        // Cache snapshot for future healing
        await this.cacheSnapshot(elementName, primaryLocator.first(), selector);
        return primaryLocator.first();
      }
    } catch (e) {
      console.warn(`Primary selector failed for '${elementName}': ${selector}`);
    }

    if (!this.enabled) {
      throw new Error(`Element not found: ${elementName}`);
    }

    // Attempt self-healing
    return await this.healAndFind(selector, elementName);
  }

  /**
   * Attempts to heal a broken locator and find the element.
   */
  private async healAndFind(originalSelector: string, elementName: string): Promise<Locator> {
    console.log(`Attempting self-healing for element: ${elementName}`);

    const snapshot = this.snapshotCache.get(elementName);
    const alternatives = this.generateAlternatives(originalSelector, snapshot);

    for (const alternative of alternatives) {
      try {
        const locator = this.page.locator(alternative);
        const count = await locator.count();

        if (count > 0) {
          const element = locator.first();
          if (await element.isVisible()) {
            const score = await this.scoreElement(element, snapshot);
            console.log(`Self-healed '${elementName}' using: ${alternative} (score: ${score})`);

            // Cache the healed selector
            await this.cacheSnapshot(elementName, element, alternative);
            return element;
          }
        }
      } catch (e) {
        // Try next alternative
      }
    }

    throw new Error(`Could not heal locator for: ${elementName}`);
  }

  /**
   * Generates alternative selectors.
   */
  private generateAlternatives(originalSelector: string, snapshot?: ElementSnapshot): string[] {
    const alternatives: string[] = [];

    if (snapshot) {
      // Try by ID
      if (snapshot.id) {
        alternatives.push(`#${snapshot.id}`);
        alternatives.push(`[id="${snapshot.id}"]`);
        alternatives.push(`[id*="${snapshot.id}"]`);
      }

      // Try by data-testid
      if (snapshot.dataTestId) {
        alternatives.push(`[data-testid="${snapshot.dataTestId}"]`);
        alternatives.push(`[data-test-id="${snapshot.dataTestId}"]`);
        alternatives.push(`[data-cy="${snapshot.dataTestId}"]`);
      }

      // Try by name
      if (snapshot.name) {
        alternatives.push(`[name="${snapshot.name}"]`);
      }

      // Try by aria-label
      if (snapshot.ariaLabel) {
        alternatives.push(`[aria-label="${snapshot.ariaLabel}"]`);
      }

      // Try by placeholder
      if (snapshot.placeholder) {
        alternatives.push(`[placeholder="${snapshot.placeholder}"]`);
      }

      // Try by text content
      if (snapshot.text && snapshot.text.length <= 50) {
        alternatives.push(`text="${snapshot.text}"`);
        alternatives.push(`text=${snapshot.text}`);
        alternatives.push(`*:has-text("${snapshot.text}")`);
      }

      // Try by tag and type
      if (snapshot.tagName && snapshot.type) {
        alternatives.push(`${snapshot.tagName}[type="${snapshot.type}"]`);
      }

      // Try by class
      if (snapshot.className) {
        const classes = snapshot.className.split(/\s+/).filter((c) => !this.isGenericClass(c));
        for (const cls of classes) {
          alternatives.push(`.${cls}`);
        }
      }
    }

    // Generate alternatives from original selector
    alternatives.push(...this.generateFromOriginal(originalSelector));

    return [...new Set(alternatives)]; // Remove duplicates
  }

  /**
   * Generates alternatives from the original selector.
   */
  private generateFromOriginal(selector: string): string[] {
    const alternatives: string[] = [];

    // Try partial ID match
    const idMatch = selector.match(/#([a-zA-Z0-9_-]+)/);
    if (idMatch) {
      alternatives.push(`[id*="${idMatch[1]}"]`);
      alternatives.push(`[id$="${idMatch[1]}"]`);
    }

    // Try partial class match
    const classMatch = selector.match(/\.([a-zA-Z0-9_-]+)/);
    if (classMatch) {
      alternatives.push(`[class*="${classMatch[1]}"]`);
    }

    return alternatives;
  }

  /**
   * Scores how well an element matches a snapshot.
   */
  private async scoreElement(locator: Locator, snapshot?: ElementSnapshot): Promise<number> {
    if (!snapshot) {
      return 0.5; // Base score without snapshot
    }

    let score = 0;
    let weights = 0;

    try {
      const element = await locator.elementHandle();
      if (!element) return 0;

      // Tag name
      const tagName = await locator.evaluate((el) => el.tagName.toLowerCase());
      if (tagName === snapshot.tagName?.toLowerCase()) {
        score += 0.15;
      }
      weights += 0.15;

      // ID
      const id = await locator.getAttribute('id');
      if (id && id === snapshot.id) {
        score += 0.2;
      }
      weights += 0.2;

      // Text content
      const text = await locator.textContent();
      if (text && snapshot.text) {
        const similarity = this.stringSimilarity(text.trim(), snapshot.text);
        score += 0.15 * similarity;
      }
      weights += 0.15;

      // Position
      const box = await locator.boundingBox();
      if (box && snapshot.x !== undefined) {
        const dx = Math.abs(box.x - snapshot.x);
        const dy = Math.abs(box.y - snapshot.y);
        const distance = Math.sqrt(dx * dx + dy * dy);
        const positionScore = Math.max(0, 1 - distance / 200);
        score += 0.1 * positionScore;
      }
      weights += 0.1;

      // Size
      if (box && snapshot.width && snapshot.height) {
        const widthRatio =
          Math.min(box.width, snapshot.width) / Math.max(box.width, snapshot.width);
        const heightRatio =
          Math.min(box.height, snapshot.height) / Math.max(box.height, snapshot.height);
        score += 0.1 * ((widthRatio + heightRatio) / 2);
      }
      weights += 0.1;

      return weights > 0 ? score / weights : 0;
    } catch (e) {
      return 0;
    }
  }

  /**
   * Caches an element snapshot.
   */
  private async cacheSnapshot(name: string, locator: Locator, selector: string): Promise<void> {
    try {
      const snapshot: ElementSnapshot = {
        selector,
        tagName: await locator.evaluate((el) => el.tagName.toLowerCase()),
        id: (await locator.getAttribute('id')) || undefined,
        name: (await locator.getAttribute('name')) || undefined,
        className: (await locator.getAttribute('class')) || undefined,
        text: (await locator.textContent())?.trim().substring(0, 100) || undefined,
        type: (await locator.getAttribute('type')) || undefined,
        placeholder: (await locator.getAttribute('placeholder')) || undefined,
        ariaLabel: (await locator.getAttribute('aria-label')) || undefined,
        dataTestId:
          (await locator.getAttribute('data-testid')) ||
          (await locator.getAttribute('data-test-id')) ||
          undefined,
        href: (await locator.getAttribute('href')) || undefined,
        x: 0,
        y: 0,
        width: 0,
        height: 0,
        capturedAt: Date.now(),
      };

      const box = await locator.boundingBox();
      if (box) {
        snapshot.x = box.x;
        snapshot.y = box.y;
        snapshot.width = box.width;
        snapshot.height = box.height;
      }

      this.snapshotCache.set(name, snapshot);
    } catch (e) {
      console.debug(`Failed to cache snapshot for ${name}: ${e}`);
    }
  }

  /**
   * Calculates string similarity (Levenshtein-based).
   */
  private stringSimilarity(s1: string, s2: string): number {
    if (s1 === s2) return 1;
    if (!s1 || !s2) return 0;

    const maxLen = Math.max(s1.length, s2.length);
    if (maxLen === 0) return 1;

    const distance = this.levenshteinDistance(s1, s2);
    return 1 - distance / maxLen;
  }

  /**
   * Calculates Levenshtein distance.
   */
  private levenshteinDistance(s1: string, s2: string): number {
    const m = s1.length;
    const n = s2.length;
    const dp: number[][] = Array(m + 1)
      .fill(null)
      .map(() => Array(n + 1).fill(0));

    for (let i = 0; i <= m; i++) dp[i][0] = i;
    for (let j = 0; j <= n; j++) dp[0][j] = j;

    for (let i = 1; i <= m; i++) {
      for (let j = 1; j <= n; j++) {
        const cost = s1[i - 1] === s2[j - 1] ? 0 : 1;
        dp[i][j] = Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost);
      }
    }

    return dp[m][n];
  }

  /**
   * Checks if a class name is generic.
   */
  private isGenericClass(className: string): boolean {
    const genericClasses = new Set([
      'container',
      'wrapper',
      'content',
      'row',
      'col',
      'btn',
      'active',
      'disabled',
      'hidden',
      'visible',
      'clearfix',
    ]);
    return genericClasses.has(className.toLowerCase());
  }

  /**
   * Clears the snapshot cache.
   */
  clearCache(): void {
    this.snapshotCache.clear();
  }

  /**
   * Gets the cache size.
   */
  getCacheSize(): number {
    return this.snapshotCache.size;
  }
}
