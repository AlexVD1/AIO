import pytest
from pathlib import Path
from httpx import ASGITransport, AsyncClient
from PIL import Image

from app.config import Settings
from app.main import create_app


@pytest.mark.anyio
async def test_video_i2v_ltx_success(tmp_path: Path):
    app = create_app(Settings(gpu_min_free_mb=0))
    transport = ASGITransport(app=app)

    keyframe = tmp_path / "keyframe_01.png"
    Image.new("RGB", (768, 512), color="orange").save(keyframe)

    out_clip = tmp_path / "clip_01.mp4"

    async with AsyncClient(transport=transport, base_url="http://test") as client:
        resp = await client.post(
            "/video/i2v",
            json={
                "model": "LTX",
                "imagePath": str(keyframe),
                "actionPrompt": "the fox waves happily",
                "negativePrompt": "worst quality, jitter",
                "durationMs": 2000,
                "fps": 16,
                "width": 768,
                "height": 512,
                "seed": 42,
                "outputPath": str(out_clip),
            },
        )
        assert resp.status_code == 200
        data = resp.json()

        assert Path(data["clipPath"]).exists()
        assert Path(data["clipPath"]).stat().st_size > 0
        assert Path(data["lastFramePath"]).exists()
        assert Path(data["lastFramePath"]).stat().st_size > 0
        assert data["frames"] == 32
        assert data["durationMs"] == 2000


@pytest.mark.anyio
async def test_video_i2v_wan_success(tmp_path: Path):
    app = create_app(Settings(gpu_min_free_mb=0))
    transport = ASGITransport(app=app)

    keyframe = tmp_path / "keyframe_02.png"
    Image.new("RGB", (768, 512), color="blue").save(keyframe)

    async with AsyncClient(transport=transport, base_url="http://test") as client:
        resp = await client.post(
            "/video/i2v",
            json={
                "model": "WAN",
                "imagePath": str(keyframe),
                "actionPrompt": "the trees sway gently in the breeze",
                "durationMs": 1500,
                "fps": 16,
                "width": 768,
                "height": 512,
                "seed": 100,
            },
        )
        assert resp.status_code == 200
        data = resp.json()

        assert Path(data["clipPath"]).exists()
        assert Path(data["lastFramePath"]).exists()
        assert data["frames"] == 24
        assert data["durationMs"] == 1500


@pytest.mark.anyio
async def test_video_i2v_404_when_image_missing(tmp_path: Path):
    app = create_app(Settings(gpu_min_free_mb=0))
    transport = ASGITransport(app=app)

    missing_image = tmp_path / "does_not_exist.png"

    async with AsyncClient(transport=transport, base_url="http://test") as client:
        resp = await client.post(
            "/video/i2v",
            json={
                "model": "LTX",
                "imagePath": str(missing_image),
                "actionPrompt": "test",
                "durationMs": 2000,
            },
        )
        assert resp.status_code == 404
