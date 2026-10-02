# 🎬 Guía Completa: Generación Automática de Videos con Voz (TTS) y SFX

Esta guía detalla el funcionamiento, arquitectura, configuración y uso de la funcionalidad de **generación automatizada de videos** a partir de trivias, integrando narración por voz con Inteligencia Artificial (Text-to-Speech), efectos de sonido sincronizados, música de fondo y transiciones gráficas.

---

## 📋 Índice
1. [Visión General y Formatos Soportados](#1-visión-general-y-formatos-soportados)
2. [Estructura y Ritmo del Video por Trivia](#2-estructura-y-ritmo-del-video-por-trivia)
3. [Requisitos Previos del Sistema](#3-requisitos-previos-del-sistema)
4. [Uso Mediante la API REST (Spring Boot)](#4-uso-mediante-la-api-rest-spring-boot)
5. [Uso Mediante CLI / Script Standalone (Python)](#5-uso-mediante-cli--script-standalone-python)
6. [Arquitectura Interna y Componentes](#6-arquitectura-interna-y-componentes)
7. [Solución Técnica a Problemas de Audio y Reproducción](#7-solución-técnica-a-problemas-de-audio-y-reproducción)
8. [Voces Disponibles (Edge-TTS)](#8-voces-disponibles-edge-tts)

---

## 1. Visión General y Formatos Soportados

El pipeline toma las trivias existentes en la base de datos (o desde archivos locales JSON e imágenes PNG de 1080x1080) y genera un video final compilado en formato **MP4 (códec H.264 / AAC)** listo para redes sociales o consumo web.

### Formatos de Salida:
| Formato | Resolución | Relación de Aspecto | Caso de Uso |
|---|---|---|---|
| `VERTICAL_9_16` *(Default)* | **1080 x 1920** | 9:16 | YouTube Shorts, TikTok, Instagram Reels |
| `HORIZONTAL_16_9` | **1920 x 1080** | 16:9 | Videos largos y estándar de YouTube, Pantallas de escritorio, TV |
| `SQUARE_1_1` | **1080 x 1080** | 1:1 | Feeds de Instagram, Facebook, X (Twitter), Web |

* **Formato Vertical (9:16)**: La tarjeta cuadrada (1080x1080) se ubica al centro sobre fondo oscuro coordinado (`#0F172A`), con una barra superior con el encabezado y contador regresivo flotante animado.
* **Formato Horizontal (16:9)**: La tarjeta se presenta en resolución nativa 1080x1080 en el tercio central sin distorsión de píxeles, con una columna lateral izquierda (420px) para el encabezado (`PREGUNTA X DE Y`) y título de trivia, y una columna lateral derecha (420px) para la cuenta regresiva en vivo (`TIEMPO 5...4...3...`) y el sello de `RESPUESTA CORRECTA`.
* **Formato Cuadrado (1:1)**: Coincide pixel por pixel con la imagen de la trivia, proyectando el contador flotante en la parte inferior.

---

## 2. Estructura y Ritmo del Video por Trivia

El video cuenta con una estructura modular y cronometrada:

```
[OPCIONAL] ESCENA DE INTRODUCCIÓN DINÁMICA (Tema / Subtema de la Trivia)
• Visual: Tarjeta gráfica con tema, llamada a la acción y gancho inicial (renderizado 1080x1080)
• Audio: Efecto 'whoosh.wav' + locución TTS neuronal (si TTS está activo)
• Duración: Adaptativa según fonética del audio (~3.5s - 5.0s) o 4.0s fijos sin voz
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│ FASE 1: PREGUNTA Y PENSAMIENTO (Muestra question.png)                                      │
├─────────────────────────────────────────────┬───────────────────────────────────────────────┤
│ [Lectura de Pregunta]                       │ [Cuenta Regresiva: 5s, 4s, 3s, 2s, 1s]       │
│ • Transición 'whoosh' al iniciar            │ • Contador visual superpuesto                 │
│ • Si TTS activo: Duración del audio de voz  │ • Efecto de sonido 'tick.wav' cada segundo    │
│ • Si TTS inactivo: 7.0 segundos calibrados  │ • Duración exacta: 5.0 segundos               │
└─────────────────────────────────────────────┴───────────────────────────────────────────────┘
                                              │
                                              ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│ FASE 2: REVELACIÓN Y EXPLICACIÓN (Muestra answer.png)                                       │
├─────────────────────────────────────────────────────────────────────────────────────────────┤
│ • Campanada de acierto ('correct.wav') inmediata al revelar                                │
│ • Muestra la opción correcta resaltada en verde y la tarjeta explicativa                    │
│ • Si TTS activo: Duración exacta de la lectura de la solución y explicación pedagógica      │
│ • Si TTS inactivo: 6.5 segundos para lectura humana cómoda                                  │
└─────────────────────────────────────────────────────────────────────────────────────────────┘
```

* **Flujo Secuencial**:
  $$\text{Introducción} \longrightarrow \text{Pregunta 1} \longrightarrow \text{Respuesta 1} \longrightarrow \dots \longrightarrow \text{Pregunta N} \longrightarrow \text{Respuesta N}$$
* **Sin TTS**: Duraciones estándar fijas calibradas para lectura cómoda.
* **Con TTS**: Duraciones **dinámicas y adaptativas** calculadas por fonética de la voz.
* **Música de Fondo (BGM)**: Mezclada continuamente en loop suave sobre la duración total del video (incluyendo la introducción).

---

## 3. Requisitos Previos del Sistema

Para generar videos en tu máquina o servidor se requieren:

1. **FFmpeg**:
   - Debe estar instalado y accesible en la variable de entorno `PATH` (puedes verificarlo ejecutando `ffmpeg -version` en la terminal).
2. **Python 3.10+ y `edge-tts` (Opcional, solo si se usa TTS)**:
   - Para narración con voces naturales neuronales gratuitas sin API Key:
     ```powershell
     pip install edge-tts
     ```
   - Si Python o `edge-tts` no están presentes, el sistema utiliza un *fallback* automático a duraciones estándar fijas sin detener la generación.

---

## 4. Uso Mediante la API REST (Spring Boot)

### Endpoint 1: Solicitar la Generación del Video
* **Método**: `POST`
* **Ruta**: `/api/v1/videos/generate`
* **Content-Type**: `application/json`

#### Cuerpo de la Petición (`VideoGenerationRequest`):
```json
{
  "triviaIds": [
    "c8a14b53-43b9-4a4b-8de9-a864d4bce001",
    "f2b87e21-99c1-482a-bc91-d856f4ece002",
    "a1b2c3d4-e5f6-47a8-b9c0-d1e2f3a4b5c6"
  ],
  "format": "VERTICAL_9_16",
  "withBgm": true,
  "withTts": true,
  "ttsVoice": "es-MX-JorgeNeural",
  "introMode": "TEMPLATE",
  "introTemplate": "tpl_que_tanto_sabes",
  "customIntroText": null,
  "introTopic": "Historia romana"
}
```

#### Parámetros:
| Parámetro | Tipo | Requerido | Default | Descripción |
|---|---|---|---|---|
| `triviaIds` | `List<UUID>` | **Sí** | - | Lista de UUIDs de las trivias que formarán el video (mínimo 1). |
| `format` | `String` | No | `VERTICAL_9_16` | `VERTICAL_9_16` (1080x1920), `HORIZONTAL_16_9` (1920x1080) o `SQUARE_1_1` (1080x1080). |
| `withBgm` | `Boolean` | No | `true` | Incluye música de fondo en bucle suave. |
| `withTts` | `Boolean` | No | `false` | Activa la narración de voz con IA (Edge-TTS). |
| `ttsVoice` | `String` | No | `es-MX-JorgeNeural` | Identificador de voz neuronal de Edge-TTS. |
| `introMode` | `String` | No | `NONE` | Modalidad de introducción: `NONE`, `TEMPLATE`, `CUSTOM` o `AI`. |
| `introTemplate` | `String` | No | `null` | Plantilla del catálogo (ej. `tpl_que_tanto_sabes` o texto con `{tema}`). |
| `customIntroText` | `String` | No | `null` | Texto personalizado cuando `introMode` es `CUSTOM`. |
| `introTopic` | `String` | No | Auto | Tema o subtema explícito (si se omite, se deduce de las trivias). |

### Endpoint 2: Consultar Catálogo de Plantillas de Introducción
* **Método**: `GET`
* **Ruta**: `/api/v1/videos/intro-templates`

Retorna la lista de plantillas disponibles con sus identificadores y ejemplos:
```json
[
  {
    "id": "tpl_pon_a_prueba",
    "template": "Pon a prueba tus conocimientos sobre {tema}.",
    "example": "Pon a prueba tus conocimientos sobre Historia romana."
  },
  {
    "id": "tpl_que_tanto_sabes",
    "template": "¿Qué tanto sabes de {tema}?",
    "example": "¿Qué tanto sabes de Interstellar?"
  }
]
```

### Endpoint 3: Previsualizar o Generar Frase de Introducción
* **Método**: `POST`
* **Ruta**: `/api/v1/videos/preview-intro`
* **Cuerpo**:
```json
{
  "mode": "AI",
  "topic": "Historia romana",
  "triviaIds": ["c8a14b53-43b9-4a4b-8de9-a864d4bce001"],
  "idioma": "es-MX"
}
```
* **Respuesta**:
```json
{
  "mode": "AI",
  "topic": "Historia romana",
  "introText": "¿Cuánto sabes realmente sobre el Imperio Romano?"
}
```

#### Ejemplo con PowerShell `curl`:
```powershell
curl -X POST http://localhost:8080/api/v1/videos/generate `
  -H "Content-Type: application/json" `
  -d '{
    "triviaIds": ["4a7e9352-8756-42d4-a82f-bf6c3619572b"],
    "format": "VERTICAL_9_16",
    "includeBgm": true,
    "withTts": true,
    "ttsVoice": "es-MX-JorgeNeural"
  }'
```

#### Respuesta (`VideoGenerationResponse` - HTTP 200 OK):
```json
{
  "videoId": "9b7d3420-53bc-45f8-b3ec-80949d0ca921",
  "fileName": "trivia_video_9b7d3420.mp4",
  "downloadUrl": "/api/v1/videos/9b7d3420-53bc-45f8-b3ec-80949d0ca921/download",
  "format": "VERTICAL_9_16",
  "durationSeconds": 31.85,
  "fileSizeBytes": 1284930,
  "triviaCount": 1,
  "withTts": true,
  "ttsVoice": "es-MX-JorgeNeural"
}
```

---

### Endpoint 2: Descargar el Video Generado
* **Método**: `GET`
* **Ruta**: `/api/v1/videos/{videoId}/download`
* **Respuesta**: Stream de video binario (`video/mp4`) con encabezado `Content-Disposition: attachment; filename="..."`.

Puedes descargarlo directamente desde el navegador o mediante PowerShell:
```powershell
Invoke-WebRequest -Uri "http://localhost:8080/api/v1/videos/9b7d3420-53bc-45f8-b3ec-80949d0ca921/download" -OutFile "mi_trivia_video.mp4"
```

---

## 5. Uso Mediante CLI / Script Standalone (Python)

Si deseas procesar lotes de trivias sin pasar por el backend web (por ejemplo, a partir de una exportación JSON en disco con sus imágenes PNG), se incluye un pipeline completo en `scripts/generate_full_trivia_video.py`.

### Comandos de Ejemplo:

#### Generar Video Vertical (9:16) con Narración TTS:
```powershell
python scripts/generate_full_trivia_video.py `
  --input-dir docs/triviasExample `
  --output video_vertical_tts.mp4 `
  --format vertical `
  --tts `
  --voice es-MX-JorgeNeural
```

#### Generar Video Cuadrado (1:1) sin Voz (Ritmo Estándar):
```powershell
python scripts/generate_full_trivia_video.py `
  --input-dir docs/triviasExample `
  --output video_cuadrado.mp4 `
  --format square
```

#### Probar Sintetizador TTS de forma aislada:
```powershell
python scripts/tts_bridge.py `
  --text "¿Cuál es el planeta más grande del sistema solar?" `
  --output pregunta.mp3 `
  --voice es-MX-DaliaNeural
```
*Salida JSON devuelta:*
```json
{"status": "ok", "audio_path": "pregunta.mp3", "duration_seconds": 3.42}
```

---

## 6. Arquitectura Interna y Componentes

El paquete `com.trivia.api.service.video` encapsula la lógica desacoplada:

```
com.trivia.api.service.video/
├── VideoFormat.java              # Enum: VERTICAL_9_16 y SQUARE_1_1
├── TriviaSceneTiming.java        # Record con los tiempos de cada fase de la escena
├── TimelinePlan.java             # Plan maestro de la secuencia con rutas de audio y escenas
├── NarrationService.java         # Interfaz para cálculo de línea de tiempo y TTS
├── EdgeTtsNarrationService.java  # Implementación primaria con Edge-TTS (vía tts_bridge.py)
├── NoOpNarrationService.java     # Implementación fallback con temporizado fijo
├── VideoAssetExtractor.java      # Extrae fuentes y SFX desde el JAR a storage/.video-assets
├── FFmpegCommandBuilder.java     # Genera comandos y filtros complejos de FFmpeg
├── FFmpegProcessExecutor.java    # Ejecuta FFmpeg de forma segura capturando logs
└── TriviaVideoService.java       # Orquestador del flujo completo
```

### Assets Embebidos
Los archivos de soporte se empaquetan en `src/main/resources/video-assets/`:
* `audio/whoosh.wav`: Transición inicial de cambio de escena.
* `audio/tick.wav`: Sonido percusivo para el reloj de cuenta regresiva (1 tick por segundo).
* `audio/correct.wav`: Campanada alegre al revelar la respuesta acertada.
* `audio/bgm_loop.wav`: Base musical ambiental sintetizada para evitar silencios incómodos.
* `fonts/font.ttf`: Tipografía TrueType empotrada para renderizado nativo de textos y números por FFmpeg sin depender de fuentes del sistema operativo.

---

## 7. Solución Técnica a Problemas de Audio y Reproducción

### Diagnóstico del "Bug de los 2.1 Segundos":
Durante las primeras iteraciones, reproductores como Windows Media Player cortaban la reproducción del video a los 2.1 segundos o aceleraban el video de forma abrupta.
* **Causa**: Al usar el filtro de mezcla `amix=inputs=...:duration=first`, si el input 0 era un efecto corto como `whoosh.wav` (0.45s), FFmpeg cerraba la pista de audio antes de que el flujo de video concluyera. Los reproductores multimedia modernos, al detectar que el stream de audio se interrumpe, fuerzan el fin del archivo.
* **Solución Implementada**:
  `FFmpegCommandBuilder` ahora genera siempre una pista de audio base de silencio puro calibrada a la duración exacta del clip:
  ```
  aevalsrc=0:d=<DURACION_TOTAL>[asilence]
  ```
  Esta pista actúa como input 0 de `amix`, garantizando que el audio y el video posean exactamente la misma cantidad de muestras y paquetes, logrando reproducción 100% fluida en Windows, macOS, Android, iOS y navegadores web.

---

## 8. Voces Disponibles (Edge-TTS)

Se admiten todas las voces neuronales provistas por Microsoft Edge TTS sin costo ni necesidad de tokens:

| Código de Voz | Idioma / Región | Género | Descripción |
|---|---|---|---|
| `es-MX-JorgeNeural` *(Default)* | Español (México) | Masculino | Tono narrador formal y enérgico |
| `es-MX-DaliaNeural` | Español (México) | Femenino | Tono claro, suave y articulado |
| `es-ES-AlvaroNeural` | Español (España) | Masculino | Acento castellano neutral |
| `es-ES-ElviraNeural` | Español (España) | Femenino | Acento castellano profesional |
| `es-AR-TomasNeural` | Español (Argentina) | Masculino | Acento rioplatense |
| `es-CO-GonzaloNeural` | Español (Colombia) | Masculino | Acento colombiano neutral |
| `en-US-ChristopherNeural` | Inglés (EE.UU.) | Masculino | Ideal para trivias en inglés |
| `en-US-JennyNeural` | Inglés (EE.UU.) | Femenino | Ideal para trivias en inglés |

---

## 9. Limpieza de Identidad Visual en Imágenes

A partir de la última actualización, la marca por defecto `"Plataforma de Trivias con IA"` ha sido eliminada del pie de página en `TriviaRendererService`.
* Si la trivia tiene `subtema`, se muestra `"Subtema: <subtema>"`.
* Si la trivia no tiene `subtema`, el pie de página permanece limpio, permitiendo que los videos e imágenes luzcan 100% profesionales y listos para cualquier canal o marca.

---

## 10. Títulos Personalizados y Numeración Secuencial

Para maximizar el CTR (Click-Through Rate) en redes sociales, el servicio de video soporta personalización dinámica y sanitización de títulos:

1. **Parámetro `customTitle`**:
   Permite especificar un título de enganche directo en la petición JSON (ej. `"3 Preguntas Capciosas Imposibles"`).
2. **Jerarquía de Selección**:
   - Si se especifica `customTitle`, se utiliza ese título directamente.
   - Si se omite pero se configuró una frase de introducción (plantilla o personalizada), se toma esa frase como título base.
   - En caso contrario, se genera `"Cuanto sabes sobre {tema} - Trivia Challenge"`, optimizando y recortando temas extensos.
3. **Sanitización y Límites**:
   - Se eliminan caracteres no permitidos en sistemas de archivos Windows (`\ / : * ? " < > | ¿ ¡`).
   - Se trunca a un máximo de 85 caracteres para no sobrepasar el límite estricto de 100 caracteres de YouTube Shorts al incorporar los tags de publicación (`#N #Shorts`).
4. **Numeración Limpia**:
   - Cada título mantiene su propia secuencia independiente (`#1`, `#2`, `#3`...).

---

## 11. Auto-Exportación a Google Drive y Distribución Multicanal (Make.com)

El sistema incluye una cadena completa de publicación automatizada:

1. **Búfer de Exportación del Contenedor**:
   La variable de entorno `TRIVIA_VIDEO_AUTO_EXPORT_PATH=/export_videos` hace que el backend escriba una copia de cada video terminado en el volumen montado en el host `./export_videos`.
2. **Sincronizador en Tiempo Real (`sync-to-drive.ps1`)**:
   Monitorea la carpeta `./export_videos` con un `FileSystemWatcher` y transfiere automáticamente los archivos a tu unidad de Google Drive (`H:\Mi unidad\VideosQuizazos`).
   * *Mantenimiento*: Limpia los archivos temporales locales con más de 48 horas de antigüedad, preservando intacta la cola de Google Drive.
3. **Escenario en Make.com**:
   - **Google Drive (Watch Files)**: Monitorea la carpeta `VideosQuizazos` con `Limit: 1` ordenado por tiempo de creación.
   - **Google Drive (Download a File)**: Descarga el archivo MP4.
   - **Router de Publicación**:
     - **Canal 1 (YouTube)**: Módulo `YouTube: Upload a Video` con título `{{replace(4.Name; ".mp4"; "")}} #Shorts`.
     - **Canal 2 (Facebook)**: Módulo `Facebook Pages: Upload a Video` con título `{{replace(4.Name; ".mp4"; "")}} #Reels`.
     - **Canal 3 (TikTok)**: Módulo `Telegram Bot: Send a Video` enviando el video y texto al celular para publicación con 1 clic en TikTok.
4. **Generador Masivo (`generate_60_videos.py`)**:
   Script Python para generar lotes de 60 videos (300 trivias con IA y Edge-TTS) distribuidos en *Historia y Mitología* y *Ciencia, Espacio y Naturaleza Extrema*, con persistencia en `generation_60_state.json`.
