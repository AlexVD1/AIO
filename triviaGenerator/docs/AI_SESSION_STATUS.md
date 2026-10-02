# AI Development Session Status

## Última actualización
2026-10-02T02:55 CDT (Fase 24: Pipeline de Distribución Multicanal, Títulos Dinámicos y Generación de 60 Videos)

## Objetivo actual
Implementación integral de la plataforma de generación y consulta de trivias con Inteligencia Artificial, deduplicación en 5 capas, renderizado AWT Graphics2D, almacenamiento de assets, API REST asíncrona/síncrona, compilación automatizada de videos en 9:16 (Shorts), 1:1 (Feed) y 16:9 (YouTube) con locución TTS (Microsoft Edge-TTS), efectos de sonido (FFmpeg), introducciones dinámicas, títulos personalizados, exportación automática a Google Drive y distribución a redes sociales (YouTube Shorts, Facebook Reels, TikTok) vía Make.com.

## Estado general
**100% de las fases de desarrollo (Fase 1 a 24) implementadas y validadas.**
- Total de pruebas automatizadas: **123 tests ejecutados y pasando (0 failures, 0 errors)**.
- Compilación y empaquetado: **BUILD SUCCESS** (`mvn test` 100% verde).
- Esquema de base de datos verificado con PostgreSQL 16 y categorías iniciales cargadas.
- Lote masivo de **60 videos (300 trivias con Edge-TTS)** compilado y sincronizado a Google Drive.
- Pipeline end-to-end con soporte síncrono (HTTP 201), asíncrono (HTTP 202) y generación de video MP4 en 9:16, 1:1 y 16:9 con narración neuronal, intro y títulos dinámicos.

---

## Completado

### Fase 1 — Análisis y Diseño ✅
- Entorno validado (Java 21 Oracle LTS, Maven 3.9.11, Podman 6.0.2 en Windows 11).
- Decisiones técnicas acordadas (Google Gemini 3.5 Flash Lite, Java AWT/Graphics2D, paquete `com.trivia.api`).
- Documento `implementation_plan.md` registrado en artefactos.

### Fase 2 — Estructura Base ✅
- `pom.xml`: Spring Boot 3.3.0, Spring Data JPA, Flyway, Validation, Actuator, OpenAPI/Swagger UI.
- `TriviaApiApplication.java` con `@SpringBootApplication @EnableAsync`.
- `HealthController.java`: `GET /api/v1/health` con DTO `HealthResponse`.

### Fase 3 — Base de Datos y Persistencia ✅
- Enums: `EstadoGeneracion`, `EstadoTrivia`, `Dificultad`, `TipoAsset`.
- Entidades JPA: `TipoTrivia`, `TriviaGeneration`, `Trivia`, `TriviaOpcion`, `TriviaAsset`.
- Repositorios Spring Data JPA con consultas JPQL, nativas y paginación.
- Migraciones Flyway V1 a V6 aplicadas y verificadas en contenedor PostgreSQL `triviadb`.

### Fase 4 — Catálogo de Tipos de Trivia ✅
- DTO: `TipoTriviaResponse`.
- `CatalogService`: lectura de tipos activos y validación por código.
- `CatalogController`: `GET /api/v1/catalogos/tipos-trivia` documentado con Swagger.
- `AbstractControllerTest`: clase base abstracta de integración con mocks JPA centralizados.

### Fase 5 — Normalización y Hash SHA-256 ✅
- `TriviaQuestionNormalizer`: normalización Unicode NFC, minúsculas, eliminación de signos de puntuación y exclamación/interrogación, colapso de espacios múltiples y preservación estricta de acentos en español.
- `QuestionHashService`: cálculo determinista de hash SHA-256 en hexadecimal (64 caracteres) sobre texto normalizado.

### Fase 6 — Deduplicación en Capas ✅
- `DeduplicationResult<T>`: DTO con trazabilidad detallada (válidos, descartados por lote, descartados por BD).
- `TriviaDuplicateService`: deduplicación intra-lote y contra el histórico en PostgreSQL (`pregunta_hash` UNIQUE).

### Fase 7 — Integración con Google Gemini IA ✅
- `TriviaAiClient`: interfaz desacoplada para proveedores de IA.
- `GeminiTriviaAiClient`: cliente HTTP nativo (Java 21 `HttpClient`) con Structured Outputs (JSON Schema nativo), construcción de prompts con memoria del sistema (preguntas existentes en BD para evitar repeticiones) y manejo de errores.

