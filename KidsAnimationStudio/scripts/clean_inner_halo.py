import numpy as np
from PIL import Image

def clean_inner_halo():
    path = r"c:\Users\villa\GIT DESKTOP\AIO\KidsAnimationStudio\assets\characters\panda\body_standing.png"
    img = Image.open(path).convert("RGBA")
    arr = np.array(img)
    
    # Borrar cualquier pixel detrás de la cabeza/orejas y a los lados que pertenezca al halo verde pastel
    # El pelaje blanco de panda tiene: R > 210, G > 210, B > 210 (es gris/blanco puro, R~G~B)
    # El halo verde pastel tiene: G > 160, G - R > 15, G - B > 10
    r, g, b, a = arr[:, :, 0].astype(int), arr[:, :, 1].astype(int), arr[:, :, 2].astype(int), arr[:, :, 3]
    
    is_halo = (g > 140) & (g > r + 15) & (g > b - 5)
    
    # La bufanda está por debajo de y = 470
    h, w = arr.shape[:2]
    for y in range(h):
        for x in range(w):
            if is_halo[y, x]:
                # Verificar si no es la bufanda (la bufanda está en el cuerpo central inferior)
                if y < 480 or x < 240 or x > 720:
                    a[y, x] = 0

    clean_img = Image.fromarray(arr, "RGBA")
    clean_img.save(path)
    print("Halo interno eliminado con éxito.")

if __name__ == "__main__":
    clean_inner_halo()
