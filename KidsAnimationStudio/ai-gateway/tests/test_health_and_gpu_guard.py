import asyncio

import httpx
import pytest
import respx
from fastapi.testclient import TestClient

from app import main as main_module
from app.config import Settings
from app.gpu_guard import GpuBusyError, GpuGuard, GpuInfo, query_gpu


def test_query_gpu_parses_nvidia_smi_output():
    info = query_gpu(runner=lambda _cmd: "NVIDIA GeForce RTX 5070, 12227, 3083, 9144\n")
    assert info == GpuInfo(name="NVIDIA GeForce RTX 5070", total_mb=12227, used_mb=3083, free_mb=9144)


def test_query_gpu_returns_none_on_garbage():
    assert query_gpu(runner=lambda _cmd: "garbage") is None


def test_gpu_guard_returns_when_enough_vram():
    guard = GpuGuard(min_free_mb=9000, wait_timeout_s=1, query=lambda: GpuInfo("g", 12000, 1000, 11000))
    info = asyncio.run(guard.wait_for_vram())
    assert info.free_mb == 11000


def test_gpu_guard_times_out_when_gpu_busy():
    ticks = iter([0.0, 0.0, 5.0])
    guard = GpuGuard(
        min_free_mb=9000,
        wait_timeout_s=1,
        poll_interval_s=0,
        query=lambda: GpuInfo("g", 12000, 8000, 4000),
        clock=lambda: next(ticks),
    )
    with pytest.raises(GpuBusyError, match="GPU ocupada"):
        asyncio.run(guard.wait_for_vram())


def test_gpu_guard_waits_until_vram_is_released():
    readings = iter([GpuInfo("g", 12000, 8000, 4000), GpuInfo("g", 12000, 1000, 11000)])
    guard = GpuGuard(min_free_mb=9000, wait_timeout_s=60, poll_interval_s=0, query=lambda: next(readings))
    assert asyncio.run(guard.wait_for_vram()).free_mb == 11000


@respx.mock
def test_health_reports_comfyui_down_but_gateway_up(monkeypatch):
    monkeypatch.setattr(main_module, "query_gpu", lambda: GpuInfo("RTX", 12227, 3000, 9227))
    respx.get("http://comfy.test:8188/system_stats").mock(side_effect=httpx.ConnectError("refused"))
    app = main_module.create_app(Settings(comfyui_url="http://comfy.test:8188"))

    body = TestClient(app).get("/health").json()

    assert body["status"] == "UP"
    assert body["gpu"]["free_mb"] == 9227
    assert body["gpu"]["meets_min_free"] is True
    assert body["comfyui"]["status"] == "DOWN"


@respx.mock
def test_health_reports_comfyui_up(monkeypatch):
    monkeypatch.setattr(main_module, "query_gpu", lambda: None)
    respx.get("http://comfy.test:8188/system_stats").mock(return_value=httpx.Response(200, json={"system": {}}))
    app = main_module.create_app(Settings(comfyui_url="http://comfy.test:8188"))

    body = TestClient(app).get("/health").json()

    assert body["gpu"] is None
    assert body["comfyui"]["status"] == "UP"
