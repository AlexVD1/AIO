from __future__ import annotations

import logging
from typing import List, Optional

from fastapi import APIRouter, HTTPException, Request
from pydantic import BaseModel, Field

from app.services.kokoro_tts import synthesize_speech
from app.services.whisper_align import align_audio_timestamps

logger = logging.getLogger(__name__)

router = APIRouter(tags=["Audio"])


class TtsRequest(BaseModel):
    text: str = Field(..., description="Texto a sintetizar en voz")
    voice: str = Field("ef_dora", description="Nombre de la voz (ej. ef_dora, em_alex)")
    rate: Optional[str] = Field("+0%", description="Modificador de velocidad (ej. -12%, +10%, 0.88)")
    pitch: Optional[str] = Field("default", description="Tono de voz")
    outputPath: Optional[str] = Field(None, description="Ruta absoluta de destino del archivo WAV")
    voiceEngine: Optional[str] = Field("kokoro", description="Motor de síntesis: kokoro | cloned | recorded")
    voiceReferencePath: Optional[str] = Field(None, description="Ruta a muestra de referencia para clonación")
    expressiveness: Optional[float] = Field(0.6, description="Factor de expresividad para clonación (0.0 a 1.0)")


class TtsResponse(BaseModel):
    audioPath: str
    durationMs: int


class AlignRequest(BaseModel):
    audioPath: str = Field(..., description="Ruta absoluta del archivo de audio WAV")
    text: Optional[str] = Field(None, description="Texto de referencia opcional para mejorar alineación")
    language: str = Field("es", description="Código de idioma (es, en, etc.)")


class WordTimestamp(BaseModel):
    word: str
    startMs: int
    endMs: int


class AlignResponse(BaseModel):
    words: List[WordTimestamp]


@router.post("/tts", response_model=TtsResponse)
async def tts(req: TtsRequest, request: Request) -> TtsResponse:
    try:
        # Si se solicita clonación con muestra de referencia, invocar motor de clonación
        if req.voiceEngine and req.voiceEngine.lower() in ("cloned", "chatterbox") and req.voiceReferencePath:
            import subprocess
            import uuid
            from pathlib import Path
            root_dir = Path(__file__).resolve().parent.parent.parent
            cli_script = root_dir / "scripts" / "cloned_tts.py"
            voice_lab_python = root_dir / "voice-lab" / ".venv" / "Scripts" / "python.exe"

            if voice_lab_python.exists() and cli_script.exists():
                out_file = req.outputPath or str(root_dir / "storage" / "audio" / f"tts_cloned_{uuid.uuid4().hex[:8]}.wav")
                cmd = [
                    str(voice_lab_python), str(cli_script),
                    "--text", req.text,
                    "--voice-ref", req.voiceReferencePath,
                    "--output", out_file,
                    "--exaggeration", str(req.expressiveness or 0.6)
                ]
                logger.info("Invocando clonación de voz vía voice-lab CLI para '%s'...", req.text[:30])
                res = subprocess.run(cmd, capture_output=True, text=True)
                if res.returncode == 0:
                    for line in res.stdout.splitlines():
                        if line.startswith("OK:"):
                            parts = line.split(":")
                            return TtsResponse(audioPath=parts[1], durationMs=int(parts[2]))
                logger.warning("Fallo en CLI de clonación (código %d): %s. Usando fallback Kokoro.", res.returncode, res.stderr)

        audio_path, duration_ms = synthesize_speech(
            text=req.text,
            voice=req.voice,
            rate=req.rate,
            output_path=req.outputPath,
        )
        return TtsResponse(audioPath=audio_path, durationMs=duration_ms)
    except Exception as exc:
        logger.error("Error en endpoint /tts: %s", exc, exc_info=True)
        raise HTTPException(status_code=500, detail=str(exc))


@router.post("/align", response_model=AlignResponse)
async def align(req: AlignRequest, request: Request) -> AlignResponse:
    try:
        settings = getattr(request.app.state, "settings", None)
        model_size = getattr(settings, "whisper_model", "base") if settings else "base"

        words = align_audio_timestamps(
            audio_path=req.audioPath,
            language=req.language,
            model_size=model_size,
            initial_prompt=req.text,
        )
        return AlignResponse(words=[WordTimestamp(**w) for w in words])
    except FileNotFoundError as exc:
        logger.warning("Archivo no encontrado en /align: %s", exc)
        raise HTTPException(status_code=404, detail=str(exc))
    except Exception as exc:
        logger.error("Error en endpoint /align: %s", exc, exc_info=True)
        raise HTTPException(status_code=500, detail=str(exc))
