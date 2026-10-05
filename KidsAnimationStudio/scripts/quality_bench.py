"""
Quality Benchmarking Utility (Q0) - KidsAnimationStudio
Generates contact sheets for anatomical/temporal inspection of video clips
and interactive HTML A/B evaluation pages for human MOS audio rating.
"""

import os
import sys
import json
import subprocess
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

# Define paths
ROOT_DIR = Path(__file__).resolve().parent.parent
DOCS_DIR = ROOT_DIR / "docs"
QUALITY_DIR = DOCS_DIR / "quality"
BASELINE_V1_DIR = QUALITY_DIR / "baseline_v1"
EPISODE_V1_STORAGE = ROOT_DIR / "orchestrator" / "storage" / "episodes" / "3d29bcd5-ee1e-4803-bdbb-bf34244a6669"

# Q0.1 Golden Dataset
GOLDEN_PHRASES = [
    {
        "id": "phrase_1",
        "category": "Saludo",
        "text": "¡Hola, niños! Soy Tito, el zorrito.",
        "v1_audio": "shot_1_1.wav",
        "notes": "Introducción amigable, tono cálido y alegre."
    },
    {
        "id": "phrase_2",
        "category": "Pregunta pedagógica",
        "text": "¿Cuántas manzanas ves en el árbol?",
        "v1_audio": "shot_3_1.wav",
        "notes": "Entonación de pregunta al niño, pausa implícita de espera."
    },
    {
        "id": "phrase_3",
        "category": "Conteo rítmico",
        "text": "¡Una, dos y tres manzanas deliciosas!",
        "v1_audio": "shot_4_2.wav",
        "notes": "Métrica rítmica para apoyo visual del conteo 1, 2, 3."
    },
    {
        "id": "phrase_4",
        "category": "Celebración",
        "text": "¡Muy bien hecho, lo lograste conmigo!",
        "v1_audio": "shot_5_1.wav",
        "notes": "Entonación triunfal y de refuerzo positivo."
    },
    {
        "id": "phrase_5",
        "category": "Despedida",
        "text": "¡Nos vemos pronto para otra gran aventura!",
        "v1_audio": "shot_7_1.wav",
        "notes": "Cierre cálido del episodio."
    }
]

GOLDEN_SHOTS_V1 = [
    {
        "id": "shot_1_1",
        "type": "Primer plano",
        "clip": "shot_1_1_attempt_1.mp4",
        "description": "Tito mirando a cámara de frente presentándose."
    },
    {
        "id": "shot_2_1",
        "type": "Plano medio con acción",
        "clip": "shot_2_1_attempt_1.mp4",
        "description": "Tito moviendo las extremidades y el cuerpo."
    },
    {
        "id": "shot_4_1",
        "type": "Plano con props pedagógicos",
        "clip": "shot_4_1_attempt_1.mp4",
        "description": "Tito señalando e interactuando con manzanas."
    },
    {
        "id": "shot_6_1",
        "type": "Plano de locación / ambiente",
        "clip": "shot_6_1_attempt_1.mp4",
        "description": "Escenario del bosque con iluminación y fondo."
    }
]


def get_video_duration(video_path: Path) -> float:
    cmd = [
        "ffprobe", "-v", "error",
        "-show_entries", "format=duration",
        "-of", "default=noprint_wrappers=1:nokey=1",
        str(video_path)
    ]
    try:
        res = subprocess.run(cmd, capture_output=True, text=True, check=True)
        return float(res.stdout.strip())
    except Exception as e:
        print(f"Warning: could not get duration for {video_path}: {e}")
        return 3.0


