from __future__ import annotations

import importlib.util
import platform

import httpx
from fastapi import FastAPI

from app.config import Settings, get_settings
from app.gpu_guard import GpuGuard, query_gpu

SERVICE_NAME = "kids-ai-gateway"


def _torch_info() -> dict:
    """Información de torch sin forzar su instalación (llega en Fase 4+)."""
    if importlib.util.find_spec("torch") is None:
        return {"installed": False}
    import torch  # noqa: PLC0415

    info = {"installed": True, "version": torch.__version__, "cuda_available": torch.cuda.is_available()}
    if torch.cuda.is_available():
        info["device"] = torch.cuda.get_device_name(0)
        info["capability"] = list(torch.cuda.get_device_capability(0))
        info["cuda_version"] = torch.version.cuda
    return info


async def _comfyui_status(settings: Settings) -> dict:
    url = f"{settings.comfyui_url.rstrip('/')}/system_stats"
    try:
        timeout = min(float(settings.probe_timeout_s), 1.0)
        async with httpx.AsyncClient(timeout=timeout) as client:
            resp = await client.get(url)
        if resp.status_code == 200:
            return {"status": "UP", "url": url, "stats": resp.json()}
        return {"status": "DOWN", "url": url, "detail": f"HTTP {resp.status_code}"}
    except Exception as exc:  # noqa: BLE001
        return {"status": "DOWN", "url": url, "detail": str(exc) or type(exc).__name__}


def create_app(settings: Settings | None = None) -> FastAPI:
    settings = settings or get_settings()
    app = FastAPI(title="KidsAnimationStudio AI Gateway", version="0.1.0")
    app.state.settings = settings
    app.state.gpu_guard = GpuGuard(
        min_free_mb=settings.gpu_min_free_mb,
        wait_timeout_s=settings.gpu_wait_timeout_min * 60,
    )

    from app.routers.image import router as image_router
    from app.routers.audio import router as audio_router
    from app.routers.qa import router as qa_router
    from app.routers.gpu import router as gpu_router
    from app.routers.video import router as video_router
    app.include_router(image_router)
    app.include_router(audio_router)
    app.include_router(qa_router)
    app.include_router(gpu_router)
    app.include_router(video_router)

    @app.get("/health")
    async def health() -> dict:
        gpu = query_gpu()
        return {
            "status": "UP",
            "service": SERVICE_NAME,
            "python": platform.python_version(),
            "gpu": None if gpu is None else {
                "name": gpu.name,
                "total_mb": gpu.total_mb,
                "used_mb": gpu.used_mb,
                "free_mb": gpu.free_mb,
                "meets_min_free": gpu.free_mb >= settings.gpu_min_free_mb,
            },
            "torch": _torch_info(),
            "comfyui": await _comfyui_status(settings),
        }

    return app


app = create_app()
