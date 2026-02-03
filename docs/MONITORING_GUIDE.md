# Monitoring & Observability Guide

This guide covers the monitoring stack for the QA Enterprise Suite, including metrics collection, visualization, and alerting.

## Overview

The monitoring stack provides real-time visibility into test execution, quality metrics, and AI-driven testing features.

### Components

| Component | Purpose | Port |
|-----------|---------|------|
| **Prometheus** | Metrics collection & storage | 9090 |
| **Grafana** | Dashboards & visualization | 3000 |
| **InfluxDB** | Time-series data for JMeter | 8086 |
| **Loki** | Log aggregation | 3100 |
| **Pushgateway** | Batch job metrics | 9091 |

## Quick Start

### Start Monitoring Stack

```bash
cd docker
docker-compose -f docker-compose.monitoring.yml up -d
```

### Access Dashboards

- **Grafana**: http://localhost:3000 (admin/admin)
- **Prometheus**: http://localhost:9090
- **Pushgateway**: http://localhost:9091

## Metrics

### Test Execution Metrics

| Metric | Description | Labels |
|--------|-------------|--------|
| `test_total` | Total number of tests | module, category |
| `test_passed_total` | Number of passed tests | module, category |
| `test_failed_total` | Number of failed tests | module, category |
| `test_skipped_total` | Number of skipped tests | module |
| `test_execution_duration_ms` | Execution duration | - |
| `test_flaky_rate` | Percentage of flaky tests | - |

### Performance Metrics

| Metric | Description | Labels |
|--------|-------------|--------|
| `api_response_time_ms` | API response time | endpoint, method |
| `jmeter_throughput` | JMeter throughput (req/s) | test_plan |
| `jmeter_error_rate` | JMeter error rate | test_plan |

### AI/ML Metrics

| Metric | Description | Labels |
|--------|-------------|--------|
| `self_healing_attempt_count` | Self-healing attempts | - |
| `self_healing_success_count` | Successful healings | - |
| `ai_data_generation_count` | AI-generated data items | type |
| `ai_test_risk_prediction` | Risk score predictions | test_id |

## Dashboards

### QA Test Quality Dashboard

The main dashboard (`test-quality-dashboard.json`) provides:

1. **Test Execution Overview**
   - Pass rate gauge
   - Test counts (total, passed, failed, skipped)
   - Execution duration
   - Flaky test rate

2. **Pass Rate Trend**
   - Historical pass rate over time
   - Configurable time range

3. **Test Categories**
   - Tests by category (pie chart)
   - Pass rate by module (bar chart)
   - Top failing tests (table)

4. **Performance Metrics**
   - API response times
   - JMeter throughput and error rate

5. **AI/ML Insights**
   - Risk predictions
   - Self-healing events
   - AI data generation count

## Alerting

### Test Quality Alerts

```yaml
# Low pass rate warning
LowTestPassRate:
  threshold: < 90%
  severity: warning

# Critical pass rate
CriticalTestPassRate:
  threshold: < 70%
  severity: critical

# High flaky rate
HighFlakyTestRate:
  threshold: > 10%
  severity: warning
```

### Performance Alerts

```yaml
# High API response time
HighAPIResponseTime:
  threshold: > 5000ms (95th percentile)
  severity: warning

# High error rate
HighErrorRate:
  threshold: > 1%
  severity: critical
```

### AI Feature Alerts

```yaml
# Low self-healing success rate
LowSelfHealingSuccessRate:
  threshold: < 50%
  severity: warning
```

## Integration

### CI/CD Pipeline Integration

Add metrics push to your pipeline:

```yaml
- name: Push Test Metrics
  run: |
    # Push metrics to Pushgateway
    cat <<EOF | curl --data-binary @- http://pushgateway:9091/metrics/job/ci_tests/instance/${GITHUB_RUN_ID}
    test_total ${TEST_TOTAL}
    test_passed_total ${TEST_PASSED}
    test_failed_total ${TEST_FAILED}
    test_execution_duration_ms ${DURATION_MS}
    EOF
```

### JMeter Integration

Configure JMeter Backend Listener:

```xml
<BackendListener>
  <stringProp name="classname">
    org.apache.jmeter.visualizers.backend.influxdb.InfluxdbBackendListenerClient
  </stringProp>
  <elementProp name="arguments">
    <collectionProp>
      <elementProp name="influxdbUrl">
        <stringProp name="Argument.value">
          http://influxdb:8086/api/v2/write?org=qa-enterprise&bucket=jmeter
        </stringProp>
      </elementProp>
      <elementProp name="influxdbToken">
        <stringProp name="Argument.value">${INFLUXDB_TOKEN}</stringProp>
      </elementProp>
    </collectionProp>
  </elementProp>
</BackendListener>
```

### Allure Results Integration

The test-metrics-exporter service automatically reads Allure results:

```bash
# Mount Allure results to exporter
volumes:
  - ./java-module/target/allure-results:/results:ro
```

## Custom Metrics

### Adding Custom Metrics in Java

```java
// Using Micrometer
@Component
public class TestMetrics {
    private final Counter selfHealingCounter;

    public TestMetrics(MeterRegistry registry) {
        this.selfHealingCounter = Counter.builder("self_healing_attempts")
            .description("Number of self-healing attempts")
            .register(registry);
    }

    public void recordSelfHealingAttempt() {
        selfHealingCounter.increment();
    }
}
```

### Adding Custom Metrics in JavaScript

```javascript
// Push to Pushgateway
const pushMetric = async (name, value, labels = {}) => {
  const labelStr = Object.entries(labels)
    .map(([k, v]) => `${k}="${v}"`)
    .join(',');

  const metric = `${name}{${labelStr}} ${value}\n`;

  await fetch('http://pushgateway:9091/metrics/job/playwright_tests', {
    method: 'POST',
    body: metric,
    headers: { 'Content-Type': 'text/plain' }
  });
};
```

## Troubleshooting

### Metrics Not Appearing

1. Check exporter is running:
   ```bash
   curl http://localhost:8080/metrics
   ```

2. Verify Prometheus targets:
   - Go to http://localhost:9090/targets
   - Check all targets are "UP"

3. Check scrape configuration:
   ```bash
   docker exec prometheus cat /etc/prometheus/prometheus.yml
   ```

### Grafana Dashboard Issues

1. Verify datasource connection:
   - Go to Configuration > Data Sources
   - Click "Test" on Prometheus

2. Check dashboard JSON:
   ```bash
   cat monitoring/grafana/dashboards/test-quality-dashboard.json | jq .
   ```

### InfluxDB Connection Issues

1. Check InfluxDB is running:
   ```bash
   curl http://localhost:8086/health
   ```

2. Verify token:
   ```bash
   docker exec influxdb influx auth list
   ```

## Best Practices

1. **Metric Naming**: Use `snake_case` and include unit suffix (`_ms`, `_total`, `_rate`)

2. **Label Cardinality**: Keep label values bounded (don't use test IDs as labels)

3. **Retention**: Configure appropriate retention periods
   - Prometheus: 30 days for detailed metrics
   - InfluxDB: 90 days for performance data

4. **Alerting**: Start with warning alerts, escalate to critical based on duration

5. **Dashboard Organization**: Group related metrics, use consistent colors