def extract_frames(video_path: Path, output_dir: Path, num_frames: int = 4) -> list[Path]:
    output_dir.mkdir(parents=True, exist_ok=True)
    duration = get_video_duration(video_path)
    
    # 0%, 33%, 66%, 95%
    percentages = [0.0, 0.33, 0.66, 0.95] if num_frames == 4 else [i / (num_frames - 1) for i in range(num_frames)]
    timestamps = [max(0.01, min(duration - 0.05, p * duration)) for p in percentages]
    
    frame_paths = []
    base_name = video_path.stem
    for idx, ts in enumerate(timestamps):
        out_frame = output_dir / f"{base_name}_frame_{idx}_{int(ts*1000)}ms.png"
        cmd = [
            "ffmpeg", "-y", "-ss", f"{ts:.3f}",
            "-i", str(video_path),
            "-frames:v", "1",
            "-q:v", "2",
            str(out_frame)
        ]
        subprocess.run(cmd, capture_output=True, check=True)
        frame_paths.append(out_frame)
    return frame_paths


def build_contact_sheet(shots_data: list[dict], output_path: Path, title: str):
    """
    shots_data: list of dicts with keys: 'shot_id', 'type', 'frames' (list of 4 image paths)
    """
    thumb_w, thumb_h = 384, 216  # 16:9 ratio thumbnail
    margin = 20
    header_h = 70
    label_h = 28
    
    cols = 4
    rows = len(shots_data)
    
    total_w = margin * 2 + cols * thumb_w + (cols - 1) * margin
    total_h = header_h + margin + rows * (thumb_h + label_h + margin)
    
    sheet = Image.new("RGB", (total_w, total_h), color=(24, 26, 32))
    draw = ImageDraw.Draw(sheet)
    
    # Header
    draw.rectangle([(0, 0), (total_w, header_h)], fill=(15, 17, 23))
    draw.text((margin, 20), title, fill=(255, 255, 255))
    draw.text((margin, 44), "Análisis temporal de deformación: 0% (Inicio) · 33% · 66% · 95% (Final)", fill=(160, 165, 180))
    
    y_cursor = header_h + margin
    for row_idx, shot in enumerate(shots_data):
        label_text = f"Plano {row_idx+1}: {shot['shot_id']} ({shot['type']}) — {shot.get('desc', '')}"
        draw.text((margin, y_cursor), label_text, fill=(240, 200, 80))
        y_cursor += label_h
        
        for col_idx, frame_path in enumerate(shot['frames']):
            x = margin + col_idx * (thumb_w + margin)
            y = y_cursor
            
            try:
                with Image.open(frame_path) as img:
                    img_thumb = img.convert("RGB").resize((thumb_w, thumb_h), Image.Resampling.LANCZOS)
                    sheet.paste(img_thumb, (x, y))
                    # Frame index tag
                    tag = f"T+{col_idx*33}%" if col_idx < 3 else "T+95%"
                    draw.rectangle([(x + 6, y + 6), (x + 70, y + 26)], fill=(0, 0, 0, 180))
                    draw.text((x + 12, y + 10), tag, fill=(255, 255, 255))
            except Exception as e:
                draw.rectangle([(x, y), (x + thumb_w, y + thumb_h)], fill=(60, 60, 60))
                draw.text((x + 20, y + 100), f"Error: {e}", fill=(255, 100, 100))
                
        y_cursor += thumb_h + margin
        
    output_path.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(output_path, quality=95)
    print(f"Contact sheet saved to: {output_path}")


