from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Configuración del ai-gateway. Lee variables de entorno y el .env del proyecto."""

    model_config = SettingsConfigDict(env_file=("../.env", ".env"), extra="ignore")

    gateway_port: int = 8090
    comfyui_url: str = "http://localhost:8188"
    gpu_min_free_mb: int = 0
    gpu_wait_timeout_min: int = 30
    probe_timeout_s: float = 2.5

    # Audio & TTS
    tts_engine: str = "kokoro"
    tts_default_voice: str = "ef_dora"
    tts_default_rate: str = "+0%"
    whisper_model: str = "base"
    storage_path: str = "./storage"


@lru_cache
def get_settings() -> Settings:
    return Settings()
