import json
import os
import sys
import time
import httpx
from pathlib import Path

def generate_professional_assets():
    root = Path(r"c:\Users\villa\GIT DESKTOP\AIO\KidsAnimationStudio")
    workflow_path = root / "ai-gateway" / "workflows" / "character_sheet.json"
    with open(workflow_path, "r", encoding="utf-8") as f:
        base_wf = json.load(f)

    comfy_url = "http://127.0.0.1:8188"
    out_dir = root / "assets" / "generated_dynamic"
    out_dir.mkdir(parents=True, exist_ok=True)

    # 1. Definición de prompts de alta calidad editorial infantil
    PROMPTS = [
        {
            "name": "tito_character_pro",
            "prompt": "masterpiece, professional 2d cartoon character reference sheet, cute baby red fox named Tito, smiling warm brown expressive eyes, fluffy white cheeks, velvety vibrant orange fur, soft yellow cozy knitted scarf, rounded mitten paws without fingers, high quality children storybook illustration, clean white background, cel shading, character turn-around, charming friendly look",
            "negative": "ugly, deformed, photorealistic, realistic animal, human fingers, scary, creepy, 3d render, watermark, text, signature, grainy, low quality",
            "width": 1024,
            "height": 1024
        },
        {
            "name": "huerto_background_pro",
            "prompt": "professional storybook background illustration, magical sunny apple orchard for preschool cartoon, rolling emerald green hills, soft fluffy rounded clouds in bright blue sky, cute stylized apple trees with shiny red apples, soft warm morning lighting, charming storybook watercolor aesthetic, high resolution, clean scenery",
            "negative": "people, animals, character, ugly, dark, photorealistic, creepy, text, watermark, signature",
            "width": 1024,
            "height": 576
        },
        {
            "name": "props_pro",
            "prompt": "professional vector icon sprite sheet, shiny red delicious cartoon apple with single cute leaf, smiling golden celebration star with gentle sparkle, educational toy blocks with numbers, clean isolated white background, preschool animation asset",
            "negative": "dark, ugly, realistic, photos, text, watermark, signature",
            "width": 1024,
            "height": 1024
        }
    ]

    for item in PROMPTS:
        print(f"Generando asset dinámico en ComfyUI: '{item['name']}'...")
        wf = json.loads(json.dumps(base_wf))

        # Configurar nodos
        for node in wf.values():
            title = node.get("_meta", {}).get("title")
            if title == "LOAD_CHECKPOINT":
                node["inputs"]["ckpt_name"] = "DreamShaperXL_Turbo_V2-SFW.safetensors"
            elif title == "INPUT_POSITIVE":
                node["inputs"]["text"] = item["prompt"]
            elif title == "INPUT_NEGATIVE":
                node["inputs"]["text"] = item["negative"]
            elif title == "EMPTY_LATENT":
                node["inputs"]["width"] = item["width"]
                node["inputs"]["height"] = item["height"]
                node["inputs"]["batch_size"] = 1
            elif title == "KSAMPLER":
                node["inputs"]["steps"] = 8
                node["inputs"]["cfg"] = 2.0
                node["inputs"]["seed"] = 12345
            elif title == "OUTPUT_IMAGE":
                node["inputs"]["filename_prefix"] = f"dynamic_{item['name']}"

        # Enviar prompt a ComfyUI
        res = httpx.post(f"{comfy_url}/prompt", json={"prompt": wf}, timeout=15.0)
        pdata = res.json()
        pid = pdata.get("prompt_id")
        assert pid, f"No se obtuvo prompt_id: {pdata}"

        print(f"Job enviado a ComfyUI (ID: {pid}). Esperando resultado en RTX 5070...")
        start_t = time.time()
        img_saved = False
        while time.time() - start_t < 90.0:
            hres = httpx.get(f"{comfy_url}/history/{pid}", timeout=10.0)
            if hres.status_code == 200:
                hdata = hres.json()
                if pid in hdata:
                    outputs = hdata[pid].get("outputs", {})
                    for nout in outputs.values():
                        imgs = nout.get("images", [])
                        if imgs:
                            img_info = imgs[0]
                            vurl = f"{comfy_url}/view?filename={img_info['filename']}&subfolder={img_info.get('subfolder','')}&type={img_info.get('type','output')}"
                            img_bytes = httpx.get(vurl, timeout=10.0).content
                            target_file = out_dir / f"{item['name']}.png"
                            with open(target_file, "wb") as f_out:
                                f_out.write(img_bytes)
                            print(f"Asset generado y guardado en: {target_file}")
                            img_saved = True
                            break
                    if img_saved:
                        break
            time.sleep(2.0)

        assert img_saved, f"Tiempo de espera agotado generando {item['name']}"

    print("Todos los assets ilustrados dinámicos fueron generados con ComfyUI exitosamente.")

if __name__ == "__main__":
    generate_professional_assets()