def generate_audio_eval_html(output_html_path: Path, phrases: list[dict]):
    html_content = f"""<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Banco de Evaluación de Calidad de Voz (MOS) — KidsAnimationStudio</title>
<style>
  :root {{
    --bg: #0f172a;
    --card: #1e293b;
    --card-border: #334155;
    --text: #f8fafc;
    --muted: #94a3b8;
    --accent: #38bdf8;
    --gold: #f59e0b;
    --success: #10b981;
  }}
  body {{
    font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
    background-color: var(--bg);
    color: var(--text);
    margin: 0;
    padding: 2rem;
  }}
  .container {{
    max-width: 960px;
    margin: 0 auto;
  }}
  header {{
    margin-bottom: 2rem;
    padding-bottom: 1.5rem;
    border-bottom: 1px solid var(--card-border);
  }}
  h1 {{
    margin: 0 0 0.5rem 0;
    font-size: 1.8rem;
    color: var(--accent);
  }}
  p.subtitle {{
    margin: 0;
    color: var(--muted);
  }}
  .instructions {{
    background: rgba(56, 189, 248, 0.08);
    border-left: 4px solid var(--accent);
    padding: 1rem 1.25rem;
    border-radius: 4px;
    margin-bottom: 2rem;
    font-size: 0.95rem;
    line-height: 1.5;
  }}
  .phrase-card {{
    background: var(--card);
    border: 1px solid var(--card-border);
    border-radius: 12px;
    padding: 1.5rem;
    margin-bottom: 1.5rem;
  }}
  .phrase-header {{
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 0.75rem;
  }}
  .category-badge {{
    background: #0284c7;
    color: #fff;
    padding: 0.25rem 0.75rem;
    border-radius: 9999px;
    font-size: 0.8rem;
    font-weight: 600;
  }}
  .phrase-text {{
    font-size: 1.25rem;
    font-weight: 600;
    margin: 0.5rem 0;
    color: #f1f5f9;
  }}
  .phrase-notes {{
    font-size: 0.85rem;
    color: var(--muted);
    margin-bottom: 1.25rem;
  }}
  .audio-variants {{
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    gap: 1.25rem;
    margin-top: 1rem;
  }}
  .variant-box {{
    background: rgba(15, 23, 42, 0.6);
    border: 1px solid var(--card-border);
    border-radius: 8px;
    padding: 1rem;
  }}
  .variant-title {{
    font-size: 0.9rem;
    font-weight: 600;
    margin-bottom: 0.5rem;
    color: var(--gold);
  }}
  audio {{
    width: 100%;
    margin-bottom: 0.75rem;
  }}
  .rating-group {{
    display: flex;
    align-items: center;
    gap: 0.5rem;
    margin-top: 0.5rem;
  }}
  .rating-label {{
    font-size: 0.85rem;
    color: var(--muted);
    min-width: 90px;
  }}
  .star-rating {{
    display: inline-flex;
    flex-direction: row-reverse;
    gap: 4px;
  }}
  .star-rating input {{
    display: none;
  }}
  .star-rating label {{
    font-size: 1.3rem;
    color: #475569;
    cursor: pointer;
    transition: color 0.15s;
  }}
  .star-rating label:hover,
  .star-rating label:hover ~ label,
  .star-rating input:checked ~ label {{
    color: var(--gold);
  }}
  .btn-bar {{
    margin-top: 2rem;
    display: flex;
    gap: 1rem;
    justify-content: flex-end;
  }}
  button.action-btn {{
    background: var(--accent);
    color: #0f172a;
    font-weight: 600;
    padding: 0.75rem 1.5rem;
    border: none;
    border-radius: 8px;
    cursor: pointer;
    font-size: 0.95rem;
  }}
  button.action-btn:hover {{
    background: #7dd3fc;
  }}
</style>
</head>
<body>
<div class="container">
  <header>
    <h1>Banco de Evaluación Subjetiva de Voz (MOS)</h1>
    <p class="subtitle">Fase Q0 / Q2: Comparación auditiva de las 5 frases maestras del conjunto dorado</p>
  </header>

  <div class="instructions">
    <strong>Instrucciones para calificación humana (Escala MOS 1 a 5):</strong><br>
    Escucha cada variante de audio generada para la frase. Califica de 1 a 5 estrellas la <em>Naturalidad</em> (¿suena a locutor de cuento infantil o a robot?) y la <em>Claridad</em> (¿se entiende perfectamente?).<br>
    5 = Excelente y natural | 4 = Buena, pequeños detalles | 3 = Aceptable pero robótica | 2 = Pobre | 1 = Inaceptable.
  </div>

  <form id="mosForm">
"""
    for p in phrases:
        p_id = p["id"]
        v1_audio_path = f"baseline_v1/audio/{p['v1_audio']}"
        html_content += f"""
    <div class="phrase-card">
      <div class="phrase-header">
        <span class="category-badge">{p["category"]}</span>
        <span style="font-size: 0.85rem; color: var(--muted);">{p_id}</span>
      </div>
      <div class="phrase-text">"{p["text"]}"</div>
      <div class="phrase-notes">{p["notes"]}</div>

      <div class="audio-variants">
        <!-- Variante A: Baseline v1 (Kokoro ef_dora -12%) -->
        <div class="variant-box">
          <div class="variant-title">Opción A: Línea Base v1 (Kokoro -12%)</div>
          <audio controls src="{v1_audio_path}"></audio>
          
          <div class="rating-group">
            <span class="rating-label">Naturalidad:</span>
            <div class="star-rating">
              <input type="radio" id="{p_id}_a_nat_5" name="{p_id}_a_nat" value="5"><label for="{p_id}_a_nat_5">★</label>
              <input type="radio" id="{p_id}_a_nat_4" name="{p_id}_a_nat" value="4"><label for="{p_id}_a_nat_4">★</label>
              <input type="radio" id="{p_id}_a_nat_3" name="{p_id}_a_nat" value="3"><label for="{p_id}_a_nat_3">★</label>
              <input type="radio" id="{p_id}_a_nat_2" name="{p_id}_a_nat" value="2"><label for="{p_id}_a_nat_2">★</label>
              <input type="radio" id="{p_id}_a_nat_1" name="{p_id}_a_nat" value="1"><label for="{p_id}_a_nat_1">★</label>
            </div>
          </div>
        </div>

        <!-- Variante B: Candidato Clonación Q2 -->
        <div class="variant-box">
          <div class="variant-title">Opción B: Voz Clonada Q2 (Muestra de referencia)</div>
          <audio controls src="candidates/{p_id}_cloned.wav"></audio>
          
          <div class="rating-group">
            <span class="rating-label">Naturalidad:</span>
            <div class="star-rating">
              <input type="radio" id="{p_id}_b_nat_5" name="{p_id}_b_nat" value="5"><label for="{p_id}_b_nat_5">★</label>
              <input type="radio" id="{p_id}_b_nat_4" name="{p_id}_b_nat" value="4"><label for="{p_id}_b_nat_4">★</label>
              <input type="radio" id="{p_id}_b_nat_3" name="{p_id}_b_nat" value="3"><label for="{p_id}_b_nat_3">★</label>
              <input type="radio" id="{p_id}_b_nat_2" name="{p_id}_b_nat" value="2"><label for="{p_id}_b_nat_2">★</label>
              <input type="radio" id="{p_id}_b_nat_1" name="{p_id}_b_nat" value="1"><label for="{p_id}_b_nat_1">★</label>
            </div>
          </div>
        </div>
      </div>
    </div>
"""

    html_content += """
    <div class="btn-bar">
      <button type="button" class="action-btn" onclick="saveRatings()">Descargar Resultados (JSON)</button>
    </div>
  </form>
</div>

<script>
function saveRatings() {
  const form = document.getElementById('mosForm');
  const data = new FormData(form);
  const results = {};
  for (const [key, val] of data.entries()) {
    results[key] = val;
  }
  const blob = new Blob([JSON.stringify(results, null, 2)], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'mos_voice_ratings_' + new Date().toISOString().split('T')[0] + '.json';
  a.click();
}
</script>
</body>
</html>
"""
    output_html_path.parent.mkdir(parents=True, exist_ok=True)
    with open(output_html_path, "w", encoding="utf-8") as f:
        f.write(html_content)
    print(f"HTML Evaluation page saved to: {output_html_path}")


