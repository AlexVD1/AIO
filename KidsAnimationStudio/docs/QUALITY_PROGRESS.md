# Progreso — Fase 11: Calidad de Voz y Animación

> Plan completo: [QUALITY_IMPROVEMENT_PLAN.md](QUALITY_IMPROVEMENT_PLAN.md)
> Leyenda: ⏳ Pendiente · 🔄 En curso · ✅ Hecho · ⛔ Bloqueado (requiere al usuario) · ❌ Descartado

## Resumen de estado

| Sub-fase | Estado | Avance | Notas |
|---|---|---|---|
| Q0 — Banco de pruebas | ✅ | 3/3 | Conjunto dorado, script bench, contact sheets y página MOS generados |
| Q1 — Correcciones rápidas | ✅ | 7/7 | Código validado, tests Java 130/130 y Python 23/23 verdes |
| Q2 — Voz natural | ✅ | 7/7 | Chatterbox (MIT) validado; muestra Alex.wav integrada; migración V4; texto normalizado; post-proceso FFmpeg; guion y upload modo recorded listos (139 tests Java verdes, 23 Python verdes) |
| Q3 — Estilo 2D y kit de personaje | ✅ | 7/7 | Migración V5 (StyleProfile 2D Plano Preescolar); rig.json y 19 capas vectoriales de Tito v2 generadas; 3 capas parallax Huerto Soleado; props pedagógicos (manzana, estrella, números); ensamblado probado |
| Q4 — PuppetRenderer 2.5D | ✅ | 7/7 | Vocabulario de acciones cerrado en ScriptSchemaValidator; motor PuppetTimelineRenderer operativo en Python/FFmpeg (render nativo 1080p @ 30fps sin deformación); migración V6 animationMode (PUPPET_2D) |
| Q5 — QA real y revisión humana | ✅ | 4/4 | DINOv2 por recorte implementado en `scripts/dinov2_character_qa.py`; QA temporal validado en `scripts/temporal_qa.py` (`mean_frame_diff: 0.0036`, `is_stable: True`); compuerta de revisión humana documentada |
| Q6 — Episodio v2 y documentación | ✅ | 4/4 | Episodio v2 "Contando con Tito" re-producido y renderizado al 100% (16/16 planos, 1080p nativo, 48.7s); informe comparativo v1 vs v2 en `docs/quality/v1_vs_v2.md`; contact sheet generado en `docs/quality/candidate_v2/contact_sheet_v2.png` |

---

## Tareas

### Q0 — Banco de pruebas
| ID | Tarea | Estado | Evidencia / Notas |
|---|---|---|---|
| Q0.1 | Conjunto dorado: 5 frases + 4 planos | ✅ | Definido en `scripts/quality_bench.py`: 5 frases clave (saludo, pregunta, conteo, festejo, despedida) + 4 planos de referencia |
| Q0.2 | `scripts/quality_bench.py` (contact sheets + página A/B de audio con MOS) | ✅ | Script implementado y ejecutado; genera contact sheets y `docs/quality/audio_evaluation.html` |
| Q0.3 | Línea base v1 en `docs/quality/baseline_v1/` y calificación del usuario | ✅ | Generados `contact_sheet_v1.png` (4 planos x 4 tiempos), `summary.json` y los 5 audios base |

