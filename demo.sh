#!/bin/bash

# Demo script to simulate realistic traffic to the observability microservices

set -e

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
INVENTORY_URL="${INVENTORY_URL:-http://localhost:8082}"

echo "🚀 Starting Observability Demo Traffic Simulation"
echo "=================================================="
echo "Gateway URL: $GATEWAY_URL"
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to create orders
create_order() {
    local item_id=$1
    local quantity=$2
    
    response=$(curl -s -w "\n%{http_code}" -X POST "$GATEWAY_URL/api/orders" \
        -H "Content-Type: application/json" \
        -d "{\"itemId\":\"$item_id\",\"quantity\":$quantity}")
    
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n-1)
    
    if [ "$http_code" -eq 201 ] || [ "$http_code" -eq 200 ]; then
        echo -e "${GREEN}✓${NC} Created order for item $item_id (quantity: $quantity)"
    else
        echo -e "${RED}✗${NC} Failed to create order for item $item_id (HTTP $http_code)"
    fi
}

# Function to get orders
get_orders() {
    response=$(curl -s -w "\n%{http_code}" "$GATEWAY_URL/api/orders")
    http_code=$(echo "$response" | tail -n1)
    
    if [ "$http_code" -eq 200 ]; then
        echo -e "${GREEN}✓${NC} Retrieved orders list"
    else
        echo -e "${RED}✗${NC} Failed to get orders (HTTP $http_code)"
    fi
}

# Function to check inventory
check_inventory() {
    local item_id=$1
    
    response=$(curl -s -w "\n%{http_code}" "$GATEWAY_URL/api/inventory/$item_id")
    http_code=$(echo "$response" | tail -n1)
    
    if [ "$http_code" -eq 200 ]; then
        echo -e "${GREEN}✓${NC} Checked inventory for item $item_id"
    else
        echo -e "${RED}✗${NC} Failed to check inventory for item $item_id (HTTP $http_code)"
    fi
}

# Function to enable chaos latency
enable_chaos_latency() {
    echo -e "${YELLOW}⚠${NC}  Enabling chaos latency..."
    curl -s -X POST "$INVENTORY_URL/api/chaos/latency" \
        -H "Content-Type: application/json" \
        -d '{"enabled":true,"min":500,"max":2000}' > /dev/null
    echo -e "${YELLOW}⚠${NC}  Chaos latency enabled (500-2000ms)"
}

# Function to disable chaos latency
disable_chaos_latency() {
    echo -e "${GREEN}✓${NC} Disabling chaos latency..."
    curl -s -X POST "$INVENTORY_URL/api/chaos/latency" \
        -H "Content-Type: application/json" \
        -d '{"enabled":false}' > /dev/null
    echo -e "${GREEN}✓${NC} Chaos latency disabled"
}

# Function to enable chaos errors
enable_chaos_errors() {
    echo -e "${YELLOW}⚠${NC}  Enabling chaos errors..."
    curl -s -X POST "$INVENTORY_URL/api/chaos/errors" \
        -H "Content-Type: application/json" \
        -d '{"enabled":true,"rate":0.2}' > /dev/null
    echo -e "${YELLOW}⚠${NC}  Chaos errors enabled (20% error rate)"
}

# Function to disable chaos errors
disable_chaos_errors() {
    echo -e "${GREEN}✓${NC} Disabling chaos errors..."
    curl -s -X POST "$INVENTORY_URL/api/chaos/errors" \
        -H "Content-Type: application/json" \
        -d '{"enabled":false}' > /dev/null
    echo -e "${GREEN}✓${NC} Chaos errors disabled"
}

# Main simulation loop
simulate_traffic() {
    local duration=${1:-300}  # Default 5 minutes
    local start_time=$(date +%s)
    local end_time=$((start_time + duration))
    
    echo "Running traffic simulation for $duration seconds..."
    echo ""
    
    while [ $(date +%s) -lt $end_time ]; do
        # Normal traffic pattern
        for i in {1..5}; do
            item_id="ITEM-$(shuf -i 1-10 -n 1)"
            quantity=$(shuf -i 1-5 -n 1)
            create_order "$item_id" "$quantity"
            sleep 0.5
        done
        
        # Check some inventory
        for i in {1..3}; do
            item_id="ITEM-$(shuf -i 1-10 -n 1)"
            check_inventory "$item_id"
            sleep 0.3
        done
        
        # Get orders periodically
        get_orders
        sleep 1
        
        # Randomly introduce chaos every 30-60 seconds
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
        simulate_traffic $duration
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
    *)
        echo "Usage: $0 {traffic|chaos-latency-on|chaos-latency-off|chaos-errors-on|chaos-errors-off|order|inventory|orders} [args]"
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
        echo ""
        echo "Examples:"
        echo "  $0 traffic 600              - Run traffic for 10 minutes"
        echo "  $0 order ITEM-5 3           - Create order for ITEM-5 with quantity 3"
        echo "  $0 chaos-latency-on         - Enable latency chaos"
        exit 1
        ;;
esac
