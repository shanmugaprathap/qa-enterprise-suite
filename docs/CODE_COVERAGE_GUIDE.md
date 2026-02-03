# Code Coverage Guide

This guide covers code coverage configuration and best practices for both Java and JavaScript modules in the QA Enterprise Suite.

## Overview

Code coverage measures how much of your test framework code is exercised during test execution. We use:

- **Java**: JaCoCo (Java Code Coverage)
- **JavaScript**: NYC/Istanbul

## Java Code Coverage (JaCoCo)

### Configuration

JaCoCo is configured in the parent POM (`java-module/pom.xml`):

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>${jacoco.version}</version>
    <executions>
        <execution>
            <id>prepare-agent</id>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>verify</phase>
            <goals><goal>report</goal></goals>
        </execution>
        <execution>
            <id>check</id>
            <phase>verify</phase>
            <goals><goal>check</goal></goals>
            <configuration>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.70</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### Running Coverage

```bash
cd java-module

# Run tests with coverage
mvn clean verify

# View HTML report
open target/site/jacoco/index.html
```

### Coverage Thresholds

| Metric | Minimum | Target |
|--------|---------|--------|
| Line Coverage | 70% | 80% |
| Branch Coverage | 60% | 70% |
| Method Coverage | 70% | 80% |

### Excluding Files

Add exclusions for generated code or test utilities:

```xml
<configuration>
    <excludes>
        <exclude>**/model/*</exclude>
        <exclude>**/config/*Config.class</exclude>
        <exclude>**/*Exception.class</exclude>
    </excludes>
</configuration>
```

## JavaScript Code Coverage (NYC/Istanbul)

### Playwright Configuration

Coverage is configured in `js-module/playwright/package.json`:

```json
{
  "scripts": {
    "test:coverage": "nyc --reporter=html --reporter=text playwright test",
    "report:coverage": "nyc report --reporter=html"
  },
  "nyc": {
    "extends": "@istanbuljs/nyc-config-typescript",
    "include": ["src/**/*.ts"],
    "exclude": ["tests/**/*", "**/*.d.ts"],
    "reporter": ["text", "html", "lcov"],
    "all": true,
    "check-coverage": true,
    "branches": 60,
    "lines": 70,
    "functions": 70,
    "statements": 70
  }
}
```

### Running Coverage

```bash
cd js-module/playwright

# Run tests with coverage
npm run test:coverage

# View HTML report
open coverage/index.html
```

### Nightwatch Configuration

Coverage for Nightwatch is in `js-module/nightwatch/package.json`:

```json
{
  "scripts": {
    "test:coverage": "nyc --reporter=html --reporter=text nightwatch"
  },
  "nyc": {
    "include": ["src/**/*.js"],
    "exclude": ["tests/**/*"],
    "reporter": ["text", "html", "lcov"],
    "check-coverage": true,
    "lines": 60,
    "functions": 60
  }
}
```

## Mutation Testing (PIT)

Mutation testing goes beyond coverage by verifying that tests actually catch bugs.

### Configuration

```xml
<plugin>
    <groupId>org.pitest</groupId>
    <artifactId>pitest-maven</artifactId>
    <version>${pitest.version}</version>
    <configuration>
        <targetClasses>
            <param>com.enterprise.qa.*</param>
        </targetClasses>
        <targetTests>
            <param>com.enterprise.qa.*Test</param>
        </targetTests>
        <mutationThreshold>70</mutationThreshold>
        <coverageThreshold>70</coverageThreshold>
    </configuration>
</plugin>
```

### Running Mutation Tests

```bash
cd java-module

# Run mutation tests
mvn org.pitest:pitest-maven:mutationCoverage

# View report
open target/pit-reports/index.html
```

## CI/CD Integration

### GitHub Actions

```yaml
- name: Run Tests with Coverage
  run: |
    cd java-module
    mvn clean verify

- name: Upload Coverage to Codecov
  uses: codecov/codecov-action@v4
  with:
    files: java-module/target/site/jacoco/jacoco.xml,js-module/playwright/coverage/lcov.info
    fail_ci_if_error: true

- name: Check Coverage Thresholds
  run: |
    # Fail if coverage drops below threshold
    COVERAGE=$(grep -oP 'Total.*?(\d+)%' target/site/jacoco/index.html | grep -oP '\d+')
    if [ "$COVERAGE" -lt 70 ]; then
      echo "Coverage $COVERAGE% is below 70% threshold"
      exit 1
    fi
```

### Coverage Badge

Add to README:

```markdown
[![codecov](https://codecov.io/gh/org/qa-enterprise-suite/branch/main/graph/badge.svg)](https://codecov.io/gh/org/qa-enterprise-suite)
```

## Best Practices

### 1. Focus on Meaningful Coverage

- Don't chase 100% coverage blindly
- Focus on critical paths and business logic
- Exclude generated code and simple getters/setters

### 2. Review Uncovered Code

```bash
# Find uncovered lines in Java
grep -r "INSTRUCTION.*0%" target/site/jacoco/

# Find uncovered files in JS
nyc report --reporter=text | grep "0.00"
```

### 3. Set Appropriate Thresholds

| Code Type | Line Coverage | Branch Coverage |
|-----------|---------------|-----------------|
| Core utilities | 80% | 70% |
| Page objects | 70% | 60% |
| API clients | 75% | 65% |
| Helpers | 70% | 60% |

### 4. Use Coverage Trends

Track coverage over time:

```yaml
# In CI, compare against baseline
- name: Coverage Trend Check
  run: |
    CURRENT=$(cat coverage.txt)
    BASELINE=$(curl -s https://api.example.com/coverage/baseline)
    if [ "$CURRENT" -lt "$BASELINE" ]; then
      echo "Coverage regression: $CURRENT% < $BASELINE%"
      exit 1
    fi
```

### 5. Don't Test Coverage, Test Behavior

- High coverage ≠ good tests
- Use mutation testing to verify test quality
- Review coverage with context

## Troubleshooting

### JaCoCo Not Generating Report

1. Check agent is prepared:
   ```bash
   mvn jacoco:prepare-agent test jacoco:report
   ```

2. Verify execution data exists:
   ```bash
   ls -la target/jacoco.exec
   ```

### NYC Coverage Shows 0%

1. Check source maps:
   ```bash
   # Ensure TypeScript generates source maps
   tsc --sourceMap
   ```

2. Verify include patterns:
   ```json
   "nyc": {
     "include": ["src/**/*.ts"],
     "extension": [".ts"]
   }
   ```

### Mutation Testing Timeout

Increase timeout for slow tests:

```xml
<configuration>
    <timeoutConstant>8000</timeoutConstant>
    <timeoutFactor>1.5</timeoutFactor>
</configuration>
```

## Reports Directory Structure

```
java-module/target/
├── site/
│   └── jacoco/
│       ├── index.html          # Main report
│       ├── jacoco.xml          # XML for CI
│       └── jacoco.csv          # CSV export
└── pit-reports/
    └── index.html              # Mutation report

js-module/playwright/coverage/
├── index.html                  # NYC HTML report
├── lcov.info                   # LCOV format
└── coverage-summary.json       # JSON summary
```
