#!/bin/sh
    # init-symmetric-master.sh

    # --- Configuración ---
    DB_HOST="postgres-db"
    DB_PORT="5432"
    DB_USER="admin"
    DB_NAME="ecopila_db_online"
    export PGPASSWORD="password"

    echo "--> Waiting for PostgreSQL at ${DB_HOST}:${DB_PORT}..."

    # Bucle para esperar a que la base de datos esté lista
    while ! psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c '\q' 2>/dev/null; do
      >&2 echo "Postgres is unavailable - sleeping"
      sleep 2
    done

    >&2 echo "--> PostgreSQL is up - proceeding."

    # Insertamos la configuración inicial de SymmetricDS
    echo "--> Inserting SymmetricDS configuration from insert_config.sql..."
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f /app/conf/insert_config.sql

    # Iniciamos el servidor de SymmetricDS
    echo "--> Starting SymmetricDS master server..."
    exec /app/bin/sym --port 31415 --server