# Ecopila Stock - Sistema de Gestión de Stock Offline-First

Este proyecto es un sistema de gestión de stock robusto y escalable, construido sobre una **arquitectura de microservicios** con un enfoque **"Offline-First"**. La aplicación permite a los nodos cliente operar de forma autónoma sin conexión a internet, sincronizando los datos de manera transparente con un servidor central una vez que se restablece la conectividad.

Este repositorio está preparado para ser un escaparate de habilidades en desarrollo backend, DevOps y diseño de arquitecturas resilientes.

## Características Principales

*   **Arquitectura de Microservicios:** Construido con **Spring Boot** y **Spring Cloud**, promoviendo la escalabilidad y el mantenimiento modular.
*   **Capacidad Offline-First:** Utiliza **SymmetricDS** para una sincronización de datos bidireccional entre la base de datos central (online) y las bases de datos locales (offline).
*   **API Gateway Centralizado:** Un único punto de entrada (`Spring Cloud Gateway`) para enrutamiento, seguridad, balanceo de carga y resiliencia (Circuit Breaker).
*   **Seguridad Robusta:** Autenticación y autorización basadas en **JSON Web Tokens (JWT)**, implementado de forma reactiva en el Gateway.
*   **Descubrimiento de Servicios:** **Netflix Eureka** para el registro y descubrimiento dinámico de microservicios en el entorno online.
*   **Containerización Completa:** Todo el stack (aplicaciones, bases de datos, servicios) está containerizado con **Docker** y orquestado con **Docker Compose** para un despliegue y desarrollo consistentes.
*   **Configuración Segura:** Gestión de secretos y variables de entorno siguiendo las mejores prácticas, sin información sensible hardcodeada en el código.

## 🏗️ Arquitectura

El sistema está diseñado para operar en dos modos distintos, cada uno con su propio stack de Docker Compose:

1.  **Stack Online (Maestro):** Representa el servidor central en la nube. Contiene los microservicios, la base de datos PostgreSQL principal y el nodo maestro de SymmetricDS, responsable de coordinar la sincronización.
2.  **Stack Offline (Cliente):** Representa una instancia local que puede operar de forma independiente. Contiene los microservicios, una base de datos PostgreSQL local y un nodo cliente de SymmetricDS que se registra y sincroniza con el maestro.

## 🛠️ Stack Tecnológico

*   **Backend:** Java 17, Spring Boot 3, Spring Cloud
*   **Base de Datos:** PostgreSQL
*   **Sincronización:** SymmetricDS
*   **Contenedores:** Docker, Docker Compose
*   **Seguridad:** Spring Security, JWT
*   **Build Tool:** Maven

## 🚀 Cómo Empezar

Sigue estos pasos para configurar y ejecutar el proyecto en tu entorno local.

### Pre-requisitos

Asegúrate de tener instalado el siguiente software:
*   Git
*   Docker y Docker Compose
*   Java 17 (JDK)
*   Apache Maven

### 1. Configuración del Entorno

Primero, clona el repositorio y navega al directorio del proyecto.

*  1-El proyecto utiliza un archivo .env para gestionar las variables de entorno y los secretos. Este archivo no debe ser subido a Git.
*  2-Abre el nuevo archivo .env y personaliza las variables. Como mínimo, deberías generar un nuevo JWT_SECRET y revisar las contraseñas para un entorno de producción.

### 2. Construcción de las Imágenes Docker

El proyecto incluye un script para construir las imágenes Docker de todos los microservicios.

### 3. Ejecución de la Aplicación

Puedes iniciar el sistema en modo `offline` o `online` usando los scripts proporcionados.

  (offline)
* docker-compose up --build
  (online)
* docker-compose -f docker-compose.online.yml up --build

Estos comando iterará sobre cada microservicio, lo empaquetará con Maven y construirá su imagen Docker correspondiente.

#### Modo Offline (Cliente)
Este es el modo estándar para el desarrollo y operación en un punto de venta.

#### Modo Online (Maestro)
Este modo simula el servidor central.
