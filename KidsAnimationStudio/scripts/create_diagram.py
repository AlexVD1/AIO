from PIL import Image, ImageDraw, ImageFont
from pathlib import Path

def generate_architecture_diagram():
    w, h = 1200, 750
    img = Image.new("RGB", (w, h), color=(248, 250, 252)) # slate-50
    draw = ImageDraw.Draw(img)

    # Title Banner
    draw.rectangle([0, 0, w, 80], fill=(26, 54, 93)) # Navy
    # Header title
    draw.text((40, 25), "KidsAnimationStudio - Topologia de Microservicios Locales (100% On-Premise)", fill=(255, 255, 255))

    # Boxes configuration: (x, y, w, h, title, subtitle, port, color, border_color)
    boxes = [
        # Client / Web Dashboard
        (50, 130, 320, 140, "WEB STUDIO DASHBOARD", "Interfaz de Produccion & Monitor Live", "http://localhost:8082/", (237, 242, 247), (49, 130, 206)),
        # Orchestrator
        (440, 130, 320, 140, "SPRING BOOT ORCHESTRATOR", "Maquina de Estados, Flyway & Pipeline", "Puerto 8082 (Java 21)", (235, 248, 255), (49, 130, 206)),
        # Postgres
        (830, 130, 320, 140, "POSTGRESQL 16 (kidsdb)", "Biblia, Personajes, Episodios, Planos", "Puerto 5434 (WSL/Podman)", (237, 242, 247), (74, 85, 104)),
        # AI Gateway
        (440, 340, 320, 150, "FASTAPI AI-GATEWAY", "Kokoro-82M TTS + Faster-Whisper + GpuGuard", "Puerto 8090 (Python 3.12)", (254, 235, 226), (221, 107, 32)),
        # Ollama
        (50, 340, 320, 150, "OLLAMA ENGINE", "qwen2.5:7b-instruct (JSON Estructurado)", "Puerto 11434 (LLM Local)", (240, 255, 244), (56, 161, 105)),
        # ComfyUI
        (440, 560, 710, 140, "COMFYUI NVIDIA PORTABLE (RTX 5070 Blackwell)", "SDXL Turbo + IP-Adapter Plus + LTX-Video 2B / Wan 2.2 TI2V", "Puerto 8188 (PyTorch 2.14.0+cu130, CUDA 13.0)", (255, 250, 240), (214, 158, 46)),
    ]

    for x, y, bw, bh, title, sub, port_info, bg, border in boxes:
        # Card shadow
        draw.rectangle([x+4, y+4, x+bw+4, y+bh+4], fill=(226, 232, 240))
        # Card body
        draw.rectangle([x, y, x+bw, y+bh], fill=bg, outline=border, width=3)
        # Header inside card
        draw.rectangle([x, y, x+bw, y+35], fill=border)
        draw.text((x+15, y+10), title, fill=(255, 255, 255))
        # Subtitle and port
        draw.text((x+15, y+50), sub, fill=(45, 55, 72))
        draw.text((x+15, y+85), f">> {port_info}", fill=(113, 128, 150))

    # Connectors / Arrows (Lines)
    # Dashboard <-> Orchestrator
    draw.line([(370, 200), (440, 200)], fill=(49, 130, 206), width=4)
    # Orchestrator <-> Postgres
    draw.line([(760, 200), (830, 200)], fill=(74, 85, 104), width=4)
    # Orchestrator <-> Ollama
    draw.line([(440, 220), (210, 220), (210, 340)], fill=(56, 161, 105), width=4)
    # Orchestrator <-> AI Gateway
    draw.line([(600, 270), (600, 340)], fill=(221, 107, 32), width=4)
    # AI Gateway <-> ComfyUI
    draw.line([(600, 490), (600, 560)], fill=(214, 158, 46), width=4)

    # Save
    out_path = Path("docs/architecture_diagram.png")
    out_path.parent.mkdir(parents=True, exist_ok=True)
    img.save(str(out_path), "PNG")
    print(f"Architecture diagram generated: {out_path.resolve()}")

if __name__ == "__main__":
    generate_architecture_diagram()
