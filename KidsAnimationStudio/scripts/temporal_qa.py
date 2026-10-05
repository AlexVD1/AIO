import os
import subprocess
from pathlib import Path
from PIL import Image
import numpy as np

def extract_frames_from_video(video_path: str, num_frames: int = 10) -> list[Image.Image]:
    """Extrae frames distribuidos equitativamente de un video usando ffmpeg."""
    out_dir = Path(video_path).parent / f"frames_tmp_{Path(video_path).stem}"
    out_dir.mkdir(exist_ok=True)
    pattern = str(out_dir / "f_%03d.png")

    cmd = [
        "ffmpeg", "-y", "-loglevel", "error",
        "-i", str(video_path),
        "-vf", f"select='not(mod(n\\,10))',scale=512:288",
        "-vsync", "vfr",
        pattern
    ]
    subprocess.run(cmd, check=True)

    frame_files = sorted(list(out_dir.glob("f_*.png")))
    frames = [Image.open(p).convert("RGB") for p in frame_files]

    # Limpieza
    for p in frame_files:
        p.unlink()
    out_dir.rmdir()

    return frames

def calculate_temporal_stability(frames: list[Image.Image]) -> dict:
    """
    Calcula métricas de estabilidad temporal entre frames consecutivos:
    - PSNR / SSIM aproximado o diferencia absoluta media (MAD).
    - Salto máximo entre fotogramas consecutivos (detecta morphing brusco).
    """
    if len(frames) < 2:
        return {"status": "PASS", "mean_frame_diff": 0.0, "max_jump": 0.0}

    diffs = []
    for i in range(len(frames) - 1):
        arr1 = np.array(frames[i], dtype=np.float32)
        arr2 = np.array(frames[i + 1], dtype=np.float32)
        # Diferencia media normalizada [0, 1]
        diff = np.mean(np.abs(arr1 - arr2)) / 255.0
        diffs.append(diff)

    mean_diff = float(np.mean(diffs))
    max_jump = float(np.max(diffs))

    # Reglas de QA: Si max_jump > 0.40 o mean_diff > 0.25, el clip sufrió deformación/flicker severo
    is_stable = (max_jump <= 0.40) and (mean_diff <= 0.25)

    return {
        "status": "PASS" if is_stable else "FAIL",
        "mean_frame_diff": round(mean_diff, 4),
        "max_jump": round(max_jump, 4),
        "is_stable": is_stable
    }

if __name__ == "__main__":
    sample_video = r"c:\Users\villa\GIT DESKTOP\AIO\KidsAnimationStudio\assets\puppet_test.mp4"
    if os.path.exists(sample_video):
        frames = extract_frames_from_video(sample_video)
        res = calculate_temporal_stability(frames)
        print("Resultado QA Temporal para puppet_test.mp4:", res)
        assert res["status"] == "PASS", "El clip de PuppetRenderer debe pasar QA temporal"
        print("QA Temporal Verificado con éxito.")
