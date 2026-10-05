import os
import sys
import subprocess
from pathlib import Path

# Definición de los 16 planos del episodio con su texto exacto
EPISODE_SHOTS_TEXT = [
    (1, 1, "¡Hola niños! Soy Tito, el zorrito."),
    (1, 2, "Hoy vamos a contar manzanas."),
    (2, 1, "Una manzana."),
    (2, 2, "¿Cuántas manzanas ves?"),
    (3, 1, "¡Dos manzanas!"),
    (3, 2, "Dos manzanas."),
    (4, 1, "¡Tres manzanas!"),
    (4, 2, "Tres manzanas."),
    (5, 1, "¡Una, dos, tres!"),
    (5, 2, "¿Cuántas manzanas?"),
    (5, 3, "¡Tres manzanas!"),
    (6, 1, "Vamos a contar de nuevo."),
    (6, 2, "¡Una manzana!"),
    (6, 3, "¡Dos manzanas!"),
    (6, 4, "Tres manzanas."),
    (7, 1, "¡Fue divertido, verdad! ¡Nos vemos la próxima vez!")
]

def synthesize_all_cloned_voice():
    root = Path(r"c:\Users\villa\GIT DESKTOP\AIO\KidsAnimationStudio")
    out_dir = root / "orchestrator" / "storage" / "episodes" / "3d29bcd5-ee1e-4803-bdbb-bf34244a6669_v2" / "audio_cloned"
    out_dir.mkdir(parents=True, exist_ok=True)

    voice_ref = str(root / "audio" / "voices" / "Alex.wav")
    cli_script = str(root / "scripts" / "cloned_tts.py")
    python_bin = str(root / "voice-lab" / ".venv" / "Scripts" / "python.exe")

    generated_files = []
    print(f"Sintetizando {len(EPISODE_SHOTS_TEXT)} planos con Chatterbox Multilingual (Alex.wav) en GPU...")

    for sc, sh, text in EPISODE_SHOTS_TEXT:
        out_wav = out_dir / f"shot_{sc}_{sh}_cloned.wav"
        norm_wav = out_dir / f"shot_{sc}_{sh}_cloned_master.wav"

        print(f"[{sc}.{sh}] Sintetizando: '{text}'...")
        cmd = [
            python_bin, cli_script,
            "--text", text,
            "--voice-ref", voice_ref,
            "--output", str(out_wav),
            "--exaggeration", "0.6"
        ]
        res = subprocess.run(cmd, capture_output=True, text=True)
        if res.returncode != 0:
            print(f"Error sintetizando [{sc}.{sh}]: {res.stderr}")
            continue

        # Post-proceso FFmpeg: highpass 80Hz, compand suave y normalización a -16 LUFS
        ffmpeg_cmd = [
            "ffmpeg", "-y", "-loglevel", "error",
            "-i", str(out_wav),
            "-af", "highpass=f=80,compand=attacks=0.02:decays=0.2:points=-80/-80|-24/-20|-12/-10|0/-3,loudnorm=I=-16:TP=-1.5:LRA=11",
            "-ar", "24000",
            "-ac", "1",
            str(norm_wav)
        ]
        subprocess.run(ffmpeg_cmd, check=True)
        generated_files.append((sc, sh, str(norm_wav), text))
        print(f"[{sc}.{sh}] OK: {norm_wav.name}")

    print(f"Total audios clonados generados y masterizados: {len(generated_files)}/16")

if __name__ == "__main__":
    synthesize_all_cloned_voice()
