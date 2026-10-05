import argparse
import json
import math
import os
import random
import subprocess
from pathlib import Path
from PIL import Image

class PandaTimelineRenderer:
    def __init__(self, character_dir: str, location_dir: str, props_dir: str):
        self.character_dir = Path(character_dir)
        self.location_dir = Path(location_dir)
        self.props_dir = Path(props_dir)

        rig_path = self.character_dir / "rig.json"
        with open(rig_path, "r", encoding="utf-8") as f:
            self.rig = json.load(f)

        # Cargar imágenes
        self.body = Image.open(self.character_dir / "body_standing.png").convert("RGBA")
        
        # Visemas de Panda
        self.visemes = {
            "rest": Image.open(self.character_dir / "mouth_rest.png").convert("RGBA"),
            "a": Image.open(self.character_dir / "mouth_a.png").convert("RGBA"),
            "o": Image.open(self.character_dir / "mouth_o.png").convert("RGBA"),
            "e": Image.open(self.character_dir / "mouth_e.png").convert("RGBA"),
            "smile": Image.open(self.character_dir / "mouth_smile.png").convert("RGBA")
        }
        
        # Manopla
        self.hand_wave = Image.open(self.character_dir / "hand_wave.png").convert("RGBA")

        # Escenario Bosque de Bambú (1920x1080)
        self.bg_back = Image.open(self.location_dir / "layer_back.png").convert("RGBA")

        # Badges de las vocales
        self.vowel_badges = {
            "a": Image.open(self.props_dir / "vocal_a.png").convert("RGBA"),
            "e": Image.open(self.props_dir / "vocal_e.png").convert("RGBA"),
            "i": Image.open(self.props_dir / "vocal_i.png").convert("RGBA"),
            "o": Image.open(self.props_dir / "vocal_o.png").convert("RGBA"),
            "u": Image.open(self.props_dir / "vocal_u.png").convert("RGBA")
        }

    def render_frame(
        self,
        time_s: float,
        action: str = "idle",
        word_viseme: str = "rest",
        vocal_letter: str = None,
        celebrate: bool = False
    ) -> Image.Image:
        # Fondo base
        frame = self.bg_back.copy()

        # Posicionamiento de Bao el Panda en el lado izquierdo de la pantalla (x: 480, y: 560)
        # para dejar la mitad derecha para el panel pedagógico de las vocales
        panda_base_x = 480
        panda_base_y = 560
        
        # Respiración sinusoidal suave
        breath_y = math.sin(time_s * 2.5) * 5.0

        # Posición del cuerpo escalado armónicamente
        # Escalamos al panda para que ocupe altura adecuada (~780 px de alto)
        panda_scale = 0.82
        cw, ch = int(self.body.width * panda_scale), int(self.body.height * panda_scale)
        scaled_body = self.body.resize((cw, ch), Image.Resampling.LANCZOS)
        
        bx = int(panda_base_x - cw // 2)
        by = int(panda_base_y - ch // 2 + breath_y)
        frame.alpha_composite(scaled_body, (bx, by))

        # Visema activo sincronizado con fonación
        if action in ["talk", "vowel", "celebrate", "wave"]:
            phonation_phase = int(time_s * 8.0) % 5
            viseme_cycle = ["a", "smile", "o", "e", "rest"]
            vis_key = viseme_cycle[phonation_phase]
        else:
            vis_key = "rest"
            
        vis_img = self.visemes.get(vis_key, self.visemes["rest"])
        vw, vh = int(vis_img.width * panda_scale), int(vis_img.height * panda_scale)
        scaled_vis = vis_img.resize((vw, vh), Image.Resampling.LANCZOS)
        
        # Boca centrada en el rostro del panda
        mx = int(bx + (cw * 0.505) - (vw // 2))
        my = int(by + (ch * 0.475) - (vh // 2))
        frame.alpha_composite(scaled_vis, (mx, my))

        # Saludo si corresponde
        if action in ["wave", "celebrate"]:
            wave_angle = math.sin(time_s * 7.0) * 18.0
            rotated_hand = self.hand_wave.rotate(wave_angle, resample=Image.Resampling.BILINEAR)
            hw, hh = int(rotated_hand.width * panda_scale), int(rotated_hand.height * panda_scale)
            scaled_hand = rotated_hand.resize((hw, hh), Image.Resampling.LANCZOS)
            hx = int(bx + cw * 0.72)
            hy = int(by + ch * 0.52 + math.sin(time_s * 7.0) * 8.0)
            frame.alpha_composite(scaled_hand, (hx, hy))

        # Panel didáctico pedagógico en la mitad derecha (x: 1220, y: 540)
        if vocal_letter or celebrate:
            panel_w, panel_h = 560, 560
            panel_img = Image.new("RGBA", (panel_w, panel_h), (0, 0, 0, 0))
            from PIL import ImageDraw
            pdraw = ImageDraw.Draw(panel_img)
            # Fondo blanco translúcido redondeado con marco dorado
            pdraw.rounded_rectangle([0, 0, panel_w, panel_h], radius=38, fill=(255, 255, 255, 230), outline=(255, 215, 0, 255), width=8)

            px = 1200
            py = int(540 - panel_h // 2 + math.sin(time_s * 2.0) * 6.0)
            frame.alpha_composite(panel_img, (px, py))

            if vocal_letter and vocal_letter.lower() in self.vowel_badges:
                badge = self.vowel_badges[vocal_letter.lower()]
                # El badge flota alegremente con escala y pulso
                pulse = 1.0 + math.sin(time_s * 3.5) * 0.04
                bw, bh = int(badge.width * pulse * 1.3), int(badge.height * pulse * 1.3)
                scaled_badge = badge.resize((bw, bh), Image.Resampling.LANCZOS)
                badge_x = int(px + (panel_w - bw) // 2)
                badge_y = int(py + (panel_h - bh) // 2)
                frame.alpha_composite(scaled_badge, (badge_x, badge_y))
            elif celebrate:
                # Celebración: mostrar las 5 vocales en el panel pedagógico
                vocales = ["a", "e", "i", "o", "u"]
                coords = [
                    (px + 70, py + 80),
                    (px + 320, py + 80),
                    (px + 200, py + 220),
                    (px + 70, py + 360),
                    (px + 320, py + 360)
                ]
                for v, (vx, vy) in zip(vocales, coords):
                    b = self.vowel_badges[v]
                    b_small = b.resize((120, 120), Image.Resampling.LANCZOS)
                    frame.alpha_composite(b_small, (vx, vy))

        return frame

    def render_shot_video(self, duration_s: float, fps: int, output_path: str, action: str, vocal_letter: str = None, celebrate: bool = False):
        total_frames = int(duration_s * fps)
        out_dir = Path(output_path).parent
        out_dir.mkdir(parents=True, exist_ok=True)

        ffmpeg_cmd = [
            "ffmpeg", "-y", "-loglevel", "error",
            "-f", "rawvideo",
            "-pix_fmt", "rgba",
            "-s", "1920x1080",
            "-r", str(fps),
            "-i", "-",
            "-c:v", "libx264",
            "-pix_fmt", "yuv420p",
            "-preset", "ultrafast",
            "-crf", "18",
            output_path
        ]

        proc = subprocess.Popen(ffmpeg_cmd, stdin=subprocess.PIPE)
        for f in range(total_frames):
            t = f / fps
            frame = self.render_frame(
                time_s=t,
                action=action,
                vocal_letter=vocal_letter,
                celebrate=celebrate
            )
            proc.stdin.write(frame.tobytes())

        proc.stdin.close()
        proc.wait()
        print(f"Clip renderizado: {output_path} ({duration_s:.2f}s @ {fps}fps)")
