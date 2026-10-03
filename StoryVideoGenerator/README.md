# 🎬 StoryVideoGenerator

Sistema automatizado e independiente para transformar ideas y configuraciones narrativas en **videos verticales terminados (9:16, 1080×1920)** con edición dinámica, subtítulos de alto impacto, música y efectos sonoros para **TikTok**, **YouTube Shorts** e **Instagram Reels**.

---

## 🌟 Características Principales

- **100% Desacoplado e Independiente**: Código, base de datos (`storydb` en puerto 5433), contenedores y pipeline completamente autónomos.
- **Generación Visual Gratuita en GPU Local**: Integrado con **Stable Diffusion WebUI Forge API** aprovechando tu tarjeta **NVIDIA RTX 5070 (12GB VRAM)**. Costo de imágenes: **$0.00**. Incluye fallback automático a fondos visuales de prueba cuando Forge no está activo.
- **Locución Neural de Alta Fidelidad**: Síntesis en español e idiomas multilingües mediante **Edge-TTS** (`es-MX-JorgeNeural`, `es-ES-AlvaroNeural`, etc.) con cálculo exacto de tiempos de escena vía `ffprobe`.
- **Edición Dinámica y Composición FFmpeg (1080×1920)**:
  - Movimientos de cámara procedurales: `SLOW_ZOOM_IN`, `SLOW_ZOOM_OUT`, `PAN_LEFT`, `PAN_RIGHT`, `KEN_BURNS`, `PARALLAX` y `STATIC`.
  - Efectos visuales de atmósfera por género: `vignette`, `noise`, `rgbashift`, `boxblur`.
  - Subtítulos dinámicos de alta retención: Tipografía bold de alto contraste (amarillo con borde negro `&H0000FFFF`) renderizados a alta velocidad vía DirectWrite en Windows.
  - Mezcla de audio inteligente: Ducking dinámico `volume='if(between(...),0.05,base)':eval=frame`, BGM looping, efectos de sonido (SFX) sincronizados y limitador maestro (`alimiter=0.95`).
- **Deduplicación Semántica e Histórica**: Previene títulos, premisas, finales repetidos y repartos clonados combinando hash SHA-256, distancia Levenshtein ($> 0.75$), similitud coseno de n-gramas ($> 0.85$) y solapamiento Jaccard.
- **Procesamiento Batch (Por Lotes)**: Generación desatendida de 1 a 50 videos por lote con aislamiento total de fallos, control de concurrencia y reintentos selectivos.
- **Exportación Desacoplada y Make.com**: Exporta el par `[Video].mp4` y `[Video]_metadata.json` con hashtags automáticos organizados para ingesta directa de Make.com.
- **Limpieza Automática y Monitoreo**: Purga programada a las 03:00 AM para archivos de más de 7 días y alertas de espacio en disco (< 1 GB).

---

## 🛠️ Stack Tecnológico

| Componente | Tecnología |
|---|---|
| Lenguaje | **Java 21 LTS** |
| Framework Backend | **Spring Boot 3.3.0** |
| Base de Datos | **PostgreSQL 16** con **Flyway (Migraciones V1 a V9)** |
| Generación de Imágenes | **Stable Diffusion WebUI Forge (SDXL Juggernaut XL v9)** |
| Inteligencia Narrativa | **Google Gemini (Structured Outputs JSON)** |
| Locución Neural (TTS) | **Python 3 + Edge-TTS** |
| Composición Audiovisual | **FFmpeg 8.x con DirectWrite / libass** |
| Contenedores | **Podman Compose** / Docker |

---

## 🚀 Inicio Rápido (3 Pasos)

Para una guía detallada paso a paso, consulta el [**Manual de Operación**](file:///c:/Users/villa/GIT%20DESKTOP/AIO/StoryVideoGenerator/docs/MANUAL_DE_OPERACION.md).

### 1. Iniciar la Base de Datos
```powershell
podman compose up -d postgres
```

### 2. Configurar Entorno (`.env`)
```powershell
Copy-Item .env.example .env
# Agregar tu AI_API_KEY de Google AI Studio en el archivo .env
```

### 3. Ejecutar la Aplicación
```powershell
mvn spring-boot:run
```
- **API disponible en**: `http://localhost:8081`
- **Swagger UI interactivo**: `http://localhost:8081/swagger-ui.html`
- **Health Check**: `http://localhost:8081/api/v1/health`

---

## 📡 Referencia de la API REST

### Pipeline y Generación
| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/v1/pipeline/execute` | Ejecuta el pipeline completo de punta a punta (retorna `202 Accepted`) |
| `GET` | `/api/v1/pipeline/status/{id}` | Consulta el estado y avance porcentual (0% a 100%) |
| `POST` | `/api/v1/stories/generate` | Genera y desglosa la historia estructurada |
| `POST` | `/api/v1/stories/{id}/generate-images` | Genera imágenes locales para la historia |
| `POST` | `/api/v1/stories/{id}/generate-audio` | Sintetiza la narración neural |
| `POST` | `/api/v1/stories/{id}/render` | Compone y renderiza el video con FFmpeg |
| `POST` | `/api/v1/stories/{id}/export` | Exporta a `./export_videos/` con `metadata.json` |

### Control Granular y Regeneración
| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/v1/stories/{id}/scenes/{sceneId}/regenerate-image` | Regenera únicamente la imagen de una escena |
| `POST` | `/api/v1/stories/{id}/scenes/{sceneId}/regenerate-audio` | Regenera la locución de una escena |
| `POST` | `/api/v1/stories/{id}/rerender` | Re-renderiza video sin recalcular assets |
| `POST` | `/api/v1/stories/{id}/retry` | Reintenta una historia fallida |

### Procesamiento Batch
| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/v1/stories/batch` | Encola un lote masivo con distribución por género |
| `GET` | `/api/v1/batches/{id}/status` | Consulta el progreso agregado y detalle por historia |
| `POST` | `/api/v1/batches/{id}/retry-failed` | Reejecuta únicamente las historias fallidas del lote |

### Descargas y Catálogos
| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/v1/stories/{id}/video/download` | Descarga directa del archivo MP4 final |
| `GET` | `/api/v1/stories/{id}/video/metadata` | Metadatos y hashtags generados |
| `GET` | `/api/v1/catalogs/genres` | Lista de géneros narrativos admitidos |
| `GET` | `/api/v1/catalogs/visual-styles` | Lista de estilos visuales preconfigurados |
| `GET` | `/api/v1/stats` | Estadísticas del sistema y espacio en disco |

---

## 🧪 Pruebas Automatizadas

El proyecto cuenta con una cobertura integral de pruebas unitarias, de integración y de endpoints:

```powershell
mvn test
```

```
[INFO] Results:
[INFO] 
[INFO] Tests run: 79, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## 📚 Documentación Adicional

- [**Manual de Operación Completo**](file:///c:/Users/villa/GIT%20DESKTOP/AIO/StoryVideoGenerator/docs/MANUAL_DE_OPERACION.md): Guía detallada para poner en marcha el sistema con tu GPU y generar videos.
- [**Guía de Configuración en Make.com con Google Drive**](file:///c:/Users/villa/GIT%20DESKTOP/AIO/StoryVideoGenerator/docs/GUIA_CONFIGURACION_MAKE_DRIVE.md): Emparejamiento automático de MP4 + JSON, filtros y Blueprint importable en Make.com.
- [**Guía de Integración con Make.com**](file:///c:/Users/villa/GIT%20DESKTOP/AIO/StoryVideoGenerator/docs/MAKE_INTEGRATION.md): Arquitectura de publicación desacoplada en redes sociales.
