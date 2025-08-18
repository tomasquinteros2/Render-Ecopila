FROM openjdk:17-jdk-slim

# Instala dependencias
RUN apt-get update && apt-get install -y --no-install-recommends postgresql-client sed && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copia la aplicación SymmetricDS
COPY symmetricds/symmetric-ds-3.14.0/ .

# Copia el script de inicio y dale permisos
COPY symmetricds/master/init-symmetric-master.sh /app/init-symmetric-master.sh
RUN sed -i 's/\r$//' /app/init-symmetric-master.sh && \
    chmod +x /app/init-symmetric-master.sh

# El comando de inicio se define en docker-compose
    