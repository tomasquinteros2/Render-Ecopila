# Etapa 1: Construcción con Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# 1. Copiar solo los POMs para descargar dependencias
# Copia el POM padre
COPY pom.xml .
# Copia el POM del módulo específico
COPY microservicio_proveedor/pom.xml ./microservicio_proveedor/

# 2. Descargar dependencias. Docker cacheará esta capa si los POMs no cambian.
RUN mvn -B -f microservicio_proveedor/pom.xml dependency:go-offline

# 3. Copiar el resto del código fuente del proyecto
COPY . .

# 4. Compilar la aplicación. Esto será más rápido porque las dependencias ya están descargadas.
RUN mvn -B -f microservicio_proveedor/pom.xml clean package -DskipTests

# Etapa 2: Imagen final de ejecución (más ligera)
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copia únicamente el JAR compilado desde la etapa de construcción
COPY --from=build /app/microservicio_proveedor/target/microservicio_proveedor-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]