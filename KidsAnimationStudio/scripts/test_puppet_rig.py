import json
import os
from PIL import Image

def test_puppet_assembly():
    base_dir = r"c:\Users\villa\GIT DESKTOP\AIO\KidsAnimationStudio\assets\characters\tito"
    rig_path = os.path.join(base_dir, "rig.json")
    assert os.path.exists(rig_path), "rig.json debe existir"

    with open(rig_path, "r", encoding="utf-8") as f:
        rig = json.load(f)

    canvas_w = rig["canvas"]["width"]
    canvas_h = rig["canvas"]["height"]

    # Ensamblaje en buffer RGBA
    composite = Image.new("RGBA", (canvas_w, canvas_h), (0, 0, 0, 0))

    layers = sorted(rig["bones_and_layers"], key=lambda l: l.get("z_index", 0))

    for layer in layers:
        name = layer["name"]
        filename = None
        if "default_file" in layer:
            filename = layer["default_file"]
        elif "poses" in layer and "default_pose" in layer:
            filename = layer["poses"][layer["default_pose"]]
        elif "visemes" in layer and "default_viseme" in layer:
            filename = layer["visemes"][layer["default_viseme"]]

        assert filename is not None, f"Capa {name} debe definir archivo por defecto"
        layer_path = os.path.join(base_dir, filename)
        assert os.path.exists(layer_path), f"Archivo de capa {filename} debe existir en disco"

        img = Image.open(layer_path).convert("RGBA")
        assert img.size == (canvas_w, canvas_h), f"Dimensiones de {filename} deben ser {canvas_w}x{canvas_h}"
        composite = Image.alpha_composite(composite, img)

    out_test = os.path.join(base_dir, "tito_assembled_preview.png")
    composite.save(out_test, "PNG")
    print(f"Ensamblado exitoso verificado: {out_test}")
    return True

if __name__ == "__main__":
    test_puppet_assembly()
