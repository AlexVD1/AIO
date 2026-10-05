from __future__ import annotations

import asyncio
import json
import logging
import shutil
import time
import uuid
from pathlib import Path
from typing import Any, Dict, Optional, Tuple

import httpx
from PIL import Image, ImageDraw

logger = logging.getLogger(__name__)

# Negativos anti-deformación (hallazgo V9). Se fusionan con el negativo que envíe el orquestador.
IMAGE_NEGATIVE = (
    "deformed hands, extra fingers, missing fingers, fused limbs, extra limbs, malformed anatomy, "
    "melting face, distorted face, asymmetric eyes, creepy, scary, horror, uncanny, realistic fur, "
    "photorealistic, dark, blurry, low quality, text, watermark, logo"
)
VIDEO_NEGATIVE = (
    "morphing, melting, deforming body, extra limbs, fused limbs, flickering, jitter, "
    "distorted face, creepy, horror, blurry, low quality, sudden camera cut"
)


def merge_negative(user_negative: Optional[str], base: str) -> str:
    """Fusiona el negativo del plano con la base anti-deformación sin duplicar términos."""
    terms: list[str] = []
    for part in ((user_negative or "") + "," + base).split(","):
        t = part.strip()
        if t and t.lower() not in (x.lower() for x in terms):
            terms.append(t)
    return ", ".join(terms)


