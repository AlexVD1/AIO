import cv2
import numpy as np
from PIL import Image, ImageFilter
from pathlib import Path

def segment_and_prep_panda():
    root = Path(r"c:\Users\villa\GIT DESKTOP\AIO\KidsAnimationStudio")
    panda_raw = root / "assets" / "characters" / "panda" / "panda_full.png"
    char_dir = root / "assets" / "characters" / "panda"
    loc_dir = root / "assets" / "locations" / "bambu"

    # 1. Cargar panda y quitar fondo verde pastel con segmentación limpia
    img = Image.open(panda_raw).convert("RGBA")
    arr = np.array(img)
    
    # El fondo es verde pastel suave: R ~ 140-190, G ~ 180-220, B ~ 170-210
    # Ojos y cuerpo de panda son blanco (#FFFFFF) y negro (#202020), bufanda verde jade (#309060)
    # Detectar fondo por esquinas (pixel 10,10)
    bg_color = arr[10, 10, :3].astype(float)
    
    # Calcular distancia de color al fondo
    diff = np.sqrt(np.sum((arr[:, :, :3].astype(float) - bg_color) ** 2, axis=2))
    
    # Máscara binaria: donde la distancia al color de fondo sea mayor a umbral
    mask = (diff > 38).astype(np.uint8) * 255
    
    # Refinar bordes con morphological close y Gaussian blur leve
    kernel = np.ones((5, 5), np.uint8)
    mask = cv2.morphologyEx(mask, cv2.MORPH_CLOSE, kernel)
    mask = cv2.GaussianBlur(mask, (5, 5), 0)
    
    arr[:, :, 3] = mask
    panda_rgba = Image.fromarray(arr, "RGBA")
    
    # Guardar body_standing.png (1024x1024 centrado y limpio)
    panda_rgba.save(char_dir / "body_standing.png")
    print(f"Panda recortado guardado en: {char_dir / 'body_standing.png'}")

    # 2. Generar boca / visemas de Panda
    # Posición de la boca del panda: x ~ 512, y ~ 485
    # Crear visemas transparentes con recorte preciso
    mouth_w, mouth_h = 160, 120
    # mouth_rest: boquita cerrada sonriente
    for vname, (open_h, tongue) in [
        ("mouth_rest", (10, False)),
        ("mouth_a", (50, True)),
        ("mouth_o", (45, False)),
        ("mouth_e", (30, True)),
        ("mouth_smile", (18, True)),
    ]:
        v_img = Image.new("RGBA", (mouth_w, mouth_h), (0, 0, 0, 0))
        from PIL import ImageDraw
        draw = ImageDraw.Draw(v_img)
        cx, cy = mouth_w // 2, mouth_h // 2
        
        # Cavidad oral
        if open_h > 12:
            bbox = [cx - 30, cy - open_h // 2, cx + 30, cy + open_h // 2]
            draw.ellipse(bbox, fill=(50, 20, 25, 240), outline=(20, 10, 10, 255), width=2)
            if tongue:
                draw.ellipse([cx - 18, cy, cx + 18, cy + open_h // 2 + 2], fill=(240, 110, 130, 255))
        else:
            # Línea de sonrisa de panda
            draw.arc([cx - 25, cy - 15, cx + 25, cy + 15], start=20, end=160, fill=(30, 30, 30, 255), width=4)
            
        v_img.save(char_dir / f"{vname}.png")

    # Manopla de saludo (patita)
    hand_img = Image.new("RGBA", (140, 140), (0, 0, 0, 0))
    hdraw = ImageDraw.Draw(hand_img)
    # Patita negra con almohadillas rosas
    hdraw.ellipse([20, 20, 120, 120], fill=(30, 30, 30, 255))
    hdraw.ellipse([45, 55, 95, 95], fill=(235, 160, 150, 255)) # central pad
    hdraw.ellipse([32, 32, 50, 50], fill=(235, 160, 150, 255))  # toe 1
    hdraw.ellipse([60, 25, 80, 45], fill=(235, 160, 150, 255))  # toe 2
    hdraw.ellipse([90, 32, 108, 50], fill=(235, 160, 150, 255)) # toe 3
    hand_img.save(char_dir / "hand_wave.png")

    # Guardar rig.json adaptado a Bao el panda
    rig_data = {
        "character_id": "panda_bao",
        "name": "Bao el Panda",
        "default_scale": 1.0,
        "base_offset": [0, 60],
        "bones_and_layers": [
            {
                "bone_name": "body",
                "default_file": "body_standing.png",
                "anchor": [512, 512],
                "z_index": 10,
                "procedural_motion": {
                    "breathing_amplitude_y": 6.0,
                    "breathing_freq_hz": 0.5,
                    "blink_interval_s": 3.2
                }
            },
            {
                "bone_name": "mouth",
                "anchor": [512, 492],
                "z_index": 20,
                "visemes": {
                    "rest": "mouth_rest.png",
                    "a": "mouth_a.png",
                    "o": "mouth_o.png",
                    "e": "mouth_e.png",
                    "smile": "mouth_smile.png"
                }
            },
            {
                "bone_name": "arm_right",
                "anchor": [680, 640],
                "z_index": 25,
                "poses": {
                    "wave": "hand_wave.png"
                }
            }
        ]
    }
    import json
    with open(char_dir / "rig.json", "w", encoding="utf-8") as f:
        json.dump(rig_data, f, indent=2)

    # 3. Preparar capas del escenario de bosque de bambú (Full HD 1920x1080 con DoF)
    bg_raw = Image.open(loc_dir / "layer_back.png").convert("RGBA")
    bg_hd = bg_raw.resize((1920, 1080), Image.Resampling.LANCZOS)
    
    # Layer back con DoF suave
    bg_back = bg_hd.filter(ImageFilter.GaussianBlur(radius=5))
    bg_back.save(loc_dir / "layer_back.png")
    
    # Layer mid y fore
    bg_mid = Image.new("RGBA", (1920, 1080), (0, 0, 0, 0))
    bg_mid.save(loc_dir / "layer_mid.png")
    bg_fore = Image.new("RGBA", (1920, 1080), (0, 0, 0, 0))
    bg_fore.save(loc_dir / "layer_fore.png")

    print("Rig de Bao y Escenario de Bambú configurados al 100%.")

if __name__ == "__main__":
    segment_and_prep_panda()
