import json
import os
import sys
import time
import httpx
from pathlib import Path
from PIL import Image

def generate_panda_assets():
    root = Path(r"c:\Users\villa\GIT DESKTOP\AIO\KidsAnimationStudio")
    workflow_path = root / "ai-gateway" / "workflows" / "character_sheet.json"
    with open(workflow_path, "r", encoding="utf-8") as f:
        base_wf = json.load(f)

    comfy_url = "http://127.0.0.1:8188"
    out_dir = root / "assets" / "characters" / "panda"
    out_dir.mkdir(parents=True, exist_ok=True)
    loc_dir = root / "assets" / "locations" / "bambu"
    loc_dir.mkdir(parents=True, exist_ok=True)
    props_dir = root / "assets" / "props" / "vocales"
    props_dir.mkdir(parents=True, exist_ok=True)

    TASKS = [
        {
            "name": "panda_character",
            "out_path": out_dir / "panda_full.png",
            "prompt": "masterpiece, professional 2d cartoon character, adorable cute baby panda bear named Bao, smiling big sparkling friendly eyes, fluffy round ears, cozy jade green scarf, holding arms in friendly gesture, clean white isolated background, children storybook illustration style, joyful expression, high quality cel shading",
            "negative": "ugly, deformed, photorealistic, realistic animal, human fingers, scary, 3d render, watermark, text, grainy, low quality",
            "width": 1024,
            "height": 1024
        },
        {
            "name": "bambu_forest_background",
            "out_path": loc_dir / "layer_back.png",
            "prompt": "professional storybook background illustration, magical sunny bamboo forest for preschool cartoon, lush green tall bamboo stalks, soft warm sunlight filtering through leaves, gentle misty clearing, soft pastel colors, peaceful enchanted nature, high resolution 1920x1080 aesthetic",
            "negative": "animals, character, people, dark, creepy, ugly, photorealistic, text, watermark",
            "width": 1024,
            "height": 576
        }
    ]

    for item in TASKS:
        print(f"Generando en ComfyUI: '{item['name']}'...")
        wf = json.loads(json.dumps(base_wf))
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
                node["inputs"]["seed"] = 424242
            elif title == "OUTPUT_IMAGE":
                node["inputs"]["filename_prefix"] = f"panda_{item['name']}"

        res = httpx.post(f"{comfy_url}/prompt", json={"prompt": wf}, timeout=15.0)
        pdata = res.json()
        pid = pdata.get("prompt_id")
        assert pid, f"No prompt_id: {pdata}"

        print(f"Esperando en RTX 5070 (Prompt ID: {pid})...")
        t0 = time.time()
        saved = False
        while time.time() - t0 < 60.0:
            hres = httpx.get(f"{comfy_url}/history/{pid}", timeout=10.0)
            if hres.status_code == 200:
                hdata = hres.json()
                if pid in hdata:
                    outputs = hdata[pid].get("outputs", {})
                    for nout in outputs.values():
                        imgs = nout.get("images", [])
                        if imgs:
                            info = imgs[0]
                            vurl = f"{comfy_url}/view?filename={info['filename']}&subfolder={info.get('subfolder','')}&type={info.get('type','output')}"
                            img_bytes = httpx.get(vurl, timeout=10.0).content
                            with open(item["out_path"], "wb") as f_out:
                                f_out.write(img_bytes)
                            print(f"Guardado: {item['out_path']}")
                            saved = True
                            break
                    if saved:
                        break
            time.sleep(1.5)
        assert saved, f"Timeout generando {item['name']}"

    print("Imágenes generadas correctamente.")

if __name__ == "__main__":
    generate_panda_assets()