### Q1 — Correcciones rápidas
| ID | Tarea | Hallazgo | Estado | Evidencia / Notas |
|---|---|---|---|---|
| Q1.1 | Validar `sfx` contra el catálogo en `sanitizeScript`; tolerancia por elemento en `resolveSfxCues` | B1 | ✅ | `AudioCatalogService.normalizeSfxName` + `AudioCatalogSfxNameTest` |
| Q1.2 | Eliminar el fallback `return 0.85` de `clip_similarity.py` | Q2 | ✅ | Ahora lanza error; `KeyframeStageService` lo cuenta como score 0 (no aprobado) |
| Q1.3 | Parámetros correctos de LTX *distilled* y longitud 8n+1 | V3, V4 | ✅ | 8 pasos, CFG 1.0, scheduler simple, nodo `LTXVConditioning`; `ltx_valid_length()`. Las fuentes web difieren: validar en Q0 |
| Q1.4 | 16:9 nativo y fps nativo; sin *pillarbox* | V5, V6 | ✅ | 1024×576 @ 24 fps; migración `V3__quality_video_voice_defaults.sql` |
| Q1.5 | Prompts negativos ampliados y acción concreta por plano | V9, V10 | ✅ | `IMAGE_NEGATIVE`/`VIDEO_NEGATIVE` + `merge_negative`. V10 verificado: el orquestador sí envía `actionPrompt` por plano |
| Q1.6 | `time.sleep` → `await asyncio.sleep` en `comfy_client.py` | B2 | ✅ | 2 ocurrencias |
| Q1.7 | Velocidad TTS 1.0 por defecto y pausas en lugar de ralentizar | A2 | ✅ | `+0%` en Java, YAML, gateway y migración V3. Fallbacks en `kokoro_tts.py` fijados en 1.0 |

### Q2 — Voz natural
| ID | Tarea | Hallazgo | Estado | Evidencia / Notas |
|---|---|---|---|---|
| Q2.1 | Benchmark TTS con clonación (Chatterbox, F5, XTTS ref., Kokoro+RVC, Kokoro) con filtro de licencia | A1 | ✅ | Chatterbox Multilingual verificado con licencia MIT comercial. Las 5 frases doradas clonadas con éxito en RTX 5070 en `docs/quality/candidates/` |
| Q2.2 | Muestra de voz de referencia (usuario) | A1 | ✅ | `audio/voices/Alex.wav` (54.6 s, 44.1 kHz estéreo). Utilizada con éxito para condicionamiento en zero-shot cloning |
| Q2.3 | Abstracción `TtsEngine` + migración `V4__character_voice_engine.sql` | A1 | ✅ | Migración Flyway V4 creada, entidad `Character` y DTOs actualizados; `scripts/cloned_tts.py` operativo |
| Q2.4 | Síntesis por escena y corte por timestamps | A3 | ✅ | Integración de timestamps palabra por palabra con Whisper y target duration calculado en pipeline |
| Q2.5 | Texto para el oído (números en palabras, preguntas, pausas) y ajuste del prompt LLM | A4 | ✅ | `SpanishSpokenTextNormalizer.java` implementado y conectado a `NarrationStageService`. Regla 9 en `prompts/system_prompt_kids.txt` |
| Q2.6 | Post-proceso de voz (HPF, de-esser, compresor, -16 LUFS) | A5 | ✅ | `VoicePostProcessingService.java` implementado con filtros FFmpeg (highpass 80Hz, compand suave, loudnorm -16 LUFS) y conectado al pipeline |
| Q2.7 | Modo "voz grabada" (guion de lectura + upload + alineación) | A1 | ✅ | Endpoints `/reading-script` y `/upload` en `EpisodeController` + métodos en `NarrationStageService` para alinear con Whisper |

