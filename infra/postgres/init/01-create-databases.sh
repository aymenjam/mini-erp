#!/bin/bash
# Runs once, on the first start of an empty data volume.
# One database and one own user per service. A service user cannot connect
# to the database of another service.
set -euo pipefail

create_database() {
    local name="$1"
    local password="$2"
    # The password goes through a psql variable, so quotes in it cannot break the SQL.
    psql -v ON_ERROR_STOP=1 -v pw="$password" \
        --username "$POSTGRES_USER" --dbname postgres <<-SQL
        CREATE ROLE ${name} LOGIN PASSWORD :'pw';
        CREATE DATABASE ${name} OWNER ${name};
        REVOKE CONNECT ON DATABASE ${name} FROM PUBLIC;
SQL
    echo "Created database and user: ${name}"
}

create_database customers  "${CUSTOMERS_DB_PASSWORD:?}"
create_database inventory  "${INVENTORY_DB_PASSWORD:?}"
create_database sales      "${SALES_DB_PASSWORD:?}"
create_database purchasing "${PURCHASING_DB_PASSWORD:?}"
create_database invoicing  "${INVOICING_DB_PASSWORD:?}"
create_database accounting "${ACCOUNTING_DB_PASSWORD:?}"
create_database keycloak   "${KEYCLOAK_DB_PASSWORD:?}"
