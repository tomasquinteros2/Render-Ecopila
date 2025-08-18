#!/bin/sh
# init-symmetric-master.sh (Versión Definitiva y Robusta)

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

# --- Proceso de Doble Arranque para evitar Race Conditions ---

# 1. Inicia una instancia TEMPORAL de SymmetricDS en segundo plano.
#    Su único propósito es crear el esquema de tablas en la base de datos.
echo "--> Iniciando instancia temporal de SymmetricDS para crear el esquema..."
/app/symmetric-ds-3.14.0/bin/sym --port 31415 --server &
# Guarda el Process ID (PID) del proceso en segundo plano
SYMMETRIC_PID=$!

# 2. Espera a que el esquema se cree. Aumentamos el tiempo para más seguridad.
echo "--> Dando tiempo a SymmetricDS para que inicialice su esquema (esperando 25 segundos)..."
sleep 25

# 3. Detén la instancia TEMPORAL. Ya cumplió su misión.
echo "--> Deteniendo la instancia temporal de SymmetricDS (PID: $SYMMETRIC_PID)..."
kill $SYMMETRIC_PID
sleep 5 # Dale un momento para que libere los recursos

# 4. Ahora que las tablas existen y el servidor está detenido, inserta tu configuración.
echo "--> Insertando configuración personalizada en la base de datos..."
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f /app/insert_config.sql
echo "--> Configuración personalizada insertada con éxito."

# 5. Inicia la instancia FINAL y PERMANENTE de SymmetricDS en primer plano.
#    Esta instancia leerá la base de datos que ya tiene el esquema Y tu configuración.
echo "--> Iniciando instancia final de SymmetricDS..."
exec /app/symmetric-ds-3.14.0/bin/sym --port 31415 --server