def init_baseline():
    """Extract baseline v1 artifacts and generate quality bench files"""
    print("=== Inicializando Banco de Calidad Q0 (Línea Base v1) ===")
    BASELINE_V1_DIR.mkdir(parents=True, exist_ok=True)
    audio_target_dir = BASELINE_V1_DIR / "audio"
    audio_target_dir.mkdir(parents=True, exist_ok=True)
    frames_temp_dir = BASELINE_V1_DIR / "frames"
    frames_temp_dir.mkdir(parents=True, exist_ok=True)

    # 1. Copiar audios de referencia del conjunto dorado desde v1
    v1_audio_src = EPISODE_V1_STORAGE / "audio"
    for phrase in GOLDEN_PHRASES:
        src = v1_audio_src / phrase["v1_audio"]
        dst = audio_target_dir / phrase["v1_audio"]
        if src.exists():
            with open(src, "rb") as rf, open(dst, "wb") as wf:
                wf.write(rf.read())
            print(f"Copiado audio v1: {phrase['v1_audio']}")
        else:
            print(f"Aviso: no encontrado {src}")

    # 2. Extraer fotogramas de los 4 planos dorados de v1
    v1_clips_src = EPISODE_V1_STORAGE / "clips"
    shots_data = []
    for shot in GOLDEN_SHOTS_V1:
        clip_path = v1_clips_src / shot["clip"]
        if clip_path.exists():
            print(f"Extrayendo 4 frames temporales de {shot['clip']}...")
            frames = extract_frames(clip_path, frames_temp_dir, num_frames=4)
            shots_data.append({
                "shot_id": shot["id"],
                "type": shot["type"],
                "desc": shot["description"],
                "frames": frames
            })
        else:
            print(f"Aviso: clip no encontrado {clip_path}")

    # 3. Generar contact sheet PNG
    contact_sheet_path = BASELINE_V1_DIR / "contact_sheet_v1.png"
    if shots_data:
        build_contact_sheet(shots_data, contact_sheet_path, "Línea Base v1 — Episodio 'Contando con Tito'")

    # 4. Generar página HTML de evaluación MOS
    eval_html_path = QUALITY_DIR / "audio_evaluation.html"
    generate_audio_eval_html(eval_html_path, GOLDEN_PHRASES)

    # 5. Generar summary.json con métricas objetivas del episodio v1
    summary = {
        "episode_id": "3d29bcd5-ee1e-4803-bdbb-bf34244a6669",
        "title": "Contando con Tito",
        "baseline_version": "v1",
        "date_extracted": "2026-10-04",
        "video_metrics": {
            "source_resolution": "768x512 (3:2)",
            "output_resolution": "1920x1080 (16:9 con pillarbox)",
            "source_fps": 16,
            "output_fps": 30,
            "ltx_steps": 20,
            "ltx_cfg": 3.0,
            "ltx_scheduler": "normal",
            "frame_length": 48,
            "temporal_deformations_observed": [
                "Deformación severa de manos (múltiples dedos, dedos fusionados)",
                "Boca distorsionada al hablar",
                "Extremidades borrosas en movimiento"
            ]
        },
        "audio_metrics": {
            "tts_engine": "Kokoro-82M",
            "voice": "ef_dora",
            "tts_speed": "-12% (0.88)",
            "synthesis_mode": "Plano por plano (aislado)",
            "sfx_failures": 4,
            "lufs_target": -14.0
        },
        "golden_phrases": GOLDEN_PHRASES,
        "golden_shots": GOLDEN_SHOTS_V1
    }
    with open(BASELINE_V1_DIR / "summary.json", "w", encoding="utf-8") as f:
        json.dump(summary, f, indent=2, ensure_ascii=False)
    print(f"Resumen de línea base guardado en: {BASELINE_V1_DIR / 'summary.json'}")
    print("=== Q0 completado exitosamente ===")


if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1] == "--init-baseline":
        init_baseline()
    else:
        init_baseline()
