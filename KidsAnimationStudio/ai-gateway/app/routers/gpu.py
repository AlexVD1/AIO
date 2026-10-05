from __future__ import annotations

import logging
from fastapi import APIRouter, Request
from pydantic import BaseModel

from app.services.comfy_client import free_comfyui_gpu

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/gpu", tags=["GPU"])


class GpuFreeResponse(BaseModel):
    freedMb: int


@router.post("/free", response_model=GpuFreeResponse)
async def free_gpu(request: Request) -> GpuFreeResponse:
    settings = getattr(request.app.state, "settings", None)
    comfyui_url = settings.comfyui_url if settings else "http://localhost:8188"

    freed = await free_comfyui_gpu(comfyui_url)
    return GpuFreeResponse(freedMb=freed)
