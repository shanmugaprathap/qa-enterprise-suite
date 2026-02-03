# QA Enterprise Suite

A comprehensive enterprise test automation framework demonstrating AI-driven testing, multi-tool expertise (Java + JavaScript), CI/CD integration, and quality management.

## Features

- **Multi-Language Support**: Java (Selenium, REST Assured, Appium) + JavaScript (Playwright, Nightwatch)
- **AI-Driven Testing**: Self-healing locators, AI test data generation, predictive test selection
- **BDD Support**: Cucumber integration with Gherkin feature files
- **Mobile Testing**: Appium integration for iOS/Android
- **Performance Testing**: JMeter test plans
- **API Testing**: REST Assured + Postman collections
- **Advanced Reporting**: Allure reports + Report Portal integration
- **CI/CD Ready**: GitHub Actions workflows included

## Quick Start

### Prerequisites

- Java 17+
- Node.js 18+
- Maven 3.8+
- Docker (optional, for Selenium Grid and Report Portal)

### Setup

```bash
# Clone the repository
git clone https://github.com/shanmugaprathap/qa-enterprise-suite.git
cd qa-enterprise-suite

# Build Java modules
cd java-module
mvn clean compile

# Install JS dependencies
cd ../js-module
npm install
npx playwright install
```

### Running Tests

#### Java Tests
```bash
cd java-module

# Run smoke tests
mvn test -Dgroups=smoke

# Run all UI tests
mvn test -pl ui-selenium

# Run API tests
mvn test -pl api-restassured

# Run with Allure report
mvn test && mvn allure:serve
```

#### Playwright Tests
```bash
cd js-module

# Run all tests
npm run test:playwright

# Run in headed mode
npm run test:playwright:headed

# Run with UI mode
npm run test:playwright:ui
```

## Project Structure

```
qa-enterprise-suite/
├── java-module/                    # Java Testing (Maven multi-module)
│   ├── core/                       # Shared framework core
│   │   └── src/main/java/
│   │       └── com/enterprise/qa/core/
│   │           ├── config/         # Configuration management
│   │           ├── drivers/        # WebDriver management
│   │           ├── ai/             # AI-driven testing features
│   │           │   ├── selfhealing/
│   │           │   ├── datagen/
│   │           │   └── predictive/
│   │           ├── listeners/      # TestNG listeners
│   │           └── reporting/      # Report Portal client
│   ├── ui-selenium/                # Selenium UI tests
│   ├── api-restassured/            # REST Assured API tests
│   ├── bdd-cucumber/               # Cucumber BDD tests
│   ├── mobile-appium/              # Appium mobile tests
│   └── junit5-module/              # JUnit 5 tests
│
├── js-module/                      # JavaScript Testing
│   ├── playwright/                 # Playwright E2E tests
│   └── nightwatch/                 # Nightwatch UI tests
│
├── performance/                    # Performance Testing
│   └── jmeter/
│
├── api-collections/                # Postman Collections
│
├── docker/                         # Docker configs
│   ├── docker-compose.selenium-grid.yml
│   └── docker-compose.reportportal.yml
│
├── .github/workflows/              # CI/CD Pipelines
│
└── docs/                           # Documentation
```

## AI-Driven Testing

### Self-Healing Locators

Automatically recovers from broken locators using multiple strategies:

```java
// Java
SelfHealingLocator locator = new SelfHealingLocator(driver);
WebElement element = locator.findElement(By.id("old-id"), "login-button");
```

```typescript
// TypeScript/Playwright
const locator = new SelfHealingLocator(page);
const element = await locator.find('#old-id', 'login-button');
```

### AI Test Data Generation

Generate realistic test data using OpenAI:

```java
AITestDataGenerator generator = new AITestDataGenerator();
String userData = generator.generateUserData("premium user with expired subscription");
```

### Predictive Test Selection

Select tests based on code changes:

```java
PredictiveTestSelector selector = new PredictiveTestSelector();
List<String> testsToRun = selector.selectTests(gitDiff);
```

## Environment Configuration

Set environment variables for AI features:

```bash
export OPENAI_API_KEY=your-api-key
```

For Report Portal integration:

```bash
export RP_ENDPOINT=http://localhost:8080
export RP_API_KEY=your-api-key
export RP_PROJECT=qa-enterprise-suite
```

## Docker Services

### Selenium Grid

```bash
cd docker
docker-compose -f docker-compose.selenium-grid.yml up -d
```

### Report Portal

```bash
cd docker
docker-compose -f docker-compose.reportportal.yml up -d
# Access at http://localhost:8080 (default: admin/erebus)
```

## CI/CD Pipelines

| Workflow | Trigger | Description |
|----------|---------|-------------|
| main-ci.yml | Push/PR to main | Build, lint, smoke tests |
| nightly-regression.yml | Cron (daily) | Full regression suite |
| pr-validation.yml | PR opened | Targeted tests based on changes |

## Documentation

- [Testing Standards](docs/TESTING_STANDARDS.md)
- [AI Testing Guide](docs/AI_TESTING_GUIDE.md)
- [Locator Strategy](docs/LOCATOR_STRATEGY.md)
- [CI/CD Guide](docs/CI_CD_GUIDE.md)
- [Report Portal Setup](docs/REPORTPORTAL_SETUP.md)

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
