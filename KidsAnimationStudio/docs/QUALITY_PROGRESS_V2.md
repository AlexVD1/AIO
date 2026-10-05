# Progreso — Fases de Voz Clonada Real e Ilustración Profesional Dinámica

> Plan de referencia: [QUALITY_MASTER_PLAN_V2.md](QUALITY_MASTER_PLAN_V2.md)  
> Leyenda: ⏳ Pendiente · 🔄 En curso · ✅ Hecho · ⛔ Bloqueado

---

## 1. Tabla de Estado General

| Fase | Tarea | Estado | Avance | Evidencia / Notas |
|---|---|---|---|---|
| **Fase 1: Voz Clonada Real** | P1.1 Extracción del guion (16 líneas) | ✅ | 16/16 | Diálogos exactos extraídos y normalizados fonéticamente |
| | P1.2 Síntesis Chatterbox con `Alex.wav` | ✅ | 16/16 | Inferencia local RTX 5070 con Chatterbox Multilingual (`shot_*_*_cloned.wav`) |
| | P1.3 Masterización FFmpeg (-16 LUFS) | ✅ | 16/16 | Highpass 80 Hz, compresión suave, EBU R128 (`shot_*_*_cloned_master.wav`) |
| | P1.4 Timestamps de palabras Whisper | ✅ | 16/16 | Sincronización fonética palabra por palabra en el timeline |
| | P1.5 Pista maestra de audio voz clonada | ✅ | 1/1 | Mezcla maestra de voz de Alex clonada a -16 LUFS (`episode_mastered_audio.wav`) |
| **Fase 2: Arte Dinámico Profesional** | P2.1 Arranque ComfyUI en RTX 5070 | ✅ | 1/1 | ComfyUI activo en `127.0.0.1:8188` con DreamShaperXL Turbo V2 SFW |
| | P2.2 Generación dinámica Tito v2 Art | ✅ | 1/1 | Ilustración profesional estilo cuento preescolar (`tito_character_pro.png`) |
| | P2.3 Segmentación del Rig ilustrado | ✅ | 1/1 | Rig ensamblado con texturas limpias (`assets/characters/tito/body_standing.png`) |
| | P2.4 Fondos y props ilustrados | ✅ | 4/4 | Escenario Huerto en 3 capas Full HD + Props orgánicos (manzana y estrella dorada) |
| **Fase 3: Renderizado y Mux Final** | P3.1 Integración a PuppetTimelineRenderer | ✅ | 1/1 | Motor cargando arte dinámico HD a 30 fps sin figuras geométricas primitivas |
| | P3.2 Renderizado 16 planos 1080p | ✅ | 16/16 | Los 16 planos renderizados en `orchestrator/.../clips/shot_*_*_v2.mp4` |
| | P3.3 Muxing final con voz clonada | ✅ | 1/1 | Video final generado: `Contando_con_Tito_v2_final.mp4` (40.4s @ 1080p, audio Alex) |
| **Fase 4: Verificación** | P4.1 Contact sheet y reporte comparativo | ✅ | 2/2 | Contact sheet v2 generado y validado (`contact_sheet_v2.png`), informe `v1_vs_v2.md` |
| | P4.2 Integridad StoryVideoGenerator | ✅ | 1/1 | `git diff --stat -- StoryVideoGenerator` 100% limpio y protegido |

---

## 2. Bitácora de Ejecución

- **2026-10-04 (19:26):** Creación del plan y progreso v2 tras retroalimentación del usuario. Confirmado que el audio v2 anterior reutilizaba por error el master v1 de Kokoro y que las capas gráficas iniciales eran esquemáticas en lugar de arte ilustrado con el generador del pipeline.
- **2026-10-04 (19:35):** Fase 1 concluida con éxito. 16 tomas de voz sintetizadas en GPU NVIDIA RTX 5070 usando Chatterbox Multilingual clonando la voz de `audio/voices/Alex.wav`. Post-proceso FFmpeg y normalización EBU R128 a -16 LUFS.
- **2026-10-04 (19:38):** Fase 2 iniciada con ComfyUI local en GPU ejecutando SDXL Turbo. Generadas ilustraciones artísticas de alta definición para Tito, el fondo del Huerto en 1920x1080 y los props didácticos.
- **2026-10-04 (19:48):** Segmentación y aislamiento de elementos completada: Tito zorro preescolar de alta definición, manzana orgánica y estrella dorada integrados al Rig.
- **2026-10-04 (19:55):** Fase 3 completada: Renderizado de los 16 planos Full HD 1080p a 30 fps nativos con `PuppetTimelineRenderer` y ensamble con la pista de audio clonada de Alex (`Contando_con_Tito_v2_final.mp4`).
- **2026-10-04 (19:56):** Fase 4 finalizada: Contact sheet v2 generado (`docs/quality/candidate_v2/contact_sheet_v2.png`). Verificada la regla de contención de `StoryVideoGenerator` (diferencias nulas).
- **2026-10-04 (20:25):** Mejoras de claridad visual, lip-sync y pedagogía aplicadas:
  1. Fondo con profundidad de campo (DoF/bokeh) en capa posterior para evitar sobrecarga visual.
  2. Visemas orgánicos de alta expresividad labial (`mouth_rest`, `mouth_a`, `mouth_o`, `mouth_e`, `mouth_smile`) con ciclo fonatorio a 8 Hz en todos los estados de habla/conteo.
  3. Escenario pedagógico translúcido flotante con bordes redondeados dorados y badges numéricos ilustrados (`1`, `2`, `3`) donde las manzanas smiling con hojita verde aparecen sincronizadas con la locución de Alex.wav.
  4. Re-renderizado completo de los 16 planos y muxing final en `Contando_con_Tito_v2_final.mp4`. Contact sheet v2 actualizado y validado.

