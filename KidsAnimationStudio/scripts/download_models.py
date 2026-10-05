"""Descarga de modelos para KidsAnimationStudio (Fase 0).

Usa huggingface_hub + hf_xet (más rápido y fiable que curl contra el CDN xet).
Guarda en C:\\ComfyUI_NV\\models\\<tipo>\\<nombre>; ComfyUI los lee vía extra_model_paths.yaml.
Reanudable: los archivos ya completos se saltan.

Uso:  python scripts/download_models.py [image|ltx|wan|all]
"""
from __future__ import annotations

import shutil
import sys
import time
from pathlib import Path

from huggingface_hub import hf_hub_download

ROOT = Path(r"C:\ComfyUI_NV\models")
CACHE = Path(r"C:\ComfyUI_NV\hf_cache")

# (grupo, carpeta destino, repo, archivo en el repo, nombre final)
MODELS = [
    ("image", "checkpoints", "Lykon/dreamshaper-xl-v2-turbo", "DreamShaperXL_Turbo_V2-SFW.safetensors", None),
    ("image", "ipadapter", "h94/IP-Adapter", "sdxl_models/ip-adapter-plus_sdxl_vit-h.safetensors", None),
    ("image", "clip_vision", "h94/IP-Adapter", "models/image_encoder/model.safetensors",
     "CLIP-ViT-H-14-laion2B-s32B-b79K.safetensors"),
    ("ltx", "checkpoints", "Lightricks/LTX-Video", "ltxv-2b-0.9.8-distilled.safetensors", None),
    ("ltx", "text_encoders", "comfyanonymous/flux_text_encoders", "t5xxl_fp8_e4m3fn_scaled.safetensors", None),
    ("wan", "diffusion_models", "Comfy-Org/Wan_2.2_ComfyUI_Repackaged",
     "split_files/diffusion_models/wan2.2_ti2v_5B_fp16.safetensors", None),
    ("wan", "vae", "Comfy-Org/Wan_2.2_ComfyUI_Repackaged", "split_files/vae/wan2.2_vae.safetensors", None),
    ("wan", "text_encoders", "Comfy-Org/Wan_2.1_ComfyUI_repackaged",
     "split_files/text_encoders/umt5_xxl_fp8_e4m3fn_scaled.safetensors", None),
]


def main(group: str) -> int:
    failures = 0
    for g, folder, repo, filename, final_name in MODELS:
        if group != "all" and g != group:
            continue
        target = ROOT / folder / (final_name or Path(filename).name)
        if target.exists() and target.stat().st_size > 0:
            print(f"[skip] {target} ({target.stat().st_size / 1e9:.2f} GB)", flush=True)
            continue
        target.parent.mkdir(parents=True, exist_ok=True)
        print(f"[{time.strftime('%H:%M:%S')}] {repo}/{filename} -> {target}", flush=True)
        try:
            downloaded = Path(hf_hub_download(repo_id=repo, filename=filename, cache_dir=str(CACHE)))
            # Mover desde la caché (resolviendo symlink) para no duplicar espacio
            src = downloaded.resolve()
            shutil.move(str(src), str(target))
            print(f"   OK {target.stat().st_size / 1e9:.2f} GB", flush=True)
        except Exception as exc:  # noqa: BLE001
            failures += 1
            print(f"   ERROR {exc}", flush=True)
    print(f"FIN grupo={group} fallos={failures}", flush=True)
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1] if len(sys.argv) > 1 else "all"))
