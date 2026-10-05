import json
import math
import os
from pathlib import Path
from PIL import Image, ImageDraw

def create_vector_sprite(size, draw_fn, filepath):
    img = Image.new("RGBA", size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    draw_fn(draw, size)
    os.makedirs(os.path.dirname(filepath), exist_ok=True)
    img.save(filepath, "PNG")
    print(f"Generado: {filepath}")

# Paleta Tito v2 (2D Preescolar Plano)
FOX_ORANGE = (245, 110, 35, 255)
FOX_WHITE = (250, 248, 240, 255)
FOX_DARK = (45, 35, 30, 255)
SCARF_YELLOW = (255, 205, 30, 255)
OUTLINE = (30, 25, 25, 255)

def draw_head(draw, size):
    w, h = size
    # Orejas
    draw.polygon([(260, 360), (330, 180), (430, 300)], fill=FOX_ORANGE, outline=OUTLINE, width=6)
    draw.polygon([(290, 340), (340, 220), (410, 300)], fill=FOX_WHITE)
    draw.polygon([(764, 360), (694, 180), (594, 300)], fill=FOX_ORANGE, outline=OUTLINE, width=6)
    draw.polygon([(734, 340), (684, 220), (614, 300)], fill=FOX_WHITE)
    # Cabeza redonda simpática
    draw.ellipse([270, 260, 754, 700], fill=FOX_ORANGE, outline=OUTLINE, width=8)
    # Mejillas blancas esponjosas
    draw.ellipse([290, 440, 520, 680], fill=FOX_WHITE, outline=OUTLINE, width=6)
    draw.ellipse([504, 440, 734, 680], fill=FOX_WHITE, outline=OUTLINE, width=6)
    # Nariz redondita de botón
    draw.ellipse([487, 500, 537, 545], fill=FOX_DARK)

def draw_eyes_open(draw, size):
    # Ojos grandes brillantes estilo preescolar
    draw.ellipse([375, 410, 445, 495], fill=FOX_DARK)
    draw.ellipse([579, 410, 649, 495], fill=FOX_DARK)
    # Brillo
    draw.ellipse([390, 420, 415, 445], fill=(255, 255, 255, 255))
    draw.ellipse([594, 420, 619, 445], fill=(255, 255, 255, 255))

def draw_eyes_happy(draw, size):
    # Arco sonriente ^ ^
    draw.arc([370, 420, 450, 480], start=190, end=350, fill=FOX_DARK, width=8)
    draw.arc([574, 420, 654, 480], start=190, end=350, fill=FOX_DARK, width=8)

def draw_eyes_closed(draw, size):
    # Línea horizontal con ligera curvatura
    draw.arc([370, 440, 450, 480], start=10, end=170, fill=FOX_DARK, width=8)
    draw.arc([574, 440, 654, 480], start=10, end=170, fill=FOX_DARK, width=8)

def draw_eyes_surprised(draw, size):
    # Círculos muy abiertos con pupila centrada
    draw.ellipse([365, 400, 455, 500], fill=FOX_DARK)
    draw.ellipse([569, 400, 659, 500], fill=FOX_DARK)
    draw.ellipse([385, 415, 420, 450], fill=(255, 255, 255, 255))
    draw.ellipse([589, 415, 624, 450], fill=(255, 255, 255, 255))

def draw_mouth_rest(draw, size):
    draw.arc([470, 535, 554, 575], start=10, end=170, fill=FOX_DARK, width=6)

def draw_mouth_smile(draw, size):
    draw.chord([460, 535, 564, 600], start=0, end=180, fill=(210, 60, 60, 255), outline=OUTLINE, width=6)
    draw.chord([475, 560, 549, 600], start=0, end=180, fill=(255, 150, 150, 255))

def draw_mouth_a(draw, size):
    draw.ellipse([475, 535, 549, 615], fill=(210, 60, 60, 255), outline=OUTLINE, width=6)
    draw.ellipse([485, 575, 539, 610], fill=(255, 150, 150, 255))

def draw_mouth_o(draw, size):
    draw.ellipse([485, 540, 539, 600], fill=(210, 60, 60, 255), outline=OUTLINE, width=6)

def draw_mouth_e(draw, size):
    draw.chord([460, 540, 564, 585], start=0, end=180, fill=(210, 60, 60, 255), outline=OUTLINE, width=6)
    draw.line([465, 555, 559, 555], fill=(255, 255, 255, 255), width=6)

def draw_mouth_u(draw, size):
    draw.ellipse([492, 545, 532, 590], fill=(210, 60, 60, 255), outline=OUTLINE, width=6)

def draw_body(draw, size):
    # Cuerpo cónico suave con bufanda amarilla icónica
    draw.ellipse([380, 610, 644, 910], fill=FOX_ORANGE, outline=OUTLINE, width=8)
    draw.ellipse([430, 670, 594, 880], fill=FOX_WHITE)
    # Patitas inferiores redondeadas (sin garras, tipo peluche/manopla)
    draw.ellipse([400, 860, 480, 925], fill=FOX_DARK, outline=OUTLINE, width=6)
    draw.ellipse([544, 860, 624, 925], fill=FOX_DARK, outline=OUTLINE, width=6)
    # Bufanda amarilla enrollada al cuello
    draw.rounded_rectangle([390, 590, 634, 665], radius=25, fill=SCARF_YELLOW, outline=OUTLINE, width=7)
    draw.polygon([(540, 650), (590, 650), (580, 730), (530, 730)], fill=SCARF_YELLOW, outline=OUTLINE, width=6)

def draw_tail(draw, size):
    # Cola grande y redondeada de zorro con punta blanca
    draw.ellipse([210, 640, 440, 870], fill=FOX_ORANGE, outline=OUTLINE, width=8)
    draw.polygon([(210, 710), (280, 650), (310, 750), (250, 830)], fill=FOX_WHITE, outline=OUTLINE, width=6)

def draw_arm_left_idle(draw, size):
    # Manopla redondeada pegada al cuerpo
    draw.rounded_rectangle([380, 620, 445, 760], radius=30, fill=FOX_ORANGE, outline=OUTLINE, width=7)
    draw.ellipse([380, 720, 445, 775], fill=FOX_DARK, outline=OUTLINE, width=6)

def draw_arm_left_wave(draw, size):
    # Brazo levantado saludando
    draw.polygon([(440, 620), (370, 520), (410, 490), (470, 590)], fill=FOX_ORANGE, outline=OUTLINE, width=7)
    draw.ellipse([360, 470, 420, 530], fill=FOX_DARK, outline=OUTLINE, width=6)

def draw_arm_left_point(draw, size):
    # Señalando lateral
    draw.polygon([(440, 620), (320, 620), (320, 670), (440, 670)], fill=FOX_ORANGE, outline=OUTLINE, width=7)
    draw.ellipse([300, 615, 350, 675], fill=FOX_DARK, outline=OUTLINE, width=6)

def draw_arm_right_idle(draw, size):
    draw.rounded_rectangle([579, 620, 644, 760], radius=30, fill=FOX_ORANGE, outline=OUTLINE, width=7)
    draw.ellipse([579, 720, 644, 775], fill=FOX_DARK, outline=OUTLINE, width=6)

def draw_arm_right_wave(draw, size):
    draw.polygon([(584, 620), (654, 520), (614, 490), (554, 590)], fill=FOX_ORANGE, outline=OUTLINE, width=7)
    draw.ellipse([604, 470, 664, 530], fill=FOX_DARK, outline=OUTLINE, width=6)

def draw_arm_right_point(draw, size):
    draw.polygon([(584, 620), (704, 620), (704, 670), (584, 670)], fill=FOX_ORANGE, outline=OUTLINE, width=7)
    draw.ellipse([674, 615, 724, 675], fill=FOX_DARK, outline=OUTLINE, width=6)

def generate_all_layers(base_dir):
    canvas_size = (1024, 1024)
    layers = {
        "head_base.png": draw_head,
        "eyes_open.png": draw_eyes_open,
        "eyes_happy.png": draw_eyes_happy,
        "eyes_closed.png": draw_eyes_closed,
        "eyes_surprised.png": draw_eyes_surprised,
        "mouth_rest.png": draw_mouth_rest,
        "mouth_smile.png": draw_mouth_smile,
        "mouth_a.png": draw_mouth_a,
        "mouth_o.png": draw_mouth_o,
        "mouth_e.png": draw_mouth_e,
        "mouth_u.png": draw_mouth_u,
        "body_standing.png": draw_body,
        "tail_idle.png": draw_tail,
        "arm_left_idle.png": draw_arm_left_idle,
        "arm_left_wave.png": draw_arm_left_wave,
        "arm_left_point.png": draw_arm_left_point,
        "arm_right_idle.png": draw_arm_right_idle,
        "arm_right_wave.png": draw_arm_right_wave,
        "arm_right_point.png": draw_arm_right_point
    }
    for filename, fn in layers.items():
        create_vector_sprite(canvas_size, fn, os.path.join(base_dir, filename))

if __name__ == "__main__":
    target = r"c:\Users\villa\GIT DESKTOP\AIO\KidsAnimationStudio\assets\characters\tito"
    generate_all_layers(target)
    print("Todas las capas del rig de Tito v2 generadas exitosamente.")
