#!/bin/bash

# Newman Collection Runner Script
# Runs Postman collections with comprehensive reporting

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COLLECTIONS_DIR="$SCRIPT_DIR/../collections"
ENVIRONMENTS_DIR="$SCRIPT_DIR/../environments"
REPORTS_DIR="$SCRIPT_DIR/../reports"

# Default values
COLLECTION="${1:-smoke-tests.postman_collection.json}"
ENVIRONMENT="${2:-qa.postman_environment.json}"
ITERATIONS="${ITERATIONS:-1}"
DELAY="${DELAY:-0}"
TIMEOUT="${TIMEOUT:-30000}"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Check if Newman is installed
check_newman() {
    if ! command -v newman &> /dev/null; then
        echo -e "${YELLOW}Newman not found. Installing...${NC}"
        npm install -g newman newman-reporter-htmlextra newman-reporter-allure
    fi
}

# Setup directories
setup_directories() {
    mkdir -p "$REPORTS_DIR"/{html,json,allure}
    echo -e "${GREEN}Reports will be saved to: $REPORTS_DIR${NC}"
}

# Run collection
run_collection() {
    local collection_file="$COLLECTIONS_DIR/$COLLECTION"
    local environment_file="$ENVIRONMENTS_DIR/$ENVIRONMENT"
    local timestamp=$(date +%Y%m%d_%H%M%S)
    local collection_name=$(basename "$COLLECTION" .postman_collection.json)

    if [[ ! -f "$collection_file" ]]; then
        echo -e "${RED}Error: Collection not found: $collection_file${NC}"
        exit 1
    fi

    echo -e "${YELLOW}Running Newman...${NC}"
    echo "Collection: $collection_file"
    echo "Environment: $environment_file"
    echo "Iterations: $ITERATIONS"
    echo ""

    local env_arg=""
    if [[ -f "$environment_file" ]]; then
        env_arg="-e $environment_file"
    fi

    newman run "$collection_file" \
        $env_arg \
        --iteration-count "$ITERATIONS" \
        --delay-request "$DELAY" \
        --timeout-request "$TIMEOUT" \
        --reporters cli,htmlextra,json,allure \
        --reporter-htmlextra-export "$REPORTS_DIR/html/${collection_name}_${timestamp}.html" \
        --reporter-htmlextra-title "API Test Report - $collection_name" \
        --reporter-htmlextra-browserTitle "Newman Report" \
        --reporter-htmlextra-showEnvironmentData \
        --reporter-htmlextra-skipSensitiveData \
        --reporter-json-export "$REPORTS_DIR/json/${collection_name}_${timestamp}.json" \
        --reporter-allure-export "$REPORTS_DIR/allure" \
        --color on

    local exit_code=$?

    echo ""
    if [[ $exit_code -eq 0 ]]; then
        echo -e "${GREEN}✓ All tests passed!${NC}"
    else
        echo -e "${RED}✗ Some tests failed!${NC}"
    fi

    echo ""
    echo "Reports generated:"
    echo "  HTML: $REPORTS_DIR/html/${collection_name}_${timestamp}.html"
    echo "  JSON: $REPORTS_DIR/json/${collection_name}_${timestamp}.json"
    echo "  Allure: $REPORTS_DIR/allure/"

    return $exit_code
}

# Run all collections
run_all() {
    echo -e "${YELLOW}Running all collections...${NC}"

    local failed=0
    for collection in "$COLLECTIONS_DIR"/*.postman_collection.json; do
        if [[ -f "$collection" ]]; then
            COLLECTION=$(basename "$collection")
            echo -e "\n${YELLOW}Running: $COLLECTION${NC}"
            run_collection || ((failed++))
        fi
    done

    if [[ $failed -gt 0 ]]; then
        echo -e "\n${RED}$failed collection(s) had failures${NC}"
        exit 1
    else
        echo -e "\n${GREEN}All collections passed!${NC}"
    fi
}

# Print usage
usage() {
    echo "Usage: $0 [collection.json] [environment.json]"
    echo ""
    echo "Arguments:"
    echo "  collection     Collection file name (default: smoke-tests.postman_collection.json)"
    echo "  environment    Environment file name (default: qa.postman_environment.json)"
    echo ""
    echo "Options (via environment variables):"
    echo "  ITERATIONS     Number of iterations (default: 1)"
    echo "  DELAY          Delay between requests in ms (default: 0)"
    echo "  TIMEOUT        Request timeout in ms (default: 30000)"
    echo ""
    echo "Commands:"
    echo "  all            Run all collections in the collections directory"
    echo ""
    echo "Examples:"
    echo "  $0                                    # Run smoke tests with QA env"
    echo "  $0 regression.postman_collection.json"
    echo "  $0 all                                # Run all collections"
    echo "  ITERATIONS=5 $0 smoke-tests.postman_collection.json"
}

# Main
main() {
    if [[ "$1" == "-h" || "$1" == "--help" ]]; then
        usage
        exit 0
    fi

    check_newman
    setup_directories

    if [[ "$1" == "all" ]]; then
        run_all
    else
        run_collection
    fi
}

main "$@"