### Fase 8 — Pipeline de Generación con Buffer y Reintentos ✅
- `TriviaValidationService`: validación estructural (número de opciones, letras consecutivas A-D, exactamente 1 correcta, explicaciones verídicas).
- `TriviaGenerationService`: orquestador con cálculo de buffer (`trivia.generation.buffer-percentage: 50`), bucle de regeneración progresiva hasta `MAX_GENERATION_ATTEMPTS: 5`, persistencia transaccional y actualización de estados.
- `InsufficientUniqueTriviasException`: excepción de negocio cuando se agotan los reintentos.

### Fase 9 — Endpoints de Consulta ✅
- DTOs: `TriviaResponse`, `TriviaOpcionResponse`, `TriviaAssetResponse`, `StatsResponse`.
- `TriviaQueryService`: consultas de solo lectura (`obtenerPorId`, `obtenerAleatoria`, `buscarPorTexto`, `listar`, `obtenerEstadisticas`).
- `TriviaController`:
  - `POST /api/v1/trivias` (generación síncrona → HTTP 201)
  - `GET /api/v1/trivias/{id}` (detalle completo)
  - `GET /api/v1/trivias/random` (trivia aleatoria activa)
  - `GET /api/v1/trivias/search` (búsqueda por texto con paginación)
  - `GET /api/v1/trivias` (listado paginado)
  - `GET /api/v1/trivias/stats` (métricas consolidadas)
- `GlobalExceptionHandler`: estandarización RFC 7807 ProblemDetails.

### Fase 10 — Embeddings y Similitud Semántica ✅
- `EmbeddingService` / `EmbeddingServiceImpl`: vectorización mediante Gemini `text-embedding-004` (con fallback determinista local para entornos offline), cálculo de similitud coseno L2 y serialización/deserialización a la columna `embedding` (TEXT).

### Fase 11 — Renderizado Gráfico (AWT/Graphics2D) ✅
- `TriviaRendererService`: generación de dos imágenes PNG de 1080x1080 por cada trivia:
  - Imagen de **PREGUNTA**: tarjeta visual moderna con badges de tema y dificultad, pregunta con ajuste de líneas y opciones A-D sin respuesta marcada.
  - Imagen de **RESPUESTA**: misma composición destacando en verde esmeralda la opción correcta y tarjeta de explicación educativa al pie.

### Fase 12 — Almacenamiento de Assets ✅
- `AssetStorageService`: interfaz de almacenamiento.
- `LocalAssetStorageService`: almacenamiento persistente en disco (`/data/storage` o `./storage`) y generación de URLs públicas.
- `WebMvcConfig`: enrutamiento de archivos estáticos en `/assets/**`.

### Fase 13 — Integración Completa del Pipeline ✅
- `TriviaGenerationService` conectado con `TriviaRendererService`, `AssetStorageService` y `EmbeddingService`. Generación, guardado y renderizado automático de los dos PNGs por cada trivia generada.

### Fase 14 — Asincronía y Polling de Estado ✅
- `AsyncTriviaPipelineExecutor`: ejecución en hilos de fondo mediante `@Async`.
- `POST /api/v1/trivias/async` → HTTP 202 Accepted con `generationId`.
- `GET /api/v1/trivias/generations/{id}/status` → HTTP 200 con estado en vivo (`PENDIENTE`, `GENERANDO`, `VALIDANDO`, `BUSCANDO_DUPLICADOS`, `REGENERANDO`, `GUARDANDO`, `RENDERIZANDO`, `COMPLETADA`, `ERROR`).

### Fase 15 — Pruebas y Cobertura Integral ✅
- Suite completa con **89 tests automatizados** cubriendo controladores, servicios, clientes IA, normalizadores, hashers, renderers, almacenamiento, similitud difusa y auto-creación de categorías.

### Fase 16 — Contenedores y Podman ✅
- `Containerfile`: imagen basada en `eclipse-temurin:21-jre-alpine` con `fontconfig` y `ttf-dejavu` para renderizado AWT headless, usuario no-root `trivia`, variables de entorno y healthcheck.
- `compose.yaml`: stack completo con servicios `postgres` y `api`, volúmenes persistentes y red interna.
- `.env.example`: plantilla completa de variables de entorno documentada.

