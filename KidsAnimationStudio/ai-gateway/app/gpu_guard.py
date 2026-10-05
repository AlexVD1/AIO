"""Coordinación de la GPU compartida con StoryVideoGenerator / SD Forge.

Regla: este servicio NUNCA controla Forge. Solo mide VRAM libre y espera.
"""
from __future__ import annotations

import asyncio
import shutil
import subprocess
import time
from dataclasses import dataclass
from typing import Callable, Optional


@dataclass(frozen=True)
class GpuInfo:
    name: str
    total_mb: int
    used_mb: int
    free_mb: int


class GpuBusyError(RuntimeError):
    """La GPU no liberó suficiente VRAM dentro del tiempo de espera."""


def query_gpu(runner: Callable[[list[str]], str] | None = None) -> Optional[GpuInfo]:
    """Consulta nvidia-smi. Devuelve None si no hay GPU NVIDIA o nvidia-smi no existe."""
    cmd = [
        "nvidia-smi",
        "--query-gpu=name,memory.total,memory.used,memory.free",
        "--format=csv,noheader,nounits",
    ]
    if runner is None:
        if shutil.which("nvidia-smi") is None:
            return None

        def runner(c: list[str]) -> str:
            return subprocess.run(c, capture_output=True, text=True, check=True, timeout=10).stdout

    try:
        line = runner(cmd).strip().splitlines()[0]
        name, total, used, free = [p.strip() for p in line.split(",")]
        return GpuInfo(name=name, total_mb=int(total), used_mb=int(used), free_mb=int(free))
    except Exception:
        return None


class GpuGuard:
    """Serializa el trabajo GPU de este proyecto y espera VRAM libre suficiente."""

    def __init__(
        self,
        min_free_mb: int,
        wait_timeout_s: float,
        poll_interval_s: float = 10.0,
        query: Callable[[], Optional[GpuInfo]] = query_gpu,
        clock: Callable[[], float] = time.monotonic,
    ) -> None:
        self.min_free_mb = min_free_mb
        self.wait_timeout_s = wait_timeout_s
        self.poll_interval_s = poll_interval_s
        self._query = query
        self._clock = clock
        self._lock = asyncio.Lock()

    @property
    def lock(self) -> asyncio.Lock:
        return self._lock

    async def wait_for_vram(self) -> GpuInfo:
        deadline = self._clock() + self.wait_timeout_s
        while True:
            info = self._query()
            if info is None:
                raise GpuBusyError("No se pudo consultar la GPU con nvidia-smi")
            if info.free_mb >= self.min_free_mb:
                return info
            if self._clock() >= deadline:
                raise GpuBusyError(
                    f"GPU ocupada: {info.free_mb} MB libres < {self.min_free_mb} MB requeridos "
                    "(¿SD Forge / StoryVideoGenerator generando?)"
                )
            await asyncio.sleep(self.poll_interval_s)
