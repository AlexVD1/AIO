import argparse
import json
import math
import os
import random
import subprocess
from pathlib import Path
from PIL import Image

class PuppetTimelineRenderer:
    def __init__(self, character_dir: str, location_dir: str, props_dir: str):
        self.character_dir = Path(character_dir)
        self.location_dir = Path(location_dir)
        self.props_dir = Path(props_dir)

        rig_path = self.character_dir / "rig.json"
        with open(rig_path, "r", encoding="utf-8") as f:
            self.rig = json.load(f)

        # Cache de capas precargadas en RGBA
        self._layer_cache = {}
        for item in self.rig["bones_and_layers"]:
            if "default_file" in item:
                self._load_image(item["default_file"])
            if "poses" in item:
                for f in item["poses"].values():
                    self._load_image(f)
            if "visemes" in item:
                for f in item["visemes"].values():
                    self._load_image(f)

        # Fondos
        self.bg_back = Image.open(self.location_dir / "layer_back.png").convert("RGBA")
        self.bg_mid = Image.open(self.location_dir / "layer_mid.png").convert("RGBA")
        self.bg_fore = Image.open(self.location_dir / "layer_fore.png").convert("RGBA")

        # Props didácticos
        self.prop_apple = Image.open(self.props_dir / "apple.png").convert("RGBA")
        self.prop_star = Image.open(self.props_dir / "star.png").convert("RGBA")
        
        # Badges numéricos didácticos 1, 2, 3
        self.badge_1 = Image.open(self.props_dir / "badge_1.png").convert("RGBA")
        self.badge_2 = Image.open(self.props_dir / "badge_2.png").convert("RGBA")
        self.badge_3 = Image.open(self.props_dir / "badge_3.png").convert("RGBA")

    def _load_image(self, filename: str) -> Image.Image:
        if filename not in self._layer_cache:
            p = self.character_dir / filename
            if p.exists():
                self._layer_cache[filename] = Image.open(p).convert("RGBA")
        return self._layer_cache.get(filename)

    def render_frame(
        self,
        time_s: float,
        action: str = "idle",
        word_viseme: str = "rest",
        apple_count: int = 0,
        show_star: bool = False,
        pan_x: float = 0.0,
        character_x: int = 680,
        character_y: int = 930
    ) -> Image.Image:
        width, height = 1920, 1080
        frame = Image.new("RGBA", (width, height), (0, 0, 0, 255))

        # 1. Fondo atmosférico con profundidad
        back_offset = int(pan_x * 0.2)
        frame.alpha_composite(self.bg_back, (back_offset, 0))

        # 2. Ensamblar títere de Tito
        puppet = Image.new("RGBA", (1024, 1024), (0, 0, 0, 0))

        # Dinámicas orgánicas
        # Respiración sinusoidal
        breath_scale_y = 1.0 + 0.02 * math.sin(time_s * (2.0 * math.pi / 2.8))
        
        # Parpadeo natural cada 3.2 segundos
        is_blinking = (time_s % 3.2) < 0.16

        # Rebote de acción al hablar o celebrar
        action_bounce_y = 0
        if action in ("jump", "celebrate"):
            action_bounce_y = int(-35 * abs(math.sin(time_s * 4.5)))
        elif action == "talk" or word_viseme != "rest":
            action_bounce_y = int(-10 * abs(math.sin(time_s * 5.0)))

        # Inclinación sutil de cabeza
        for layer in sorted(self.rig["bones_and_layers"], key=lambda l: l.get("z_index", 0)):
            name = layer["name"]
            img = None

            if name == "eyes":
                if is_blinking:
                    img = self._load_image("eyes_closed.png")
                else:
                    img = None  # ojos abiertos ya integrados con brillo en body_standing
            elif name == "mouth":
                vis = word_viseme if word_viseme in ("rest", "a", "e", "o", "u", "smile") else "rest"
                img = self._load_image(f"mouth_{vis}.png")
            else:
                if "default_file" in layer:
                    img = self._load_image(layer["default_file"])

            if img:
                if name == "body" and breath_scale_y != 1.0:
                    pw, ph = img.size
                    new_h = int(ph * breath_scale_y)
                    scaled = img.resize((pw, new_h), Image.Resampling.BILINEAR)
                    offset_y = ph - new_h
                    temp = Image.new("RGBA", (pw, ph), (0, 0, 0, 0))
                    temp.paste(scaled, (0, offset_y))
                    puppet.alpha_composite(temp, (0, 0))
                else:
                    puppet.alpha_composite(img, (0, 0))

        # Posicionar títere en el frame
        target_size = (760, 760)
        puppet_scaled = puppet.resize(target_size, Image.Resampling.BILINEAR)
        px = character_x - target_size[0] // 2
        py = character_y - target_size[1] + action_bounce_y
        frame.alpha_composite(puppet_scaled, (px, py))

        # 3. Pedestal pedagógico translúcido y elementos de conteo (manzanas + número)
        if apple_count > 0:
            # Crear una caja/placa pedestal didáctica translúcida moderna (glassmorphism infantil)
            # Para garantizar máximo contraste y legibilidad absoluta
            panel_w, panel_h = 620, 380
            panel_x, panel_y = 1180, 420
            
            panel = Image.new("RGBA", (panel_w, panel_h), (0, 0, 0, 0))
            from PIL import ImageDraw
            pdraw = ImageDraw.Draw(panel)
            # Fondo blanco perlado translúcido con borde dorado suave
            pdraw.rounded_rectangle([0, 0, panel_w, panel_h], radius=40, fill=(255, 255, 255, 215), outline=(255, 210, 80, 240), width=6)
            frame.alpha_composite(panel, (panel_x, panel_y))

            # Posiciones de las manzanas según la cantidad
            # Si apple_count == 1: 1 manzana centrada
            # Si apple_count == 2: 2 manzanas
            # Si apple_count == 3: 3 manzanas
            apple_w, apple_h = 160, 160
            apple_scaled = self.prop_apple.resize((apple_w, apple_h), Image.Resampling.LANCZOS)
            
            # Animación de flotación suave
            float_y = int(math.sin(time_s * 4.0) * 8)

            if apple_count == 1:
                frame.alpha_composite(apple_scaled, (panel_x + 230, panel_y + 110 + float_y))
            elif apple_count == 2:
                frame.alpha_composite(apple_scaled, (panel_x + 120, panel_y + 110 + float_y))
                frame.alpha_composite(apple_scaled, (panel_x + 340, panel_y + 110 - float_y))
            elif apple_count >= 3:
                frame.alpha_composite(apple_scaled, (panel_x + 70, panel_y + 110 + float_y))
                frame.alpha_composite(apple_scaled, (panel_x + 230, panel_y + 110 - float_y))
                frame.alpha_composite(apple_scaled, (panel_x + 390, panel_y + 110 + float_y))

            # Placa badge con el número grande (1, 2, 3) sobre el panel
            badge_map = {1: self.badge_1, 2: self.badge_2, 3: self.badge_3}
            if apple_count in badge_map:
                b_img = badge_map[apple_count].resize((140, 140), Image.Resampling.LANCZOS)
                # Pequeño pop o pulso
                frame.alpha_composite(b_img, (panel_x + panel_w // 2 - 70, panel_y - 60))

        # 4. Estrella de celebración si corresponde
        if show_star:
            star_w, star_h = 200, 200
            star_scaled = self.prop_star.resize((star_w, star_h), Image.Resampling.LANCZOS)
            fy = int(math.sin(time_s * 5.0) * 15)
            frame.alpha_composite(star_scaled, (1380, 260 + fy))

        return frame.convert("RGB")

    def render_shot_video(
        self,
        output_mp4: str,
        duration_s: float,
        action: str = "idle",
        apple_count: int = 0,
        show_star: bool = False,
        speech_timeline: list = None,
        fps: int = 30
    ):
        total_frames = int(round(duration_s * fps))
        os.makedirs(os.path.dirname(os.path.abspath(output_mp4)), exist_ok=True)

        ffmpeg_cmd = [
            "ffmpeg", "-y",
            "-loglevel", "error",
            "-f", "rawvideo",
            "-vcodec", "rawvideo",
            "-s", "1920x1080",
            "-pix_fmt", "rgb24",
            "-r", str(fps),
            "-i", "-",
            "-c:v", "libx264",
            "-pix_fmt", "yuv420p",
            "-preset", "ultrafast",
            "-crf", "19",
            output_mp4
        ]

        proc = subprocess.Popen(ffmpeg_cmd, stdin=subprocess.PIPE)

        for f_idx in range(total_frames):
            t = f_idx / fps

            # Determinar visema según speech_timeline o ciclo de habla dinámico si action == "talk" o "count"
            viseme = "rest"
            if speech_timeline:
                for item in speech_timeline:
                    start_s = item.get("start", 0)
                    end_s = item.get("end", 0)
                    if start_s <= t <= end_s:
                        w = item.get("word", "").lower()
                        if any(v in w for v in ["a", "á"]):
                            viseme = "a"
                        elif any(v in w for v in ["o", "ó"]):
                            viseme = "o"
                        elif any(v in w for v in ["e", "é"]):
                            viseme = "e"
                        elif any(v in w for v in ["u", "ú"]):
                            viseme = "u"
                        else:
                            viseme = "smile"
                        break
            elif action in ("talk", "count", "wave", "celebrate") and t < (duration_s - 0.25):
                # Ciclo de fonación natural articulada sincronizada (4 fonemas/segundo)
                # que emula el habla fonética viva en español
                phase = int(t * 8.0) % 5
                visemes_cycle = ["a", "smile", "o", "e", "rest"]
                viseme = visemes_cycle[phase]

            frame_img = self.render_frame(
                time_s=t,
                action=action,
                word_viseme=viseme,
                apple_count=apple_count,
                show_star=show_star
            )
            proc.stdin.write(frame_img.tobytes())

        proc.stdin.close()
        proc.wait()
        print(f"Render completado: {output_mp4} ({duration_s}s @ {fps}fps)")
        return output_mp4

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", default=r"assets/puppet_test_new.mp4")
    parser.add_argument("--duration", type=float, default=2.5)
    parser.add_argument("--action", default="talk")
    parser.add_argument("--apples", type=int, default=2)
    parser.add_argument("--star", action="store_true")
    args = parser.parse_args()

    root = Path(r"c:\Users\villa\GIT DESKTOP\AIO\KidsAnimationStudio")
    renderer = PuppetTimelineRenderer(
        str(root / "assets" / "characters" / "tito"),
        str(root / "assets" / "locations" / "huerto"),
        str(root / "assets" / "props")
    )
    renderer.render_shot_video(
        output_mp4=args.output,
        duration_s=args.duration,
        action=args.action,
        apple_count=args.apples,
        show_star=args.star
    )
