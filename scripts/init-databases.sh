#!/bin/bash
# CockroachDB Database Initialization Script
# This script creates the required databases (orders and inventory) for the microservices.
# It is designed to run as a sidecar container in Docker Compose.

set -euo pipefail

# Configuration
readonly COCKROACH_HOST="${COCKROACH_HOST:-cockroachdb:26257}"
readonly MAX_RETRIES=30
readonly RETRY_INTERVAL=2

# Logging functions
log_info() {
    echo "[INFO] $*"
}

log_error() {
    echo "[ERROR] $*" >&2
}

# Main banner
echo "========================================"
echo "CockroachDB Database Initialization Script"
echo "========================================"
log_info "Target host: ${COCKROACH_HOST}"

# Wait for CockroachDB to be ready
log_info "Waiting for CockroachDB to be ready..."
retries=0
until /cockroach/cockroach sql --insecure --host="${COCKROACH_HOST}" -e "SELECT 1" > /dev/null 2>&1; do
    retries=$((retries + 1))
    if [ "${retries}" -ge "${MAX_RETRIES}" ]; then
        log_error "CockroachDB did not become ready within ${MAX_RETRIES} retries. Exiting."
        exit 1
    fi
    log_info "CockroachDB is not ready yet, waiting... (attempt ${retries}/${MAX_RETRIES})"
    sleep "${RETRY_INTERVAL}"
done
log_info "CockroachDB is ready!"

# Create the orders database
log_info "Creating 'orders' database..."
if /cockroach/cockroach sql --insecure --host="${COCKROACH_HOST}" -e "CREATE DATABASE IF NOT EXISTS orders;"; then
    log_info "Database 'orders' created successfully!"
else
    log_error "Failed to create 'orders' database"
    exit 1
fi

# Create the inventory database
log_info "Creating 'inventory' database..."
if /cockroach/cockroach sql --insecure --host="${COCKROACH_HOST}" -e "CREATE DATABASE IF NOT EXISTS inventory;"; then
    log_info "Database 'inventory' created successfully!"
else
    log_error "Failed to create 'inventory' database"
    exit 1
fi

# Verify databases exist
log_info "Verifying databases..."
/cockroach/cockroach sql --insecure --host="${COCKROACH_HOST}" -e "SHOW DATABASES;"

echo "========================================"
log_info "Database initialization complete!"
echo "========================================"
