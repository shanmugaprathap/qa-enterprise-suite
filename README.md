<div align="center">

# QA Enterprise Suite

**Enterprise-Grade, Multi-Framework Test Automation Platform with AI-Driven Testing**

[![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Selenium](https://img.shields.io/badge/Selenium-4.21.0-43B02A?logo=selenium&logoColor=white)](https://www.selenium.dev/)
[![Playwright](https://img.shields.io/badge/Playwright-latest-2EAD33?logo=playwright&logoColor=white)](https://playwright.dev/)
[![TestNG](https://img.shields.io/badge/TestNG-7.10.2-DC382D?logo=testng&logoColor=white)](https://testng.org/)
[![Cucumber](https://img.shields.io/badge/Cucumber-7.15.0-23D96C?logo=cucumber&logoColor=white)](https://cucumber.io/)
[![REST Assured](https://img.shields.io/badge/REST%20Assured-5.4.0-6DB33F?logo=java&logoColor=white)](https://rest-assured.io/)
[![Allure](https://img.shields.io/badge/Allure-2.25.0-FF6600?logo=allure&logoColor=white)](https://allurereport.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A production-grade test automation platform combining **Java** and **JavaScript** ecosystems with **AI-driven testing** capabilities &mdash; self-healing locators, AI test data generation, and predictive test selection &mdash; backed by enterprise CI/CD pipelines, Docker infrastructure, and Kubernetes orchestration.

[Quick Start](#quick-start) | [Architecture](#architecture) | [AI Features](#ai-driven-testing) | [CI/CD](#cicd-pipelines) | [Documentation](#documentation)

</div>

---

## Architecture

```
┌──────────────────────────────────────────────────────────────────────┐
│                         Test Modules                                 │
│                                                                      │
│  Java (Maven Multi-Module)              JavaScript (npm Workspaces)  │
│  ┌──────────┐ ┌──────────┐ ┌────────┐  ┌──────────┐ ┌───────────┐  │
│  │ui-selenium│ │api-rest  │ │bdd-    │  │playwright│ │nightwatch │  │
│  │          │ │assured   │ │cucumber│  │          │ │           │  │
│  └────┬─────┘ └────┬─────┘ └───┬────┘  └────┬─────┘ └─────┬─────┘  │
│       │             │           │             │             │        │
│  ┌────┴─────┐ ┌─────┴────┐     │        ┌────┴─────────────┴────┐   │
│  │mobile-   │ │junit5-   │     │        │  Shared fixtures,     │   │
│  │appium    │ │module    │     │        │  pages, helpers       │   │
│  └────┬─────┘ └────┬─────┘     │        └───────────────────────┘   │
│       │             │           │                                    │
├───────┴─────────────┴───────────┴────────────────────────────────────┤
│                        Core Framework                                │
│  ┌─────────────┐ ┌─────────────┐ ┌──────────────────────────┐       │
│  │ ConfigMgr   │ │ DriverMgr   │ │ AI Engine                │       │
│  │ (Env-aware) │ │ (ThreadLocal│ │ ┌────────────────────┐   │       │
│  │             │ │  + Grid)    │ │ │ Self-Healing        │   │       │
│  └─────────────┘ └─────────────┘ │ │ Locators            │   │       │
│  ┌─────────────┐ ┌─────────────┐ │ ├────────────────────┤   │       │
│  │ Listeners   │ │ RetryAnalyz │ │ │ AI Data Generator   │   │       │
│  │ (TestNG +   │ │ er + Report │ │ │ (OpenAI)            │   │       │
│  │  Allure)    │ │ Portal      │ │ ├────────────────────┤   │       │
│  └─────────────┘ └─────────────┘ │ │ Predictive Test     │   │       │
│  ┌─────────────┐ ┌─────────────┐ │ │ Selector (git-diff) │   │       │
│  │ Security    │ │ A11y Utils  │ │ └────────────────────┘   │       │
│  │ Utils       │ │             │ └──────────────────────────┘       │
│  └─────────────┘ └─────────────┘                                    │
├──────────────────────────────────────────────────────────────────────┤
│                     Infrastructure                                   │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐                 │
│  │ GitHub       │ │ Docker       │ │ Kubernetes   │                 │
│  │ Actions (3   │ │ Selenium Grid│ │ Test Runner  │                 │
│  │ pipelines)   │ │ Report Portal│ │ Jobs         │                 │
│  └──────────────┘ └──────────────┘ └──────────────┘                 │
├──────────────────────────────────────────────────────────────────────┤
│                      Reporting                                       │
│  ┌──────────────────────┐  ┌──────────────────────┐                 │
│  │ Allure Reports       │  │ Report Portal        │                 │
│  │ (GitHub Pages)       │  │ (Trends & Analytics) │                 │
│  └──────────────────────┘  └──────────────────────┘                 │
└──────────────────────────────────────────────────────────────────────┘
```

## What's Included

| Module | Stack | Purpose |
|--------|-------|---------|
| **ui-selenium** | Selenium 4 + TestNG | Browser-based UI testing with POM |
| **api-restassured** | REST Assured + Pact | API testing with contract validation |
| **bdd-cucumber** | Cucumber 7 + Gherkin | Behavior-driven test scenarios |
| **mobile-appium** | Appium 9 | iOS and Android mobile testing |
| **junit5-module** | JUnit 5 | Modern parameterized test patterns |
| **playwright** | Playwright + TypeScript | Cross-browser E2E (Chromium, Firefox, WebKit) |
| **nightwatch** | Nightwatch.js | Node-based UI testing |
| **performance** | JMeter | Load and performance test plans |
| **core** | Shared library | Config, drivers, AI engine, listeners, utilities |

## AI-Driven Testing

The framework includes three AI capabilities that reduce test maintenance and improve coverage:

### Self-Healing Locators

When a locator breaks due to a DOM change, the self-healing engine automatically finds the element using cached snapshots and multi-attribute scoring.

```java
SelfHealingLocator locator = new SelfHealingLocator(driver);

// If By.id("login-btn") fails, the engine:
// 1. Retrieves cached element snapshot (id, name, class, text, aria-label)
// 2. Scores all candidate elements against the snapshot
// 3. Selects the best match and continues the test
WebElement element = locator.findElement(By.id("login-btn"), "login-button");
```

Also available in TypeScript for Playwright tests:

```typescript
const locator = new SelfHealingLocator(page);
const element = await locator.find('#login-btn', 'login-button');
```

### AI Test Data Generation

Uses OpenAI to generate realistic, context-aware test data &mdash; user profiles, credit cards, edge cases, localized data, and security payloads.

```java
AITestDataGenerator generator = new AITestDataGenerator();

// Generate persona-based user data
String userData = generator.generateUserData("premium user with expired subscription");

// Generate edge cases for a specific field
String edgeCases = generator.generateEdgeCases("email", "string");

// Generate locale-specific data
String jpData = generator.generateLocalizedData("address", "ja-JP");
```

Results are cached to minimize API costs. Falls back to static data when the API is unavailable.

### Predictive Test Selection

Analyzes `git diff` output to identify which tests are most likely affected by recent code changes, reducing regression suite runtime.

```java
PredictiveTestSelector selector = new PredictiveTestSelector();

// Analyze current uncommitted changes
List<String> testsToRun = selector.selectTests(selector.getGitDiff());

// Analyze last N commits
List<String> tests = selector.selectTests(selector.getGitDiffLastCommits(3));
```

Uses file-to-test mapping, package-to-test mapping, and convention-based discovery (`ClassName` -> `ClassNameTest`).

## Quick Start

### Prerequisites

- **Java 17+** &mdash; [Download](https://adoptium.net/)
- **Maven 3.8+** &mdash; [Download](https://maven.apache.org/download.cgi)
- **Node.js 18+** &mdash; [Download](https://nodejs.org/) (for Playwright/Nightwatch)
- **Docker** (optional) &mdash; for Selenium Grid and Report Portal
- **OpenAI API Key** (optional) &mdash; for AI data generation features

### Setup

```bash
# Clone
git clone https://github.com/shanmugaprathap/qa-enterprise-suite.git
cd qa-enterprise-suite

# Build Java modules
cd java-module
mvn clean compile

# Install JavaScript dependencies
cd ../js-module
npm install
npx playwright install
```

### Running Tests

#### Java

```bash
cd java-module

# Smoke tests (fast validation)
mvn test -Dgroups=smoke

# Full regression
mvn test -Dgroups=regression

# Run a specific module
mvn test -pl ui-selenium
mvn test -pl api-restassured
mvn test -pl bdd-cucumber

# Headless mode
mvn test -Dgroups=smoke -Dheadless=true

# Specific environment
mvn test -Denv=staging

# Generate Allure report
mvn test && mvn allure:serve
```

#### Playwright

```bash
cd js-module

# All tests
npm run test:playwright

# Headed mode (watch tests run)
npm run test:playwright:headed

# Interactive UI mode
npm run test:playwright:ui

# Specific browser
npx playwright test --project=firefox
```

#### Docker Services

```bash
cd docker

# Start Selenium Grid (Hub + Chrome/Firefox/Edge nodes + video recording)
docker-compose -f docker-compose.selenium-grid.yml up -d

# Start Report Portal
docker-compose -f docker-compose.reportportal.yml up -d
# Access at http://localhost:8080 (default: admin/erebus)
```

## Project Structure

```
qa-enterprise-suite/
├── java-module/                        # Maven multi-module project
│   ├── pom.xml                         # Parent POM (dependency management)
│   ├── core/                           # Shared framework core
│   │   └── src/main/java/.../core/
│   │       ├── config/                 # ConfigManager (singleton, env-aware)
│   │       ├── drivers/                # DriverManager (ThreadLocal + Grid)
│   │       ├── ai/
│   │       │   ├── selfhealing/        # SelfHealingLocator, LocatorScorer
│   │       │   ├── datagen/            # AITestDataGenerator, LLMClient
│   │       │   ├── predictive/         # PredictiveTestSelector (git-based)
│   │       │   ├── visual/             # VisualTestingEngine
│   │       │   └── analytics/          # TestAnalyticsEngine
│   │       ├── listeners/              # TestNG + Allure + Report Portal
│   │       ├── reporting/              # ReportPortalClient
│   │       ├── security/               # SecurityTestingUtils
│   │       ├── accessibility/          # AccessibilityTestingUtils
│   │       └── data/                   # TestDataBuilder
│   ├── ui-selenium/                    # Selenium UI tests (POM, helpers)
│   ├── api-restassured/                # REST Assured + Pact contract tests
│   ├── bdd-cucumber/                   # Cucumber features, steps, hooks
│   ├── mobile-appium/                  # Appium iOS/Android drivers
│   └── junit5-module/                  # JUnit 5 parameterized tests
│
├── js-module/                          # JavaScript workspace
│   ├── playwright/                     # Playwright E2E (TypeScript)
│   │   ├── src/ai/                     # Self-healing locators (TS)
│   │   ├── src/pages/                  # BasePage, page objects
│   │   └── tests/                      # Test specs
│   └── nightwatch/                     # Nightwatch UI tests
│
├── docker/
│   ├── docker-compose.selenium-grid.yml   # Grid 4 + noVNC + video
│   ├── docker-compose.reportportal.yml    # Report Portal stack
│   └── docker-compose.monitoring.yml      # Monitoring stack
│
├── k8s/                                # Kubernetes manifests
│   ├── test-runner-job.yaml            # Test execution job
│   └── selenium-grid.yaml              # Grid deployment
│
├── performance/jmeter/                 # JMeter test plans
├── api-collections/                    # Postman collections
├── config/reportportal/                # Report Portal config
│
├── .github/workflows/
│   ├── main-ci.yml                     # Push/PR pipeline
│   ├── nightly-regression.yml          # Daily full regression
│   └── pr-validation.yml              # PR change-detection pipeline
│
└── docs/                               # Guides and standards
```

## CI/CD Pipelines

Three GitHub Actions workflows cover the full development lifecycle:

| Workflow | Trigger | What It Does |
|----------|---------|--------------|
| **main-ci.yml** | Push/PR to `main` | Build + smoke tests (Java & Playwright), Allure report to GitHub Pages, lint |
| **nightly-regression.yml** | Daily at 2 AM UTC | Full regression across all Java modules + Playwright on 3 browsers, combined Allure report |
| **pr-validation.yml** | PR opened/updated | Change detection (Java/JS/docs), targeted validation, markdown link checks |

Reports are auto-published to GitHub Pages. Nightly results go to the `nightly/` subfolder.

## Environment Configuration

```bash
# AI features (optional)
export OPENAI_API_KEY=your-api-key

# Report Portal (optional)
export RP_ENDPOINT=http://localhost:8080
export RP_API_KEY=your-api-key
export RP_PROJECT=qa-enterprise-suite
```

Core settings in `java-module/core/src/main/resources/config.properties`:

```properties
env=qa                              # qa | staging | prod
browser=chrome                      # chrome | firefox | edge
headless=false
selfhealing.enabled=true
ai.datagen.enabled=false            # Set true + OPENAI_API_KEY to enable
parallel.thread.count=4
retry.max.attempts=2
```

## Quality Gates

| Gate | Tool | Threshold |
|------|------|-----------|
| Code Coverage | JaCoCo | 70% line / 60% branch |
| Mutation Testing | PIT | 70% mutation score |
| Static Analysis | Checkstyle + ESLint | Zero violations |

## Documentation

Detailed guides are available in the `docs/` folder:

| Guide | Description |
|-------|-------------|
| [Testing Standards](docs/TESTING_STANDARDS.md) | Framework conventions and best practices |
| [AI Testing Guide](docs/AI_TESTING_GUIDE.md) | Self-healing, data generation, test selection |
| [CI/CD Guide](docs/CI_CD_GUIDE.md) | Pipeline overview and configuration |
| [Locator Strategy](docs/LOCATOR_STRATEGY.md) | Robust locator best practices |
| [Report Portal Setup](docs/REPORTPORTAL_SETUP.md) | Enterprise reporting integration |
| [Mobile Testing Guide](docs/MOBILE_TESTING_GUIDE.md) | Appium setup and usage |
| [Performance Testing Guide](docs/PERFORMANCE_TESTING_GUIDE.md) | JMeter load testing |
| [Code Coverage Guide](docs/CODE_COVERAGE_GUIDE.md) | JaCoCo and mutation testing |

## Tech Stack

| Category | Technologies |
|----------|-------------|
| **Languages** | Java 17, TypeScript |
| **UI Automation** | Selenium 4.21.0, Playwright, Nightwatch |
| **API Testing** | REST Assured 5.4.0, Pact (contract testing) |
| **Mobile** | Appium 9.2.2 |
| **BDD** | Cucumber 7.15.0, Gherkin |
| **Test Frameworks** | TestNG 7.10.2, JUnit 5.10.2 |
| **AI/ML** | OpenAI API, Healenium 3.4.4 |
| **Reporting** | Allure 2.25.0, Report Portal 5.1.0 |
| **Infrastructure** | Docker, Kubernetes, Selenium Grid 4 |
| **CI/CD** | GitHub Actions |
| **Code Quality** | JaCoCo, PIT, Checkstyle, ESLint |

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/your-feature`)
3. Commit your changes (`git commit -m 'Add your feature'`)
4. Push to the branch (`git push origin feature/your-feature`)
5. Open a Pull Request

## License

[MIT License](LICENSE)
