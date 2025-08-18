#!/bin/sh
# init-symmetric-master.sh (Versión Final y Robusta)

# --- Configuración del entorno ONLINE ---
DB_HOST="postgres-db"
DB_PORT="5432"
DB_USER="admin"
DB_NAME="ecopila_db_online"
export PGPASSWORD="password"

echo "--> Esperando a PostgreSQL en ${DB_HOST}:${DB_PORT}..."

while ! psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c '\q' 2>/dev/null; do
  >&2 echo "Postgres no está disponible - esperando..."
  sleep 2
done

>&2 echo "--> PostgreSQL está listo."

# 1. Inicia SymmetricDS en segundo plano para que cree su esquema de tablas
echo "--> Iniciando SymmetricDS en segundo plano para la creación del esquema..."
/app/symmetric-ds-3.14.0/bin/sym --port 31415 --server &

# 2. Dale tiempo a SymmetricDS para que cree todas sus tablas
echo "--> Dando tiempo a SymmetricDS para que inicialice su esquema (esperando 20 segundos)..."
sleep 20

# 3. Ahora que las tablas existen, inserta tu configuración personalizada
echo "--> Insertando configuración personalizada desde insert_config.sql..."
# Ruta simplificada porque ahora el archivo está en /app/
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f /app/insert_config.sql
echo "--> Configuración personalizada insertada."

# 4. Trae el proceso de SymmetricDS al primer plano para mantener el contenedor vivo
echo "--> Configuración completa. Mostrando logs de SymmetricDS..."
wait