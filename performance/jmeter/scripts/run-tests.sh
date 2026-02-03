#!/bin/bash

# JMeter Test Runner Script
# Usage: ./run-tests.sh [test-plan.jmx] [options]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TEST_PLANS_DIR="$SCRIPT_DIR/../test-plans"
REPORTS_DIR="$SCRIPT_DIR/../reports"

# Default values
TEST_PLAN="${1:-load-test.jmx}"
THREADS="${THREADS:-10}"
LOOPS="${LOOPS:-10}"
RAMP_UP="${RAMP_UP:-30}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if JMeter is installed
check_jmeter() {
    if ! command -v jmeter &> /dev/null; then
        echo -e "${RED}Error: JMeter is not installed or not in PATH${NC}"
        echo "Install JMeter: brew install jmeter (macOS) or apt-get install jmeter (Ubuntu)"
        exit 1
    fi
}

# Create reports directory
setup_directories() {
    mkdir -p "$REPORTS_DIR"
    echo -e "${GREEN}Reports directory: $REPORTS_DIR${NC}"
}

# Run the test
run_test() {
    local test_plan="$TEST_PLANS_DIR/$TEST_PLAN"
    local timestamp=$(date +%Y%m%d_%H%M%S)
    local results_file="$REPORTS_DIR/results_${timestamp}.jtl"
    local report_dir="$REPORTS_DIR/report_${timestamp}"

    if [[ ! -f "$test_plan" ]]; then
        echo -e "${RED}Error: Test plan not found: $test_plan${NC}"
        exit 1
    fi

    echo -e "${YELLOW}Running JMeter test...${NC}"
    echo "Test Plan: $test_plan"
    echo "Threads: $THREADS"
    echo "Loops: $LOOPS"
    echo "Ramp-up: $RAMP_UP seconds"
    echo ""

    # Run JMeter in non-GUI mode
    jmeter -n \
        -t "$test_plan" \
        -l "$results_file" \
        -e -o "$report_dir" \
        -Jthreads="$THREADS" \
        -Jloops="$LOOPS" \
        -Jrampup="$RAMP_UP"

    echo ""
    echo -e "${GREEN}Test completed!${NC}"
    echo "Results: $results_file"
    echo "HTML Report: $report_dir/index.html"
}

# Print usage
usage() {
    echo "Usage: $0 [test-plan.jmx] [options]"
    echo ""
    echo "Options (via environment variables):"
    echo "  THREADS   Number of concurrent users (default: 10)"
    echo "  LOOPS     Number of iterations per user (default: 10)"
    echo "  RAMP_UP   Ramp-up time in seconds (default: 30)"
    echo ""
    echo "Examples:"
    echo "  $0 load-test.jmx"
    echo "  THREADS=50 LOOPS=100 $0 load-test.jmx"
}

# Main
main() {
    if [[ "$1" == "-h" || "$1" == "--help" ]]; then
        usage
        exit 0
    fi

    check_jmeter
    setup_directories
    run_test
}

main "$@"
