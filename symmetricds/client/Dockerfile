# symmetricds/client/Dockerfile (Versión Corregida)
FROM openjdk:17-jdk-slim

# Establecemos el directorio de trabajo principal.
WORKDIR /app

# Copiamos la distribución de SymmetricDS.
COPY symmetricds/symmetric-ds-3.14.0/ ./

# Copiamos el fichero de configuración del cliente.
COPY symmetricds/client/engines/client.properties ./engines/

# ✅ SOLUCIÓN: Establecemos la variable de entorno JAVA_OPTS.
# El script 'sym' usará esta variable para pasar opciones a la JVM.
# -Dh2.baseDir=/app le dice a la base de datos H2 que el directorio /app
# es un directorio base seguro para crear archivos de base de datos.
ENV JAVA_OPTS="-Dh2.baseDir=/app"

# Damos permisos de ejecución al script.
RUN chmod +x bin/sym

# Exponemos el puerto.
EXPOSE 8081

# Comando de inicio (sin cambios).
CMD ["./bin/sym", "--port", "8081", "--server"]