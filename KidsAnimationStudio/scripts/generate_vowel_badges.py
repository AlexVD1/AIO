from PIL import Image, ImageDraw, ImageFont
from pathlib import Path

def generate_vowel_badges():
    out_dir = Path(r"c:\Users\villa\GIT DESKTOP\AIO\KidsAnimationStudio\assets\props\vocales")
    out_dir.mkdir(parents=True, exist_ok=True)
    
    # 5 vocales con colores brillantes atractivos y amigables
    vowels_config = [
        ("A", (255, 99, 132), "¡A de Avión!"),     # Coral / Rosa alegre
        ("E", (54, 162, 235), "¡E de Estrella!"),   # Azul cielo
        ("I", (255, 206, 86), "¡I de Isla!"),      # Amarillo sol
        ("O", (75, 192, 192), "¡O de Oso!"),       # Turquesa menta
        ("U", (153, 102, 255), "¡U de Uva!")      # Violeta dulce
    ]
    
    size = 280
    for letter, color, desc in vowels_config:
        img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        draw = ImageDraw.Draw(img)
        
        # Círculo exterior dorado brillante
        draw.ellipse([8, 8, size - 8, size - 8], fill=(255, 225, 100, 255), outline=(230, 160, 20, 255), width=6)
        # Círculo interior con color de la vocal
        draw.ellipse([22, 22, size - 22, size - 22], fill=color + (255,), outline=(255, 255, 255, 255), width=8)
        
        # Letra en el centro (fuente predeterminada o grande)
        try:
            font = ImageFont.truetype("arialbd.ttf", 150)
        except Exception:
            font = ImageFont.load_default()
            
        bbox = draw.textbbox((0, 0), letter, font=font)
        tw = bbox[2] - bbox[0]
        th = bbox[3] - bbox[1]
        x = (size - tw) // 2
        y = (size - th) // 2 - 15
        
        # Sombra de la letra y letra blanca sólida
        draw.text((x + 4, y + 6), letter, fill=(30, 30, 30, 90), font=font)
        draw.text((x, y), letter, fill=(255, 255, 255, 255), font=font)
        
        save_path = out_dir / f"vocal_{letter.lower()}.png"
        img.save(save_path)
        print(f"Badge de vocal guardado: {save_path}")

if __name__ == "__main__":
    generate_vowel_badges()
