# Testing Standards

This document defines the testing standards and best practices for the QA Enterprise Suite.

## Table of Contents
- [Test Organization](#test-organization)
- [Naming Conventions](#naming-conventions)
- [Test Categories](#test-categories)
- [Code Quality](#code-quality)
- [Documentation](#documentation)

## Test Organization

### Directory Structure

```
tests/
├── smoke/           # Critical path tests
├── regression/      # Full regression suite
├── integration/     # Integration tests
└── e2e/            # End-to-end tests
```

### Test File Naming
- Java: `*Test.java` or `*IT.java` (integration tests)
- TypeScript: `*.spec.ts` or `*.test.ts`
- Feature files: `*.feature`

## Naming Conventions

### Test Methods/Functions

Use descriptive names that explain what is being tested:

```java
// Good
@Test
void shouldDisplayErrorMessageWhenLoginWithInvalidCredentials() { }

@Test
void shouldRedirectToHomePageAfterSuccessfulLogin() { }

// Bad
@Test
void test1() { }

@Test
void loginTest() { }
```

### Page Objects

Name page objects after the page they represent:
- `LoginPage`
- `HomePage`
- `CheckoutPage`

### Locators

Use data-testid attributes when possible:
```html
<button data-testid="submit-btn">Submit</button>
```

## Test Categories

### Smoke Tests (@smoke)
- **Purpose**: Verify critical functionality works
- **Execution time**: < 5 minutes
- **Coverage**: Core user journeys only
- **When to run**: Every commit, PR validation

### Regression Tests (@regression)
- **Purpose**: Comprehensive functionality validation
- **Execution time**: 30-60 minutes
- **Coverage**: All features and edge cases
- **When to run**: Nightly, before releases

### Integration Tests (@integration)
- **Purpose**: Verify component interactions
- **Execution time**: 10-15 minutes
- **Coverage**: API integrations, service interactions

### Performance Tests (@performance)
- **Purpose**: Validate response times and load handling
- **When to run**: Weekly, before major releases

## Code Quality

### Test Independence
- Each test must be independent and not rely on other tests
- Use proper setup/teardown to ensure clean state
- Avoid test order dependencies

### Assertions
- One logical assertion per test (may be multiple related asserts)
- Use descriptive assertion messages
- Prefer soft assertions for multiple checks in one test

```java
// Good
assertThat(loginPage.getErrorMessage())
    .as("Error message for invalid login")
    .contains("Invalid credentials");

// Bad
assertTrue(loginPage.getErrorMessage().contains("Invalid"));
```

### Wait Strategies
- Prefer explicit waits over implicit waits
- Never use Thread.sleep() in production tests
- Use meaningful wait conditions

```java
// Good
wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("result")));

// Bad
Thread.sleep(5000);
```

### Test Data
- Use test data generators for dynamic data
- Externalize test data in property files or JSON
- Don't hardcode sensitive data

## Documentation

### Test Documentation
Every test class should have:
- Class-level documentation explaining the feature under test
- Method-level documentation for complex test scenarios

```java
/**
 * Tests for user authentication functionality.
 * Covers login, logout, password reset, and MFA scenarios.
 */
public class AuthenticationTest {

    /**
     * Verifies that a user can log in with valid credentials
     * and is redirected to the dashboard.
     */
    @Test
    void shouldLoginSuccessfully() { }
}
```

### Allure Annotations
Use Allure annotations for better reporting:

```java
@Epic("Authentication")
@Feature("Login")
@Story("Standard Login")
@Description("Verify login with valid username and password")
@Severity(SeverityLevel.CRITICAL)
@Test
void testLogin() { }
```

## Review Checklist

Before submitting tests for review:
- [ ] Tests pass locally
- [ ] Tests are independent
- [ ] Proper wait strategies used
- [ ] No hardcoded test data
- [ ] Allure annotations added
- [ ] Test names are descriptive
- [ ] Appropriate test category assigned
