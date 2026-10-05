from __future__ import annotations

import logging
from pathlib import Path

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field

from app.services.clip_similarity import calculate_clip_similarity

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/qa", tags=["QA"])


class SimilarityRequest(BaseModel):
    imagePath: str = Field(..., description="Ruta absoluta de la imagen candidata generada")
    referencePath: str = Field(..., description="Ruta absoluta de la imagen canónica de referencia")


class SimilarityResponse(BaseModel):
    score: float = Field(..., description="Puntuación de similitud coseno CLIP en el rango [0.0, 1.0]")


@router.post("/similarity", response_model=SimilarityResponse)
async def check_similarity(req: SimilarityRequest) -> SimilarityResponse:
    try:
        score = calculate_clip_similarity(req.imagePath, req.referencePath)
        return SimilarityResponse(score=score)
    except FileNotFoundError as exc:
        logger.warning("Archivo no encontrado en /qa/similarity: %s", exc)
        raise HTTPException(status_code=404, detail=str(exc))
    except Exception as exc:
        logger.error("Error en /qa/similarity: %s", exc, exc_info=True)
        raise HTTPException(status_code=500, detail=str(exc))
