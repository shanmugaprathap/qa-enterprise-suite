# Performance Testing Guide

This guide covers performance testing practices using JMeter and integration with CI/CD pipelines.

## Overview

Performance testing validates that the application meets performance requirements under expected load conditions.

### Types of Performance Tests

| Type | Purpose | Duration |
|------|---------|----------|
| **Load Test** | Validate behavior under expected load | 10-30 minutes |
| **Stress Test** | Find breaking point | Until failure |
| **Soak Test** | Check for memory leaks over time | 4-24 hours |
| **Spike Test** | Validate sudden load increases | 5-15 minutes |
| **Scalability Test** | Measure scaling capability | Variable |

## JMeter Setup

### Prerequisites

- JMeter 5.6.3+
- Java 17+
- JMeter Plugins Manager

### Installation

```bash
# macOS
brew install jmeter

# Ubuntu
apt-get install jmeter

# Download manually
wget https://downloads.apache.org/jmeter/binaries/apache-jmeter-5.6.3.tgz
tar -xzf apache-jmeter-5.6.3.tgz
```

### Essential Plugins

Install via Plugins Manager:
- Custom Thread Groups
- 3 Basic Graphs
- Throughput Shaping Timer
- PerfMon Metrics Collector
- Response Times Over Time

## Test Plan Structure

### Basic Load Test Plan

```
Test Plan
├── User Defined Variables
│   ├── BASE_URL
│   ├── THREADS
│   ├── RAMP_UP
│   └── DURATION
├── Thread Group (Virtual Users)
│   ├── HTTP Request Defaults
│   ├── HTTP Header Manager
│   ├── CSV Data Set Config (test data)
│   ├── Login Request
│   │   └── JSON Extractor (auth token)
│   ├── Transaction Controller (User Journey)
│   │   ├── Request 1
│   │   ├── Request 2
│   │   └── Request 3
│   ├── Response Assertions
│   └── Timers (Think Time)
└── Listeners
    ├── View Results Tree
    ├── Summary Report
    ├── Aggregate Report
    └── Response Times Over Time
```

### Thread Group Configuration

```
Threads: ${THREADS}
Ramp-Up: ${RAMP_UP}
Loop Count: Forever / ${LOOPS}
Duration: ${DURATION}
```

### Common Settings

| Parameter | Dev | QA | Staging | Production-like |
|-----------|-----|-----|---------|-----------------|
| Threads | 5 | 10 | 50 | 200+ |
| Ramp-Up (s) | 10 | 30 | 60 | 120 |
| Duration (s) | 60 | 300 | 600 | 1800 |

## Running Tests

### Command Line Execution

```bash
# Basic run
jmeter -n -t test-plan.jmx -l results.jtl

# With properties
jmeter -n -t test-plan.jmx \
  -l results.jtl \
  -JTHREADS=100 \
  -JRAMP_UP=60 \
  -JDURATION=600

# Generate HTML report
jmeter -n -t test-plan.jmx \
  -l results.jtl \
  -e -o ./report
```

### Using the Run Script

```bash
cd performance/jmeter

# Run with defaults
./scripts/run-tests.sh load-test.jmx

# Run with custom parameters
THREADS=50 LOOPS=100 ./scripts/run-tests.sh load-test.jmx
```

## Key Metrics

### Response Time Metrics

| Metric | Description | Target |
|--------|-------------|--------|
| Average | Mean response time | < 2s |
| 90th Percentile | 90% of requests below this | < 3s |
| 95th Percentile | 95% of requests below this | < 5s |
| 99th Percentile | 99% of requests below this | < 10s |

### Throughput Metrics

| Metric | Description |
|--------|-------------|
| Transactions/sec | Requests completed per second |
| Bytes/sec | Data transferred per second |
| Error Rate | Percentage of failed requests |

### Target Thresholds

```
- Response Time (avg): < 2000ms
- Response Time (p95): < 5000ms
- Error Rate: < 1%
- Throughput: > 100 TPS (depends on app)
```

## Best Practices

### 1. Test Data Management

