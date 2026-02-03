# CLAUDE.md

This file provides guidance to Claude Code when working with the qa-enterprise-suite project.

## Repository Overview

Enterprise-grade QA test automation framework demonstrating AI-driven testing, multi-tool expertise (Java + JavaScript), CI/CD integration, and quality management.

## Project Structure

```
qa-enterprise-suite/
├── java-module/           # Java Testing (Maven multi-module)
│   ├── core/              # Shared framework core (config, drivers, AI, reporting)
│   │   └── ai/            # AI features (self-healing, datagen, predictive, analytics, visual)
│   ├── ui-selenium/       # Selenium UI tests (TestNG)
│   ├── api-restassured/   # REST Assured API tests + Pact contract tests
│   ├── bdd-cucumber/      # Cucumber BDD tests
│   ├── mobile-appium/     # Appium mobile tests
│   └── junit5-module/     # JUnit 5 tests
├── js-module/             # JavaScript Testing
│   ├── playwright/        # Playwright E2E tests with coverage
│   └── nightwatch/        # Nightwatch UI & API tests with coverage
├── performance/           # JMeter performance tests
├── api-collections/       # Postman collections + Newman runner
├── docker/                # Docker configs (Selenium Grid, Report Portal, Monitoring)
├── monitoring/            # Prometheus, Grafana dashboards, alerting
├── k8s/                   # Kubernetes manifests (Selenium Grid, test runners)
└── docs/                  # Testing standards and guides
```

## Build and Test Commands

### Java Module (Maven)

```bash
# From qa-enterprise-suite directory
cd java-module

# Build all modules
mvn clean compile

# Run all tests
mvn clean test

# Run specific module tests
mvn test -pl ui-selenium
mvn test -pl api-restassured
mvn test -pl bdd-cucumber

# Run by test groups
mvn test -Dgroups=smoke
mvn test -Dgroups=regression
mvn test -Dgroups=api

# Run specific test class
mvn -Dtest=GoogleSearchTest test

# Run in headless mode
mvn test -Dheadless=true

# Use specific environment
mvn test -Denv=qa
mvn test -Denv=staging
mvn test -Denv=prod

# Generate Allure report
mvn allure:serve
```

### JavaScript Module (npm)

```bash
# From qa-enterprise-suite directory
cd js-module

# Install all dependencies
npm install

# Run Playwright tests
npm run test:playwright
npm run test:playwright:headed
npm run test:playwright:debug

# Run Nightwatch tests
npm run test:nightwatch

# Run specific Playwright test file
npx playwright test tests/ui/google.spec.ts
```

### Performance Tests (JMeter)

```bash
# From qa-enterprise-suite directory
cd performance/jmeter
./scripts/run-tests.sh load-test.jmx
```

### Postman/Newman

```bash
# Run collection with Newman
newman run api-collections/collections/smoke-tests.postman_collection.json \
  -e api-collections/environments/qa.postman_environment.json
```

## Key Technologies

| Category | Technology | Version |
|----------|------------|---------|
| Java | JDK | 17 |
| UI Testing | Selenium | 4.21.0 |
| UI Testing | Playwright | 1.42.0 |
| UI Testing | Nightwatch | 3.4.0 |
| API Testing | REST Assured | 5.4.0 |
| Mobile | Appium | 9.2.2 |
| BDD | Cucumber | 7.15.0 |
| Test Framework | TestNG | 7.10.2 |
| Test Framework | JUnit 5 | 5.10.2 |
| Performance | JMeter | 5.6.3 |
| Reporting | Allure | 2.25.0 |
| Reporting | Report Portal | 5.2.4 |
| AI/Self-Healing | Healenium | 3.4.4 |
| AI/LLM | OpenAI API | 0.18.2 |
| Code Coverage | JaCoCo | 0.8.12 |
| Code Coverage | NYC/Istanbul | 15.1.0 |
| Contract Testing | Pact | 4.6.7 |
| Mutation Testing | PIT | 1.15.8 |
| Monitoring | Prometheus | 2.49.1 |
| Monitoring | Grafana | 10.3.1 |

## AI-Driven Testing Features

### Self-Healing Locators
The framework includes AI-powered self-healing locators that automatically recover from broken selectors:
- Location: `java-module/core/.../ai/selfhealing/`
- JavaScript: `js-module/playwright/src/ai/`

### AI Test Data Generation
Uses OpenAI API to generate realistic test data:
- Requires `OPENAI_API_KEY` environment variable
- Location: `java-module/core/.../ai/datagen/`

### Predictive Test Selection
ML-based test selection based on code changes:
- Location: `java-module/core/.../ai/predictive/`

### Test Analytics Engine
ML-based quality metrics and predictions:
- Flaky test detection
- Failure classification (environment, data, timing, code)
- Risk prediction with recommendations
- Location: `java-module/core/.../ai/analytics/`

