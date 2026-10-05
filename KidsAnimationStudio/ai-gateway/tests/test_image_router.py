import pytest
from pathlib import Path
from httpx import ASGITransport, AsyncClient
from PIL import Image

from app.config import Settings
from app.main import create_app


@pytest.mark.anyio
async def test_character_sheet_endpoint(tmp_path: Path):
    app = create_app(Settings(gpu_min_free_mb=0))
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        resp = await client.post(
            "/image/character-sheet",
            json={
                "prompt": "a cute fox",
                "stylePrompt": "3d style",
                "negativePrompt": "ugly",
                "seed": 100,
                "count": 4,
                "outputDir": str(tmp_path),
            },
        )
        assert resp.status_code == 200
        data = resp.json()
        assert len(data["images"]) == 4
        assert data["seeds"] == [100, 101, 102, 103]


@pytest.mark.anyio
async def test_keyframe_endpoint(tmp_path: Path):
    app = create_app(Settings(gpu_min_free_mb=0))
    transport = ASGITransport(app=app)

    ref_img = tmp_path / "ref.png"
    Image.new("RGB", (64, 64), color="orange").save(ref_img)

    out_file = tmp_path / "keyframe_out.png"

    async with AsyncClient(transport=transport, base_url="http://test") as client:
        resp = await client.post(
            "/image/keyframe",
            json={
                "prompt": "a cute fox running in the forest",
                "negativePrompt": "blurry, low quality",
                "referenceImages": [{"path": str(ref_img), "weight": 0.85}],
                "seed": 42,
                "width": 512,
                "height": 512,
                "outputPath": str(out_file),
            },
        )
        assert resp.status_code == 200
        data = resp.json()
        assert Path(data["imagePath"]).exists()
        assert data["seed"] == 42


@pytest.mark.anyio
async def test_qa_similarity_endpoint(tmp_path: Path):
    app = create_app(Settings(gpu_min_free_mb=0))
    transport = ASGITransport(app=app)

    img1 = tmp_path / "img1.png"
    img2 = tmp_path / "img2.png"
    Image.new("RGB", (64, 64), color="orange").save(img1)
    Image.new("RGB", (64, 64), color="blue").save(img2)

    async with AsyncClient(transport=transport, base_url="http://test") as client:
        resp = await client.post(
            "/qa/similarity",
            json={
                "imagePath": str(img1),
                "referencePath": str(img2),
            },
        )
        assert resp.status_code == 200
        score = resp.json()["score"]
        assert 0.0 <= score <= 1.0

        # Test 404 when file does not exist
        resp_404 = await client.post(
            "/qa/similarity",
            json={
                "imagePath": str(tmp_path / "nonexistent.png"),
                "referencePath": str(img2),
            },
        )
        assert resp_404.status_code == 404


@pytest.mark.anyio
async def test_gpu_free_endpoint():
    app = create_app(Settings(gpu_min_free_mb=0))
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        resp = await client.post("/gpu/free")
        assert resp.status_code == 200
        data = resp.json()
        assert "freedMb" in data
        assert isinstance(data["freedMb"], int)
