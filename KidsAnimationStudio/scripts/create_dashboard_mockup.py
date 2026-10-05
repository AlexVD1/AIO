from PIL import Image, ImageDraw, ImageFont
from pathlib import Path

def create_dashboard_mockup():
    w, h = 1200, 720
    img = Image.new("RGB", (w, h), color=(245, 247, 250))
    draw = ImageDraw.Draw(img)

    # Header Gradient / Top Banner
    draw.rectangle([0, 0, w, 75], fill=(255, 107, 107))
    draw.rectangle([0, 70, w, 75], fill=(238, 82, 83))
    
    # Header Title
    draw.text((30, 20), "Kids Animation Studio - Web Dashboard", fill=(255, 255, 255))
    draw.text((30, 42), "Produccion Automatizada de Animaciones Infantiles con IA Local", fill=(255, 240, 240))

    # Health Badges (Top Right)
    badges = [
        ("Orchestrator: 8082", (46, 204, 113)),
        ("AI-Gateway: 8090", (46, 204, 113)),
        ("ComfyUI: 8188", (46, 204, 113)),
        ("Ollama: 11434", (46, 204, 113)),
        ("Postgres: 5434", (46, 204, 113))
    ]
    bx = 470
    for label, col in badges:
        draw.rectangle([bx, 24, bx + 135, 50], fill=col)
        draw.text((bx + 10, 30), label, fill=(255, 255, 255))
        bx += 142

    # Left Column: Panel de Control & Creacion (w=360)
    draw.rectangle([25, 95, 385, 695], fill=(255, 255, 255), outline=(226, 232, 240), width=2)
    draw.rectangle([25, 95, 385, 135], fill=(44, 62, 80))
    draw.text((40, 105), "1. Parametros del Episodio", fill=(255, 255, 255))

    # Form Fields
    fields = [
        ("Serie / Franquicia:", "Tito el zorrito (3D Cartoon Pixar)"),
        ("Personaje Principal:", "Tito (Zorrito naranja, Seed 42 - Aprobado)"),
        ("Tema Pedagogico:", "COUNTING (Conteo 1 a 3 con manzanas)"),
        ("Publico Objetivo:", "Preescolar (2 a 6 anos)"),
        ("Voz Locutor:", "Kokoro-82M (ef_dora - es-MX, -12% vel)"),
        ("Motor Visual / I2V:", "SDXL Turbo + IP-Adapter + LTX-Video 2B"),
        ("Resolucion / FPS:", "1920x1080 (Full HD) @ 30 FPS")
    ]
    fy = 150
    for lbl, val in fields:
        draw.text((40, fy), lbl, fill=(100, 116, 139))
        draw.rectangle([40, fy + 18, 370, fy + 44], fill=(248, 250, 252), outline=(203, 213, 225), width=1)
        draw.text((48, fy + 24), val, fill=(30, 41, 59))
        fy += 56

    # Action Button
    draw.rectangle([40, 560, 370, 605], fill=(255, 107, 107))
    draw.text((105, 574), "Generar Guion con IA (Ollama)", fill=(255, 255, 255))

    draw.rectangle([40, 620, 370, 665], fill=(46, 204, 113))
    draw.text((100, 634), "Iniciar Pipeline Completo (5 Fases)", fill=(255, 255, 255))

    # Right Column: Monitor de Pipeline en Vivo & Reproductor (w=775)
    draw.rectangle([405, 95, 1175, 695], fill=(255, 255, 255), outline=(226, 232, 240), width=2)
    draw.rectangle([405, 95, 1175, 135], fill=(30, 58, 138))
    draw.text((425, 105), "2. Monitor de Produccion en Vivo - Episodio: 'Contando con Tito'", fill=(255, 255, 255))
    draw.text((980, 105), "ESTADO: COMPLETADO (100%)", fill=(74, 222, 128))

    # Pipeline Step Progress Indicator
    steps = [
        ("Fase A: Narracion", "16/16 audios OK", (46, 204, 113)),
        ("Fase B: Keyframes", "16/16 CLIP > 98%", (46, 204, 113)),
        ("Fase C: Animacion", "16/16 clips LTX OK", (46, 204, 113)),
        ("Fase D: Renderizado", "FFmpeg ASS + Mix OK", (46, 204, 113)),
        ("Fase E: Exportado", "MP4 1080p Final", (46, 204, 113)),
    ]
    sx = 425
    for stitle, sdesc, scol in steps:
        draw.rectangle([sx, 150, sx + 140, 205], fill=(240, 253, 244), outline=scol, width=2)
        draw.rectangle([sx, 150, sx + 140, 172], fill=scol)
        draw.text((sx + 8, 154), stitle, fill=(255, 255, 255))
        draw.text((sx + 8, 180), sdesc, fill=(22, 101, 52))
        sx += 148

    # Video Player Preview Area (Center Right)
    # Thumbnail embedding or mockup
    thumb_path = Path("orchestrator/export_videos/tito-el-zorrito/20261003_contando-con-tito_thumb.png")
    if not thumb_path.exists():
        thumb_path = Path("docs/frame_scene1.png")
    
    if thumb_path.exists():
        timg = Image.open(thumb_path).convert("RGB")
        # Resize to fit 480x270
        timg = timg.resize((480, 270))
        img.paste(timg, (425, 225))
    else:
        draw.rectangle([425, 225, 905, 495], fill=(15, 23, 42))
        draw.text((580, 350), "[ VIDEO PREVIEW 1080p ]", fill=(255, 255, 255))

    # Player Controls Bar below video
    draw.rectangle([425, 495, 905, 530], fill=(30, 41, 59))
    draw.rectangle([435, 502, 470, 522], fill=(255, 107, 107))
    draw.text((447, 506), ">", fill=(255, 255, 255)) # Play button
    draw.rectangle([485, 510, 780, 514], fill=(71, 85, 105)) # Progress bar
    draw.rectangle([485, 510, 780, 514], fill=(255, 107, 107))
    draw.text((795, 506), "00:48 / 00:48", fill=(203, 213, 225))
    draw.text((875, 506), "[HD]", fill=(74, 222, 128))

    # Side Card next to video: Video Specs & Export Links (x=920, y=225)
    draw.rectangle([920, 225, 1160, 530], fill=(248, 250, 252), outline=(226, 232, 240), width=1)
    draw.rectangle([920, 225, 1160, 260], fill=(241, 245, 249))
    draw.text((935, 235), "Especificaciones Exportadas", fill=(30, 41, 59))

    vspecs = [
        ("Duracion:", "48.7 segundos"),
        ("Tamano MP4:", "25.8 MB"),
        ("Resolucion:", "1920x1080 (16:9)"),
        ("Video Codec:", "H.264 (NVENC)"),
        ("Audio Codec:", "AAC Stereo 44.1kHz"),
        ("Subtitulos:", "ASS Karaoke / Didactico"),
        ("Overlays:", "Numeros 1, 2, 3 Grandes"),
        ("Clip QA Score:", "98.2% Consistencia")
    ]
    vy = 270
    for k, v in vspecs:
        draw.text((932, vy), k, fill=(100, 116, 139))
        draw.text((932, vy + 14), v, fill=(15, 23, 42))
        vy += 32

    # Bottom Area: Live Log Terminal Box
    draw.rectangle([425, 545, 1160, 680], fill=(15, 23, 42))
    draw.rectangle([425, 545, 1160, 570], fill=(30, 41, 59))
    draw.text((440, 550), "Consola de Eventos y Telemetria en Tiempo Real", fill=(203, 213, 225))

    logs = [
        "[INFO] Etapa SUBTITLES_OVERLAYS completada: Subtitulos ASS generados con word-level timestamps",
        "[INFO] Proceso FFmpeg finalizado en 4240 ms (exit code 0). Validacion de calidad: OK (1920x1080, 48.7s)",
        "[INFO] Exportando video y metadatos a 'orchestrator/export_videos/tito-el-zorrito/20261003_contando-con-tito.mp4'",
        "[SUCCESS] Pipeline completado exitosamente para episodio 'Contando con Tito' (Progreso: 100%)"
    ]
    ly = 580
    for l in logs:
        col = (74, 222, 128) if "[SUCCESS]" in l else ((56, 189, 248) if "[INFO]" in l else (226, 232, 240))
        draw.text((440, ly), l, fill=col)
        ly += 22

    out_file = Path("docs/web_studio_dashboard_mockup.png")
    out_file.parent.mkdir(parents=True, exist_ok=True)
    img.save(str(out_file), "PNG")
    print(f"Dashboard mockup saved to: {out_file.resolve()}")

if __name__ == "__main__":
    create_dashboard_mockup()
