#!/bin/bash
set -e

echo "========================================"
echo "CockroachDB Database Initialization Script"
echo "========================================"

# Wait for CockroachDB to be ready
echo "Waiting for CockroachDB to be ready..."
until /cockroach/cockroach sql --insecure --host=cockroachdb:26257 -e "SELECT 1" > /dev/null 2>&1; do
    echo "CockroachDB is not ready yet, waiting..."
    sleep 2
done
echo "CockroachDB is ready!"

# Create the orders database
echo "Creating 'orders' database..."
/cockroach/cockroach sql --insecure --host=cockroachdb:26257 -e "CREATE DATABASE IF NOT EXISTS orders;"
echo "Database 'orders' created successfully!"

# Create the inventory database
echo "Creating 'inventory' database..."
/cockroach/cockroach sql --insecure --host=cockroachdb:26257 -e "CREATE DATABASE IF NOT EXISTS inventory;"
echo "Database 'inventory' created successfully!"

# Verify databases exist
echo "Verifying databases..."
/cockroach/cockroach sql --insecure --host=cockroachdb:26257 -e "SHOW DATABASES;"

echo "========================================"
echo "Database initialization complete!"
echo "========================================"
