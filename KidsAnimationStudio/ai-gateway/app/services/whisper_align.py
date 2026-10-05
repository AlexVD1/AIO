from __future__ import annotations

import logging
from pathlib import Path
from typing import Any, Dict, List, Optional

try:
    import truststore
    truststore.inject_into_ssl()
except Exception:  # noqa: BLE001
    pass

from faster_whisper import WhisperModel

logger = logging.getLogger(__name__)

# Cache del modelo Whisper
_whisper_model: Optional[WhisperModel] = None
_cached_model_size: Optional[str] = None


def get_whisper_model(model_size: str = "base") -> WhisperModel:
    global _whisper_model, _cached_model_size
    if _whisper_model is None or _cached_model_size != model_size:
        logger.info("Cargando modelo Faster-Whisper '%s' en CPU...", model_size)
        _whisper_model = WhisperModel(model_size, device="cpu", compute_type="int8")
        _cached_model_size = model_size
    return _whisper_model


def align_audio_timestamps(
    audio_path: str | Path,
    language: str = "es",
    model_size: str = "base",
    initial_prompt: Optional[str] = None,
) -> List[Dict[str, Any]]:
    """
    Alinea el audio a nivel de palabras utilizando Faster-Whisper.
    Retorna una lista de diccionarios: [{"word": "palabra", "startMs": 100, "endMs": 450}, ...]
    """
    path = Path(audio_path)
    if not path.exists():
        raise FileNotFoundError(f"El archivo de audio no existe: {path}")

    model = get_whisper_model(model_size)
    logger.info("Alineando timestamps de audio con Whisper: %s (idioma: %s)", path.name, language)

    kwargs = {
        "language": language,
        "word_timestamps": True,
        "vad_filter": True,
    }
    if initial_prompt:
        kwargs["initial_prompt"] = initial_prompt

    segments, info = model.transcribe(str(path.resolve()), **kwargs)

    words = []
    for segment in segments:
        if segment.words:
            for w in segment.words:
                cleaned_word = w.word.strip()
                if cleaned_word:
                    words.append({
                        "word": cleaned_word,
                        "startMs": int(round(w.start * 1000)),
                        "endMs": int(round(w.end * 1000)),
                    })

    logger.info("Alineación completada: %d palabras detectadas en %s", len(words), path.name)
    return words