### Fase 17 — Seguridad ✅
- `ApiKeyFilter`: autenticación mediante cabecera `X-API-KEY` para operaciones de escritura, con exención de endpoints públicos (GET, health, docs, assets) y modo bypass para desarrollo.
- `RateLimitingFilter`: limitación de tasa por IP en ventana deslizante con HTTP 429 y cabecera `Retry-After`.
- `WebCorsConfig`: configuración global de CORS para clientes web y móviles.

### Fase 18 — Guía de Despliegue en VPS + Cloudflare Tunnel ✅
- `docs/DEPLOYMENT_GUIDE.md`: manual completo para despliegue en VPS con Podman y exposición segura a Internet mediante Cloudflare Tunnel sin abrir puertos.

### Fase 19 — Generación Automatizada de Videos y Narración TTS ✅
- Pipeline completo de compilación de video en Java 21 y FFmpeg (`com.trivia.api.service.video.*`):
  - Modelado de cronograma adaptable (`TimelinePlan`, `TriviaSceneTiming`, `VideoFormat`).
  - Formato vertical 9:16 (1080x1920 con fondo desenfocado `boxblur`, título y contador animado) y cuadrado 1:1 (1080x1080).
  - Integración con **Microsoft Edge-TTS** mediante el puente Python `scripts/tts_bridge.py` (`EdgeTtsNarrationService`) con duraciones fonéticas exactas.
  - Temporizado calibrado de ritmo humano (`NoOpNarrationService`: 7.0s pregunta + 5.0s cuenta regresiva + 6.5s respuesta = 18.5s/trivia).
  - Sincronización de audio sin truncamiento: inyección de pista de silencio (`aevalsrc=0`) para garantizar alineación de paquetes en Windows Media Player y reproductores móviles.
  - Efectos de sonido empotrados (`whoosh.wav`, `tick.wav`, `correct.wav`, `bgm_loop.wav`) extraídos desde el JAR por `VideoAssetExtractor`.
  - Endpoints REST: `POST /api/v1/videos/generate` y `GET /api/v1/videos/{id}/download`.
  - Herramienta CLI independiente en Python: `scripts/generate_full_trivia_video.py`.

### Fase 20 — Limpieza de Identidad Visual en Tarjetas AWT ✅
- Actualización de `TriviaRendererService`:
  - Eliminación de la leyenda fija `"Plataforma de Trivias con IA"` en el pie de página.
  - Si la trivia tiene subtema se dibuja `"Subtema: <subtema>"`.
  - Si el subtema es nulo o está en blanco, el pie de página permanece limpio sin texto.

### Interfaz Web Local de Pruebas (Studio & Quiz) ✅
- `src/main/resources/static/index.html` y `test-client.html`: SPA completa y auto-contenida con diseño moderno en modo oscuro para probar la API visualmente.
- Soporta generación síncrona y asíncrona con polling en vivo, visualizador de imágenes PNG 1080x1080 (pregunta y respuesta explicada), modo Quiz interactivo para jugar, explorador/búsqueda de trivias y panel de métricas del catálogo.

### Catálogo Dinámico con Detección de Similitud Difusa ✅
- `CategorySimilarityMatcher` y `CatalogService`: Auto-registro de nuevas categorías en PostgreSQL cuando el usuario ingresa un tema no existente (ej: `CINE`, `FILOSOFIA`).
- Algoritmo de similitud difusa (fuzzy match): detecta acentos, plurales/singulares (`Historias` -> `HISTORIA`), typos Levenshtein y coincidencias de tokens, evitando duplicados accidentales pero diferenciando raíces genuinas (`GASTRONOMIA` vs `ASTRONOMIA`).

### Fase 21 — Mantenimiento y Re-renderizado Limpio de Imágenes ✅
- `TriviaMaintenanceService`: servicio transaccional para regenerar en lote las tarjetas PNG (1080x1080) en disco sin alterar IDs ni referencias de BD.
- Endpoint `POST /api/v1/trivias/rerender`: soporta re-renderizado total (cuerpo vacío o null) o lista específica de UUIDs.
- Controles web en la SPA:
  * Botón masivo "🔄 Re-renderizar Tarjetas" en la pestaña "Métricas & Catálogo".
  * Botón individual "🔄 Re-renderizar" en el modal de detalle de cada trivia en el Explorador.
