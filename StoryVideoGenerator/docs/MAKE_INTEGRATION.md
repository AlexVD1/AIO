# Guía de Integración con Make.com — StoryVideoGenerator

## 1. Filosofía de Desacoplamiento

El sistema **StoryVideoGenerator** está diseñado bajo el principio de **desacoplamiento total**:
- **StoryVideoGenerator** se encarga exclusivamente de:
  - Generación de historias, personajes y escenas (IA).
  - Generación de imágenes 100% locales en GPU (Stable Diffusion WebUI Forge).
  - Síntesis de narración de alta fidelidad (Edge-TTS).
  - Edición dinámica, subtitulado de alto impacto y composición de video vertical (FFmpeg 1080×1920).
  - Exportación de archivos finales MP4 con un archivo `_metadata.json` enriquecido.
- **Make.com** (o cualquier automatizador externo) se encarga de:
  - Detectar nuevos videos exportados.
  - Leer el archivo `metadata.json` asociado.
  - Distribuir y publicar el video en **TikTok**, **YouTube Shorts**, **Instagram Reels**, **Facebook Reels**, etc.
  - Manejar autenticación OAuth2 de plataformas sociales, límites de publicación diarios y programación por horarios.

---

## 2. Estructura de Exportación

Cada video exportado genera dos archivos gemelos en el directorio configurado (`./export_videos/` por defecto):

```
export_videos/
├── La_Habitacion_404.mp4
└── La_Habitacion_404_metadata.json
```

### Contenido de `metadata.json`

```json
{
  "storyId": "3fa8ede8-66ee-432f-9174-056e931a2b5c",
  "title": "La Habitación 404",
  "description": "Un guardia nocturno investiga ruidos en el ala cerrada y descubre una puerta que no estaba en los planos...",
  "hashtags": [
    "#horror",
    "#terror",
    "#creepy",
    "#miedo",
    "#hospital",
    "#abandonado",
    "#storytime",
    "#historias",
    "#relatos",
    "#cuentos",
    "#fyp",
    "#viral",
    "#parati"
  ],
  "genre": "HORROR",
  "category": "Entertainment",
  "language": "es-MX",
  "duration_seconds": 88.5,
  "resolution": "1080x1920",
  "format": "mp4",
  "codec_video": "h264",
  "codec_audio": "aac",
  "fps": 30,
  "created_at": "2026-10-02T15:30:00",
  "video_file": "La_Habitacion_404.mp4",
  "scene_count": 5,
  "has_subtitles": true,
  "has_narration": true,
  "has_music": true,
  "tts_voice": "es-MX-JorgeNeural"
}
```

---

## 3. Escenarios de Make.com Recomendados

### Escenario A: Watch Files (Google Drive / Dropbox / Carpeta Local Compartida)

1. **Módulo 1: Watch Files / Folder**
   - Monitorea la carpeta `export_videos/` buscando archivos que terminen en `_metadata.json`.
2. **Módulo 2: Read JSON File**
   - Parsea el contenido de `metadata.json`.
3. **Módulo 3: Download Video File**
   - Descarga o referencia el archivo `.mp4` con el nombre indicado en `video_file`.
4. **Módulo 4: Publicar en TikTok / YouTube**
   - **Título**: `{{title}}`
   - **Descripción**: `{{description}} {{join(hashtags, " ")}}`
   - **Video**: Binary data del MP4.
5. **Módulo 5 (Opcional): Notificación**
   - Envía notificación por Telegram/Discord indicando éxito de la publicación.

### Escenario B: Webhook / Polling API

1. **Módulo 1: HTTP Polling (cada 30 min)**
   - `GET http://<host>:8081/api/v1/stories?status=EXPORTED`
2. **Módulo 2: HTTP Download**
   - `GET http://<host>:8081/api/v1/stories/{{storyId}}/video/download`
3. **Módulo 3: Publicación en Redes Sociales**

---

## 4. Retención y Limpieza Automática

El sistema incluye `AutoCleanupService`:
- Se ejecuta automáticamente cada noche a las 03:00 AM (`0 0 3 * * ?`).
- Limpia archivos exportados y temporales con antigüedad superior a 7 días (configurable con `story.cleanup.retention-days`).
- Monitorea el espacio libre en disco alertando si desciende de 1 GB (`story.cleanup.min-free-disk-mb`).