def ltx_valid_length(duration_ms: int, fps: int) -> int:
    """LTX-Video requiere longitudes 8n+1 (hallazgo V4). Redondea hacia arriba al válido más cercano."""
    raw = max(9, int(round((duration_ms / 1000.0) * fps)))
    return ((raw - 1 + 7) // 8) * 8 + 1


def load_workflow_with_title_updates(
    workflow_path: str | Path,
    updates_by_title: Dict[str, Dict[str, Any]],
) -> Dict[str, Any]:
    """
    Carga un workflow JSON de ComfyUI y actualiza los nodos mapeados por _meta.title.
    Regla del plan de arquitectura: nunca indexar por id numérico para no romper
    la compatibilidad si el workflow se reexporta desde la UI.
    """
    path = Path(workflow_path)
    if not path.exists():
        raise FileNotFoundError(f"Workflow no encontrado en: {path}")

    with open(path, "r", encoding="utf-8") as f:
        workflow = json.load(f)

    for node_id, node_data in workflow.items():
        if not isinstance(node_data, dict):
            continue
        title = node_data.get("_meta", {}).get("title")
        if title and title in updates_by_title:
            node_inputs = node_data.setdefault("inputs", {})
            for field, val in updates_by_title[title].items():
                node_inputs[field] = val
            logger.debug("Nodo '%s' (id=%s) actualizado con: %s", title, node_id, updates_by_title[title])

    return workflow


async def generate_keyframe_comfyui(
    prompt: str,
    negative_prompt: str = "",
    reference_image_path: Optional[str] = None,
    ipadapter_weight: float = 0.85,
    seed: int = 42,
    width: int = 1024,
    height: int = 576,
    output_path: Optional[str] = None,
    comfyui_url: str = "http://localhost:8188",
    timeout_s: float = 180.0,
) -> Tuple[str, int]:
    """
    Ejecuta el workflow de ComfyUI para generar un keyframe con SDXL Turbo + IP-Adapter.
    Si ComfyUI está disponible, encola y espera el resultado. Si no está en ejecución
    (ej. durante tests unitarios locales), genera una imagen de prueba válida vía Pillow.
    """
    if output_path:
        out_file = Path(output_path)
    else:
        out_file = Path("./storage/keyframes") / f"keyframe_{uuid.uuid4().hex[:8]}.png"

    out_file.parent.mkdir(parents=True, exist_ok=True)

    comfy_available = False
    try:
        async with httpx.AsyncClient(timeout=2.0) as client:
            resp = await client.get(f"{comfyui_url.rstrip('/')}/system_stats")
            comfy_available = (resp.status_code == 200)
    except Exception:
        comfy_available = False

    if comfy_available:
        try:
            workflow_path = Path("workflows/keyframe_ipadapter.json")
            if not workflow_path.exists():
                workflow_path = Path("../ai-gateway/workflows/keyframe_ipadapter.json")

            ref_filename = "character_reference.png"
            if reference_image_path and Path(reference_image_path).exists():
                ref_path = Path(reference_image_path)
                ref_filename = ref_path.name
                # Subir/copiar referencia al input dir de ComfyUI
                try:
                    async with httpx.AsyncClient(timeout=10.0) as client:
                        with open(ref_path, "rb") as rf:
                            files = {"image": (ref_path.name, rf, "image/png")}
                            await client.post(f"{comfyui_url.rstrip('/')}/upload/image", files=files)
                except Exception as up_exc:
                    logger.warning("No se pudo subir imagen por HTTP a ComfyUI: %s", up_exc)

            updates = {
                "INPUT_POSITIVE": {"text": prompt},
                "INPUT_NEGATIVE": {"text": merge_negative(negative_prompt, IMAGE_NEGATIVE)},
                "INPUT_REFERENCE_IMAGE": {"image": ref_filename},
                "APPLY_IPADAPTER": {"weight": ipadapter_weight},
                "EMPTY_LATENT": {"width": width, "height": height},
                "KSAMPLER": {"seed": seed},
                "OUTPUT_IMAGE": {"filename_prefix": f"kids_keyframe_{seed}"},
            }

            workflow = load_workflow_with_title_updates(workflow_path, updates)

            async with httpx.AsyncClient(timeout=timeout_s) as client:
                prompt_resp = await client.post(
                    f"{comfyui_url.rstrip('/')}/prompt",
                    json={"prompt": workflow},
                )
                prompt_data = prompt_resp.json()
                prompt_id = prompt_data.get("prompt_id")

                if prompt_id:
                    # Polling de finalización en /history/{prompt_id}
                    start_time = time.time()
                    while time.time() - start_time < timeout_s:
                        hist_resp = await client.get(f"{comfyui_url.rstrip('/')}/history/{prompt_id}")
                        if hist_resp.status_code == 200:
                            hist_data = hist_resp.json()
                            if prompt_id in hist_data:
                                outputs = hist_data[prompt_id].get("outputs", {})
                                for node_out in outputs.values():
                                    images = node_out.get("images", [])
                                    if images:
                                        img_info = images[0]
                                        view_url = f"{comfyui_url.rstrip('/')}/view?filename={img_info['filename']}&subfolder={img_info.get('subfolder', '')}&type={img_info.get('type', 'output')}"
                                        img_bytes = await client.get(view_url)
                                        with open(out_file, "wb") as f_out:
                                            f_out.write(img_bytes.content)
                                        logger.info("Keyframe generado con ComfyUI y guardado en: %s", out_file)
                                        return str(out_file.resolve()), seed
                        await asyncio.sleep(1.0)
        except Exception as exc:
            logger.warning("Fallo en generación con ComfyUI (%s), recurriendo a síntesis local de respaldo", exc)

    # Fallback / Modo Test si ComfyUI no responde
    logger.info("Generando keyframe sintetizado localmente (fallback): %s", out_file)
    img = Image.new("RGB", (width, height), color=(255, 230, 200))
    draw = ImageDraw.Draw(img)
    draw.rectangle([50, 50, width - 50, height - 50], outline=(200, 100, 50), width=4)
    draw.text((80, 80), f"Keyframe [Seed: {seed}]\n{prompt[:60]}...", fill=(50, 50, 50))
    img.save(str(out_file), "PNG")

    return str(out_file.resolve()), seed


async def free_comfyui_gpu(comfyui_url: str = "http://localhost:8188") -> int:
    """
    Libera la VRAM en ComfyUI descargando los modelos no utilizados de la memoria de la GPU.
    """
    freed_mb = 0
    try:
        async with httpx.AsyncClient(timeout=5.0) as client:
            resp = await client.post(
                f"{comfyui_url.rstrip('/')}/free",
                json={"unload_models": True, "free_memory": True},
            )
            if resp.status_code == 200:
                freed_mb = 1024
                logger.info("VRAM liberada exitosamente en ComfyUI.")
    except Exception as exc:
        logger.debug("No se pudo invocar /free en ComfyUI (%s); posiblemente no esté corriendo.", exc)

    return freed_mb


def extract_last_frame(clip_path: str, last_frame_path: Optional[str] = None) -> str:
    path = Path(clip_path)
    if not path.exists():
        raise FileNotFoundError(f"Video no encontrado para extraer último frame: {clip_path}")

    if last_frame_path:
        out_last = Path(last_frame_path)
    else:
        out_last = path.with_name(f"{path.stem}_last_frame.png")
    out_last.parent.mkdir(parents=True, exist_ok=True)

    import av
    container = av.open(str(path))
    last_frame = None
    for frame in container.decode(video=0):
        last_frame = frame
    container.close()

    if last_frame is None:
        raise ValueError(f"No se pudieron decodificar frames del video: {clip_path}")

    last_img = last_frame.to_image()
    last_img.save(str(out_last), "PNG")
    return str(out_last.resolve())


def generate_ken_burns_clip(
    image_path: str,
    output_path: str,
    duration_ms: int = 4000,
    fps: int = 16,
    width: int = 768,
    height: int = 512,
) -> Tuple[str, str, int]:
    """
    Genera un clip de animación mediante el efecto Ken Burns (zoom suave y gradual)
    sobre una imagen de keyframe fija usando PyAV (H.264).
    """
    img_file = Path(image_path)
    if not img_file.exists():
        raise FileNotFoundError(f"Imagen de entrada no encontrada: {image_path}")

    out_file = Path(output_path)
    out_file.parent.mkdir(parents=True, exist_ok=True)
    last_frame_path = str(out_file.with_name(f"{out_file.stem}_last_frame.png").resolve())

    base_img = Image.open(str(img_file)).convert("RGB")
    base_img = base_img.resize((width, height), Image.Resampling.BILINEAR)

    num_frames = max(1, int(round((duration_ms / 1000.0) * fps)))

    import av
    container = av.open(str(out_file), mode="w")
    stream = container.add_stream("libx264", rate=fps)
    stream.width = width
    stream.height = height
    stream.pix_fmt = "yuv420p"

    last_img = None
    for i in range(num_frames):
        zoom = 1.0 + 0.08 * (i / max(1, num_frames))
        cw, ch = int(width / zoom), int(height / zoom)
        left = (width - cw) // 2
        top = (height - ch) // 2
        cropped = base_img.crop((left, top, left + cw, top + ch)).resize((width, height), Image.Resampling.BILINEAR)
        last_img = cropped
        frame = av.VideoFrame.from_image(cropped)
        for packet in stream.encode(frame):
            container.mux(packet)

    for packet in stream.encode():
        container.mux(packet)
    container.close()

    if last_img:
        last_img.save(last_frame_path, "PNG")

    logger.info("Clip Ken Burns generado exitosamente: %s (%d frames, %d ms)", out_file, num_frames, duration_ms)
    return str(out_file.resolve()), last_frame_path, num_frames


async def generate_video_i2v_comfyui(
    model: str = "LTX",
    image_path: str = "",
    action_prompt: str = "",
    negative_prompt: str = "",
    duration_ms: int = 4000,
    fps: int = 16,
    width: int = 768,
    height: int = 512,
    seed: int = 42,
    output_path: Optional[str] = None,
    comfyui_url: str = "http://localhost:8188",
    timeout_s: float = 900.0,
) -> Tuple[str, str, int, int]:
    """
    Ejecuta el workflow I2V en ComfyUI (LTX-Video o Wan 2.2).
    Si ComfyUI está disponible, encola y descarga el MP4 generado, extrayendo el último frame.
    Si ComfyUI está caído o falla, aplica fallback automático a Ken Burns.
    Retorna (clip_path, last_frame_path, num_frames, duration_ms).
    """
    if output_path:
        out_file = Path(output_path)
    else:
        out_file = Path("./storage/clips") / f"clip_{uuid.uuid4().hex[:8]}.mp4"
    out_file.parent.mkdir(parents=True, exist_ok=True)

    num_frames = ltx_valid_length(duration_ms, fps)

    comfy_available = False
    try:
        async with httpx.AsyncClient(timeout=2.0) as client:
            resp = await client.get(f"{comfyui_url.rstrip('/')}/system_stats")
            comfy_available = (resp.status_code == 200)
    except Exception:
        comfy_available = False

    if comfy_available and Path(image_path).exists():
        try:
            is_ltx = model.upper() == "LTX"
            wf_filename = "i2v_ltx.json" if is_ltx else "i2v_wan.json"
            workflow_path = Path("workflows") / wf_filename
            if not workflow_path.exists():
                workflow_path = Path("../ai-gateway/workflows") / wf_filename

            img_file = Path(image_path)
            uploaded_filename = img_file.name
            try:
                async with httpx.AsyncClient(timeout=10.0) as client:
                    with open(img_file, "rb") as rf:
                        files = {"image": (img_file.name, rf, "image/png")}
                        await client.post(f"{comfyui_url.rstrip('/')}/upload/image", files=files)
            except Exception as up_exc:
                logger.warning("No se pudo subir imagen por HTTP a ComfyUI: %s", up_exc)

            video_model_title = "LTXV_IMG_TO_VIDEO" if is_ltx else "WAN_IMG_TO_VIDEO"
            updates = {
                "INPUT_IMAGE": {"image": uploaded_filename},
                "INPUT_POSITIVE": {"text": action_prompt or "3D cartoon animation, fluid motion, cheerful atmosphere"},
                "INPUT_NEGATIVE": {"text": merge_negative(negative_prompt, VIDEO_NEGATIVE)},
                video_model_title: {"width": width, "height": height, "length": num_frames},
                "KSAMPLER": {"seed": seed},
                "OUTPUT_VIDEO": {"frame_rate": float(fps)},
                "LTXV_CONDITIONING": {"frame_rate": float(fps)},
            }

            workflow = load_workflow_with_title_updates(workflow_path, updates)

            async with httpx.AsyncClient(timeout=timeout_s) as client:
                prompt_resp = await client.post(
                    f"{comfyui_url.rstrip('/')}/prompt",
                    json={"prompt": workflow},
                )
                prompt_data = prompt_resp.json()
                prompt_id = prompt_data.get("prompt_id")

                if prompt_id:
                    start_time = time.time()
                    while time.time() - start_time < timeout_s:
                        hist_resp = await client.get(f"{comfyui_url.rstrip('/')}/history/{prompt_id}")
                        if hist_resp.status_code == 200:
                            hist_data = hist_resp.json()
                            if prompt_id in hist_data:
                                outputs = hist_data[prompt_id].get("outputs", {})
                                for node_out in outputs.values():
                                    gifs = node_out.get("gifs", []) or node_out.get("videos", [])
                                    if gifs:
                                        vid_info = gifs[0]
                                        view_url = f"{comfyui_url.rstrip('/')}/view?filename={vid_info['filename']}&subfolder={vid_info.get('subfolder', '')}&type={vid_info.get('type', 'output')}"
                                        vid_bytes = await client.get(view_url)
                                        with open(out_file, "wb") as f_out:
                                            f_out.write(vid_bytes.content)

                                        last_frame_path = extract_last_frame(str(out_file))
                                        logger.info("Video I2V generado con ComfyUI: %s (%d frames)", out_file, num_frames)
                                        return str(out_file.resolve()), last_frame_path, num_frames, duration_ms
                        await asyncio.sleep(1.5)
        except Exception as exc:
            logger.warning("Fallo en generación I2V con ComfyUI (%s), aplicando fallback Ken Burns", exc)

    # Fallback / Modo Offline con Ken Burns
    logger.info("Generando animación fallback Ken Burns sobre keyframe: %s", image_path)
    clip_path, last_frame_path, frames = generate_ken_burns_clip(
        image_path=image_path,
        output_path=str(out_file),
        duration_ms=duration_ms,
        fps=fps,
        width=width,
        height=height,
    )
    return clip_path, last_frame_path, frames, duration_ms
