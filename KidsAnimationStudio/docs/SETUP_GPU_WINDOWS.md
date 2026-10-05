# Guía de Configuración GPU en Windows (NVIDIA RTX 5070 Blackwell)

Instrucciones técnicas definitivas y verificadas para desplegar el stack de inteligencia artificial de **Kids Animation Studio** en entornos Windows con hardware NVIDIA de última generación (GeForce RTX 5070 Blackwell, 12 GB VRAM).

---

## 1. Verificación de Hardware y Arquitectura

La NVIDIA GeForce RTX 5070 pertenece a la arquitectura **Blackwell** y posee Compute Capability **(12, 0)**.
Para evitar errores de kernel no soportado (`CUDA error: no kernel image is available for execution on the device`), el entorno requiere:
- **NVIDIA Driver**: 572.16 o superior.
- **CUDA Toolkit**: 12.8 o 13.0.
- **PyTorch**: 2.14.0+cu130 (o cu128 mínimo).

### Verificación desde Python:
```python
import torch
print("PyTorch Version:", torch.__version__)
print("CUDA Version:", torch.version.cuda)
print("Device Name:", torch.cuda.get_device_name(0))
print("Compute Capability:", torch.cuda.get_device_capability(0))  # Debe retornar (12, 0)
```

---

## 2. Instalación de ComfyUI Portable para NVIDIA

> [!IMPORTANT]
> **No utilizar instalaciones ZLUDA (AMD)**. En este sistema existía previamente `C:\ComfyUI` configurado con `zluda.exe` para AMD. Para la RTX 5070 se utiliza la instalación limpia de ComfyUI Portable en **`C:\ComfyUI_NV`**.

### Estructura de Directorios:
```text
C:\ComfyUI_NV\
├── ComfyUI\
│   ├── custom_nodes\
│   │   ├── ComfyUI-Manager\
│   │   ├── ComfyUI_IPAdapter_plus\
│   │   ├── ComfyUI-VideoHelperSuite\
│   │   └── ComfyUI-GGUF\
│   └── models\
│       ├── checkpoints\       <- DreamShaperXL_Turbo_V2-SFW.safetensors
│       ├── ipadapter\         <- ip-adapter-plus_sdxl_vit-h.safetensors
│       ├── clip_vision\       <- CLIP-ViT-H-14-laion2B-s32B-b79K.safetensors
│       ├── ltx_video\         <- ltxv-2b-distilled.safetensors
│       └── wan\               <- wan2.2_ti2v_5b_fp8.safetensors + vae + text_encoders
├── python_embeded\
└── run_nvidia_gpu.bat
```

### Comando de Arranque en Modo API:
```powershell
cd C:\ComfyUI_NV
.\run_nvidia_gpu.bat --listen 127.0.0.1 --port 8188 --highvram --fast
```

---

## 3. Workflows de ComfyUI Exportados (API Format)

Los workflows deben estar exportados en formato JSON API en `ai-gateway/workflows/`:
1. `character_sheet.json`: Generación de hoja de personaje en 4 ángulos con SDXL Turbo.
2. `keyframe_ipadapter.json`: Generación de keyframe individual con preservación de identidad vía IP-Adapter Plus.
3. `i2v_ltx.json`: Animación Image-to-Video de 3–5 segundos con LTX-Video 2B distilled.
4. `i2v_wan.json`: Animación Image-to-Video alternativa con Wan 2.2 TI2V-5B.

Todos los nodos de entrada y salida están identificados por su `_meta.title` canónico (`INPUT_POSITIVE`, `INPUT_NEGATIVE`, `INPUT_REFERENCE_IMAGE`, `APPLY_IPADAPTER`, `KSAMPLER`, `OUTPUT_IMAGE`, etc.), permitiendo modificar parámetros sin depender de IDs numéricos que cambian al reexportar.

---

## 4. Benchmark de Modelos en la RTX 5070

Resultados reales obtenidos y documentados en `docs/BENCHMARK_VIDEO_MODELS.md`:

| Modelo / Tarea | Resolución | Tiempo por Muestra | VRAM Pico | FPS / Rendimiento |
| :--- | :--- | :--- | :--- | :--- |
| **SDXL Turbo + IP-Adapter** | 1024×576 | **6.0 s** | ~6.2 GB | 1.32 it/s |
| **LTX-Video 2B distilled** | 768×512 (3s, 24fps) | **26.6 s** | ~8.9 GB | 1.70 it/s |
| **Wan 2.2 TI2V-5B (FP8)** | 832×480 (4s, 16fps) | **3.8 min** | ~11.1 GB | Calidad cinematográfica |
| **Kokoro-82M TTS (CPU)** | 24 kHz WAV | **<0.5 s** | 0 GB VRAM | Tiempo real instantáneo |
| **Faster-Whisper (small)** | Alineación palabra | **<0.8 s** | ~1.1 GB | Precisión <150 ms |

**Modelo de video por defecto recomendado:** **LTX-Video 2B distilled** por su velocidad extrema (clip de video en 26 s) y bajo consumo de VRAM (<9 GB).

---

## 5. Configuración de Ollama (LLM)

Instalación de Ollama en Windows:
```powershell
ollama pull qwen2.5:7b-instruct
```

Verificación del servicio:
```powershell
curl http://localhost:11434/api/tags
```
El modelo `qwen2.5:7b-instruct` corre con soporte JSON Schema nativo para garantizar que los guiones pedagógicos siempre cumplan con la estructura de escenas, planos y overlays educativos.

---

## 6. Configuración del Entorno Python en `ai-gateway`

El gateway requiere Python 3.12 (debido a compatibilidad de PyAV, OnnxRuntime y Faster-Whisper):

```powershell
cd "c:\Users\villa\GIT DESKTOP\AIO\KidsAnimationStudio\ai-gateway"
py -3.12 -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
```

### Dependencias Principales:
- `fastapi`, `uvicorn`, `httpx`, `pydantic`
- `av` (PyAV para render de Ken Burns y decodificación de video)
- `kokoro-onnx` y `soundfile` para locución infantil
- `faster-whisper` para alineación de timestamps
- `transformers`, `torch`, `torchvision` para QA de similitud CLIP
- `pytest`, `respx` para testing automático

---

## 7. Regla de Convivencia de VRAM (`GpuGuard`)

Para evitar colisiones de memoria entre Kids Animation Studio y otros procesos de IA (como SD Forge o StoryVideoGenerator):
1. **Concurrencia 1:** El orquestador ejecuta los pipelines de manera estrictamente secuencial.
2. **GpuGuard:** Antes de lanzar tareas intensivas de ComfyUI, `ai-gateway` consulta la memoria libre mediante `nvidia-smi`. Si la memoria libre es inferior a `9000 MB`, espera de forma no invasiva hasta que se libere.
3. **Purga Activa:** Tras finalizar cada etapa de generación, se invoca automáticamente el endpoint `/gpu/free` en ComfyUI para descargar los modelos de la memoria de video.