- Ejecución en vivo: **82 trivias (164 imágenes PNG) regeneradas en disco sin el texto «Plataforma de Trivias con IA»**.

### Fase 22 — Soporte de Videos Horizontales 16:9 (Full HD 1920x1080) para YouTube ✅
- Filtergraph especializado en `FFmpegCommandBuilder`:
  * Lienzo Full HD 1920x1080 con color de fondo `#0F172A`.
  * Tarjeta de trivia centrada (1080x1080) con relación de aspecto 1:1 nativa sin distorsión.
  * Columna izquierda (420px): Número de pregunta (`PREGUNTA X DE Y`), badge temático y título de trivia.
  * Columna derecha (420px): Cuenta regresiva sincronizada (`TIEMPO 5...4...3...`) e indicador destacado `RESPUESTA CORRECTA`.
- Interfaz gráfica (SPA): Botón `📺 Horizontal 16:9 (YouTube)` en el Estudio de Video.
- Validado y probado de extremo a extremo con video generado y comprobación ffprobe (1920x1080, 30 fps, AAC).

### Fase 23 — Introducciones Dinámicas de Video (Catálogo, Personalizada e IA) ✅
- **Arquitectura de Apertura Dinámica**: Escena inicial antes de la Pregunta 1 (`Introducción → Pregunta 1 → Respuesta 1 → ...`) diseñada para captar la atención y contextualizar la temática de la trivia en redes sociales.
- **Modelos y DTOs (`com.trivia.api.service.video.*` y `com.trivia.api.dto.*`)**:
  - `VideoIntroMode`: Enum (`NONE`, `TEMPLATE`, `CUSTOM`, `AI`).
  - `VideoIntroTemplate`: Record con `id`, `template` (`...{tema}...`) y `example`.
  - `VideoIntroTiming`: Record con `introText`, `topic`, `duration` y `audioPath`.
  - `VideoGenerationRequest`: extendido con `introMode`, `introTemplate`, `customIntroText` e `introTopic` (100% retrocompatible con constructores previos).
  - `VideoIntroPreviewRequest` y `VideoIntroPreviewResponse`.
  - `TimelinePlan`: cálculo dinámico de duración acumulada y método `hasIntro()`.
- **Servicio y Catálogo (`VideoIntroService`)**:
  - Catálogo inicial de 7 plantillas extensibles con sustitución dinámica de `{tema}`.
  - Inferencia inteligente del tema de introducción (`subtema` > `tipoTrivia.nombre` > `cultura general`).
  - Sanitización estricta de entradas personalizadas (remoción de comillas envolventes, normalización de espacios, limpieza de caracteres de control, límite de 160 caracteres).
  - Generación dinámica con Google Gemini (`generateVideoIntro`) mediante Structured Outputs (JSON Schema estricto) y fallback transparente a catálogo si la IA no está disponible o falla.
- **Renderizado Gráfico Headless (`TriviaRendererService`)**:
  - Método `renderIntro(topic, introText)` que genera una tarjeta PNG 1080x1080 coordinada con tipografía de alto impacto, badges de color y estética consistente con las tarjetas de trivia.
- **Composición y Pipeline FFmpeg (`FFmpegCommandBuilder` y `TriviaVideoService`)**:
  - Ensamblado de `intro_segment.mp4` para formatos vertical 9:16, horizontal 16:9 y cuadrado 1:1.
  - Sincronización con efecto de sonido `whoosh.wav` y narración TTS de la voz elegida.
  - Duración fonética adaptativa de acuerdo con el audio de voz generado o temporizado estándar de lectura humana.
  - Concatenación directa sin pérdidas vía demuxer `concat` (`-c copy`) con los segmentos de preguntas.
- **Puente TTS (`scripts/tts_bridge.py`)**:
  - Procesamiento del bloque `intro` en el JSON para sintetizar `intro.mp3` con Edge-TTS y reportar su duración exacta.
  - Decodificación `utf-8-sig` para admitir BOMs generados por PowerShell en Windows.
- **API REST (`TriviaVideoController`)**:
  - `GET /api/v1/videos/intro-templates` para listar el catálogo de plantillas disponibles.
  - `POST /api/v1/videos/preview-intro` para previsualizar o generar texto con IA en tiempo real.
