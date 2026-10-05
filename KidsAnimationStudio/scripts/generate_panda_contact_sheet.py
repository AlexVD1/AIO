import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageFont
from pathlib import Path

def generate_panda_contact_sheet():
    root = Path(r"c:\Users\villa\GIT DESKTOP\AIO\KidsAnimationStudio")
    clips_dir = root / "orchestrator" / "storage" / "episodes" / "panda_vocales_ep1" / "clips"
    out_dir = root / "docs" / "quality" / "candidate_panda"
    out_dir.mkdir(parents=True, exist_ok=True)
    out_file = out_dir / "contact_sheet_panda_vocales.png"

    # Seleccionar 4 planos representativos
    # Plano 1: Saludo inicial
    # Plano 3: Vocal A
    # Plano 6: Vocal O
    # Plano 8: Celebración final
    planos = [
        ("Plano 1: shot_1_1 | Saludo de Bao el Panda en Bosque de Bambú", clips_dir / "shot_1_1.mp4"),
        ("Plano 3: shot_3_1 | Aprendiendo la vocal 'A' (A de Avión)", clips_dir / "shot_3_1.mp4"),
        ("Plano 6: shot_6_1 | Aprendiendo la vocal 'O' (O de Oso Panda)", clips_dir / "shot_6_1.mp4"),
        ("Plano 8: shot_8_1 | Celebración y recapitulación de las 5 vocales", clips_dir / "shot_8_1.mp4"),
    ]

    time_pcts = [0.0, 0.33, 0.66, 0.95]
    thumb_w, thumb_h = 460, 258
    header_h = 70
    shot_header_h = 30
    pad = 16

    total_w = pad * 2 + (thumb_w * 4) + (pad * 3)
    total_h = header_h + len(planos) * (shot_header_h + thumb_h + pad) + pad

    canvas = Image.new("RGB", (total_w, total_h), (18, 20, 24))
    draw = ImageDraw.Draw(canvas)

    try:
        title_font = ImageFont.truetype("arialbd.ttf", 20)
        shot_font = ImageFont.truetype("arial.ttf", 13)
        tag_font = ImageFont.truetype("arialbd.ttf", 12)
    except Exception:
        title_font = shot_font = tag_font = ImageFont.load_default()

    draw.text((pad, 15), "KidsAnimationStudio • Pipeline Dinámico End-to-End: Bao el Panda (Enseñanza de Vocales)", fill=(240, 240, 240), font=title_font)
    draw.text((pad, 42), "Nuevo Personaje (Panda Bao) + Nuevo Escenario (Bosque Bambú DoF) + Nueva Trama Pedagógica (Vocales)", fill=(140, 160, 180), font=shot_font)

    curr_y = header_h
    for title, clip_path in planos:
        draw.text((pad, curr_y), title, fill=(255, 215, 0), font=shot_font)
        curr_y += shot_header_h

        cap = cv2.VideoCapture(str(clip_path))
        n_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))

        for idx, pct in enumerate(time_pcts):
            frame_idx = min(int(n_frames * pct), n_frames - 1)
            cap.set(cv2.CAP_PROP_POS_FRAMES, frame_idx)
            ret, frame = cap.read()
            if not ret:
                thumb = Image.new("RGB", (thumb_w, thumb_h), (40, 40, 40))
            else:
                frame_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
                thumb = Image.fromarray(frame_rgb).resize((thumb_w, thumb_h), Image.Resampling.LANCZOS)

            tdraw = ImageDraw.Draw(thumb)
            tdraw.rectangle([6, 6, 75, 26], fill=(0, 0, 0, 200))
            tdraw.text((12, 9), f"T+{int(pct*100)}%", fill=(255, 255, 255), font=tag_font)

            curr_x = pad + idx * (thumb_w + pad)
            canvas.paste(thumb, (curr_x, curr_y))

        cap.release()
        curr_y += thumb_h + pad

    canvas.save(out_file)
    print(f"Contact sheet generado en: {out_file}")

if __name__ == "__main__":
    generate_panda_contact_sheet()
