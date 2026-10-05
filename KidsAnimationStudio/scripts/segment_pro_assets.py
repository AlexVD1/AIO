import os
from pathlib import Path
from PIL import Image
from rembg import remove

def segment_pro_assets():
    root = Path(r"c:\Users\villa\GIT DESKTOP\AIO\KidsAnimationStudio")
    src_dir = root / "assets" / "generated_dynamic"
    tito_src = src_dir / "tito_character_pro.png"
    props_src = src_dir / "props_pro.png"
    huerto_src = src_dir / "huerto_background_pro.png"

    char_target = root / "assets" / "characters" / "tito"
    props_target = root / "assets" / "props"
    loc_target = root / "assets" / "locations" / "huerto"

    print("Eliminando fondo con rembg para Tito...")
    with open(tito_src, "rb") as fi:
        tito_bytes = remove(fi.read())
    tito_clean_path = src_dir / "tito_clean_rgba.png"
    with open(tito_clean_path, "wb") as fo:
        fo.write(tito_bytes)

    tito_img = Image.open(tito_clean_path).convert("RGBA")
    w, h = tito_img.size

    # Descomponer en capas del rig manteniendo resolución 1024x1024
    # 1. Cuerpo base completo
    tito_img.save(char_target / "body_standing.png")
    # 2. Cabeza (crop superior suave)
    head_mask = Image.new("L", (w, h), 0)
    from PIL import ImageDraw
    draw = ImageDraw.Draw(head_mask)
    draw.ellipse([150, 50, 880, 550], fill=255)
    head_img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    head_img.paste(tito_img, (0, 0), head_mask)
    head_img.save(char_target / "head_base.png")

    # Segmentar props: manzana y estrella
    print("Extrayendo props de alta calidad...")
    with open(props_src, "rb") as fi:
        props_clean_bytes = remove(fi.read())
    props_clean_path = src_dir / "props_clean_rgba.png"
    with open(props_clean_path, "wb") as fo:
        fo.write(props_clean_bytes)

    props_img = Image.open(props_clean_path).convert("RGBA")
    pw, ph = props_img.size

    # Crop manzana superior izquierda
    apple_crop = props_img.crop((60, 20, int(pw * 0.44), int(ph * 0.48)))
    apple_crop.save(props_target / "apple.png")

    # Crop estrella superior centro
    star_crop = props_img.crop((int(pw * 0.45), 20, int(pw * 0.76), int(ph * 0.22)))
    star_crop.save(props_target / "star.png")

    # Preparar el fondo del Huerto en 1920x1080
    print("Adaptando fondo artístico a 1920x1080 Full HD...")
    huerto_img = Image.open(huerto_src).convert("RGBA")
    huerto_full = huerto_img.resize((1920, 1080), Image.Resampling.LANCZOS)

    # Separar en 3 capas de profundidad
    # Capa trasera (cielo y colinas distantes)
    back_layer = huerto_full.copy()
    back_layer.save(loc_target / "layer_back.png")

    # Capa media (árboles centrales)
    mid_layer = Image.new("RGBA", (1920, 1080), (0, 0, 0, 0))
    mid_crop = huerto_full.crop((0, 250, 1920, 850))
    mid_layer.paste(mid_crop, (0, 250))
    mid_layer.save(loc_target / "layer_mid.png")

    # Capa frontal (camino y suelo cercano)
    fore_layer = Image.new("RGBA", (1920, 1080), (0, 0, 0, 0))
    fore_crop = huerto_full.crop((0, 750, 1920, 1080))
    fore_layer.paste(fore_crop, (0, 750))
    fore_layer.save(loc_target / "layer_fore.png")

    print("Todas las capas artísticas de alta calidad fueron segmentadas y guardadas.")

if __name__ == "__main__":
    segment_pro_assets()