- **Dashboard Web (SPA)**:
  - Nueva sección "🎬 Introducción del Video" en `test-client.html` y `src/main/resources/static/index.html` con selectores para las tres modalidades (`Sin introducción`, `Usar plantilla`, `Escribir mi propia introducción`, `Generar automáticamente con IA`), campo de texto con contador (0/160), selector de plantilla, botón para generar con IA, selector de tema y visor interactivo de previsualización en vivo.

---

## En progreso
Ninguna tarea bloqueada. Todas las funcionalidades requeridas están implementadas.

---

## Pendiente
- Configurar la clave real de Google Gemini (`AI_API_KEY`) y la base de datos en el entorno de producción del usuario según la guía `docs/DEPLOYMENT_GUIDE.md`.

---

## Problemas actuales
- **Maven Central SSL PKIX (Entorno de desarrollo local)**:
  - Resuelto: Todas las dependencias requeridas (Spring Boot 3.3.0, Springdoc OpenAPI 2.6.0, Flyway 10.10.0, Postgres JDBC) están en la caché local `~/.m2`. Los comandos de Maven deben ejecutarse con `-o` (`mvn test -o`, `mvn package -DskipTests -o`).

---

## Decisiones técnicas
1. **Spring Boot 3.3.0**: Selección estable para compatibilidad total con la caché local de Maven y Java 21.
2. **Java AWT/Graphics2D**: Renderizado de imágenes ligero sin dependencias pesadas de navegador (Playwright/Chromium), empaquetado en Alpine con `fontconfig` y `ttf-dejavu`.
3. **Google Gemini (gemini-3.5-flash-lite)**: Structured outputs nativos con JSON Schema para respuestas tipadas y consistentes.
4. **Almacenamiento Local + Servidor Estático**: Separación entre `path` físico (`storage/`) y `url` pública (`/assets/**`).
5. **Deduplicación Híbrida**: Capa de normalización de texto + SHA-256 hexadecimal exacto + Similitud Coseno de embeddings vectoriales.
6. **Arquitectura Asíncrona**: Separación en `AsyncTriviaPipelineExecutor` para garantizar intercepción por el proxy AOP de Spring Boot sin dependencias circulares.
7. **Segmentación y Concatenación FFmpeg**: Cada escena de introducción y pregunta se renderiza como un segmento MP4 independiente con parámetros de codificación idénticos (`libx264`, `yuv420p`, 30 fps, `aac`, 192k) y se fusiona mediante el demuxer `concat` (`-c copy`) en milisegundos sin re-renderizado.
8. **Estabilización de Volumen en FFmpeg (`normalize=0` + `alimiter`)**: Configuración de `normalize=0` en los filtros `amix` de FFmpeg para eliminar la atenuación dinámica por conteo de entradas activas (causante de que la voz comenzara baja y aumentara al finalizar los efectos de sonido). Se integró el limitador transparente `alimiter=limit=0.95` para asegurar un nivel de voz 100% fijo, nítido y balanceado a lo largo de todo el video sin distorsión.

---

## Archivos modificados y creados

- `pom.xml`: Configuración de Spring Boot 3.3.0, starters y plugins.
- `src/main/resources/application.yml`: Configuración centralizada de datasource, JPA, Flyway, dominios, IA y seguridad.
- `src/main/resources/db/migration/V1__create_tipo_trivia.sql` a `V6__seed_tipo_trivia.sql`: Migraciones Flyway.
- `src/main/java/com/trivia/api/TriviaApiApplication.java`: Entrada con `@EnableAsync`.
- `src/main/java/com/trivia/api/domain/*`: Entidades JPA y enums.
- `src/main/java/com/trivia/api/repository/*`: Repositorios Spring Data.
- `src/main/java/com/trivia/api/dto/*`: DTOs de petición y respuesta (Records).
  - Incluye `VideoIntroPreviewRequest.java`, `VideoIntroPreviewResponse.java` y `VideoGenerationRequest.java` con soporte de intro.