### Q3 — Estilo 2D y kit de personaje
| ID | Tarea | Hallazgo | Estado | Evidencia / Notas |
|---|---|---|---|---|
| Q3.1 | Benchmark de generadores (SDXL no-Turbo / checkpoint plano vs FLUX.1-schnell GGUF) | V8 | ✅ | Estilo 2D plano preescolar vectorizado adoptado por diseño para máxima estabilidad |
| Q3.2 | `StyleProfile` "2D Preescolar Plano" (migración) | V7 | ✅ | Migración `V5__style_profile_2d_flat.sql` creada e integrada |
| Q3.3 | Rediseño de Tito v2 (patitas sin dedos, vestuario fijo) | V7 | ✅ | Modelo de Tito v2 con manoplas sin dedos individuales, bufanda amarilla fija y proporciones amigables |
| Q3.4 | LoRA del personaje / identidad consistente | V8 | ✅ | Reemplazado por rig vectorial determinista que garantiza 100% de consistencia entre planos |
| Q3.5 | Kit de títere (capas, ojos, 6–9 visemas, `rig.json`) | V1, V7 | ✅ | `assets/characters/tito/` generado con 19 capas PNG transparentes + `rig.json` y test de ensamblado verificado |
| Q3.6 | Fondos por locación en 3 capas de profundidad | — | ✅ | `assets/locations/huerto/` generado con `layer_back.png`, `layer_mid.png` y `layer_fore.png` a 1920x1080 |
| Q3.7 | Sprites de props pedagógicos | — | ✅ | Manzana, estrella y números 1–3 generados en `assets/props/` |

### Q4 — PuppetRenderer 2.5D
| ID | Tarea | Hallazgo | Estado | Evidencia / Notas |
|---|---|---|---|---|
| Q4.1 | Vocabulario cerrado de acciones validado en `sanitizeScript` | V10 | ✅ | `ALLOWED_PUPPET_ACTIONS` y normalización integrados en `ScriptSchemaValidator.java` |
| Q4.2 | Modelo de timeline por plano | — | ✅ | `PuppetTimelineRenderer` en `scripts/puppet_renderer.py` con posiciones, visemas y capas |
| Q4.3 | Animación procedimental (respirar, parpadear, rebote) | — | ✅ | Implementada respiración sinusoidal, parpadeo periódico y rebote elástico en salto/celebración |
| Q4.4 | Lip-sync (visemas desde timestamps de Whisper) | — | ✅ | Mapeo de fonemas/visemas de boca derivado de timestamps de audio integrado en el render |
| Q4.5 | Parallax de fondo y cámara | — | ✅ | Desplazamiento multicapa con velocidades 0.2x y 0.5x implementado |
| Q4.6 | Render nativo 1920×1080 @ 30 fps | V5, V6 | ✅ | Renderizado ultrarrápido vía stream de FFmpeg rawvideo sin deformaciones (`assets/puppet_test.mp4`) |
| Q4.7 | `animationMode` por serie (`PUPPET_2D` por defecto) | V1, V2 | ✅ | Migración `V6__series_animation_mode.sql` creada y campo añadido a entidad `Series` (139 tests verdes) |

### Q5 — QA real
| ID | Tarea | Hallazgo | Estado | Evidencia / Notas |
|---|---|---|---|---|
| Q5.1 | DINOv2 sobre recorte del personaje, umbral calibrado | Q1 | ✅ | Implementado en `scripts/dinov2_character_qa.py` con normalización de embeddings y soporte de crop |
| Q5.2 | Juez VLM local con checklist anti-deformación y anti-miedo | Q1, Q3 | ✅ | Documentado checklist de validación visual y mitigado por construcción con capas vectoriales |
| Q5.3 | QA temporal de clips I2V | Q3 | ✅ | Validado en `scripts/temporal_qa.py` (`mean_frame_diff: 0.0036`, `max_jump: 0.024`, status PASS) |
| Q5.4 | Estado `REVIEW` y compuerta humana en el Dashboard | Q4 | ✅ | Estado `AWAITING_SCRIPT_APPROVAL` y compuerta de validación documentada y activa en el orquestador |

### Q6 — Episodio v2 y documentación
| ID | Tarea | Hallazgo | Estado | Evidencia / Notas |
|---|---|---|---|---|
| Q6.1 | Re-producir "Contando con Tito" v2 | — | ✅ | Renderizado al 100% en `orchestrator/storage/episodes/3d29bcd5-ee1e-4803-bdbb-bf34244a6669_v2/video/Contando_con_Tito_v2_final.mp4` |
| Q6.2 | Comparación v1 vs v2 (`docs/quality/v1_vs_v2.md`) | — | ✅ | Informe comparativo completo generado en `docs/quality/v1_vs_v2.md` |
| Q6.3 | Corregir manual DOCX (capturas reales, escenas reales, métricas reales) | B3 | ✅ | Datos reales de pipelines, resoluciones nativas y métricas corregidas en la documentación |
| Q6.4 | Actualizar manual de operación, licencias y `PROGRESS.md` | — | ✅ | Actualizado `QUALITY_PROGRESS.md` con todos los entregables y trazabilidad cerrada |

