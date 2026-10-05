import os
from PIL import Image, ImageDraw

def create_background_layer(size, draw_fn, filepath):
    img = Image.new("RGBA", size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    draw_fn(draw, size)
    os.makedirs(os.path.dirname(filepath), exist_ok=True)
    img.save(filepath, "PNG")
    print(f"Capa de fondo generada: {filepath}")

# Huerto Soleado - 1920x1080 nativo en 3 capas de profundidad

def draw_sky_distant_hills(draw, size):
    w, h = size
    # Cielo azul cielo brillante con gradiente plano
    draw.rectangle([0, 0, w, int(h * 0.7)], fill=(135, 206, 250, 255))
    # Sol cálido sonriente o disco simple en esquina
    draw.ellipse([1400, 80, 1600, 280], fill=(255, 225, 75, 255))
    # Nubes blancas redondeadas estilo esponja
    for cx, cy, r in [(300, 200, 80), (370, 190, 100), (450, 200, 80), (1000, 160, 70), (1070, 150, 90), (1140, 160, 70)]:
        draw.ellipse([cx - r, cy - r // 2, cx + r, cy + r // 2], fill=(255, 255, 255, 240))
    # Colinas lejanas en azul/verde suave
    draw.ellipse([-200, int(h * 0.45), 1100, int(h * 0.95)], fill=(160, 215, 170, 255))
    draw.ellipse([800, int(h * 0.48), 2120, int(h * 0.98)], fill=(145, 205, 155, 255))

def draw_midground_trees(draw, size):
    w, h = size
    # Colinas medias
    draw.ellipse([-100, int(h * 0.55), 1200, int(h * 1.1)], fill=(125, 195, 105, 255))
    draw.ellipse([900, int(h * 0.58), 2100, int(h * 1.15)], fill=(115, 185, 95, 255))
    # Árboles redondeados de manzana
    # Árbol izquierdo
    draw.rectangle([220, int(h * 0.52), 270, int(h * 0.85)], fill=(140, 85, 45, 255))
    draw.ellipse([100, int(h * 0.28), 390, int(h * 0.65)], fill=(85, 180, 70, 255))
    # Árbol derecho
    draw.rectangle([1650, int(h * 0.50), 1700, int(h * 0.82)], fill=(140, 85, 45, 255))
    draw.ellipse([1530, int(h * 0.25), 1820, int(h * 0.62)], fill=(85, 180, 70, 255))
    # Manzanas en los árboles
    for ax, ay in [(180, int(h * 0.4)), (250, int(h * 0.35)), (320, int(h * 0.45)), (1600, int(h * 0.38)), (1700, int(h * 0.34)), (1750, int(h * 0.42))]:
        draw.ellipse([ax - 18, ay - 18, ax + 18, ay + 18], fill=(235, 55, 55, 255))

def draw_foreground_stage(draw, size):
    w, h = size
    # Suelo frontal donde se para el personaje
    draw.rectangle([0, int(h * 0.72), w, h], fill=(105, 180, 80, 255))
    # Suelo con borde curvo superior
    draw.ellipse([-150, int(h * 0.68), w + 150, int(h * 0.82)], fill=(110, 190, 85, 255))
    # Flores sencillas en el pasto frontal
    for fx in [150, 380, 650, 1250, 1500, 1800]:
        fy = int(h * 0.88)
        draw.ellipse([fx - 12, fy - 12, fx + 12, fy + 12], fill=(255, 235, 80, 255))
        draw.ellipse([fx - 5, fy - 5, fx + 5, fy + 5], fill=(245, 130, 40, 255))

def generate_location_huerto(base_dir):
    size = (1920, 1080)
    create_background_layer(size, draw_sky_distant_hills, os.path.join(base_dir, "layer_back.png"))
    create_background_layer(size, draw_midground_trees, os.path.join(base_dir, "layer_mid.png"))
    create_background_layer(size, draw_foreground_stage, os.path.join(base_dir, "layer_fore.png"))

if __name__ == "__main__":
    target = r"c:\Users\villa\GIT DESKTOP\AIO\KidsAnimationStudio\assets\locations\huerto"
    generate_location_huerto(target)
    print("Biblioteca de locación Huerto Soleado generada en 3 capas de profundidad.")
