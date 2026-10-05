"""
CLI Wrapper for Chatterbox Multilingual Voice Cloning (Q2.1 / Q2.3)
Executed by AI Gateway or standalone scripts using the voice-lab CUDA environment.
"""

import argparse
import sys
import time
from pathlib import Path

# Inject system SSL truststore
try:
    import truststore
    truststore.inject_into_ssl()
except Exception:
    pass

import torch
import soundfile as sf

def main():
    parser = argparse.ArgumentParser(description="Chatterbox Voice Cloning CLI")
    parser.add_argument("--text", required=True, help="Texto a sintetizar")
    parser.add_argument("--voice-ref", required=True, help="Ruta al archivo WAV de referencia")
    parser.add_argument("--output", required=True, help="Ruta del archivo WAV de salida")
    parser.add_argument("--language-id", default="es", help="Código de idioma (es, en, etc.)")
    parser.add_argument("--exaggeration", type=float, default=0.6, help="Factor de expresividad (0.0 a 1.0)")
    args = parser.parse_args()

    device = "cuda" if torch.cuda.is_available() else "cpu"
    
    from chatterbox import ChatterboxMultilingualTTS

    ref_path = Path(args.voice_ref).resolve()
    if not ref_path.exists():
        sys.stderr.write(f"Error: Referencia de voz no existe en {ref_path}\n")
        sys.exit(1)

    out_path = Path(args.output).resolve()
    out_path.parent.mkdir(parents=True, exist_ok=True)

    t0 = time.time()
    tts = ChatterboxMultilingualTTS.from_pretrained(device=device)
    tts.prepare_conditionals(str(ref_path), exaggeration=args.exaggeration)

    audio = tts.generate(
        text=args.text,
        language_id=args.language_id
    )

    if isinstance(audio, torch.Tensor):
        audio = audio.detach().cpu().numpy()
    if audio.ndim > 1:
        audio = audio.squeeze()

    sf.write(str(out_path), audio, tts.sr)
    duration_ms = int(len(audio) / tts.sr * 1000)
    print(f"OK:{out_path}:{duration_ms}")


if __name__ == "__main__":
    main()
