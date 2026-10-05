import os
from PIL import Image, ImageDraw

def create_sprite(size, draw_fn, filepath):
    img = Image.new("RGBA", size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    draw_fn(draw, size)
    os.makedirs(os.path.dirname(filepath), exist_ok=True)
    img.save(filepath, "PNG")
    print(f"Prop generado: {filepath}")

OUTLINE = (30, 25, 25, 255)

def draw_apple(draw, size):
    # Manzana roja brillante estilo caricatura preescolar
    draw.ellipse([80, 100, 270, 310], fill=(235, 55, 55, 255), outline=OUTLINE, width=6)
    draw.ellipse([190, 100, 380, 310], fill=(235, 55, 55, 255), outline=OUTLINE, width=6)
    # Tallo
    draw.arc([210, 30, 270, 110], start=200, end=340, fill=(110, 65, 35, 255), width=8)
    # Hoja verde
    draw.ellipse([240, 45, 310, 85], fill=(70, 190, 60, 255), outline=OUTLINE, width=5)
    # Brillo reflejo
    draw.ellipse([120, 130, 155, 175], fill=(255, 255, 255, 200))

def draw_star(draw, size):
    # Estrella amarilla brillante de celebración
    points = [
        (256, 50), (316, 170), (450, 190), (353, 285),
        (376, 420), (256, 355), (136, 420), (159, 285),
        (62, 190), (196, 170)
    ]
    draw.polygon(points, fill=(255, 220, 40, 255), outline=OUTLINE, width=7)
    # Carita feliz en la estrella
    draw.ellipse([215, 200, 235, 225], fill=OUTLINE)
    draw.ellipse([277, 200, 297, 225], fill=OUTLINE)
    draw.arc([226, 225, 286, 265], start=0, end=180, fill=OUTLINE, width=5)

def draw_number(num):
    def _draw(draw, size):
        # Número en círculo estilo ficha didáctica
        draw.ellipse([40, 40, 472, 472], fill=(255, 245, 210, 255), outline=OUTLINE, width=8)
        # Borde decorativo
        draw.ellipse([60, 60, 452, 452], fill=(80, 175, 240, 255), outline=OUTLINE, width=6)
        # Dibujo esquemático o texto grande
        # Usamos texto o formas para los números
        cx, cy = 256, 256
        if num == 1:
            draw.polygon([(210, 170), (256, 120), (276, 120), (276, 380), (236, 380)], fill=(255, 255, 255, 255), outline=OUTLINE, width=6)
            draw.rectangle([180, 360, 332, 395], fill=(255, 255, 255, 255), outline=OUTLINE, width=6)
        elif num == 2:
            draw.arc([190, 130, 322, 250], start=180, end=360, fill=(255, 255, 255, 255), width=24)
            draw.line([(315, 210), (195, 360)], fill=(255, 255, 255, 255), width=24)
            draw.rectangle([185, 350, 325, 385], fill=(255, 255, 255, 255), outline=OUTLINE, width=6)
        elif num == 3:
            draw.arc([190, 130, 310, 240], start=210, end=90, fill=(255, 255, 255, 255), width=24)
            draw.arc([190, 240, 310, 360], start=270, end=150, fill=(255, 255, 255, 255), width=24)
        else:
            draw.ellipse([216, 216, 296, 296], fill=(255, 255, 255, 255), outline=OUTLINE, width=6)
    return _draw

def generate_props(base_dir):
    create_sprite((450, 400), draw_apple, os.path.join(base_dir, "apple.png"))
    create_sprite((512, 512), draw_star, os.path.join(base_dir, "star.png"))
    for i in range(1, 4):
        create_sprite((512, 512), draw_number(i), os.path.join(base_dir, f"number_{i}.png"))

if __name__ == "__main__":
    target = r"c:\Users\villa\GIT DESKTOP\AIO\KidsAnimationStudio\assets\props"
    generate_props(target)
    print("Props pedagógicos generados.")
