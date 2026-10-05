from __future__ import annotations

import logging
from pathlib import Path
from typing import Optional

from fastapi import APIRouter, HTTPException, Request
from pydantic import BaseModel, Field

from app.services.comfy_client import generate_video_i2v_comfyui

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/video", tags=["Video"])


class VideoI2VRequest(BaseModel):
    model: str = Field("LTX", description="Modelo I2V a emplear: 'LTX' o 'WAN'")
    imagePath: str = Field(..., description="Ruta absoluta de la imagen de entrada (keyframe)")
    actionPrompt: str = Field(..., description="Descripción del movimiento y acción del plano")
    negativePrompt: str = Field("", description="Negative prompt de animación")
    durationMs: int = Field(4000, ge=1000, le=10000, description="Duración del clip en milisegundos")
    fps: int = Field(24, ge=8, le=30, description="Cuadros por segundo")
    width: int = Field(1024, ge=256, le=1920, description="Ancho de generación")
    height: int = Field(576, ge=256, le=1080, description="Alto de generación")
    seed: int = Field(42, description="Semilla aleatoria")
    outputPath: Optional[str] = Field(None, description="Ruta absoluta de salida para el video MP4")


class VideoI2VResponse(BaseModel):
    clipPath: str
    lastFramePath: str
    frames: int
    durationMs: int


@router.post("/i2v", response_model=VideoI2VResponse)
async def generate_video_i2v(req: VideoI2VRequest, request: Request) -> VideoI2VResponse:
    logger.info(
        "Solicitud de video I2V: model=%s, res=%dx%d, fps=%d, dur=%dms, seed=%d",
        req.model, req.width, req.height, req.fps, req.durationMs, req.seed
    )

    if req.model.upper() == "PUPPET":
        import sys
        from scripts.puppet_renderer import PuppetTimelineRenderer

        root_dir = Path(__file__).resolve().parent.parent.parent
        char_dir = str(root_dir / "assets" / "characters" / "tito")
        loc_dir = str(root_dir / "assets" / "locations" / "huerto")
        props_dir = str(root_dir / "assets" / "props")

        out_file = req.outputPath or str(root_dir / "storage" / "clips" / f"clip_puppet_{req.seed}.mp4")
        dur_s = req.durationMs / 1000.0

        prop = "apple" if "manzana" in req.actionPrompt.lower() or "apple" in req.actionPrompt.lower() else None
        action = "wave" if "salud" in req.actionPrompt.lower() or "wave" in req.actionPrompt.lower() else "idle"

        renderer = PuppetTimelineRenderer(char_dir, loc_dir, props_dir)
        renderer.render_shot_video(
            output_mp4=out_file,
            duration_s=dur_s,
            action=action,
            show_prop=prop,
            fps=req.fps
        )

        last_frame_path = out_file.replace(".mp4", "_last.png")
        # Extraer último frame para continuidad
        last_frame = renderer.render_frame(time_s=dur_s, action=action, show_prop=prop)
        last_frame.save(last_frame_path)

        return VideoI2VResponse(
            clipPath=out_file,
            lastFramePath=last_frame_path,
            frames=int(round(dur_s * req.fps)),
            durationMs=req.durationMs
        )

    if not Path(req.imagePath).exists():
        logger.warning("Imagen de entrada no encontrada: %s", req.imagePath)
        raise HTTPException(status_code=404, detail=f"Imagen no encontrada: {req.imagePath}")

    gpu_guard = getattr(request.app.state, "gpu_guard", None)

    async def _execute() -> VideoI2VResponse:
        settings = getattr(request.app.state, "settings", None)
        comfyui_url = settings.comfyui_url if settings else "http://localhost:8188"

        clip_path, last_frame_path, frames, duration_ms = await generate_video_i2v_comfyui(
            model=req.model,
            image_path=req.imagePath,
            action_prompt=req.actionPrompt,
            negative_prompt=req.negativePrompt,
            duration_ms=req.durationMs,
            fps=req.fps,
            width=req.width,
            height=req.height,
            seed=req.seed,
            output_path=req.outputPath,
            comfyui_url=comfyui_url,
        )

        return VideoI2VResponse(
            clipPath=clip_path,
            lastFramePath=last_frame_path,
            frames=frames,
            durationMs=duration_ms,
        )

    try:
        if gpu_guard:
            async with gpu_guard.lock:
                if gpu_guard.min_free_mb > 0:
                    await gpu_guard.wait_for_vram()
                return await _execute()
        else:
            return await _execute()
    except Exception as exc:
        logger.error("Error generando video I2V: %s", exc, exc_info=True)
        raise HTTPException(status_code=500, detail=str(exc))
