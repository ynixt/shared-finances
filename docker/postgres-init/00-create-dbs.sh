#!/bin/bash
set -e

echo ">>> creating databases '${POSTGRES_AUTH_DB}' and '${POSTGRES_APP_DB}'…"

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
  CREATE ROLE ${POSTGRES_AUTH_USER} LOGIN PASSWORD '${POSTGRES_AUTH_PASSWORD}';
  CREATE ROLE ${POSTGRES_APP_USER}  LOGIN PASSWORD '${POSTGRES_APP_PASSWORD}';

  CREATE DATABASE ${POSTGRES_AUTH_DB} OWNER ${POSTGRES_AUTH_USER};
  CREATE DATABASE ${POSTGRES_APP_DB}  OWNER ${POSTGRES_APP_USER};

  CREATE USER ${POSTGRES_PGHERO_USER} WITH PASSWORD '${POSTGRES_PGHERO_PASSWORD}';
  GRANT CONNECT ON DATABASE ${POSTGRES_AUTH_DB} TO pghero;
  GRANT CONNECT ON DATABASE ${POSTGRES_APP_DB} TO pghero;
  GRANT pg_monitor TO pghero;
EOSQL

for DB in "$POSTGRES_AUTH_DB" "$POSTGRES_APP_DB"; do
  if [ -n "$DB" ]; then
    echo ">>> enabling pg_stat_statements on '$DB'…"
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$DB" <<-EOSQL
      CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
EOSQL
  fi
done

echo ">>> created roles, databases and enabled pg_stat_statements!"
