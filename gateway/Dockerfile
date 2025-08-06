# Etapa 1: Construcción con Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# --- LA PARTE CLAVE ESTÁ AQUÍ ---
# 1. Copia el pom.xml PADRE. Maven ahora sabe que esto es un multi-módulo.
COPY pom.xml .

# 2. Copia el pom.xml del MÓDULO específico.
COPY gateway/pom.xml ./gateway/

# 3. Descarga dependencias. Esta capa se cachea y solo se ejecuta si los POMs cambian.
#    Usa el POM del módulo para que solo descargue lo que 'gateway' necesita.
RUN mvn -B -f gateway/pom.xml dependency:go-offline

# 4. AHORA SÍ, copia solo el código fuente del módulo que estás construyendo.
COPY gateway/src ./gateway/src
# --- FIN DE LA PARTE CLAVE ---

# 4. Compilar la aplicación
RUN mvn -B -f gateway/pom.xml clean package -DskipTests

# Etapa 2: Imagen final de ejecución
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copia únicamente el JAR compilado
COPY --from=build /app/gateway/target/gateway-*.jar app.jar

EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]