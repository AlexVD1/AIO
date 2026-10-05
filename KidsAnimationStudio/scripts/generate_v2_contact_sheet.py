import json
import os
import sys
from pathlib import Path
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from quality_bench import extract_frames, build_contact_sheet

def generate_v2_bench():
    root = Path(r"c:\Users\villa\GIT DESKTOP\AIO\KidsAnimationStudio")
    v2_clips_dir = root / "orchestrator" / "storage" / "episodes" / "3d29bcd5-ee1e-4803-bdbb-bf34244a6669_v2" / "clips"
    out_dir = root / "docs" / "quality" / "candidate_v2"
    out_dir.mkdir(parents=True, exist_ok=True)
    frames_tmp = out_dir / "frames"
    frames_tmp.mkdir(parents=True, exist_ok=True)

    shots_to_bench = [
        {"shot_id": "shot_1_1", "type": "Primer plano", "clip": "shot_1_1_v2.mp4", "desc": "Tito mirando a cámara saludando con manopla 2D"},
        {"shot_id": "shot_2_1", "type": "Plano medio con acción", "clip": "shot_2_1_v2.mp4", "desc": "Tito con respiración y parpadeo procedural"},
        {"shot_id": "shot_4_1", "type": "Plano con props pedagógicos", "clip": "shot_4_1_v2.mp4", "desc": "Tito interactuando con prop manzana en huerto"},
        {"shot_id": "shot_6_1", "type": "Plano de locación / ambiente", "clip": "shot_6_1_v2.mp4", "desc": "Escenario del huerto con parallax de 3 capas"}
    ]

    shots_data = []
    for s in shots_to_bench:
        clip_path = v2_clips_dir / s["clip"]
        frames = extract_frames(clip_path, frames_tmp, num_frames=4)
        shots_data.append({
            "shot_id": s["shot_id"],
            "type": s["type"],
            "desc": s["desc"],
            "frames": frames
        })

    sheet_out = out_dir / "contact_sheet_v2.png"
    build_contact_sheet(
        shots_data=shots_data,
        output_path=sheet_out,
        title="KidsAnimationStudio — Contact Sheet Episodio v2 (PuppetRenderer 2.5D)"
    )
    print(f"Contact sheet v2 generado: {sheet_out}")

if __name__ == "__main__":
    generate_v2_bench()
