# 🧠 TriviaGenerator AI — Plataforma de Trivias con Inteligencia Artificial

Backend REST de alto rendimiento construido en **Spring Boot 3.3.0** y **Java 21 LTS** para la generación automatizada, deduplicación multicapa, almacenamiento permanente y renderizado gráfico de preguntas de trivia mediante **Google Gemini 3.5 Flash-Lite**.

---

## 🚀 Características Principales

* **Generación Tipada con IA**: Integración nativa con Google Gemini (`gemini-3.5-flash-lite`) mediante *Structured Outputs* (JSON Schema estricto).
* **Memoria Histórica**: Inyección de preguntas existentes en los prompts para evitar repeticiones desde el origen.
* **Deduplicación Híbrida Cuádruple**:
  1. *Normalización Canónica Unicode (NFC)* preservando diacríticos en español (`á, é, í, ó, ú, ñ`).
  2. *Hash Criptográfico SHA-256 (64 hex)* con índice único en base de datos.
  3. *Filtrado Intra-Lote* para descartar repeticiones internas.
  4. *Deduplicación Semántica por Similitud Coseno* con embeddings vectoriales (umbral `0.90`).
* **Renderizado Gráfico Headless (PNG 1080x1080)**: Dos imágenes generadas por trivia con **Java AWT / Graphics2D**:
  * `question.png`: Tarjeta con la pregunta, categoría y opciones de respuesta.
  * `answer.png`: Tarjeta con la solución correcta resaltada y explicación pedagógica.
* **Generación Automatizada de Videos con TTS, SFX e Introducciones Dinámicas**:
  * Compilación de videos en formato vertical 9:16 (Shorts/TikTok/Reels, 1080x1920), cuadrado 1:1 (Feed, 1080x1080) y horizontal 16:9 (YouTube, 1920x1080) mediante **FFmpeg**.
  * **Introducción Dinámica de Video**: Escena de apertura antes de la primera pregunta para captar atención y generar contexto sobre el tema/subtema en redes sociales.
    * **Opción A (Catálogo)**: Plantillas reutilizables sustituyendo `{tema}` dinámicamente.
    * **Opción B (Personalizada)**: Texto libre del usuario sanitizado automáticamente.
    * **Opción C (IA)**: Frases generadas dinámicamente con Google Gemini, naturales y de alto impacto.
  * **Títulos Personalizados y Sanitización para Redes**: Soporte de títulos atractivos de alto CTR (`customTitle`), sanitización para archivos Windows y límite de 85 caracteres para respetar los 100 caracteres de YouTube Shorts con numeración secuencial limpia (`#1`, `#2`...).
  * Narración por voz de IA con **Microsoft Edge-TTS** (voces neuronales en español como `es-MX-JorgeNeural`, `es-MX-DaliaNeural`, etc.).
  * Efectos de sonido integrados (`whoosh.wav`, `tick.wav` sincronizado por segundo, `correct.wav`, `bgm_loop.wav` ambiental) con pista de audio continua libre de truncamiento.
  * Duración adaptativa basada en la fonética de la voz o temporizado calibrado para lectura humana.
* **Pipeline de Auto-Publicación en Redes Sociales (Make.com + Google Drive)**:
  * **Sincronizador Vigilante (`sync-to-drive.ps1`)**: Detecta nuevos videos en tiempo real y los replica a Google Drive conservando la cola de publicación.
  * **Distribución Triple Automática**:
    * **YouTube Shorts**: Subida directa con título dinámico y `#Shorts`.
    * **Facebook Reels**: Subida directa mediante API de Páginas de Facebook.
    * **TikTok**: Entrega instantánea a celular vía Telegram Bot con texto y video listo para publicar con 1 toque.
* **Generación Masiva por Lotes (`generate_60_videos.py`)**: Script desatendido para generar lotes de 60+ videos temáticos (Historia, Ciencia, Espacio, etc.) con tolerancia a fallos y persistencia de estado.
* **Pipelines Síncrono y Asíncrono**:
  * `POST /api/v1/trivias` → HTTP 201 Created.
  * `POST /api/v1/trivias/async` → HTTP 202 Accepted + Sondeo de estado en `/generations/{id}/status`.
* **Seguridad Integrada**: Autenticación por `X-API-KEY` y limitación de tasa por IP (*Rate Limiting* en ventana deslizante).
* **Interfaz Web Local (SPA)**: Dashboard interactivo embebido en el servidor (`/` o `test-client.html`) para generar trivias, jugar en modo Quiz, exportar en CSV/JSON, configurar introducciones con previsualización en vivo y compilar videos.
* **Despliegue Contenedorizado**: Empaquetado con **Podman Compose** y PostgreSQL 16 sobre Alpine Linux con fuentes DejaVu.

---

## 📚 Documentación del Proyecto

El repositorio incluye guías técnicas completas y detalladas:

