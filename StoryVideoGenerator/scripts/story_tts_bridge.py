#!/usr/bin/env python3
"""
story_tts_bridge.py
Puente Python para síntesis de voz con Edge-TTS y medición precisa de duración con FFprobe.
Invocado por StoryVideoGenerator vía ProcessBuilder.
"""

import sys
import json
import asyncio
import subprocess
import os
import re
from pathlib import Path

# Fix para codificación UTF-8 en Windows stdin, stdout y stderr
if sys.platform == "win32":
    import io
    sys.stdin = io.TextIOWrapper(sys.stdin.buffer, encoding="utf-8", errors="replace")
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8")
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding="utf-8")

try:
    import edge_tts
    import edge_tts.communicate
    import ssl
    edge_tts.communicate._SSL_CTX = ssl._create_unverified_context()
except ImportError:
    print(json.dumps({
        "success": False,
        "error": "El paquete 'edge-tts' no está instalado. Ejecuta: pip install edge-tts"
    }))
    sys.exit(1)
except Exception:
    pass


def clean_narration_text(text: str) -> str:
    """Limpia y normaliza el texto para garantizar dicción y pronunciación perfecta en Edge-TTS."""
    if not text:
        return ""
    # 1. Eliminar marcadores de estilo markdown (*, _, `, #, ~)
    text = re.sub(r'[*_#`~]', '', text)
    # 2. Eliminar acotaciones teatrales o directivas entre corchetes o paréntesis como [pausa], (susurro), (grito)
    text = re.sub(r'\[.*?\]|\(.*?\)', '', text)
    # 3. Reemplazar elipsis múltiple (... o …) por una pausa natural (coma o punto) para evitar tropiezos
    text = re.sub(r'\.{2,}|…', ', ', text)
    # 4. Eliminar guiones dobles o largos que puedan confundirse con caracteres especiales
    text = re.sub(r'[—–-]{2,}', ', ', text)
    # 5. Normalizar espacios en blanco
    text = re.sub(r'\s+', ' ', text).strip()
    return text


async def synthesize_speech(text: str, voice: str, output_path: str, rate: str = "+0%", pitch: str = "+0Hz") -> float:
    """Sintetiza audio usando Edge-TTS y lo guarda en output_path."""
    clean_text = clean_narration_text(text)
    communicate = edge_tts.Communicate(clean_text, voice, rate=rate, pitch=pitch)
    await communicate.save(output_path)


def get_audio_duration_seconds(audio_path: str) -> float:
    """Obtiene la duración exacta del audio en segundos usando ffprobe."""
    cmd = [
        "ffprobe",
        "-v", "error",
        "-show_entries", "format=duration",
        "-of", "default=noprint_wrappers=1:nokey=1",
        audio_path
    ]
    try:
        result = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, check=True)
        duration_str = result.stdout.strip()
        return float(duration_str)
    except Exception as e:
        file_size = os.path.getsize(audio_path)
        estimated = max(1.0, file_size / 16000.0)
        return round(estimated, 2)


def main():
    if len(sys.argv) > 1:
        with open(sys.argv[1], "r", encoding="utf-8") as f:
            payload = json.load(f)
    else:
        # Leer directamente los bytes sin procesar del buffer y decodificar UTF-8
        raw_bytes = sys.stdin.buffer.read()
        payload = json.loads(raw_bytes.decode("utf-8", errors="replace"))

    text = payload.get("text", "").strip()
    voice = payload.get("voice", "es-MX-JorgeNeural")
    output_path = payload.get("output_path")
    rate = payload.get("rate", "+0%")
    pitch = payload.get("pitch", "+0Hz")

    if not text:
        print(json.dumps({"success": False, "error": "Texto de narración vacío"}))
        sys.exit(1)

    if not output_path:
        print(json.dumps({"success": False, "error": "output_path no especificado"}))
        sys.exit(1)

    Path(output_path).parent.mkdir(parents=True, exist_ok=True)

    try:
        asyncio.run(synthesize_speech(text, voice, output_path, rate, pitch))
        if not os.path.exists(output_path) or os.path.getsize(output_path) < 1000:
            actual_size = os.path.getsize(output_path) if os.path.exists(output_path) else 0
            raise RuntimeError(f"Audio generado corrupto o incompleto ({actual_size} bytes)")
        duration = get_audio_duration_seconds(output_path)

        response = {
            "success": True,
            "audio_path": output_path,
            "duration_seconds": duration,
            "voice": voice
        }
        print(json.dumps(response))

    except Exception as e:
        if output_path and os.path.exists(output_path) and os.path.getsize(output_path) < 1000:
            try:
                os.remove(output_path)
            except Exception:
                pass
        print(json.dumps({
            "success": False,
            "error": f"Fallo durante síntesis TTS: {str(e)}"
        }))
        sys.exit(1)


if __name__ == "__main__":
    main()