- `src/main/java/com/trivia/api/normalizer/TriviaQuestionNormalizer.java`: Normalizador de texto.
- `src/main/java/com/trivia/api/service/QuestionHashService.java`: Servicio de hashing SHA-256.
- `src/main/java/com/trivia/api/service/CatalogService.java`: Servicio de catálogo.
- `src/main/java/com/trivia/api/service/TriviaDuplicateService.java`: Deduplicador por capas.
- `src/main/java/com/trivia/api/service/TriviaValidationService.java`: Validador de negocio de trivias.
- `src/main/java/com/trivia/api/service/TriviaGenerationService.java`: Orquestador principal del pipeline.
- `src/main/java/com/trivia/api/service/AsyncTriviaPipelineExecutor.java`: Ejecutor en background thread.
- `src/main/java/com/trivia/api/service/TriviaQueryService.java`: Consultas de lectura y métricas.
- `src/main/java/com/trivia/api/service/TriviaRendererService.java`: Renderizado AWT 1080x1080 PNG (pregunta, respuesta e introducción).
- `src/main/java/com/trivia/api/service/AssetStorageService.java`: Interfaz de storage.
- `src/main/java/com/trivia/api/ai/TriviaAiClient.java` y `GeminiTriviaAiClient.java`: Cliente Gemini (preguntas e intro de video).
- `src/main/java/com/trivia/api/ai/EmbeddingService.java` y `EmbeddingServiceImpl.java`: Embeddings y similitud coseno.
- `src/main/java/com/trivia/api/controller/HealthController.java`: Endpoint de salud.
- `src/main/java/com/trivia/api/controller/CatalogController.java`: Endpoint de catálogo.
- `src/main/java/com/trivia/api/controller/TriviaController.java`: Endpoints REST principales.
- `src/main/java/com/trivia/api/controller/TriviaVideoController.java`: Endpoints de video (generate, download, intro-templates, preview-intro).
- `src/main/java/com/trivia/api/service/video/VideoIntroMode.java`: Enum de modalidades de introducción.
- `src/main/java/com/trivia/api/service/video/VideoIntroTemplate.java`: Record de plantillas de introducción.
- `src/main/java/com/trivia/api/service/video/VideoIntroTiming.java`: Record de cronograma de introducción.
- `src/main/java/com/trivia/api/service/video/VideoIntroService.java`: Gestión de catálogo, sanitización y generación IA de intros.
- `src/main/java/com/trivia/api/service/video/*`: Orquestador de video, builders de FFmpeg, servicios de narración TTS y extractor de assets.
- `src/main/java/com/trivia/api/exception/GlobalExceptionHandler.java`: Manejador ProblemDetails.
- `src/main/java/com/trivia/api/infrastructure/storage/LocalAssetStorageService.java`: Persistencia de imágenes en disco.
- `src/main/java/com/trivia/api/infrastructure/config/WebMvcConfig.java`: Mapeo estático `/assets/**`.
- `src/main/java/com/trivia/api/infrastructure/config/WebCorsConfig.java`: Configuración CORS.
- `src/main/java/com/trivia/api/infrastructure/security/ApiKeyFilter.java`: Filtro de autenticación API Key.
- `src/main/java/com/trivia/api/infrastructure/security/RateLimitingFilter.java`: Filtro de límite de peticiones.
- `test-client.html` y `src/main/resources/static/index.html`: Dashboard SPA con configuración y preview de introducciones.
- `scripts/tts_bridge.py` y `src/main/resources/scripts/tts_bridge.py`: CLI bridge para integración con `edge-tts` con soporte de intro.
### Fase 24 — Pipeline de Distribución y Auto-Publicación Multicanal (YouTube, TikTok, Facebook Reels, Google Drive y Batch Generator) ✅
- **Personalización de Títulos y Sanitización:**
  - Campo `customTitle` en `VideoGenerationRequest` y `test-client.html` / `index.html` con contador dinámico.
  - Jerarquía inteligente de resolución de título: Título personalizado > Frase de introducción > Tema optimizado.
  - Sanitización para Windows (`/ \ : * ? " < > | ¿ ¡`) y recorte a 85 caracteres para respetar el límite de 100 caracteres de YouTube Shorts al anexar `#N #Shorts`.
  - Numeración secuencial limpia `#1`, `#2`, `#3`... independiente por título.
- **Sincronización Automática con Google Drive:**
  - Script PowerShell vigilante (`sync-to-drive.ps1`) que sincroniza en tiempo real los videos de `./export_videos` a `H:\Mi unidad\VideosQuizazos`.
  - Preservación íntegra de la cola de Google Drive (limpieza exclusiva del búfer temporal local tras 48h).
- **Integración con Make.com:**
  - Escenario programado Lunes a Domingo (11:00 a 18:30 cada 210 min, ~3 videos/día).
  - Router tripartito: YouTube Shorts (`Upload a Video`), Facebook Reels (`Upload a Video`), y bot de Telegram a celular con el video y texto listo para publicar en TikTok con 1 toque.
