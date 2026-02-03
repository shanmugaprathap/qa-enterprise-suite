# CI/CD Guide

This guide explains the CI/CD pipelines and integration practices for the QA Enterprise Suite.

## Pipeline Overview

| Pipeline | Trigger | Purpose |
|----------|---------|---------|
| main-ci.yml | Push/PR to main | Build validation, smoke tests |
| nightly-regression.yml | Cron (2 AM UTC) | Full regression suite |
| pr-validation.yml | PR opened/updated | Targeted validation |

## Main CI Pipeline

### Workflow Diagram

```
Push to main
    │
    ├─► Java Build ──► Smoke Tests ──► Upload Results
    │
    ├─► JS Build ──► Playwright Tests ──► Upload Results
    │
    ├─► Code Quality (lint)
    │
    └─► Generate Allure Report ──► Deploy to GitHub Pages
```

### Running Locally

Simulate CI locally:

```bash
# Java
cd java-module
mvn clean test -Dgroups=smoke -Dheadless=true

# JavaScript
cd js-module
npm test -- --grep @smoke
```

## Nightly Regression

### Test Matrix

The nightly pipeline runs:
- Java: ui-selenium, api-restassured, bdd-cucumber modules
- Playwright: chromium, firefox, webkit browsers

### Manual Trigger

Trigger manually with custom test groups:

```bash
gh workflow run nightly-regression.yml -f test_groups=regression,api
```

## PR Validation

### Change Detection

The pipeline uses [dorny/paths-filter](https://github.com/dorny/paths-filter) to detect:
- `java/` changes → Run Java tests
- `js/` changes → Run JavaScript tests
- `docs/` changes → Validate documentation

### Skipping Validation

Add `[skip ci]` to commit message to skip pipeline:

```bash
git commit -m "docs: fix typo [skip ci]"
```

## Report Portal Integration

### Setup

1. Start Report Portal:
```bash
cd docker
docker-compose -f docker-compose.reportportal.yml up -d
```

2. Configure credentials:
```bash
export RP_ENDPOINT=http://localhost:8080
export RP_API_KEY=your-api-key
export RP_PROJECT=qa-enterprise-suite
```

3. Add listener to TestNG:
```xml
<listener class-name="com.enterprise.qa.core.listeners.ReportPortalListener"/>
```

### GitHub Secrets

Add these secrets to your repository:
- `RP_ENDPOINT`
- `RP_API_KEY`
- `RP_PROJECT`

## Allure Reporting

### Local Report Generation

```bash
# Generate and serve
cd java-module
mvn test
mvn allure:serve

# Or generate only
mvn allure:report
# Open target/site/allure-maven-plugin/index.html
```

### GitHub Pages Deployment

Reports are automatically deployed to:
```
https://{username}.github.io/{repo}/
```

Nightly reports are at:
```
https://{username}.github.io/{repo}/nightly/
```

### First-Time Setup

Enable GitHub Pages via API:
```bash
gh api repos/{owner}/{repo}/pages -X POST \
  --input - <<< '{"source":{"branch":"gh-pages","path":"/"}}'
```

## Selenium Grid in CI

### Using Grid for Parallel Execution

```yaml
services:
  selenium:
    image: selenium/standalone-chrome:4.18.1
    ports:
      - 4444:4444

steps:
  - name: Run tests with Grid
    env:
      GRID_URL: http://localhost:4444/wd/hub
    run: mvn test -Dgrid.url=$GRID_URL
```

## Environment Configuration

### CI Environment Variables

| Variable | Purpose |
|----------|---------|
| `CI` | Indicates CI environment |
| `HEADLESS` | Enable headless browser |
| `GRID_URL` | Selenium Grid URL |
| `BASE_URL` | Application URL |
| `TEST_ENV` | Target environment (qa/staging/prod) |

### GitHub Actions Secrets

Configure these in repository settings:
- `OPENAI_API_KEY` - For AI features
- `RP_ENDPOINT` - Report Portal URL
- `RP_API_KEY` - Report Portal token

## Best Practices

### 1. Keep CI Fast

- Run smoke tests on every commit (< 5 min)
- Reserve full regression for nightly runs
- Use parallelization

### 2. Fail Fast

- Stop on first critical failure
- Use `fail-fast: false` for matrix jobs when investigating

### 3. Artifact Management

- Upload test results as artifacts
- Set reasonable retention (7-30 days)
- Archive important screenshots/videos

### 4. Notifications

Configure notifications in GitHub:
- Failed builds → Slack/Teams
- PR checks → Require passing for merge

### 5. Branch Protection

Enable branch protection rules:
- Require PR reviews
- Require status checks to pass
- Require up-to-date branches

## Troubleshooting

### Tests Pass Locally but Fail in CI

1. Check for timing issues (use explicit waits)
2. Verify headless mode compatibility
3. Check for missing dependencies
4. Review CI logs for environment differences

### Flaky Tests

1. Add retry logic for known flaky tests
2. Investigate root cause (timing, data, resources)
3. Consider test isolation improvements

### Slow Pipelines

1. Enable caching (Maven, npm)
2. Run tests in parallel
3. Use change detection to skip unchanged modules
