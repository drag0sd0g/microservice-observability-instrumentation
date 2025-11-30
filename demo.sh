#!/usr/bin/env bash

# Demo script to simulate realistic traffic to the observability microservices
# macOS-compatible: replaces `shuf` with a rand_range helper using $RANDOM
# Fixed BSD `head -n-1` incompatibility by using `sed '$d'` to drop the last line.
#
# Usage: ./demo.sh {traffic|chaos-latency-on|chaos-latency-off|order|inventory|orders} [args]
# See --help for more details.

set -euo pipefail

# Configuration - can be overridden with environment variables
GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
INVENTORY_URL="${INVENTORY_URL:-http://localhost:8082}"
readonly CURL_TIMEOUT=10

echo "🚀 Starting Observability Demo Traffic Simulation"
echo "=================================================="
echo "Gateway URL: $GATEWAY_URL"
echo "Inventory URL: $INVENTORY_URL"
echo ""

# Colors for output
readonly GREEN='\033[0;32m'
readonly RED='\033[0;31m'
readonly YELLOW='\033[1;33m'
readonly NC='\033[0m' # No Color

# Helper: generate a random integer in [min,max]
rand_range() {
    local min=$1
    local max=$2
    # Ensure min <= max
    if [ "$min" -gt "$max" ]; then
        local tmp=$min
        min=$max
        max=$tmp
    fi
    echo $(( RANDOM % (max - min + 1) + min ))
}

# Helper: check if a service is available
check_service() {
    local url=$1
    local service_name=$2
    if curl -s --connect-timeout 5 "$url/api/health" > /dev/null 2>&1; then
        echo -e "${GREEN}✓${NC} $service_name is available"
        return 0
    else
        echo -e "${RED}✗${NC} $service_name is not available at $url"
        return 1
    fi
}

# Function to create orders
create_order() {
    local item_id=$1
    local quantity=$2

    response=$(curl -s -w "\n%{http_code}" --connect-timeout "${CURL_TIMEOUT}" -X POST "$GATEWAY_URL/api/orders" \
        -H "Content-Type: application/json" \
        -d "{\"itemId\":\"$item_id\",\"quantity\":$quantity}")

    http_code=$(printf '%s\n' "$response" | tail -n1)
    # Portable removal of last line (the http code) on macOS and Linux
    body=$(printf '%s\n' "$response" | sed '$d')

    if [ "$http_code" -eq 201 ] || [ "$http_code" -eq 200 ]; then
        echo -e "${GREEN}✓${NC} Created order for item $item_id (quantity: $quantity)"
    else
        echo -e "${RED}✗${NC} Failed to create order for item $item_id (HTTP $http_code)"
        # Print body for debugging
        echo "  Response: $body"
    fi
}

# Function to get orders
get_orders() {
    response=$(curl -s -w "\n%{http_code}" --connect-timeout "${CURL_TIMEOUT}" "$GATEWAY_URL/api/orders")
    http_code=$(printf '%s\n' "$response" | tail -n1)

    if [ "$http_code" -eq 200 ]; then
        echo -e "${GREEN}✓${NC} Retrieved orders list"
    else
        echo -e "${RED}✗${NC} Failed to get orders (HTTP $http_code)"
    fi
}

# Function to check inventory
check_inventory() {
    local item_id=$1

    response=$(curl -s -w "\n%{http_code}" --connect-timeout "${CURL_TIMEOUT}" "$GATEWAY_URL/api/inventory/$item_id")
    http_code=$(printf '%s\n' "$response" | tail -n1)

    if [ "$http_code" -eq 200 ]; then
        echo -e "${GREEN}✓${NC} Checked inventory for item $item_id"
    else
        echo -e "${RED}✗${NC} Failed to check inventory for item $item_id (HTTP $http_code)"
    fi
}

# Function to enable chaos latency
enable_chaos_latency() {
    echo -e "${YELLOW}⚠${NC}  Enabling chaos latency..."
    curl -s --connect-timeout "${CURL_TIMEOUT}" -X POST "$INVENTORY_URL/api/chaos/latency" \
        -H "Content-Type: application/json" \
        -d '{"enabled":true,"min":500,"max":2000}' > /dev/null
    echo -e "${YELLOW}⚠${NC}  Chaos latency enabled (500-2000ms)"
}

# Function to disable chaos latency
disable_chaos_latency() {
    echo -e "${GREEN}✓${NC} Disabling chaos latency..."
    curl -s --connect-timeout "${CURL_TIMEOUT}" -X POST "$INVENTORY_URL/api/chaos/latency" \
        -H "Content-Type: application/json" \
        -d '{"enabled":false}' > /dev/null
    echo -e "${GREEN}✓${NC} Chaos latency disabled"
}