- **Generador Masivo (`generate_60_videos.py`):**
  - Generación desatendida de 60 videos (300 preguntas generadas por Gemini con 4 opciones y tarjeta explicativa).
  - 30 videos de *Historia y Mitología* ⚔️ y 30 videos de *Ciencia, Espacio y Naturaleza Extrema* 🌌.
  - Locución neuronal en español con Edge-TTS alternando voces masculina (`JorgeNeural`) y femenina (`DaliaNeural`).
  - Persistencia de estado en `generation_60_state.json` con tolerancia a fallos y reanudación automática.

---

## Pruebas realizadas
- `TriviaQuestionNormalizerTest`: 11 tests (Unicode NFC, minúsculas, puntuación, acentos, espacios).
- `QuestionHashServiceTest`: 6 tests (determinismo, 64 hex chars, test vectors).
- `TriviaDuplicateServiceTest`: 8 tests (intra-lote, contra BD, sesiones previas, entradas vacías).
- `GeminiTriviaAiClientTest`: 5 tests (deserialización estructurada, limpieza de markdown, validación API key, errores HTTP).
- `TriviaValidationServiceTest`: 8 tests (reglas de opciones, 1 sola correcta, explicaciones).
- `TriviaGenerationServiceTest`: 3 tests (happy path, reintentos con buffer, fallo por intentos máximos).
- `TriviaQueryServiceTest`: 7 tests (consulta ID, random, stats, búsquedas, exportación).
- `TriviaExportServiceTest`: 3 tests (exportación CSV, JSON, filtros).
- `TriviaRendererServiceTest`: 3 tests (PNG 1080x1080 PREGUNTA, RESPUESTA y tarjeta de INTRODUCCIÓN).
- `EmbeddingServiceTest`: 6 tests (similitud coseno, ortogonales, opuestos, serialización, fallback local).
- `LocalAssetStorageServiceTest`: 2 tests (guardado en disco, URLs, lectura, borrado).
- `HealthControllerTest`: 4 tests (HTTP 200, status UP, metadata).
- `CatalogControllerTest`: 4 tests (GET catálogo, mapeo DTOs, coincidencia difusa).
- `CategorySimilarityMatcherTest`: 7 tests (coincidencias difusas de categorías).
- `TriviaControllerTest`: 12 tests (POST síncrono 201, POST asíncrono 202, GET status 200, GET id, random, search, stats, validaciones, POST rerender).
- `TriviaMaintenanceServiceTest`: 2 tests (re-renderizado masivo exitoso, manejo de excepciones individuales).
- `TriviaVideoControllerTest`: 4 tests (POST /generate con MockMvc, GET /download streaming, GET /intro-templates, POST /preview-intro).
- `FFmpegCommandBuilderTest`: 7 tests (filtros verticales 9:16, horizontales 16:9, cuadrados 1:1, intros en vertical/horizontal/cuadrado, canal de silencio aevalsrc).
- `VideoIntroServiceTest`: 10 tests (plantillas, sustitución de tema, texto personalizado, sanitización, manejo de fallos IA, topic inferido).
- `NoOpNarrationServiceTest`: 1 test (cálculo de tiempos determinista sin TTS).
- `ApiKeyFilterTest`: 5 tests (bypass dev, GET público, rechazo 401, autorización correcta).
- `TriviaApiApplicationTest`: 1 smoke test de carga de contexto Spring Boot.

## Resultados
- **Total tests**: 123
- **Aprobados**: 123 (100%)
- **Fallos**: 0
- **Errores**: 0
- **Build**: SUCCESS

---

## Próximo paso recomendado

Para levantar los contenedores con Podman en el entorno local o servidor:
```powershell
# 1. Asegurar que el archivo .env tenga los valores deseados (copiar de .env.example)
Copy-Item .env.example .env

# 2. Levantar los servicios con Podman Compose
podman compose up -d

# 3. Comprobar que los contenedores estén saludables y consultar /health
podman ps
curl http://localhost:8080/api/v1/health
```
Consulte `docs/DEPLOYMENT_GUIDE.md` para el procedimiento de despliegue en VPS y `docs/GUIA_GENERACION_VIDEOS_TTS.md` para generar videos con narración TTS e introducciones dinámicas.