| Documento | Descripción |
|---|---|
| 🎬 [**Guía de Generación de Videos con TTS**](docs/GUIA_GENERACION_VIDEOS_TTS.md) | Pipeline de video FFmpeg, narración Edge-TTS, introducciones dinámicas, formatos y CLI. |
| 📖 [**Documentación del Código y Arquitectura**](docs/DOCUMENTACION_CODIGO.md) | Explicación profunda de cada paquete, clase, diseño, flujo de datos y base de datos. |
| 🧪 [**Guía de Pruebas en Local**](docs/LOCAL_TESTING_GUIDE.md) | Instrucciones paso a paso para levantar Podman, usar Swagger, `curl`, scripts y la Web. |
| 🌐 [**Guía de Despliegue en VPS**](docs/DEPLOYMENT_GUIDE.md) | Configuración de servidor Linux, Podman Compose y exposición con Cloudflare Tunnel. |
| 📋 [**Estado del Proyecto (Sesiones)**](docs/AI_SESSION_STATUS.md) | Bitácora de fases implementadas, decisiones técnicas y suite de 123 pruebas. |

---

## ⚡ Inicio Rápido (Local en 3 Pasos)

### 1. Configurar variables de entorno
Copia la plantilla y configura tu clave de Gemini:
```powershell
Copy-Item .env.example .env
```
Edita `.env` y coloca tu API Key de Google AI Studio:
```properties
AI_API_KEY=tu_api_key_de_gemini
AI_MODEL=gemini-3.5-flash-lite
```

### 2. Levantar la plataforma con Podman Compose
```powershell
podman compose up -d
```

### 3. Abrir la interfaz web
- **En local:** 👉 **[http://localhost:8080/](http://localhost:8080/)** *(o Swagger UI en **[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)**)*
- **Desde Internet (Gratuito con Cloudflare Tunnel):**
  Consulta tu URL pública HTTPS generada automáticamente ejecutando:
  ```powershell
  .\get-public-url.ps1    # En Windows PowerShell
  ./get-public-url.sh     # En Linux / WSL
  ```
  *(Permite acceder desde cualquier dispositivo móvil o computadora en el mundo de forma 100% gratuita y sin abrir puertos).*

---

## 🛠️ Tecnologías Utilizadas

* **Lenguaje**: Java 21 (Oracle LTS)
* **Framework**: Spring Boot 3.3.0 (Spring Data JPA, Spring Validation, Spring MVC, Actuator)
* **Inteligencia Artificial**: Google Gemini API (`gemini-3.5-flash-lite` y `text-embedding-004`)
* **Procesamiento y Renderizado de Video**: FFmpeg 8.x (Filtros dinámicos, desenfoque de fondo, superposición de texto y mezcla de audio con canal de silencio)
* **Síntesis de Voz (TTS)**: Microsoft Edge-TTS (Voces neuronales naturales multilenguaje)
* **Base de Datos**: PostgreSQL 16 con migraciones Flyway (V1 a V6)
* **Gráficos**: Java AWT Graphics2D (Renderizado PNG 1080x1080)
* **Contenedores**: Podman 6.0.2 / Podman Compose / Alpine Temurin 21 JRE
* **Documentación API**: OpenAPI 3 / Swagger UI 2.6.0
* **Testing**: JUnit 5, Mockito, AssertJ, Spring MockMvc (123 tests automatizados)

---

## 📡 Resumen de Endpoints de la API

```
POST   /api/v1/trivias                 # Generar trivias con IA (Síncrono)
POST   /api/v1/trivias/async           # Generar trivias con IA (Asíncrono - HTTP 202)
GET    /api/v1/trivias/generations/{id}/status # Estado en vivo de generación asíncrona
GET    /api/v1/trivias/{id}            # Consultar trivia por UUID
GET    /api/v1/trivias/random          # Obtener trivia activa aleatoria
GET    /api/v1/trivias/search?q=...    # Búsqueda por palabra clave
GET    /api/v1/trivias                 # Listado paginado de todas las trivias
GET    /api/v1/trivias/stats           # Métricas globales de la plataforma
POST   /api/v1/trivias/rerender        # Re-renderizado masivo de tarjetas PNG limpias
GET    /api/v1/catalogos/tipos-trivia  # Catálogo oficial de categorías
POST   /api/v1/videos/generate         # Generar video MP4 de trivias con TTS, SFX e intro
GET    /api/v1/videos/{id}/download    # Descargar video MP4 generado
GET    /api/v1/videos/intro-templates  # Catálogo de plantillas de introducción
POST   /api/v1/videos/preview-intro    # Previsualizar / generar texto de introducción (Plantilla, Personalizada o IA)
GET    /assets/**                      # Servidor de imágenes PNG generadas
GET    /api/v1/health                  # Healthcheck del servidor
GET    /                               # Interfaz Web SPA interactiva
```

---

## 🧪 Ejecución de Pruebas

Para ejecutar la suite completa de 123 pruebas unitarias y de integración de forma local:

```powershell
mvn test
```
