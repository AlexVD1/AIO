import json
import pytest
import respx
import httpx
from pathlib import Path
from PIL import Image

from app.services.comfy_client import (
    load_workflow_with_title_updates,
    generate_keyframe_comfyui,
    free_comfyui_gpu,
    generate_ken_burns_clip,
    extract_last_frame
)


def test_load_workflow_with_title_updates(tmp_path):
    workflow_file = tmp_path / "mock_workflow.json"
    dummy_workflow = {
        "1": {
            "_meta": {"title": "INPUT_POSITIVE"},
            "inputs": {"text": "default"}
        },
        "2": {
            "_meta": {"title": "KSAMPLER"},
            "inputs": {"seed": 1}
        }
    }
    workflow_file.write_text(json.dumps(dummy_workflow), encoding="utf-8")

    updates = {
        "INPUT_POSITIVE": {"text": "Tito el zorrito en el huerto"},
        "KSAMPLER": {"seed": 9999}
    }

    updated = load_workflow_with_title_updates(workflow_file, updates)
    assert updated["1"]["inputs"]["text"] == "Tito el zorrito en el huerto"
    assert updated["2"]["inputs"]["seed"] == 9999


@pytest.mark.anyio
@respx.mock
async def test_generate_keyframe_comfyui_full_flow(tmp_path):
    comfy_url = "http://localhost:8188"
    out_img = tmp_path / "out_kf.png"

    # Mock system stats
    respx.get(f"{comfy_url}/system_stats").respond(status_code=200, json={"system": {"os": "nt"}})

    # Mock prompt
    prompt_id = "test-prompt-123"
    respx.post(f"{comfy_url}/prompt").respond(status_code=200, json={"prompt_id": prompt_id})

    # Mock history
    history_data = {
        prompt_id: {
            "outputs": {
                "9": {
                    "images": [
                        {"filename": "out_0001.png", "subfolder": "", "type": "output"}
                    ]
                }
            }
        }
    }
    respx.get(f"{comfy_url}/history/{prompt_id}").respond(status_code=200, json=history_data)

    # Crear una imagen png real en memoria para responder en /view
    dummy_png = tmp_path / "dummy.png"
    Image.new("RGB", (64, 64), color="orange").save(str(dummy_png), "PNG")
    dummy_bytes = dummy_png.read_bytes()

    respx.get(url__startswith=f"{comfy_url}/view").respond(
        status_code=200,
        content=dummy_bytes,
        headers={"Content-Type": "image/png"}
    )

    path, seed = await generate_keyframe_comfyui(
        prompt="Zorrito amigable",
        seed=105,
        output_path=str(out_img),
        comfyui_url=comfy_url,
        timeout_s=5.0
    )

    assert Path(path).exists()
    assert seed == 105
    assert Path(path).stat().st_size > 0


@pytest.mark.anyio
@respx.mock
async def test_generate_keyframe_comfyui_fallback_on_error(tmp_path):
    comfy_url = "http://localhost:8188"
    out_img = tmp_path / "out_fallback.png"

    # ComfyUI offline
    respx.get(f"{comfy_url}/system_stats").respond(status_code=500)

    path, seed = await generate_keyframe_comfyui(
        prompt="Plano didactico",
        seed=42,
        output_path=str(out_img),
        comfyui_url=comfy_url
    )

    assert Path(path).exists()
    assert seed == 42


@pytest.mark.anyio
@respx.mock
async def test_free_comfyui_gpu_success():
    comfy_url = "http://localhost:8188"
    respx.post(f"{comfy_url}/free").respond(status_code=200, json={})

    freed = await free_comfyui_gpu(comfyui_url=comfy_url)
    assert freed == 1024


@pytest.mark.anyio
@respx.mock
async def test_free_comfyui_gpu_offline():
    comfy_url = "http://localhost:8188"
    respx.post(f"{comfy_url}/free").respond(status_code=503)

    freed = await free_comfyui_gpu(comfyui_url=comfy_url)
    assert freed == 0


def test_ken_burns_clip_generation(tmp_path):
    in_img = tmp_path / "test_input.png"
    Image.new("RGB", (320, 240), color="blue").save(str(in_img), "PNG")

    out_mp4 = tmp_path / "ken_burns.mp4"
    clip_path, last_frame_path, frames = generate_ken_burns_clip(
        image_path=str(in_img),
        output_path=str(out_mp4),
        duration_ms=1000,
        fps=15,
        width=320,
        height=240
    )

    assert Path(clip_path).exists()
    assert Path(clip_path).stat().st_size > 500
    assert Path(last_frame_path).exists()
    assert frames > 0