# Function to enable chaos errors
enable_chaos_errors() {
    echo -e "${YELLOW}⚠${NC}  Enabling chaos errors..."
    curl -s --connect-timeout "${CURL_TIMEOUT}" -X POST "$INVENTORY_URL/api/chaos/errors" \
        -H "Content-Type: application/json" \
        -d '{"enabled":true,"rate":0.2}' > /dev/null
    echo -e "${YELLOW}⚠${NC}  Chaos errors enabled (20% error rate)"
}

# Function to disable chaos errors
disable_chaos_errors() {
    echo -e "${GREEN}✓${NC} Disabling chaos errors..."
    curl -s --connect-timeout "${CURL_TIMEOUT}" -X POST "$INVENTORY_URL/api/chaos/errors" \
        -H "Content-Type: application/json" \
        -d '{"enabled":false}' > /dev/null
    echo -e "${GREEN}✓${NC} Chaos errors disabled"
}

# Main simulation loop
simulate_traffic() {
    local duration=${1:-300}  # Default 5 minutes
    local start_time
    start_time=$(date +%s)
    local end_time=$((start_time + duration))

    # Check if services are available before starting
    echo "Checking service availability..."
    if ! check_service "$GATEWAY_URL" "Gateway Service"; then
        echo -e "${RED}Error: Gateway service is not available. Please ensure services are running.${NC}"
        exit 1
    fi

    echo ""
    echo "Running traffic simulation for $duration seconds..."
    echo ""

    while [ "$(date +%s)" -lt "$end_time" ]; do
        # Normal traffic pattern
        for _ in {1..5}; do
            item_id="ITEM-$(rand_range 1 10)"
            quantity=$(rand_range 1 5)
            create_order "$item_id" "$quantity"
            sleep 0.5
        done

        # Check some inventory
        for _ in {1..3}; do
            item_id="ITEM-$(rand_range 1 10)"
            check_inventory "$item_id"
            sleep 0.3
        done

        # Get orders periodically
        get_orders
        sleep 1

        # Randomly introduce chaos occasionally (~10% chance each loop)
        if [ $((RANDOM % 10)) -eq 0 ]; then
            enable_chaos_latency
            sleep 10
            disable_chaos_latency
        fi

        sleep 2
    done

    echo ""
    echo "Traffic simulation completed!"
}

# Parse command line arguments
case "${1:-}" in
    "traffic")
        duration=${2:-300}
        simulate_traffic "$duration"
        ;;
    "chaos-latency-on")
        enable_chaos_latency
        ;;
    "chaos-latency-off")
        disable_chaos_latency
        ;;
    "chaos-errors-on")
        enable_chaos_errors
        ;;
    "chaos-errors-off")
        disable_chaos_errors
        ;;
    "order")
        item_id=${2:-"ITEM-1"}
        quantity=${3:-1}
        create_order "$item_id" "$quantity"
        ;;
    "inventory")
        item_id=${2:-"ITEM-1"}
        check_inventory "$item_id"
        ;;
    "orders")
        get_orders
        ;;
    "check")
        echo "Checking service availability..."
        check_service "$GATEWAY_URL" "Gateway Service"
        check_service "$INVENTORY_URL" "Inventory Service"
        ;;
    "-h"|"--help"|*)
        echo "Usage: $0 {traffic|chaos-latency-on|chaos-latency-off|chaos-errors-on|chaos-errors-off|order|inventory|orders|check} [args]"
        echo ""
        echo "Commands:"
        echo "  traffic [duration]           - Run traffic simulation (default: 300 seconds)"
        echo "  chaos-latency-on            - Enable chaos latency (500-2000ms)"
        echo "  chaos-latency-off           - Disable chaos latency"
        echo "  chaos-errors-on             - Enable chaos errors (20% rate)"
        echo "  chaos-errors-off            - Disable chaos errors"
        echo "  order [item_id] [quantity]  - Create a single order"
        echo "  inventory [item_id]         - Check inventory for an item"
        echo "  orders                      - Get all orders"
        echo "  check                       - Check if services are available"
        echo ""
        echo "Environment Variables:"
        echo "  GATEWAY_URL    - Gateway service URL (default: http://localhost:8080)"
        echo "  INVENTORY_URL  - Inventory service URL (default: http://localhost:8082)"
        echo ""
        echo "Examples:"
        echo "  $0 traffic 600              - Run traffic for 10 minutes"
        echo "  $0 order ITEM-5 3           - Create order for ITEM-5 with quantity 3"
        echo "  $0 chaos-latency-on         - Enable latency chaos"
        echo "  $0 check                    - Verify services are running"
        exit 1
        ;;
esac
