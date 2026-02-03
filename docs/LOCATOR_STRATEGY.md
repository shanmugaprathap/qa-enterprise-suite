# Locator Strategy Guide

This document outlines the locator strategy and best practices for the QA Enterprise Suite.

## Locator Priority

Use locators in this order of preference:

1. **data-testid** (Most stable)
2. **id** (Stable, but may be auto-generated)
3. **name** (Good for form elements)
4. **aria-label** (Accessibility-friendly)
5. **CSS class** (Less stable, use specific classes)
6. **XPath** (Last resort)

## Recommended Locators

### data-testid (Preferred)

Add test-specific attributes to your HTML:

```html
<button data-testid="submit-button">Submit</button>
<input data-testid="email-input" type="email" />
```

```java
By.cssSelector("[data-testid='submit-button']")
```

```typescript
page.locator('[data-testid="submit-button"]')
```

### ID Locators

```java
By.id("username")
```

```typescript
page.locator('#username')
```

### Name Locators

```java
By.name("email")
```

```typescript
page.locator('[name="email"]')
```

### ARIA Locators (Accessibility)

```java
By.cssSelector("[aria-label='Close dialog']")
```

```typescript
page.getByRole('button', { name: 'Close dialog' })
```

## Locators to Avoid

### Avoid: Position-based XPath

```java
// Bad - fragile
By.xpath("//div[3]/form/button[2]")
```

### Avoid: Auto-generated IDs

```java
// Bad - changes on each build
By.id("ember1234")
```

### Avoid: Generic classes

```java
// Bad - too generic
By.className("btn")
By.className("container")
```

### Avoid: Text content alone

```java
// Bad - changes with localization
By.xpath("//*[text()='Submit']")
```

## Playwright-Specific Locators

### getByRole (Accessibility-first)

```typescript
// Button by accessible name
page.getByRole('button', { name: 'Submit' })

// Link by text
page.getByRole('link', { name: 'Learn more' })

// Input by label
page.getByRole('textbox', { name: 'Email' })
```

### getByLabel

```typescript
page.getByLabel('Email address')
```

### getByPlaceholder

```typescript
page.getByPlaceholder('Enter your email')
```

### getByText

```typescript
page.getByText('Welcome', { exact: false })
```

## Dynamic Locators

### Parameterized Locators (Java)

```java
public By getProductCard(String productName) {
    return By.cssSelector(String.format("[data-testid='product-%s']", productName));
}

public By getNthTableRow(int index) {
    return By.cssSelector(String.format("table tbody tr:nth-child(%d)", index));
}
```

### Parameterized Locators (TypeScript)

```typescript
getProductCard(productName: string) {
    return this.page.locator(`[data-testid="product-${productName}"]`);
}

getNthTableRow(index: number) {
    return this.page.locator(`table tbody tr:nth-child(${index})`);
}
```

## Self-Healing Locator Integration

When using self-healing locators, provide meaningful element names:

```java
// Good - descriptive name helps healing
healer.findElement(By.id("login-btn"), "login-button")

// Bad - generic name makes healing harder
healer.findElement(By.id("login-btn"), "button1")
```

## Shadow DOM Handling

### Java (Selenium)

```java
WebElement shadowHost = driver.findElement(By.cssSelector("my-component"));
SearchContext shadowRoot = shadowHost.getShadowRoot();
WebElement innerElement = shadowRoot.findElement(By.cssSelector(".inner-button"));
```

### TypeScript (Playwright)

```typescript
// Playwright handles shadow DOM automatically with >>
await page.locator('my-component >> .inner-button').click();
```

## iFrame Handling

### Java

```java
driver.switchTo().frame("iframe-name");
// interact with elements
driver.switchTo().defaultContent();
```

### TypeScript (Playwright)

```typescript
const frame = page.frameLocator('#iframe-name');
await frame.locator('.element').click();
```

## Locator Review Checklist

Before committing locators, verify:

- [ ] Uses data-testid or stable attribute
- [ ] Not position-dependent
- [ ] Not reliant on auto-generated IDs
- [ ] Not using text that may change with localization
- [ ] Descriptive element name for self-healing
- [ ] Works in all supported browsers

## Adding Test Attributes to Source Code

Coordinate with developers to add test attributes:

```html
<!-- Add data-testid for stable test locators -->
<button
  data-testid="checkout-submit"
  class="btn btn-primary"
  type="submit">
  Complete Purchase
</button>
```

**Naming convention for data-testid:**
- Use kebab-case: `data-testid="user-profile-menu"`
- Include context: `data-testid="header-login-button"`
- Be specific: `data-testid="product-card-add-to-cart"` (not just `add-to-cart`)
