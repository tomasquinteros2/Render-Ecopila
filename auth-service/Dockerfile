# Etapa 1: Construcción con Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# 1. Copiar solo los POMs para descargar dependencias
COPY pom.xml .
COPY auth-service/pom.xml ./auth-service/

# 2. Descargar dependencias (Capa cacheable)
RUN mvn -B -f auth-service/pom.xml dependency:go-offline

# 3. Copiar el resto del código fuente
COPY . .

# 4. Compilar la aplicación
RUN mvn -B -f auth-service/pom.xml clean package -DskipTests

# Etapa 2: Imagen final de ejecución
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copia únicamente el JAR compilado
COPY --from=build /app/auth-service/target/auth-service-*.jar app.jar

EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]