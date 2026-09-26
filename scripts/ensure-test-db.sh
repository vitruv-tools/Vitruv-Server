#!/usr/bin/env bash
# Creates the Maven test database if it does not already exist.
set -euo pipefail
CONTAINER="${1:-vitruvius-server-postgres}"
USER_NAME="${2:-postgres}"
DB="${3:-vitruvius-server-test}"

if docker exec "$CONTAINER" psql -U "$USER_NAME" -tAc "SELECT 1 FROM pg_database WHERE datname='$DB'" | grep -q 1; then
  echo "Database '$DB' already exists."
  exit 0
fi

docker exec "$CONTAINER" psql -U "$USER_NAME" -c "CREATE DATABASE \"$DB\";"
echo "Created database '$DB'."