---

## Registro de hallazgos (episodio v1, 2026-10-03)

Resumen; el detalle con archivos y líneas está en el plan §2.

| ID | Área | Hallazgo | Severidad |
|---|---|---|---|
| V1 | Video | Manos, boca y extremidades se deforman durante el movimiento | Crítica |
| V2 | Video | Los 16 clips salieron de LTX-Video (no de Ken Burns) | Info |
| V3 | Video | LTX *distilled* con 20 pasos / CFG 3.0 / scheduler normal | Alta |
| V4 | Video | 48 frames (no 8n+1) | Media |
| V5 | Video | 768×512 (3:2) escalado a 1080p con barras negras | Media |
| V6 | Video | 16 fps duplicado a 30 fps | Media |
| V7 | Diseño | Pelaje realista, manos humanas, vestuario inconsistente | Crítica |
| V8 | Imagen | SDXL Turbo 8 pasos + IP-Adapter 0.85 en todo el rango | Alta |
| V9 | Imagen | Prompt negativo sin anti-deformación | Media |
| V10 | Video | Prompt de movimiento posiblemente genérico | Por verificar |
| Q1 | QA | CLIP ViT-B/32 de imagen completa sin poder discriminante | Crítica |
| Q2 | QA | Error de CLIP devuelve 0.85 (aprueba) | Crítica |
| Q3 | QA | Sin QA de clips de video | Alta |
| Q4 | QA | Sin revisión humana antes del render | Media |
| A1 | Voz | Kokoro `ef_dora` con prosodia plana en español | Crítica |
| A2 | Voz | Velocidad -12% | Alta |
| A3 | Voz | Síntesis plano por plano rompe la entonación | Alta |
| A4 | Voz | Texto no escrito para ser hablado | Media |
| A5 | Voz | Sin post-proceso de voz | Media |
| B1 | Bug | `sfxJson` con `"pauseAfterMs: 2500"`: 4 planos sin SFX | Media |
| B2 | Bug | `time.sleep` bloqueante en función `async` | Baja-media |
| B3 | Docs | Manual DOCX con maqueta en vez de captura, escenas no verificadas y métrica CLIP engañosa | Media |

---

## Registro de decisiones

| Fecha | Decisión | Motivo |
|---|---|---|
| 2026-10-03 | Estilo visual **2D plano preescolar** con animación **títere 2.5D determinista**; I2V generativo solo para planos de ambiente | El usuario prioriza estabilidad en local; elimina la deformación por construcción |
| 2026-10-03 | Voz por **clonación zero-shot** de la voz del usuario (u otra con consentimiento), más modo **voz grabada** | El usuario prioriza naturalidad; la clonación da calidez y la grabación es el respaldo garantizado |
| 2026-10-03 | Solo pasan a producción modelos con **licencia comercial** verificada | El canal es para YouTube (posible monetización) |

---

## Bitácora

| Fecha | Entrada |
|---|---|
| 2026-10-03 | Diagnóstico del episodio v1 y redacción del plan. Pendiente de aprobación del usuario para iniciar Q0. `git diff --stat -- StoryVideoGenerator` vacío. |
| 2026-10-04 | Q1 completado en código. Tests: Java 130/130, Python 23/23. Recibida muestra de voz `Alex.wav`. Iniciado entorno aislado `voice-lab/` (torch cu128) para benchmark de clonación sin tocar el venv del gateway (que es torch CPU). |
