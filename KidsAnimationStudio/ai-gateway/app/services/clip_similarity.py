from __future__ import annotations

import logging
from pathlib import Path
from typing import Optional

from PIL import Image
import torch

try:
    import truststore
    truststore.inject_into_ssl()
except Exception:  # noqa: BLE001
    pass

from transformers import CLIPModel, CLIPProcessor

logger = logging.getLogger(__name__)

_clip_model: Optional[CLIPModel] = None
_clip_processor: Optional[CLIPProcessor] = None
_MODEL_ID = "openai/clip-vit-base-patch32"


def get_clip_components() -> tuple[CLIPModel, CLIPProcessor]:
    global _clip_model, _clip_processor
    if _clip_model is None or _clip_processor is None:
        logger.info("Cargando modelo CLIP '%s' en CPU para QA de similitud...", _MODEL_ID)
        _clip_model = CLIPModel.from_pretrained(_MODEL_ID)
        _clip_processor = CLIPProcessor.from_pretrained(_MODEL_ID)
    return _clip_model, _clip_processor


def calculate_clip_similarity(image_path: str | Path, reference_path: str | Path) -> float:
    """
    Calcula la similitud coseno entre dos imágenes utilizando embeddings de CLIP ViT.
    Retorna un float en el rango [0.0, 1.0].
    """
    img_a_path = Path(image_path)
    img_b_path = Path(reference_path)

    if not img_a_path.exists():
        raise FileNotFoundError(f"La imagen candidata no existe: {img_a_path}")
    if not img_b_path.exists():
        raise FileNotFoundError(f"La imagen de referencia no existe: {img_b_path}")

    # Si apuntan exactamente al mismo archivo en disco
    if img_a_path.resolve() == img_b_path.resolve():
        return 1.0

    try:
        model, processor = get_clip_components()

        with Image.open(img_a_path) as im1, Image.open(img_b_path) as im2:
            im1_rgb = im1.convert("RGB")
            im2_rgb = im2.convert("RGB")
            inputs = processor(images=[im1_rgb, im2_rgb], return_tensors="pt")

        with torch.no_grad():
            out = model.get_image_features(**inputs)
            feats = out.pooler_output if hasattr(out, "pooler_output") else out
            feats = feats / feats.norm(dim=-1, keepdim=True)
            similarity = (feats[0] @ feats[1]).item()

        # Acotar al rango [0.0, 1.0]
        score = max(0.0, min(1.0, float(similarity)))
        logger.info("Similitud CLIP calculada entre '%s' y '%s': %.4f",
                    img_a_path.name, img_b_path.name, score)
        return score
    except Exception as exc:
        logger.error("Error al calcular similitud CLIP: %s", exc, exc_info=True)
        # Antes devolvía 0.85 (> umbral 0.80): un error se convertía en aprobado.
        # Ahora el error se propaga para que el llamador reintente o marque revisión.
        raise RuntimeError(f"QA de similitud no disponible: {exc}") from exc
