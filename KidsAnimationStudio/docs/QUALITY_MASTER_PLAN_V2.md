# Plan Maestro de Excelencia: Voz Clonada Real y Generación Dinámica de Arte Profesional

> **Objetivo:** Superar la calidad inicial sustituyendo los audios sintéticos de voz robótica por la voz clonada natural del usuario (`Alex.wav`) en el 100% de las pistas, y elevar las ilustraciones y animaciones a nivel de producción infantil profesional (*Bluey* / *Peppa Pig* / *Pocoyó*), generadas dinámicamente mediante el pipeline de ComfyUI (SDXL + IP-Adapter) sobre la RTX 5070, eliminando formas geométricas primitivas.

---

## 1. Diagnóstico del Estado Actual

1. **Voz Sintética Residual:**
   - La prueba previa de ensamble `render_episode_v2.py` utilizó el track de audio del video v1 (`3d29bcd5...final.mp4`), conservando la voz previa de Kokoro `ef_dora` a -12% de velocidad.
   - **Solución comprobada:** El módulo `voice-lab` con **Chatterbox Multilingual** (licencia MIT) ya fue probado y genera audio con la voz del usuario en ~4.4 segundos por frase con excelente calidez y naturalidad.
2. **Calidad Visual de Ilustración y Personaje:**
   - El títere temporal generado con `scripts/generate_tito_rig_layers.py` utilizó primitivas vectoriales de Pillow (`ImageDraw.ellipse/polygon`), resultando en figuras planas encimadas sin profundidad ni expresión profesional.
   - **Solución profesional:** Los keyframes y escenarios deben generarse dinámicamente a través de **ComfyUI** con el checkpoint `DreamShaperXL_Turbo_V2-SFW.safetensors` configurado con prompts de ilustración editorial infantil de alta gama (*vibrant storybook illustration, soft warm cel-shading, expressive big eyes, velvety stylized fur, whimsical clean background*), y segmentación de elementos limpios.

---

## 2. Fases de Implementación

### Fase 1: Síntesis Completa de Voz Clonada (Alex.wav) para el Episodio
- [ ] **P1.1 Extracción del Guion Completo:** Extraer las 16 líneas de diálogo del episodio `3d29bcd5-ee1e-4803-bdbb-bf34244a6669`.
- [ ] **P1.2 Generación con Chatterbox CLI:** Ejecutar `scripts/cloned_tts.py` para cada uno de los 16 planos utilizando el archivo de referencia `audio/voices/Alex.wav` en la RTX 5070.
- [ ] **P1.3 Masterización Acústica:** Aplicar `VoicePostProcessingService` (filtro highpass 80 Hz, compresión multibanda suave y normalización EBU R128 a -16 LUFS).
- [ ] **P1.4 Timestamps Dinámicos:** Alinear cada frase con Faster-Whisper para obtener los marcadores temporales exactos de palabras y fonemas.
- [ ] **P1.5 Mezcla Musical BGM:** Combinar la voz procesada con la música de fondo `playful` a -14 LUFS para obtener el master de audio final `episode_v2_voice_master.wav`.

### Fase 2: Generación Dinámica de Arte Profesional (SDXL Cartoon)
- [ ] **P2.1 Arranque y Enlace de ComfyUI:** Iniciar ComfyUI en modo headless (`ComfyUI_windows_portable/ComfyUI/main.py`) sobre el puerto 8188 aprovechando la RTX 5070 y sus modelos locales.
- [ ] **P2.2 Generación de Arte de Tito Profesional:** Sintetizar el character sheet de Tito v2 con estilo cartoon preescolar pulido (sin dedos individuales, pelaje estilizado cálido, bufanda suave, ojos expresivos brillantes).
- [ ] **P2.3 Segmentación del Rig de Alta Calidad:** Extraer las partes del personaje con alfa limpio (cabeza, torso, brazos, colita, ojos y bocas) para mantener fidelidad y cero deformaciones.
- [ ] **P2.4 Generación Dinámica de Escenarios y Props:** Generar fondos ilustrados multicapa del huerto (cielo matutino, colinas con textura suave, árboles frutales detallados estilo acuarela digital) y props de manzana/estrellas ilustradas profesionalmente.

### Fase 3: Integración del PuppetRenderer Dinámico y Mux Final
- [ ] **P3.1 Carga Dinámica en PuppetTimelineRenderer:** Conectar las nuevas capas ilustradas en alta resolución al motor 2.5D.
- [ ] **P3.2 Renderizado de los 16 Planos con Lip-Sync Real:** Renderizar los 16 clips sincronizados con los timestamps de la voz clonada.
- [ ] **P3.3 Mux Final y Overlays Karaoke:** Unir el video Full HD 1080p @ 30 fps con el audio de voz clonada y subtítulos karaoke estilizados.

### Fase 4: Validación de Calidad y Entregables
- [ ] **P4.1 Verificación Auditiva y Visual:** Generar contact sheet actualizado y reporte MOS.
- [ ] **P4.2 Aislamiento de Repositorio:** Confirmar que `StoryVideoGenerator` permanece 100% inalterado.