```xml
<!-- CSV Data Set Config -->
<CSVDataSet>
  <stringProp name="filename">test-data.csv</stringProp>
  <stringProp name="variableNames">username,password</stringProp>
  <boolProp name="recycle">true</boolProp>
  <boolProp name="stopThread">false</boolProp>
  <stringProp name="shareMode">shareMode.all</stringProp>
</CSVDataSet>
```

### 2. Correlation (Dynamic Values)

```xml
<!-- JSON Extractor for auth token -->
<JSONPostProcessor>
  <stringProp name="jsonPathExprs">$.token</stringProp>
  <stringProp name="referenceNames">AUTH_TOKEN</stringProp>
</JSONPostProcessor>
```

### 3. Think Time

```xml
<!-- Uniform Random Timer -->
<UniformRandomTimer>
  <stringProp name="RandomTimer.range">2000</stringProp>
  <stringProp name="ConstantTimer.delay">1000</stringProp>
</UniformRandomTimer>
```

### 4. Assertions

```xml
<!-- Response Assertion -->
<ResponseAssertion>
  <collectionProp name="Asserion.test_strings">
    <stringProp>200</stringProp>
  </collectionProp>
  <stringProp name="Assertion.test_field">Assertion.response_code</stringProp>
</ResponseAssertion>

<!-- Duration Assertion -->
<DurationAssertion>
  <stringProp name="DurationAssertion.duration">5000</stringProp>
</DurationAssertion>
```

## CI/CD Integration

### GitHub Actions

```yaml
- name: Run Performance Tests
  run: |
    jmeter -n -t performance/jmeter/test-plans/load-test.jmx \
      -l results.jtl \
      -JTHREADS=50 \
      -JDURATION=300 \
      -e -o performance-report

- name: Check Performance Thresholds
  run: |
    # Parse results and check thresholds
    AVG_TIME=$(grep -oP 'Average:\s*\K\d+' results.jtl)
    if [ "$AVG_TIME" -gt 2000 ]; then
      echo "Performance degradation: avg response time ${AVG_TIME}ms > 2000ms"
      exit 1
    fi

- name: Upload Report
  uses: actions/upload-artifact@v4
  with:
    name: performance-report
    path: performance-report
```

### Grafana Dashboard

For real-time monitoring, configure JMeter Backend Listener:

```xml
<BackendListener>
  <stringProp name="classname">org.apache.jmeter.visualizers.backend.influxdb.InfluxdbBackendListenerClient</stringProp>
  <elementProp name="arguments">
    <collectionProp name="Arguments.arguments">
      <elementProp name="influxdbUrl">
        <stringProp name="Argument.value">http://influxdb:8086/write?db=jmeter</stringProp>
      </elementProp>
    </collectionProp>
  </elementProp>
</BackendListener>
```

## Distributed Testing

### Master-Slave Setup

```bash
# On slave machines
jmeter-server

# On master
jmeter -n -t test-plan.jmx \
  -R slave1:1099,slave2:1099,slave3:1099 \
  -l results.jtl
```

### Kubernetes Scaling

Use the provided `k8s/selenium-grid.yaml` with HorizontalPodAutoscaler for dynamic scaling of test runners.

## Troubleshooting

### Out of Memory

```bash
# Increase heap size
export HEAP="-Xms2g -Xmx4g"
jmeter $HEAP -n -t test-plan.jmx
```

### Too Many Open Files

```bash
# Increase file descriptor limit
ulimit -n 10000
```

### Connection Refused

- Check target server capacity
- Verify network connectivity
- Review connection pool settings

## Report Analysis

### Key Questions to Answer

1. What is the average response time under load?
2. What is the 95th percentile response time?
3. At what load does performance degrade?
4. Are there any errors under load?
5. How does the system behave over extended periods?

### Sample Report Template

```markdown
# Performance Test Report

## Test Summary
- Test Date: YYYY-MM-DD
- Test Duration: X minutes
- Virtual Users: X
- Total Requests: X

## Results

| Metric | Value | Target | Status |
|--------|-------|--------|--------|
| Avg Response Time | Xms | <2000ms | ✓/✗ |
| 95th Percentile | Xms | <5000ms | ✓/✗ |
| Throughput | X/s | >100/s | ✓/✗ |
| Error Rate | X% | <1% | ✓/✗ |

## Recommendations
- [List any recommendations based on findings]
```
