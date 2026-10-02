# 📖 Manual de Operación — StoryVideoGenerator
## Guía Paso a Paso: Cómo Poner en Marcha el Sistema

Este manual describe el procedimiento detallado para poner a trabajar **StoryVideoGenerator** en tu equipo local con Windows y tu tarjeta gráfica NVIDIA RTX 5070 (12 GB VRAM), permitiendo generar videos verticales 1080×1920 listos para TikTok, YouTube Shorts y Reels con un coste de generación de imágenes de $0.

---

## 📑 Tabla de Contenidos

1. [Arquitectura y Requisitos Previos](#1-arquitectura-y-requisitos-previos)
2. [Paso 1: Configurar Variables de Entorno](#paso-1-configurar-variables-de-entorno)
3. [Paso 2: Iniciar la Base de Datos PostgreSQL](#paso-2-iniciar-la-base-de-datos-postgresql)
4. [Paso 3: Iniciar Stable Diffusion WebUI Forge (GPU Local)](#paso-3-iniciar-stable-diffusion-webui-forge-gpu-local)
5. [Paso 4: Iniciar la API Backend (Spring Boot)](#paso-4-iniciar-la-api-backend-spring-boot)
6. [Paso 5: Generar tu Primer Video](#paso-5-generar-tu-primer-video)
   - [Opción A: Pipeline Todo en Uno (Recomendado)](#opción-a-pipeline-todo-en-uno-asíncrono-recomendado)
   - [Opción B: Procesamiento por Lotes Masivos (Batch)](#opción-b-procesamiento-por-lotes-masivos-batch)
   - [Opción C: Flujo Manual y Regeneración Escena por Escena](#opción-c-flujo-manual-y-regeneración-escena-por-escena)
7. [Paso 6: Dónde Encontrar los Videos Exportados](#paso-6-dónde-encontrar-los-videos-exportados)
8. [Paso 7: Rutinas de Mantenimiento y Limpieza](#paso-7-rutinas-de-mantenimiento-y-limpieza)
9. [Solución de Problemas Comunes (Troubleshooting)](#solución-de-problemas-comunes-troubleshooting)

---

## 1. Arquitectura y Requisitos Previos

El sistema se compone de 4 servicios coordinados:

```
┌────────────────────────────────────────────────────────┐
│  Host Windows (RTX 5070 12GB VRAM)                    │
│                                                        │
│  [SD WebUI Forge] :7860                                │
│       ▲ (API txt2img)                                  │
│       │                                                │
│  [StoryVideoGenerator API] :8081                       │
│    ├── Java 21 / Spring Boot 3.3.0                     │
│    ├── Python + Edge-TTS (Locución Neural)             │
│    └── FFmpeg 8.x con DirectWrite (Composición 9:16)   │
│       │                      │                         │
│       ▼                      ▼                         │
│  [PostgreSQL 16] :5433   [./export_videos/]            │
│  (Database: storydb)     ├── Video.mp4                 │
│                          └── Video_metadata.json       │
└────────────────────────────────────────────────────────┘
```

### Herramientas Requeridas en tu PC:

1. **Java 21 LTS** (`java -version` debe indicar Java 21).
2. **Maven 3.8+** (`mvn -v`).
3. **Python 3.10+**:
   Instala la librería de síntesis de voz neuronal:
   ```powershell
   pip install edge-tts
   ```
4. **FFmpeg y FFprobe 6.x o superior** (con soporte para `libass` para subtítulos):
   Verifica en consola:
   ```powershell
   ffmpeg -version
   ffprobe -version
   ```
5. **Stable Diffusion WebUI Forge** (o WebUI Automatic1111):
   - Modelo recomendado: **Juggernaut XL v9** o cualquier checkpoint SDXL / SD 1.5.
6. **Podman** o **Docker Desktop** (para la base de datos PostgreSQL).
7. **Google Gemini API Key**: Obtenla gratis en [Google AI Studio](https://aistudio.google.com/).

---

## Paso 1: Configurar Variables de Entorno

En la carpeta raíz del proyecto `c:\Users\villa\GIT DESKTOP\AIO\StoryVideoGenerator`:

1. Crea tu archivo `.env` a partir de la plantilla:
   ```powershell
   Copy-Item .env.example .env
   ```
2. Abre `.env` con tu editor y configura tus credenciales:

```dotenv
# Base de Datos
DATABASE_URL=jdbc:postgresql://localhost:5433/storydb
DATABASE_USERNAME=story_user
DATABASE_PASSWORD=story_pass

# Google Gemini (Requerido para guiones e historias)
AI_API_KEY=tu_api_key_de_gemini_aqui
AI_MODEL=gemini-2.5-flash
AI_TIMEOUT_SECONDS=120

# Stable Diffusion Local (GPU RTX 5070)
SD_API_URL=http://localhost:7860
SD_MODEL=juggernautXL_v9
IMAGE_PROVIDER=stable-diffusion-local
SD_STEPS=25
SD_CFG_SCALE=7.0
SD_WIDTH=768
SD_HEIGHT=1344

# Rutas Locales
STORAGE_PATH=./storage
STORAGE_BASE_URL=http://localhost:8081/assets
EXPORT_PATH=./export_videos

# Puerto de la API
PORT=8081
```

> [!NOTE]
> Si no configuras `AI_API_KEY` o no inicias Stable Diffusion, el sistema cuenta con fallbacks integrados de prueba (placeholders e inteligencia defensiva) para que nunca se interrumpa el flujo de trabajo.

---

## Paso 2: Iniciar la Base de Datos PostgreSQL

Inicia el contenedor de base de datos en el puerto `5433`:

```powershell
# Desde c:\Users\villa\GIT DESKTOP\AIO\StoryVideoGenerator
podman compose up -d postgres
```
*(O si usas Docker Desktop: `docker compose up -d postgres`)*

### ¿Cómo comprobar que la base de datos está lista?
```powershell
podman ps
```
Deberás ver el contenedor `story-postgres` en estado `healthy` exponiendo el puerto `5433:5432`. Flyway ejecutará automáticamente las migraciones V1 a V9 al arrancar el backend.

---

## Paso 3: Iniciar Stable Diffusion WebUI Forge (GPU Local)

Para que la generación de imágenes sea 100% gratuita y utilice tu tarjeta RTX 5070:

1. Abre tu carpeta de instalación de **Stable Diffusion WebUI Forge**.
2. Edita el archivo `webui-user.bat` y asegúrate de que los argumentos incluyan `--api`:
   ```bat
   set COMMANDLINE_ARGS=--api --listen --xformers
   ```
3. Ejecuta `webui-user.bat`.
4. Verifica que el endpoint API responda abriendo en tu navegador o consola:
   ```powershell
   Invoke-RestMethod -Uri "http://localhost:7860/sdapi/v1/options" -Method Get
   ```

> [!TIP]
> Si en algún momento no quieres abrir Stable Diffusion para ahorrar recursos, el backend cambiará automáticamente a `PlaceholderImageService` generando fondos cinemáticos abstractos de respaldo sin detener la producción de videos.

---

## Paso 4: Iniciar la API Backend (Spring Boot)

Abre una terminal PowerShell en la raíz del proyecto y ejecuta:

```powershell
mvn spring-boot:run
```

El servidor compilará e iniciará en pocos segundos. Verás en los logs:
```
Started StoryVideoApiApplication in 4.5 seconds
Directorio de exportación inicializado en: C:\Users\villa\GIT DESKTOP\AIO\StoryVideoGenerator\export_videos
EdgeTtsNarrationService inicializado (python: 'python', script: '.../scripts/story_tts_bridge.py')
Tomcat started on port 8081
```

### URLs Principales:
- **API Base**: `http://localhost:8081`
- **Swagger UI (Documentación interactiva)**: [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)
- **Health Check**: [http://localhost:8081/api/v1/health](http://localhost:8081/api/v1/health)
- **Estadísticas**: [http://localhost:8081/api/v1/stats](http://localhost:8081/api/v1/stats)

---

## Paso 5: Generar tu Primer Video

Tienes 3 modalidades de trabajo según tus necesidades:

### Opción A: Pipeline Todo en Uno Asíncrono (Recomendado)

Genera el video completo desde la idea hasta el archivo MP4 exportado en una sola llamada:

```powershell
$body = @{
    genre = "HORROR"
    tone = "DARK"
    theme = "Una cabaña en el bosque con una radio que transmite conversaciones del futuro"
    targetDurationSeconds = 60
    language = "es-MX"
    sceneCount = 4
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/pipeline/execute" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body

$response
```

**Respuesta recibida (Código `202 Accepted`):**
```json
{
  "storyId": "8b5ec284-90aa-4384-9279-d26bcf641885",
  "status": "INITIALIZED",
  "currentStage": "NOT_STARTED",
  "progressPercent": 0,
  "message": "Pipeline encolado para ejecución"
}
```

#### Consultar el avance en tiempo real:
```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/v1/pipeline/status/8b5ec284-90aa-4384-9279-d26bcf641885"
```
Verás cómo el progreso avanza:
- `15%`: Generando historia y personajes con IA (Gemini).
- `35%`: Generando imágenes de cada escena en GPU local (Stable Diffusion).
- `60%`: Sintetizando narración neural con Edge-TTS.
- `80%`: Componiendo filtergraphs de FFmpeg (1080×1920 @ 30fps con Ken Burns y subtítulos).
- `95%`: Validando calidad con `ffprobe` y exportando MP4 con `metadata.json`.
- `100%`: `COMPLETED`.

---

### Opción B: Procesamiento por Lotes Masivos (Batch)

Ideal para programar 10, 20 o 50 videos desatendidos durante la noche:

```powershell
$batchRequest = @{
    distribution = @{
        HORROR = 3
        MYSTERY = 2
        SCI_FI = 1
    }
    tone = "DARK"
    language = "es-MX"
    targetDurationSeconds = 60
    sceneCount = 4
    maxConcurrency = 1  # 1 para reservar toda la VRAM a cada imagen secuencialmente
} | ConvertTo-Json

$batch = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/stories/batch" `
    -Method Post `
    -ContentType "application/json" `
    -Body $batchRequest

$batch
```

#### Consultar el estado del lote:
```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/v1/batches/$($batch.batchId)/status"
```

#### Reintentar historias que hayan fallado en el lote:
```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/v1/batches/$($batch.batchId)/retry-failed" -Method Post
```

---

### Opción C: Flujo Manual y Regeneración Escena por Escena

Si deseas inspeccionar el guion y cambiar una imagen o narración específica antes de renderizar:

1. **Crear la historia**:
   ```powershell
   POST http://localhost:8081/api/v1/stories/generate
   ```
2. **Regenerar solo la imagen de la Escena 2** (por ejemplo, con otra semilla):
   ```powershell
   POST http://localhost:8081/api/v1/stories/{storyId}/scenes/{sceneId}/regenerate-image
   ```
3. **Regenerar solo el audio de la Escena 2**:
   ```powershell
   POST http://localhost:8081/api/v1/stories/{storyId}/scenes/{sceneId}/regenerate-audio
   ```
4. **Re-renderizar el video final**:
   ```powershell
   POST http://localhost:8081/api/v1/stories/{storyId}/rerender
   ```
5. **Exportar**:
   ```powershell
   POST http://localhost:8081/api/v1/stories/{storyId}/export
   ```

---

## Paso 6: Dónde Encontrar los Videos Exportados

Una vez completado el pipeline, los archivos terminados se ubican en la carpeta:
```
c:\Users\villa\GIT DESKTOP\AIO\StoryVideoGenerator\export_videos\
```

Cada video produce dos archivos:
1. `[Nombre_Historia].mp4`: Video vertical 9:16 (1080×1920) renderizado en H.264 / AAC a 30 FPS con subtítulos de alta visibilidad amarillos y audio balanceado.
2. `[Nombre_Historia]_metadata.json`: Metadatos completos con título, descripción, duración, hashtags (#horror, #storytime, #fyp, etc.) para que Make.com los tome y los publique automáticamente.

También puedes descargar el video directamente por HTTP:
```
GET http://localhost:8081/api/v1/stories/{id}/video/download
```

---

## Paso 7: Rutinas de Mantenimiento y Limpieza

El sistema incluye `AutoCleanupService` configurado por defecto:
- **Ejecución automática**: Todos los días a las **03:00 AM**.
- **Regla de retención**: Elimina videos exportados y archivos temporales (`.mp4`, `.wav`, `.png`, `.srt`) que tengan más de **7 días** de antigüedad.
- **Monitoreo de disco**: Emite una alerta en los logs si el disco duro tiene menos de **1 GB** libre.

Para consultar el estado del almacenamiento en cualquier momento:
```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/v1/stats"
```

---

## Solución de Problemas Comunes (Troubleshooting)

### 1. `Connection refused: localhost:7860`
- **Causa**: Stable Diffusion WebUI Forge no está abierto o no tiene la bandera `--api`.
- **Solución**: Asegúrate de que `webui-user.bat` incluya `set COMMANDLINE_ARGS=--api --listen` y que la consola de Forge muestre `Running on local URL: http://127.0.0.1:7860`.
- **Comportamiento seguro**: Si Forge no está disponible, el backend usará `PlaceholderImageService` automáticamente.

### 2. `Connection to localhost:5433 refused`
- **Causa**: El contenedor de PostgreSQL no está levantado.
- **Solución**: Ejecuta `podman compose up -d postgres`. Verifica con `podman ps`.

### 3. `ModuleNotFoundError: No module named 'edge_tts'`
- **Causa**: La librería de Python no está instalada en tu entorno de Python global o activo.
- **Solución**: Ejecuta `pip install edge-tts`.

### 4. `La historia generada fue rechazada por deduplicación`
- **Causa**: El sistema de deduplicación histórica detectó que el título o la premisa es demasiado parecida a una historia producida recientemente.
- **Solución**: Vuelve a solicitar la generación con un tema ligeramente diferente o deja el campo `theme` en blanco para que la IA proponga un concepto fresco e inédito.

---

¡Tu sistema **StoryVideoGenerator** está 100% operativo y listo para producir contenido audiovisual automatizado!