### Visual Testing
Pixel-level screenshot comparison:
- Baseline management
- Configurable thresholds
- Location: `java-module/core/.../ai/visual/`

### Security Testing
OWASP Top 10 validation utilities:
- XSS detection, SQL injection testing
- Security headers validation
- Location: `java-module/core/.../security/`

### Accessibility Testing
WCAG 2.1 compliance automation:
- Alt text, ARIA labels, color contrast
- Location: `java-module/core/.../accessibility/`

## Environment Variables

```bash
# Required for AI features
export OPENAI_API_KEY=your-api-key

# Report Portal (optional)
export RP_ENDPOINT=http://localhost:8080
export RP_API_KEY=your-rp-api-key
export RP_PROJECT=qa-enterprise-suite

# Selenium Grid (optional)
export GRID_URL=http://localhost:4444/wd/hub
```

## Docker Setup

### Selenium Grid
```bash
cd docker
docker-compose -f docker-compose.selenium-grid.yml up -d
```

### Report Portal
```bash
cd docker
docker-compose -f docker-compose.reportportal.yml up -d
# Access at http://localhost:8080 (admin/erebus)
```

### Monitoring Stack (Prometheus + Grafana)
```bash
cd docker
docker-compose -f docker-compose.monitoring.yml up -d
# Grafana: http://localhost:3000 (admin/admin)
# Prometheus: http://localhost:9090
```

### Kubernetes Deployment
```bash
# Deploy Selenium Grid to K8s
kubectl apply -f k8s/selenium-grid.yaml

# Run tests as K8s Job
kubectl apply -f k8s/test-runner-job.yaml
```

## CI/CD Pipelines

- **main-ci.yml**: Runs on every push/PR - builds, lints, runs smoke tests
- **nightly-regression.yml**: Full regression suite runs nightly
- **pr-validation.yml**: Validates PRs with targeted tests

## Code Patterns

### Page Object Model
All UI tests use POM pattern:
```java
// Java
public class LoginPage extends BasePage {
    @FindBy(id = "username") private WebElement usernameField;
}
```
```typescript
// TypeScript
export class LoginPage extends BasePage {
    readonly usernameField = this.page.locator('#username');
}
```

### API Client Pattern
```java
ApiClient client = new ApiClient(baseUrl);
Response response = client.get("/users");
```

### Test Groups
- `smoke` - Critical path tests
- `regression` - Full test suite
- `api` - API-only tests
- `ui` - UI-only tests
- `mobile` - Mobile tests

### Code Coverage
```bash
# Java (JaCoCo)
cd java-module
mvn clean verify
open target/site/jacoco/index.html

# JavaScript (NYC/Istanbul)
cd js-module/playwright
npm run test:coverage
open coverage/index.html
```

### Mutation Testing
```bash
cd java-module
mvn org.pitest:pitest-maven:mutationCoverage
open target/pit-reports/index.html
```

### Contract Testing (Pact)
```bash
cd java-module
mvn test -pl api-restassured -Dtest=*ContractTest
# Pacts generated in target/pacts/
```

### Newman (Postman CLI)
```bash
cd api-collections
./scripts/run-newman.sh smoke-tests.postman_collection.json
./scripts/run-newman.sh all  # Run all collections
```

## Troubleshooting

### Common Issues

1. **Tests fail with WebDriver error**: Ensure WebDriverManager is downloading correct driver version
2. **AI features not working**: Verify `OPENAI_API_KEY` is set
3. **Report Portal not connecting**: Check RP is running and credentials are correct
4. **Playwright tests fail on CI**: Ensure browsers are installed with `npx playwright install`
5. **Coverage not generating**: Run `mvn clean verify` (not just `test`)
6. **Contract tests failing**: Check Pact broker connection or local pact files
7. **Monitoring stack issues**: Verify Docker network connectivity

## Documentation

- `docs/TESTING_STANDARDS.md` - Test organization, naming, categories
- `docs/AI_TESTING_GUIDE.md` - Self-healing, AI data gen, predictive selection
- `docs/LOCATOR_STRATEGY.md` - Locator priorities, shadow DOM handling
- `docs/CI_CD_GUIDE.md` - Pipeline overview, Report Portal, Allure deployment
- `docs/REPORTPORTAL_SETUP.md` - Complete RP setup guide
- `docs/PERFORMANCE_TESTING_GUIDE.md` - JMeter best practices
- `docs/MOBILE_TESTING_GUIDE.md` - Appium setup and patterns
- `docs/MONITORING_GUIDE.md` - Prometheus, Grafana, alerting
- `docs/CODE_COVERAGE_GUIDE.md` - JaCoCo, NYC, mutation testing
