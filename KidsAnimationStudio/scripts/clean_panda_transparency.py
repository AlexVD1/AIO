import math
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

def perfect_panda_cleanup():
    path = r"c:\Users\villa\GIT DESKTOP\AIO\KidsAnimationStudio\assets\characters\panda\body_standing.png"
    img = Image.open(path).convert("RGBA")
    arr = np.array(img)
    
    # El panda está dentro de una forma elipsoidal central.
    # Fuera de él, los restos son verdes o verde claro (G > R + 10 y G > B).
    h, w = arr.shape[:2]
    r, g, b, a = arr[:, :, 0], arr[:, :, 1], arr[:, :, 2], arr[:, :, 3]
    
    # Condición de residuo verde del fondo
    # La bufanda del panda está entre y: 480..850, x: 130..700
    is_green_bg = (g.astype(int) > r.astype(int) + 12) & (g.astype(int) > b.astype(int) - 5)
    
    # Mantener bufanda: la bufanda es verde oscuro (R < 100, G < 170, B < 130) o tiene patrón
    is_light_bg = is_green_bg & (r > 130) & (g > 165) & (b > 150)
    
    # Borrar fondo residual claro
    a[is_light_bg] = 0
    
    # Limpiar cualquier residuo en los márgenes laterales (x < 150 o x > 870) por encima de y < 500
    for y in range(h):
        for x in range(w):
            if (x < 190 or x > 830) and y < 650:
                if a[y, x] > 0 and (r[y, x] > 110 and g[y, x] > 140):
                    a[y, x] = 0
            if (x < 160 or x > 860) and y >= 650:
                if a[y, x] > 0 and (r[y, x] > 110 and g[y, x] > 140):
                    a[y, x] = 0

    clean_img = Image.fromarray(arr, "RGBA")
    clean_img.save(path)
    print("Limpieza final de bordes realizada.")

if __name__ == "__main__":
    perfect_panda_cleanup()
