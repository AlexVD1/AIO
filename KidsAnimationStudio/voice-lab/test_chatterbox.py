"""
Synthesize all 5 Golden Phrases with Chatterbox Multilingual Voice Cloning (Alex.wav reference)
"""

import sys
import time
from pathlib import Path

# Inject system SSL truststore
try:
    import truststore
    truststore.inject_into_ssl()
except Exception as e:
    print(f"Truststore warning: {e}")

import torch
import soundfile as sf
from chatterbox import ChatterboxMultilingualTTS

device = "cuda" if torch.cuda.is_available() else "cpu"
print(f"Using Device: {device} (PyTorch: {torch.__version__})")

ROOT_DIR = Path(__file__).resolve().parent.parent
VOICE_REF = ROOT_DIR / "audio" / "voices" / "Alex.wav"
OUT_DIR = ROOT_DIR / "docs" / "quality" / "candidates"
OUT_DIR.mkdir(parents=True, exist_ok=True)

GOLDEN_PHRASES = [
    ("phrase_1", "¡Hola, niños! Soy Tito, el zorrito."),
    ("phrase_2", "¿Cuántas manzanas ves en el árbol?"),
    ("phrase_3", "¡Una, dos y tres manzanas deliciosas!"),
    ("phrase_4", "¡Muy bien hecho, lo lograste conmigo!"),
    ("phrase_5", "¡Nos vemos pronto para otra gran aventura!")
]

print(f"Loading ChatterboxMultilingualTTS model to {device}...")
t0 = time.time()
tts = ChatterboxMultilingualTTS.from_pretrained(device=device)
print(f"Model loaded in {time.time() - t0:.2f} seconds.")

print(f"Preparing voice conditioning with {VOICE_REF}...")
t1 = time.time()
tts.prepare_conditionals(str(VOICE_REF), exaggeration=0.6)
print(f"Voice conditioned in {time.time() - t1:.2f} seconds.")

for p_id, text in GOLDEN_PHRASES:
    out_path = OUT_DIR / f"{p_id}_cloned.wav"
    print(f"Synthesizing {p_id}: '{text}'...")
    t_start = time.time()
    audio = tts.generate(
        text=text,
        language_id="es"
    )
    if isinstance(audio, torch.Tensor):
        audio = audio.detach().cpu().numpy()
    if audio.ndim > 1:
        audio = audio.squeeze()
    sf.write(str(out_path), audio, tts.sr)
    print(f"  -> Saved {out_path.name} in {time.time() - t_start:.2f}s (dur: {len(audio)/tts.sr:.2f}s)")

print("All 5 golden phrases generated successfully!")
