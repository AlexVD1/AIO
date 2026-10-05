from __future__ import annotations

import logging
import re
import uuid
from pathlib import Path
from typing import Dict, Tuple

import soundfile as sf
import torch

try:
    import truststore
    truststore.inject_into_ssl()
except Exception:  # noqa: BLE001
    pass

from kokoro import KPipeline

logger = logging.getLogger(__name__)

# Cache de pipelines por código de idioma
_pipelines: Dict[str, KPipeline] = {}


def _get_lang_code(voice: str) -> str:
    """Infiere el código de idioma a partir del prefijo de voz de Kokoro."""
    if not voice:
        return "e"  # Español por defecto
    v = voice.lower().strip()
    if v.startswith("e"):
        return "e"  # Español (ef_dora, em_alex, etc.)
    elif v.startswith("a"):
        return "a"  # American English
    elif v.startswith("b"):
        return "b"  # British English
    elif v.startswith("j"):
        return "j"  # Japonés
    elif v.startswith("z"):
        return "z"  # Chino mandarín
    elif v.startswith("f"):
        return "f"  # Francés
    elif v.startswith("i"):
        return "i"  # Italiano
    elif v.startswith("p"):
        return "p"  # Portugués
    return "e"


def _parse_speed(rate: str | float | None) -> float:
    """Parsea el parámetro de velocidad/tasa (ej. '+0%', '-12%', '+10%', '0.9', 1.0)."""
    if rate is None:
        return 1.0
    if isinstance(rate, (int, float)):
        return max(0.5, min(2.0, float(rate)))

    r_str = str(rate).strip()
    if not r_str or r_str.lower() in ("default", "normal"):
        return 1.0

    match = re.search(r"([+-]?\d+(?:\.\d+)?)\s*%", r_str)
    if match:
        pct = float(match.group(1))
        speed = 1.0 + (pct / 100.0)
        return max(0.5, min(2.0, speed))

    try:
        val = float(r_str)
        return max(0.5, min(2.0, val))
    except ValueError:
        return 1.0


def get_pipeline(lang_code: str = "e") -> KPipeline:
    if lang_code not in _pipelines:
        logger.info("Inicializando KPipeline para idioma '%s'...", lang_code)
        _pipelines[lang_code] = KPipeline(lang_code=lang_code)
    return _pipelines[lang_code]


def synthesize_speech(
    text: str,
    voice: str = "ef_dora",
    rate: str | float | None = "+0%",
    output_path: str | Path | None = None,
) -> Tuple[str, int]:
    """
    Sintetiza voz con Kokoro-82M a 24 kHz y guarda en formato WAV.
    Retorna (ruta_absoluta, duracion_en_milisegundos).
    """
    if not text or not text.strip():
        raise ValueError("El texto para TTS no puede estar vacío")

    clean_text = text.strip()
    lang_code = _get_lang_code(voice)
    speed = _parse_speed(rate)

    pipeline = get_pipeline(lang_code)
    logger.info("Sintetizando TTS Kokoro: voz='%s', lang='%s', speed=%.2f, texto='%s'",
                voice, lang_code, speed, clean_text[:40])

    generator = pipeline(clean_text, voice=voice, speed=speed)
    chunks = []
    for _, _, audio in generator:
        if audio is not None:
            if isinstance(audio, torch.Tensor):
                chunks.append(audio.detach().cpu())
            else:
                chunks.append(torch.tensor(audio))

    if not chunks:
        # Fallback de silencio (0.5s) si el generador no produjo muestras
        full_audio = torch.zeros(12000, dtype=torch.float32)
    elif len(chunks) == 1:
        full_audio = chunks[0]
    else:
        full_audio = torch.cat(chunks)

    sample_rate = 24000
    duration_ms = int(len(full_audio) / sample_rate * 1000)

    if output_path:
        out_file = Path(output_path)
    else:
        out_file = Path("./storage/audio") / f"tts_{uuid.uuid4().hex[:8]}.wav"

    out_file.parent.mkdir(parents=True, exist_ok=True)
    sf.write(str(out_file), full_audio.numpy(), sample_rate)

    abs_path = str(out_file.resolve())
    logger.info("TTS generado exitosamente: %s (duración: %d ms)", abs_path, duration_ms)
    return abs_path, duration_ms
