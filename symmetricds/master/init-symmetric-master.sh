#!/bin/sh
# init-symmetric-master.sh (Versión Corregida y Robusta)

# --- Configuración del entorno ONLINE ---
DB_HOST="postgres-db" # Nombre del servicio de la BD online
DB_PORT="5432"
DB_USER="admin"
DB_NAME="ecopila_db_online"
export PGPASSWORD="password" # psql usará esta variable

echo "--> Esperando a PostgreSQL en ${DB_HOST}:${DB_PORT}..."

# Bucle para esperar a que la base de datos esté lista
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
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f /app/symmetric-ds-3.14.0/conf/insert_config.sql
echo "--> Configuración personalizada insertada."

# 4. Trae el proceso de SymmetricDS al primer plano y muestra sus logs
# Esto mantiene el contenedor vivo y te permite ver los logs con 'docker logs -f'
echo "--> Configuración completa. Mostrando logs de SymmetricDS..."
tail -f /app/symmetric-ds-3.14.0/logs/symmetric.log