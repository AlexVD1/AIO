# KidsAnimationStudio

Generador **local y automatizado** de videos animados educativos para niños menores de 7 años.
Es un proyecto hermano **independiente** de `StoryVideoGenerator`: no comparte código, base de datos, puertos ni carpetas con él.

| Componente | Puerto | Ubicación |
| :--- | :--- | :--- |
| Orquestador (Spring Boot 3.3 / Java 21) | 8082 | `orchestrator/` |
| AI Gateway (FastAPI) | 8090 | `ai-gateway/` |
| Postgres `kids-postgres` / `kidsdb` | 5434 | `compose.yaml` |
| ComfyUI (NVIDIA) | 8188 | instalación externa (`C:\ComfyUI_NV`) |
| Ollama | 11434 | instalación externa |

## Arranque rápido

```powershell
Copy-Item .env.example .env   # y editar contraseñas
cd ai-gateway; python -m venv .venv; .\.venv\Scripts\pip install -r requirements.txt; cd ..
.\start-dev.ps1
```

- Health: http://localhost:8082/api/v1/health → `UP` / `DEGRADED` (falta algún motor de IA) / `DOWN` (sin DB)
- Health del gateway: http://localhost:8090/health (VRAM, torch/CUDA, ComfyUI)

## Tests

```powershell
cd orchestrator; mvn test
cd ai-gateway; .\.venv\Scripts\python -m pytest
```

## Documentación
- [docs/IMPLEMENTATION_PLAN.md](docs/IMPLEMENTATION_PLAN.md) – Plan completo de arquitectura e implementación (Fases 0 a 10).
- [docs/SETUP_GPU_WINDOWS.md](docs/SETUP_GPU_WINDOWS.md) – Fase 0 (ComfyUI NVIDIA, modelos, Ollama).
- [docs/PROGRESS.md](docs/PROGRESS.md) – avance por fase.
