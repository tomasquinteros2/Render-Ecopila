FROM openjdk:17-jdk-slim

# Instalar el cliente de PostgreSQL para que `pg_isready` esté disponible.
# Limpiamos la caché de apt para mantener la imagen ligera.
RUN apt-get update && apt-get install -y --no-install-recommends postgresql-client && \
    rm -rf /var/lib/apt/lists/*

# Mantenemos el mismo WORKDIR para no romper las rutas de los volúmenes y scripts.
WORKDIR /app/symmetric-ds-3.14.0

# El CMD es sobrescrito por docker-compose, pero lo dejamos por claridad.
CMD ["java", "-Xmx512m", "-Dsym.home=.", "-cp", "lib/*:web/WEB-INF/lib/*", "org.jumpmind.symmetric.SymmetricLauncher", "--engine", "master"]