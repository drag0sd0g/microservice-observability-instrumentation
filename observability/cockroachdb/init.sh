#!/bin/bash
# Initialize databases for the microservices

set -e

echo "Waiting for CockroachDB to be ready..."
until /cockroach/cockroach sql --insecure -e "SELECT 1" > /dev/null 2>&1; do
  echo "CockroachDB is unavailable - sleeping"
  sleep 2
done

echo "Creating databases..."
/cockroach/cockroach sql --insecure <<-EOSQL
    CREATE DATABASE IF NOT EXISTS orders;
    CREATE DATABASE IF NOT EXISTS inventory;
EOSQL

echo "Databases created successfully!"
