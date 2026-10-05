import json
import logging
from pathlib import Path
from PIL import Image
import torch
import torch.nn.functional as F

logger = logging.getLogger(__name__)

_dinov2_model = None
_dinov2_transform = None

def get_dinov2():
    global _dinov2_model, _dinov2_transform
    if _dinov2_model is None:
        from torchvision import transforms
        # Cargar DINOv2 small (DINOv2 ViT-S/14) vía torch.hub
        _dinov2_model = torch.hub.load('facebookresearch/dinov2', 'dinov2_vits14', trust_repo=True)
        _dinov2_model.eval()
        _dinov2_transform = transforms.Compose([
            transforms.Resize((224, 224)),
            transforms.ToTensor(),
            transforms.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225]),
        ])
    return _dinov2_model, _dinov2_transform

def calculate_character_crop_similarity(image_path: str, reference_path: str, crop_box=None) -> float:
    """
    Calcula similitud DINOv2 centrada en el sujeto/personaje (crop)
    en lugar del fondo completo para evitar falsos positivos de fondos similares.
    """
    model, transform = get_dinov2()

    im1 = Image.open(image_path).convert("RGB")
    im2 = Image.open(reference_path).convert("RGB")

    if crop_box:
        im1 = im1.crop(crop_box)
        im2 = im2.crop(crop_box)

    t1 = transform(im1).unsqueeze(0)
    t2 = transform(im2).unsqueeze(0)

    with torch.no_grad():
        f1 = model(t1)
        f2 = model(t2)
        f1 = F.normalize(f1, dim=-1)
        f2 = F.normalize(f2, dim=-1)
        sim = float((f1 @ f2.T).item())

    return max(0.0, min(1.0, sim))

if __name__ == "__main__":
    ref = r"c:\Users\villa\GIT DESKTOP\AIO\KidsAnimationStudio\assets\characters\tito\tito_assembled_preview.png"
    print("Módulo dinov2_character_qa listo para integración.")
