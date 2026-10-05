# Benchmark de Modelos Locales — KidsAnimationStudio (Fase 0)

Fecha: 03/10/2026  
Hardware de prueba: **NVIDIA GeForce RTX 5070 (12 GB VRAM GDDR7, Blackwell sm_120)**  
RAM del sistema: 32 GB  
Entorno GPU: PyTorch 2.14.0+cu130, CUDA 13.0, ComfyUI v0.38.0 (Portable NVIDIA en `C:\ComfyUI_NV`)

---

## 1. Resumen Ejecutivo y Modelo Recomendado

| Categoría | Modelo Recomendado | Alternativa | Motivo de la Elección |
| :--- | :--- | :--- | :--- |
| **Generación de Imagen (Keyframe)** | **DreamShaperXL Turbo v2** + IP-Adapter Plus SDXL | Flux.1 Schnell | Genera en **6 segundos** (8 steps a 1.32 it/s) con excelente estética 3D cartoon. |
| **Animación (Image-to-Video)** | **LTX-Video 2B (0.9.8 distilled)** | Wan 2.2 TI2V-5B | Genera un clip animado de 3.06 s en **26.6 segundos totales** (11 s de sampling a 1.70 it/s). Consumo ligero (~6 GB VRAM). |
| **Voz Infantil (TTS)** | **Kokoro-82M (`ef_dora` / `em_alex`)** | Edge-TTS (fallback) | 100% local, genera 4.3 s de audio en **menos de 0.5 segundos** a 24 kHz con dicción impecable en español. |
| **Guion (LLM)** | **Qwen 2.5:7b-instruct (Ollama)** | Llama 3.1:8b | Excelente seguimiento de JSON estructurado en español infantil. |

---

## 2. Resultados Detallados de las Pruebas

### A. Prueba de Generación de Imagen (SDXL Turbo)
* **Workflow:** `character_sheet.json` / `keyframe_ipadapter.json`
* **Resolución:** 768×768 (batch 1) y 1024×576
* **Sampler:** `dpmpp_sde` + `karras`, 8 pasos, CFG 2.0
* **Tiempo de carga inicial del modelo:** ~35 s
* **Tiempo de inferencia (sampling):** **6.08 segundos** (1.32 it/s)
* **VRAM utilizada:** ~4.9 GB en carga dinámica (libera con `POST /free`).
* **Calidad:** Excelente definición del personaje estilo cartoon/Pixar 3D.

### B. Prueba de Animación (LTX-Video 2B Distilled)
* **Workflow:** `i2v_ltx.json`
* **Resolución:** 768×512, 49 frames (3.06 segundos a 16 fps)
* **Sampler:** `euler` + `normal`, 20 pasos, CFG 3.0
* **Tiempo total de ejecución:** **26.66 segundos**
* **Tiempo de sampling puro:** **11.5 segundos** (1.70 it/s)
* **VRAM pico:** ~6.2 GB VRAM
* **Archivo resultante:** `ltx_video_clip_00001.mp4` (321 KB, H.264/yuv420p). Movimiento suave de cámara y animación del personaje.

### C. Prueba de Wan 2.2 TI2V-5B
* **Archivos descargados y validados:** `wan2.2_ti2v_5B_fp16.safetensors` (9.31 GB), `umt5_xxl_fp8_e4m3fn_scaled.safetensors` (6.27 GB), `wan2.2_vae.safetensors` (1.31 GB).
* **Veredicto:** El modelo está integrado en `i2v_wan.json` como opción de alta calidad para renderizado batch nocturno, pero para iteración rápida y costo-eficiencia en 12 GB, **LTX-Video es el estándar por defecto del pipeline** debido a sus 26 segundos por clip.

### D. Prueba de Voz Kokoro-82M en Español
* **Texto probado:** *"¡Hola amiguitos! Bienvenidos a nuestra aventura para aprender a contar."*
* **Voz:** `ef_dora` (español femenino infantil/narrador, velocidad 0.9)
* **Duración audio:** 4.325 segundos (`test_speech.wav`, 207 KB a 24000 Hz)
* **Tiempo de síntesis:** ~0.42 segundos (tiempo real 10x más rápido)
* **Dependencias:** Python 3.12 con `espeak-ng` y `truststore` (para certificados SSL en Windows).

---

## 3. Estimación de Tiempo de Producción para un Episodio Completo

Para un episodio típico infantil de 2 minutos (~24 planos de 5 segundos cada uno):
1. **Guion (Ollama):** ~10 segundos.
2. **Audio de 24 escenas (Kokoro):** ~15 segundos.
3. **24 Keyframes con IP-Adapter (SDXL Turbo):** ~24 × 7 s = ~2.8 minutos.
4. **24 Clips de Video (LTX-Video 2B):** ~24 × 27 s = ~10.8 minutos.
5. **Composición y Render (FFmpeg + Subtítulos + BGM/SFX):** ~30 segundos.
* **Tiempo Total Estimado por Episodio:** **~14 a 16 minutos** de principio a fin, 100% automatizado y local en la RTX 5070.
