import os
import sys
import subprocess
from pathlib import Path

# Guion de 7 planos didácticos enseñando las vocales con Bao el Panda
PANDA_VOWELS_SCRIPT = [
    (1, 1, "¡Hola amiguitos! Soy Bao el osito panda."),
    (2, 1, "Hoy vamos a aprender las cinco vocales mágicas."),
    (3, 1, "La primera es la letra ¡A! ¡A de avión!"),
    (4, 1, "Sigue la letra ¡E! ¡E de estrella brillante!"),
    (5, 1, "Ahora viene la letra ¡I! ¡I de iguana!"),
    (6, 1, "Aquí está la letra ¡O! ¡O de oso panda como yo!"),
    (7, 1, "Y la última es la letra ¡U! ¡U de uvas dulces!"),
    (8, 1, "¡Excelente trabajo! ¡Aprendiste todas las vocales conmigo!")
]

def synthesize_panda_episode():
    root = Path(r"c:\Users\villa\GIT DESKTOP\AIO\KidsAnimationStudio")
    out_dir = root / "orchestrator" / "storage" / "episodes" / "panda_vocales_ep1" / "audio"
    out_dir.mkdir(parents=True, exist_ok=True)

    voice_ref = str(root / "audio" / "voices" / "Alex.wav")
    cli_script = str(root / "scripts" / "cloned_tts.py")
    python_bin = str(root / "voice-lab" / ".venv" / "Scripts" / "python.exe")

    print(f"Sintetizando {len(PANDA_VOWELS_SCRIPT)} planos de voz con Chatterbox (Alex.wav) para Bao el Panda...")

    for sc, sh, text in PANDA_VOWELS_SCRIPT:
        raw_wav = out_dir / f"shot_{sc}_{sh}_raw.wav"
        master_wav = out_dir / f"shot_{sc}_{sh}_master.wav"

        print(f"[{sc}.{sh}] Sintetizando: '{text}'...")
        cmd = [
            python_bin, cli_script,
            "--text", text,
            "--voice-ref", voice_ref,
            "--output", str(raw_wav),
            "--exaggeration", "0.65"
        ]
        res = subprocess.run(cmd, capture_output=True, text=True)
        if res.returncode != 0:
            print(f"Error sintetizando [{sc}.{sh}]: {res.stderr}")
            continue

        # Post-proceso FFmpeg: ecualización y normalización EBU R128 a -16 LUFS
        ffmpeg_cmd = [
            "ffmpeg", "-y", "-loglevel", "error",
            "-i", str(raw_wav),
            "-af", "highpass=f=80,compand=attacks=0.02:decays=0.2:points=-80/-80|-24/-20|-12/-10|0/-3,loudnorm=I=-16:TP=-1.5:LRA=11",
            "-ar", "24000",
            str(master_wav)
        ]
        subprocess.run(ffmpeg_cmd, check=True)
        print(f"[{sc}.{sh}] Masterizado a -16 LUFS: {master_wav.name}")

    print("Todas las tomas de voz de Bao el Panda han sido sintetizadas y masterizadas.")

if __name__ == "__main__":
    synthesize_panda_episode()
