#!/usr/bin/env python3
"""
Test Metrics Exporter for Prometheus
Reads Allure results and exposes metrics for Prometheus scraping
"""

import json
import os
import glob
from http.server import HTTPServer, BaseHTTPRequestHandler
from datetime import datetime


class MetricsHandler(BaseHTTPRequestHandler):
    """HTTP handler for Prometheus metrics endpoint"""

    def do_GET(self):
        if self.path == '/metrics':
            metrics = generate_metrics()
            self.send_response(200)
            self.send_header('Content-Type', 'text/plain; charset=utf-8')
            self.end_headers()
            self.wfile.write(metrics.encode('utf-8'))
        elif self.path == '/health':
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            self.wfile.write(b'{"status": "healthy"}')
        else:
            self.send_response(404)
            self.end_headers()

    def log_message(self, format, *args):
        """Suppress default logging"""
        pass


def generate_metrics():
    """Generate Prometheus metrics from test results"""
    results_dir = os.environ.get('RESULTS_DIR', '/results')
    metrics = []

    # Test execution metrics
    test_stats = parse_allure_results(results_dir)

    # Basic test counts
    metrics.append(f'# HELP test_total Total number of tests')
    metrics.append(f'# TYPE test_total gauge')
    metrics.append(f'test_total {test_stats["total"]}')

    metrics.append(f'# HELP test_passed_total Number of passed tests')
    metrics.append(f'# TYPE test_passed_total gauge')
    metrics.append(f'test_passed_total {test_stats["passed"]}')

    metrics.append(f'# HELP test_failed_total Number of failed tests')
    metrics.append(f'# TYPE test_failed_total gauge')
    metrics.append(f'test_failed_total {test_stats["failed"]}')

    metrics.append(f'# HELP test_skipped_total Number of skipped tests')
    metrics.append(f'# TYPE test_skipped_total gauge')
    metrics.append(f'test_skipped_total {test_stats["skipped"]}')

    metrics.append(f'# HELP test_broken_total Number of broken tests')
    metrics.append(f'# TYPE test_broken_total gauge')
    metrics.append(f'test_broken_total {test_stats["broken"]}')

    # Execution duration
    metrics.append(f'# HELP test_execution_duration_ms Total test execution duration in milliseconds')
    metrics.append(f'# TYPE test_execution_duration_ms gauge')
    metrics.append(f'test_execution_duration_ms {test_stats["duration_ms"]}')

    # Flaky test rate
    metrics.append(f'# HELP test_flaky_rate Percentage of flaky tests')
    metrics.append(f'# TYPE test_flaky_rate gauge')
    metrics.append(f'test_flaky_rate {test_stats["flaky_rate"]}')

    # Tests by category
    metrics.append(f'# HELP test_count_by_category Test count by category')
    metrics.append(f'# TYPE test_count_by_category gauge')
    for category, count in test_stats['by_category'].items():
        metrics.append(f'test_count_by_category{{category="{category}"}} {count}')

    # Tests by module
    metrics.append(f'# HELP test_pass_rate_by_module Pass rate by module')
    metrics.append(f'# TYPE test_pass_rate_by_module gauge')
    for module, rate in test_stats['pass_rate_by_module'].items():
        metrics.append(f'test_pass_rate_by_module{{module="{module}"}} {rate}')

    # Self-healing metrics
    metrics.append(f'# HELP self_healing_attempt_count Number of self-healing attempts')
    metrics.append(f'# TYPE self_healing_attempt_count counter')
    metrics.append(f'self_healing_attempt_count {test_stats.get("self_healing_attempts", 0)}')

    metrics.append(f'# HELP self_healing_success_count Number of successful self-healing events')
    metrics.append(f'# TYPE self_healing_success_count counter')
    metrics.append(f'self_healing_success_count {test_stats.get("self_healing_success", 0)}')

    # AI data generation count
    metrics.append(f'# HELP ai_data_generation_count Number of AI-generated test data items')
    metrics.append(f'# TYPE ai_data_generation_count counter')
    metrics.append(f'ai_data_generation_count {test_stats.get("ai_data_count", 0)}')

    # Timestamp
    metrics.append(f'# HELP test_metrics_last_update_timestamp Last update timestamp')
    metrics.append(f'# TYPE test_metrics_last_update_timestamp gauge')
    metrics.append(f'test_metrics_last_update_timestamp {int(datetime.now().timestamp())}')

    return '\n'.join(metrics) + '\n'


