# Auditoría de Licencias y Cumplimiento Legal — Kids Animation Studio

Este documento detalla las licencias de software, modelos de inteligencia artificial y activos de audio utilizados en **Kids Animation Studio**, evaluando su viabilidad para uso comercial y monetización en plataformas de video infantil (YouTube Kids, streaming educativo).

---

## 1. Matriz de Licencias de Modelos de IA

| Modelo / Herramienta | Versión / Variante | Tipo de Licencia | ¿Apto para Monetización Comercial? | Notas y Restricciones |
| :--- | :--- | :--- | :--- | :--- |
| **DreamShaper XL Turbo** | V2 SFW | CreativeML Open RAIL-M++ | ✅ **Sí** | Permite monetización y uso comercial. Prohibido generar contenido difamatorio, sexual o para adultos (totalmente protegido por la lista negra y prompts didácticos). |
| **IP-Adapter Plus SDXL** | ViT-H | Apache-2.0 | ✅ **Sí** | Licencia de código abierto permisiva sin restricciones comerciales. |
| **CLIP-Vision (OpenAI / LAION)** | ViT-H/14 | MIT | ✅ **Sí** | Uso completamente libre para inferencia y control de similitud. |
| **LTX-Video** | 2B distilled (Lightricks) | Lightricks Open Video License | ✅ **Sí** | Permite distribución de videos generados y monetización comercial bajo atribución estándar en metadatos. |
| **Wan 2.2** | TI2V-5B (Alibaba) | Apache-2.0 | ✅ **Sí** | Modelo abierto con derechos comerciales plenos. |
| **Kokoro** | Kokoro-82M | Apache-2.0 | ✅ **Sí** | Voces sintetizadas (`ef_dora`, `em_alex`) totalmente libres de regalías comerciales. |
| **Faster-Whisper** | Small / CT2 | MIT | ✅ **Sí** | Motor de reconocimiento y alineación libre. |
| **Qwen 2.5** | 7B-Instruct (Alibaba) | Apache-2.0 | ✅ **Sí** | Permite uso comercial ilimitado. |
| **ComfyUI** | v0.38.0 | GPL-3.0 | ✅ **Sí** | El motor se usa como servicio externo vía API HTTP (`ai-gateway`), sin distribuir binarios enlazados al código del orquestador. |

---

## 2. Catálogo de Audio (Música BGM y Efectos SFX)

Para evitar reclamaciones de Content ID o bloqueos de derechos de autor en YouTube:
- **BGM Infantil (Fondo)**: Pistas temáticas instrumentales (`playful`, `curious`, `calm`, `celebratory`, `energetic`) bajo licencia **Creative Commons CC0 (Dominio Público)** o **Royalty-Free Commercial License**.
- **Efectos Didácticos (SFX)**: Sonidos de campana, estrellas, burbujas y conteo obtenidos de repositorios con licencia libre de regalías (Freesound CC0 / OpenGameArt Public Domain).
- **Prohibición Expresa**: No se utilizan motores de generación musical neuronal con licencias restrictivas no comerciales (como MusicGen bajo CC-BY-NC o Suno/Udio).

---

## 3. Cumplimiento de Políticas para Menores (COPPA & YouTube Kids)

Kids Animation Studio automatiza la conformidad con las normativas internacionales de protección a la infancia:

### Directiva COPPA (Children's Online Privacy Protection Act)
- **Cero Recopilación de Datos Personales**: La plataforma genera contenido puramente estático pre-renderizado.
- **Etiquetado "Made for Kids"**: Cada archivo exportado (`metadata.json`) establece obligatoriamente `madeForKids: true`. Esto desactiva comentarios, anuncios personalizados y recopilación de cookies para los espectadores en YouTube.

### Divulgación de Contenido Sintético / IA Generativa
- En conformidad con las directrices de YouTube (marzo de 2024), todo episodio generado por IA debe declararse como contenido sintético.
- El archivo `_metadata.json` incluye de manera fija:
  ```json
  "containsSyntheticMedia": true
  ```
  lo que activa la etiqueta oficial transparente de contenido alterado o sintético en el reproductor.

---

## 4. Auditoría de Seguridad de Contenido (Safety)

El sistema cuenta con una triple barrera para garantizar que ningún contenido inapropiado o perturbador llegue a la pantalla de los niños:
1. **Lista Negra Estricta**: `content-safety-blocklist.txt` con 74+ términos prohibidos (violencia, miedo, armas, discriminación, lenguaje no infantil), normalizados sin acentos.
2. **Revisor Pedagógico LLM**: Segunda pasada de análisis curricular que verifica valores pro-sociales, tono constructivo y ausencia de situaciones de peligro.
3. **Negative Prompts Fijos**: Cada solicitud a ComfyUI inyecta obligatoriamente:
   `scary, dark, horror, deformed, distorted, weapon, blood, text, watermark, realistic`.
