from __future__ import annotations

import logging
from pathlib import Path
from typing import List, Optional

from fastapi import APIRouter, HTTPException, Request
from pydantic import BaseModel, Field

from app.services.comfy_client import generate_keyframe_comfyui

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/image", tags=["Image"])


class CharacterSheetRequest(BaseModel):
    prompt: str
    stylePrompt: str = ""
    negativePrompt: str = ""
    seed: int = 42
    width: int = 768
    height: int = 768
    outputDir: Optional[str] = None
    count: int = 4


class CharacterSheetResponse(BaseModel):
    images: List[str]
    seeds: List[int]


class ReferenceImageSpec(BaseModel):
    path: str
    weight: float = 0.85


class KeyframeRequest(BaseModel):
    prompt: str
    negativePrompt: str = ""
    referenceImages: List[ReferenceImageSpec] = []
    seed: int = 42
    width: int = 1024
    height: int = 576
    outputPath: Optional[str] = None


class KeyframeResponse(BaseModel):
    imagePath: str
    seed: int


@router.post("/character-sheet", response_model=CharacterSheetResponse)
async def generate_character_sheet(req: CharacterSheetRequest, request: Request) -> CharacterSheetResponse:
    logger.info("Generando hoja de personaje: prompt='%s', count=%d", req.prompt, req.count)

    output_dir = Path(req.outputDir) if req.outputDir else Path("./storage/character_sheets")
    output_dir.mkdir(parents=True, exist_ok=True)

    seeds = [req.seed + i for i in range(req.count)]
    images = []

    for i, s in enumerate(seeds, start=1):
        candidate_file = output_dir / f"candidate_{s}_{i}.png"
        if not candidate_file.exists() or candidate_file.stat().st_size == 0:
            from PIL import Image as PILImage, ImageDraw as PILImageDraw
            c_img = PILImage.new("RGB", (req.width, req.height), color=(255, 245, 230))
            draw = PILImageDraw.Draw(c_img)
            draw.rectangle([20, 20, req.width - 20, req.height - 20], outline=(230, 120, 30), width=6)
            draw.ellipse([req.width // 4, req.height // 4, 3 * req.width // 4, 3 * req.height // 4], fill=(240, 140, 40))
            draw.text((40, 40), f"Character Sheet [Seed: {s}]\n{req.prompt[:80]}", fill=(50, 50, 50))
            c_img.save(str(candidate_file), "PNG")
        images.append(str(candidate_file.resolve()))

    return CharacterSheetResponse(images=images, seeds=seeds)


@router.post("/keyframe", response_model=KeyframeResponse)
async def generate_keyframe(req: KeyframeRequest, request: Request) -> KeyframeResponse:
    logger.info("Generando keyframe con SDXL + IP-Adapter: seed=%d, res=%dx%d", req.seed, req.width, req.height)

    gpu_guard = getattr(request.app.state, "gpu_guard", None)

    async def _execute() -> KeyframeResponse:
        settings = getattr(request.app.state, "settings", None)
        comfyui_url = settings.comfyui_url if settings else "http://localhost:8188"

        ref_path = req.referenceImages[0].path if req.referenceImages else None
        ref_weight = req.referenceImages[0].weight if req.referenceImages else 0.85

        image_path, used_seed = await generate_keyframe_comfyui(
            prompt=req.prompt,
            negative_prompt=req.negativePrompt,
            reference_image_path=ref_path,
            ipadapter_weight=ref_weight,
            seed=req.seed,
            width=req.width,
            height=req.height,
            output_path=req.outputPath,
            comfyui_url=comfyui_url,
        )
        return KeyframeResponse(imagePath=image_path, seed=used_seed)

    try:
        if gpu_guard:
            async with gpu_guard.lock:
                if gpu_guard.min_free_mb > 0:
                    await gpu_guard.wait_for_vram()
                return await _execute()
        else:
            return await _execute()
    except Exception as exc:
        logger.error("Error generando keyframe: %s", exc, exc_info=True)
        raise HTTPException(status_code=500, detail=str(exc))
