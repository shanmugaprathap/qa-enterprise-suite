# Report Portal Setup Guide

This guide covers setting up and using Report Portal for centralized test reporting.

## Quick Start

### 1. Start Report Portal

```bash
cd docker
docker-compose -f docker-compose.reportportal.yml up -d
```

Wait for all services to start (2-3 minutes).

### 2. Access UI

Open http://localhost:8080

Default credentials:
- Username: `admin`
- Password: `erebus`

### 3. Get API Key

1. Click user icon → Profile
2. Scroll to "API Keys"
3. Generate new key

### 4. Configure Tests

Set environment variables:

```bash
export RP_ENDPOINT=http://localhost:8080
export RP_API_KEY=your-api-key
export RP_PROJECT=qa-enterprise-suite
```

## Full Setup Guide

### Prerequisites

- Docker 20.10+
- Docker Compose 2.0+
- 8GB+ RAM available
- 10GB disk space

### Service Components

| Service | Purpose | Port |
|---------|---------|------|
| Gateway | Traefik reverse proxy | 8080 |
| API | Core API service | - |
| UAT | Authorization | - |
| UI | Web interface | - |
| PostgreSQL | Database | 5432 |
| RabbitMQ | Message queue | 5672, 15672 |
| Elasticsearch | Search/Analytics | 9200 |
| MinIO | File storage | 9000, 9001 |
| Analyzer | Auto-analysis | - |

### Starting Services

```bash
# Start all services
docker-compose -f docker-compose.reportportal.yml up -d

# Check status
docker-compose -f docker-compose.reportportal.yml ps

# View logs
docker-compose -f docker-compose.reportportal.yml logs -f
```

### Stopping Services

```bash
# Stop services (keep data)
docker-compose -f docker-compose.reportportal.yml stop

# Stop and remove (keep volumes)
docker-compose -f docker-compose.reportportal.yml down

# Remove everything including data
docker-compose -f docker-compose.reportportal.yml down -v
```

## Project Configuration

### Create a Project

1. Login as admin
2. Go to "Administrate" → "Projects"
3. Click "Add Project"
4. Name: `qa-enterprise-suite`

### Configure Project Settings

1. Go to project settings
2. Configure:
   - **Launch name pattern**: `[LAUNCH_NAME]`
   - **Auto analysis**: Enable
   - **Pattern analysis**: Enable

## Java Integration

### Add Dependencies

Dependencies are already in the parent POM. For standalone projects:

```xml
<dependency>
    <groupId>com.epam.reportportal</groupId>
    <artifactId>agent-java-testng</artifactId>
    <version>5.2.4</version>
</dependency>
```

### Add Listener

In testng.xml:
```xml
<listeners>
    <listener class-name="com.epam.reportportal.testng.ReportPortalTestNGListener"/>
</listeners>
```

Or programmatically:
```java
@Listeners({ReportPortalTestNGListener.class})
public class MyTest { }
```

### Create reportportal.properties

Create `src/test/resources/reportportal.properties`:

```properties
rp.endpoint=http://localhost:8080
rp.api.key=${RP_API_KEY}
rp.project=qa-enterprise-suite
rp.launch=My Test Launch
rp.enable=true
rp.attributes=env:qa;team:automation
```

## JavaScript Integration

### Install Agent

```bash
npm install @reportportal/agent-js-playwright
```

### Configure Playwright

```typescript
// playwright.config.ts
import { defineConfig } from '@playwright/test';

export default defineConfig({
  reporter: [
    ['@reportportal/agent-js-playwright', {
      endpoint: process.env.RP_ENDPOINT,
      apiKey: process.env.RP_API_KEY,
      project: 'qa-enterprise-suite',
      launch: 'Playwright Tests',
      attributes: [
        { key: 'env', value: 'qa' }
      ]
    }]
  ]
});
```

## Features

### Dashboard

Create custom dashboards:
1. Go to Dashboards
2. Click "Add New Dashboard"
3. Add widgets:
   - Launch statistics
   - Failed tests trend
   - Test duration

### Filters

Create filters to organize launches:
1. Go to Filters
2. Click "Add Filter"
3. Configure criteria (name, tags, status)

### Auto-Analysis

Report Portal can automatically analyze failures:
1. Enable in project settings
2. It uses ML to suggest defect types
3. Review and train the model

### Pattern Analysis

Identify common failure patterns:
1. Configure patterns in settings
2. System highlights matching failures
3. Useful for flaky test detection

## CI/CD Integration

### GitHub Actions

```yaml
- name: Run tests with Report Portal
  env:
    RP_ENDPOINT: ${{ secrets.RP_ENDPOINT }}
    RP_API_KEY: ${{ secrets.RP_API_KEY }}
    RP_PROJECT: qa-enterprise-suite
  run: mvn test
```

### Jenkins

```groovy
environment {
    RP_ENDPOINT = credentials('rp-endpoint')
    RP_API_KEY = credentials('rp-api-key')
}
stages {
    stage('Test') {
        steps {
            sh 'mvn test'
        }
    }
}
```

## Troubleshooting

### Services Won't Start

Check resources:
```bash
docker system df
docker stats
```

Increase Docker memory to 8GB+.

### Can't Connect to UI

1. Check gateway service: `docker logs rp-gateway`
2. Verify port 8080 is not in use
3. Wait for services to fully start

### Tests Don't Report

1. Verify RP_ENDPOINT and RP_API_KEY
2. Check network connectivity
3. Verify project exists
4. Check test logs for RP errors

### Analyzer Not Working

1. Verify Elasticsearch is healthy
2. Check analyzer logs
3. Re-index if needed

## Maintenance

### Backup Data

```bash
# Backup PostgreSQL
docker exec rp-postgres pg_dump -U rpuser reportportal > backup.sql

# Backup MinIO
docker cp rp-minio:/data ./minio-backup
```

### Clean Old Data

Configure data retention in project settings:
- Launch TTL: 90 days
- Screenshot TTL: 30 days

### Update Version

```bash
# Pull latest images
docker-compose -f docker-compose.reportportal.yml pull

# Recreate services
docker-compose -f docker-compose.reportportal.yml up -d
```
