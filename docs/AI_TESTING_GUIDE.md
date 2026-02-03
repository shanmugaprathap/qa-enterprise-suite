# AI Testing Guide

This guide explains the AI-driven testing features in the QA Enterprise Suite.

## Overview

The framework includes three AI-powered features:
1. **Self-Healing Locators** - Automatically recover from broken selectors
2. **AI Test Data Generation** - Generate realistic test data using LLMs
3. **Predictive Test Selection** - Select relevant tests based on code changes

## Self-Healing Locators

### How It Works

When a locator fails to find an element, the self-healing system:
1. Retrieves cached element snapshots (attributes, position, text)
2. Generates alternative locators based on the snapshot
3. Scores candidate elements for similarity
4. Returns the best matching element

### Usage (Java)

```java
import com.enterprise.qa.core.ai.selfhealing.SelfHealingLocator;

public class MyPage extends BasePage {
    private SelfHealingLocator healer;

    public MyPage(WebDriver driver) {
        super(driver);
        healer = new SelfHealingLocator(driver);
    }

    public void clickSubmit() {
        // Element name helps track and heal the locator
        WebElement button = healer.findElement(
            By.id("submit-btn"),
            "submit-button"
        );
        button.click();
    }
}
```

### Usage (TypeScript/Playwright)

```typescript
import { SelfHealingLocator } from '../ai/SelfHealingLocator';

export class MyPage extends BasePage {
    private healer: SelfHealingLocator;

    constructor(page: Page) {
        super(page);
        this.healer = new SelfHealingLocator(page);
    }

    async clickSubmit() {
        const button = await this.healer.find('#submit-btn', 'submit-button');
        await button.click();
    }
}
```

### Configuration

Enable/disable in `config.properties`:
```properties
selfhealing.enabled=true
```

### Best Practices

1. **Use descriptive element names** - Names help identify elements across test runs
2. **Add data-testid attributes** - These provide stable locators as fallback
3. **Monitor healing logs** - Frequent healing indicates fragile locators

## AI Test Data Generation

### Prerequisites

Set your OpenAI API key:
```bash
export OPENAI_API_KEY=your-api-key
```

Enable in configuration:
```properties
ai.datagen.enabled=true
```

### Usage (Java)

```java
import com.enterprise.qa.core.ai.datagen.AITestDataGenerator;

AITestDataGenerator generator = new AITestDataGenerator();

// Generate user data
String userData = generator.generateUserData("premium user with expired subscription");
// Returns JSON: {"firstName": "John", "lastName": "Doe", "email": "john@example.com", ...}

// Generate specific data types
String formData = generator.generateFormData("checkout");
String products = generator.generateProducts("electronics", 5);

// Generate edge cases
List<String> edgeCases = generator.generateEdgeCases("email", "email");
// Returns: ["test@example.com", "test+tag@example.com", "a@b.c", "", "@invalid", ...]
```

### Supported Data Types

| Method | Description |
|--------|-------------|
| `generateUserData(persona)` | User profile data |
| `generateCreditCard(type)` | Test credit card data |
| `generateProducts(category, count)` | Product catalog data |
| `generateFormData(formType)` | Form field data |
| `generateSearchQueries(domain, count)` | Search terms |
| `generateEdgeCases(field, type)` | Boundary/edge case values |
| `generateLocalizedData(type, locale)` | Locale-specific data |

### Fallback Behavior

If OpenAI is unavailable, the generator returns predefined fallback data. This ensures tests run without AI connectivity.

## Predictive Test Selection

### How It Works

The predictive selector analyzes git diffs to identify:
1. Changed files and packages
2. Related tests based on naming conventions
3. Dependencies between code and tests

### Usage

```java
import com.enterprise.qa.core.ai.predictive.PredictiveTestSelector;

PredictiveTestSelector selector = new PredictiveTestSelector();

// Get tests to run for uncommitted changes
String gitDiff = selector.getGitDiff();
List<String> testsToRun = selector.selectTests(gitDiff);

// Get tests for specific commits
String diff = selector.getGitDiff("HEAD~5", "HEAD");
List<String> tests = selector.selectTests(diff);
```

### Mapping Configuration

Define explicit file-to-test mappings:

```java
selector.addFileMapping("src/main/java/Auth.java", "AuthTest");
selector.addPackageMapping("com.app.api", "ApiIntegrationTest");
selector.setTestPriority("SmokeTest", 100); // Higher = runs first
```

### Load from File

Create a mapping file (mappings.txt):
```
src/main/java/LoginService.java => LoginTest
src/main/java/PaymentService.java => PaymentTest
```

Load mappings:
```java
selector.loadMappingsFromFile(Paths.get("mappings.txt"));
```

### CI/CD Integration

Use in GitHub Actions:

```yaml
- name: Select tests based on changes
  run: |
    # Run Java command to output test list
    TESTS=$(java -cp ... com.app.TestSelector)
    echo "TESTS_TO_RUN=$TESTS" >> $GITHUB_ENV

- name: Run selected tests
  run: mvn test -Dtest=${{ env.TESTS_TO_RUN }}
```

## Performance Considerations

### Self-Healing
- Adds ~100-500ms overhead when healing occurs
- No overhead for successful primary locators
- Cache size grows with unique element names

### AI Data Generation
- API calls add latency (1-3 seconds)
- Results are cached to avoid repeated calls
- Fallback data is instantaneous

### Predictive Selection
- Git operations are fast (~10ms)
- Test selection scales with mapping count

## Troubleshooting

### Self-Healing Not Working

1. Check `selfhealing.enabled=true` in config
2. Verify element names are consistent across runs
3. Check logs for healing attempts

### AI Data Generation Fails

1. Verify `OPENAI_API_KEY` is set
2. Check `ai.datagen.enabled=true`
3. Review logs for API errors
4. Check API quota/rate limits

### Predictive Selection Missing Tests

1. Add explicit mappings for unmapped files
2. Ensure naming conventions match
3. Check test priority scores