def parse_allure_results(results_dir):
    """Parse Allure results directory and extract statistics"""
    stats = {
        'total': 0,
        'passed': 0,
        'failed': 0,
        'skipped': 0,
        'broken': 0,
        'duration_ms': 0,
        'flaky_rate': 0,
        'by_category': {},
        'pass_rate_by_module': {},
        'self_healing_attempts': 0,
        'self_healing_success': 0,
        'ai_data_count': 0
    }

    # Module stats tracking
    module_stats = {}

    # Parse result files
    result_files = glob.glob(os.path.join(results_dir, '*-result.json'))

    for file_path in result_files:
        try:
            with open(file_path, 'r') as f:
                result = json.load(f)

            stats['total'] += 1

            # Status
            status = result.get('status', 'unknown')
            if status == 'passed':
                stats['passed'] += 1
            elif status == 'failed':
                stats['failed'] += 1
            elif status == 'skipped':
                stats['skipped'] += 1
            elif status == 'broken':
                stats['broken'] += 1

            # Duration
            start = result.get('start', 0)
            stop = result.get('stop', 0)
            stats['duration_ms'] += (stop - start)

            # Extract labels for categorization
            labels = result.get('labels', [])
            for label in labels:
                name = label.get('name', '')
                value = label.get('value', '')

                # Category (suite, feature, story)
                if name in ['suite', 'feature', 'epic']:
                    if value not in stats['by_category']:
                        stats['by_category'][value] = 0
                    stats['by_category'][value] += 1

                # Module tracking
                if name == 'package':
                    module = value.split('.')[-2] if '.' in value else value
                    if module not in module_stats:
                        module_stats[module] = {'total': 0, 'passed': 0}
                    module_stats[module]['total'] += 1
                    if status == 'passed':
                        module_stats[module]['passed'] += 1

            # Check for flaky marker
            if result.get('flaky', False):
                stats['flaky_rate'] += 1

            # Check for self-healing in attachments/steps
            steps = result.get('steps', [])
            for step in steps:
                step_name = step.get('name', '').lower()
                if 'self-healing' in step_name or 'healed' in step_name:
                    stats['self_healing_attempts'] += 1
                    if step.get('status') == 'passed':
                        stats['self_healing_success'] += 1
                if 'ai-generated' in step_name or 'llm' in step_name:
                    stats['ai_data_count'] += 1

        except (json.JSONDecodeError, KeyError, IOError) as e:
            print(f"Error parsing {file_path}: {e}")
            continue

    # Calculate flaky rate percentage
    if stats['total'] > 0:
        stats['flaky_rate'] = (stats['flaky_rate'] / stats['total']) * 100

    # Calculate pass rate by module
    for module, data in module_stats.items():
        if data['total'] > 0:
            stats['pass_rate_by_module'][module] = (data['passed'] / data['total']) * 100
        else:
            stats['pass_rate_by_module'][module] = 0

    # Default categories if none found
    if not stats['by_category']:
        stats['by_category'] = {'smoke': 0, 'regression': 0, 'api': 0, 'ui': 0}

    if not stats['pass_rate_by_module']:
        stats['pass_rate_by_module'] = {'core': 0, 'ui': 0, 'api': 0}

    return stats


def main():
    """Start the metrics server"""
    port = int(os.environ.get('PORT', 8080))
    server = HTTPServer(('0.0.0.0', port), MetricsHandler)
    print(f"Test Metrics Exporter running on port {port}")
    print(f"Metrics endpoint: http://localhost:{port}/metrics")
    print(f"Health endpoint: http://localhost:{port}/health")
    server.serve_forever()


if __name__ == '__main__':
    main()
