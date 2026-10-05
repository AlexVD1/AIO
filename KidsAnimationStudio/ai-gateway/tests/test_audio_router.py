from __future__ import annotations

from unittest.mock import patch
import pytest
from httpx import ASGITransport, AsyncClient

from app.main import create_app


@pytest.fixture
def app():
    return create_app()


@pytest.mark.anyio
async def test_tts_endpoint_returns_audio_path_and_duration(app, tmp_path):
    out_file = tmp_path / "speech.wav"
    mock_duration = 3250

    with patch("app.routers.audio.synthesize_speech", return_value=(str(out_file), mock_duration)) as mock_synth:
        transport = ASGITransport(app=app)
        async with AsyncClient(transport=transport, base_url="http://test") as client:
            resp = await client.post(
                "/tts",
                json={
                    "text": "¡Hola amiguitos!",
                    "voice": "ef_dora",
                    "rate": "-12%",
                    "outputPath": str(out_file),
                },
            )

        assert resp.status_code == 200
        data = resp.json()
        assert data["audioPath"] == str(out_file)
        assert data["durationMs"] == mock_duration
        mock_synth.assert_called_once_with(
            text="¡Hola amiguitos!",
            voice="ef_dora",
            rate="-12%",
            output_path=str(out_file),
        )


@pytest.mark.anyio
async def test_align_endpoint_returns_word_timestamps(app, tmp_path):
    audio_file = tmp_path / "sample.wav"
    audio_file.touch()

    mock_words = [
        {"word": "Hola", "startMs": 0, "endMs": 400},
        {"word": "amiguitos", "startMs": 420, "endMs": 1100},
    ]

    with patch("app.routers.audio.align_audio_timestamps", return_value=mock_words) as mock_align:
        transport = ASGITransport(app=app)
        async with AsyncClient(transport=transport, base_url="http://test") as client:
            resp = await client.post(
                "/align",
                json={
                    "audioPath": str(audio_file),
                    "language": "es",
                },
            )

        assert resp.status_code == 200
        data = resp.json()
        assert len(data["words"]) == 2
        assert data["words"][0]["word"] == "Hola"
        assert data["words"][0]["startMs"] == 0
        assert data["words"][0]["endMs"] == 400
        assert data["words"][1]["word"] == "amiguitos"


@pytest.mark.anyio
async def test_align_endpoint_returns_404_for_missing_file(app):
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        resp = await client.post(
            "/align",
            json={
                "audioPath": "C:/non/existent/file.wav",
                "language": "es",
            },
        )

    assert resp.status_code == 404
