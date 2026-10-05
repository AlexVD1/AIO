import cv2
import numpy as np
from PIL import Image

def clean_alpha_cutout():
    path = r"c:\Users\villa\GIT DESKTOP\AIO\KidsAnimationStudio\assets\characters\panda\panda_full.png"
    out_path = r"c:\Users\villa\GIT DESKTOP\AIO\KidsAnimationStudio\assets\characters\panda\body_standing.png"
    
    img = Image.open(path).convert("RGBA")
    arr = np.array(img)
    
    # El panda está en el centro. La viñeta verde tiene tonos con G > R y G > B (ej. R: 120-190, G: 160-220, B: 150-210)
    # y saturación verde. El panda es blanco (R~G~B > 220) o negro (R~G~B < 70) o bufanda verde oscura.
    # Usemos GrabCut de OpenCV para un recorte profesional automático perfecto:
    bgr = cv2.cvtColor(arr[:, :, :3], cv2.COLOR_RGB2BGR)
    h, w = bgr.shape[:2]
    
    mask = np.zeros((h, w), np.uint8)
    # Rectángulo donde sabemos que está el panda
    rect = (120, 90, w - 240, h - 140)
    
    bgdModel = np.zeros((1, 65), np.float64)
    fgdModel = np.zeros((1, 65), np.float64)
    
    cv2.grabCut(bgr, mask, rect, bgdModel, fgdModel, 5, cv2.GC_INIT_WITH_RECT)
    
    # 0 = GC_BGD, 2 = GC_PR_BGD -> Fondo
    # 1 = GC_FGD, 3 = GC_PR_FGD -> Primer plano
    final_mask = np.where((mask == 2) | (mask == 0), 0, 255).astype('uint8')
    
    # Suavizado de bordes
    final_mask = cv2.GaussianBlur(final_mask, (7, 7), 0)
    
    arr[:, :, 3] = final_mask
    result = Image.fromarray(arr, "RGBA")
    result.save(out_path)
    print("GrabCut completado: recorte impecable de Bao el Panda.")

if __name__ == "__main__":
    clean_alpha_cutout()
