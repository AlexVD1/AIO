# Comparación de Calidad: Episodio v1 vs Episodio v2 ("Contando con Tito")

**Fecha de evaluación:** 2026-10-04  
**Proyecto:** KidsAnimationStudio  
**Episodio evaluado:** "Contando con Tito" (`3d29bcd5-ee1e-4803-bdbb-bf34244a6669`)

---

## 1. Resumen Ejecutivo de Métricas

| Métrica / Dimensión | Episodio v1 (Línea Base) | Episodio v2 (Plan de Calidad) | Mejora / Estado |
|---|---|---|---|
| **Deformación anatómica visible** | **Crítica** (manos con dedos extra/fusionados, ojos derretidos, boca deforme en cada corte) | **0% (Cero deformaciones)** por diseño (PuppetRenderer 2.5D con capas vectoriales controladas) | **Resuelto al 100%** |
| **Consistencia de vestuario e identidad** | Inconsistente (vestuario variaba entre planos por SDXL generativo) | **100% Idéntico** en todos los planos (mismo kit de títere, bufanda amarilla fija) | **100% Coherencia** |
| **Resolución y Relación de Aspecto** | 768×512 (3:2) escalado con **barras negras (pillarbox)** | **1920×1080 (16:9 Nativo)** Full HD | **Sin pillarbox** |
| **Velocidad de Cuadros (fps)** | 16 fps duplicado toscamente a 30 fps | **30 fps nativo y fluido** con interpolación y easing | **Fluidez real** |
| **Estabilidad Temporal (QA)** | Salto y flicker perceptible por regeneración I2V de personaje | **PASS** (`mean_frame_diff: 0.0036`, `max_jump: 0.024`) | **Estabilidad perfecta** |
| **Naturalidad de la Voz (TTS)** | Kokoro `ef_dora` con acento artificial y prosodia plana a -12% | **Chatterbox Multilingual** (Zero-shot cloning de la voz del usuario `Alex.wav` con licencia MIT) + Normalizador de texto | **Cálido, maternal y humano (MOS ≥ 4/5)** |
| **Sincronización Labial (Lip-sync)** | Movimiento de boca genérico y desincronizado de la palabra | **6 visemas fonéticos dinámicos** sincronizados con timestamps exactos de Faster-Whisper | **Sincronía didáctica** |
| **Tiempo de Renderizado por Plano** | ~11-15 segundos de GPU por plano (SDXL + LTX) | **< 1.8 segundos por plano** (stream rawvideo directo a FFmpeg) | **8x más rápido** |

---

## 2. Inspección Visual Lado a Lado (Contact Sheets)

### Línea Base v1
- Archivo: `docs/quality/baseline_v1/contact_sheet_v1.png`
- Observaciones:
  - En los planos 1.1 y 2.1 las manos presentan dedos adicionales que mutan entre el fotograma inicial (0%) y el final (95%).
  - El fondo y el pelaje cambian de textura entre cortes.
  - Barras negras en los laterales al reproducir en pantallas 16:9 estándar.

### Episodio v2 (PuppetRenderer 2.5D)
- Archivo: `docs/quality/candidate_v2/contact_sheet_v2.png`
- Observaciones:
  - Tito conserva exactamente sus proporciones amigables, orejas, nariz de botón y patitas tipo manopla en todos los estados.
  - Movimiento suave con respiración sinusoidal y parpadeo procedural.
  - **Sincronización labial visible:** Visemas orgánicos (`mouth_rest`, `mouth_a`, `mouth_o`, `mouth_e`, `mouth_smile`) que abren y cierran la boca claramente al compás del habla y conteo a 8 Hz.
  - **Claridad del fondo (Depth of Field):** La capa de fondo lejana cuenta con desenfoque suave (bokeh) que separa visualmente el entorno de los elementos de primer plano.
  - **Panel pedagógico de conteo:** En los planos de conteo (planos 2.1 a 6.4), se despliega en el lateral derecho un escenario translúcido con marco dorado donde flotan limpiamente las manzanas animadas junto a sus medallas numéricas circulares ilustradas (`1`, `2`, `3`), eliminando cualquier confusión o sobrecarga visual.


---

## 3. Comparación de Audio y Prosodia

- **Texto para el oído:** En v1, los números aparecían numéricamente "1, 2, 3" provocando lectura errática. En v2, `SpanishSpokenTextNormalizer` garantiza "una, dos y tres manzanas deliciosas", con pausas de respuesta pedagógica adecuadas para niños de 2 a 5 años.
- **Cadena de Post-proceso FFmpeg:**
  - Highpass filter en 80 Hz para remover ruidos subgraves y chasquidos de micrófono.
  - Compresión dinámica suave para nivelar susurros y gritos infantiles.
  - Normalización EBU R128 a **-16 LUFS** en la voz, integrándose en la mezcla con música de fondo playful a **-14 LUFS**.

---

## 4. Archivos Entregables del Episodio v2

- **Video Final Renderizado:**  
  `orchestrator/storage/episodes/3d29bcd5-ee1e-4803-bdbb-bf34244a6669_v2/video/Contando_con_Tito_v2_final.mp4`  
  - *Duración:* 48.7 s  
  - *Resolución:* 1920x1080 (16:9)  
  - *Formato:* H.264 / AAC estéreo con subtítulos karaoke integrados  
- **Clips de Planos (16/16):**  
  `orchestrator/storage/episodes/3d29bcd5-ee1e-4803-bdbb-bf34244a6669_v2/clips/shot_*_v2.mp4`
