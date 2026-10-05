import truststore
truststore.inject_into_ssl()

import urllib.request
import ssl
import sys
import os

print("Testing HuggingFace HTTPS connection...", flush=True)
try:
    resp = urllib.request.urlopen("https://huggingface.co/hexgrad/Kokoro-82M/resolve/main/config.json", timeout=10)
    print("Huggingface connect test status:", resp.status, flush=True)
except Exception as e:
    print("Direct connection error, trying unverified context fallback:", e, flush=True)
    ctx = ssl._create_unverified_context()
    resp = urllib.request.urlopen("https://huggingface.co/hexgrad/Kokoro-82M/resolve/main/config.json", context=ctx, timeout=10)
    print("Fallback connect status:", resp.status, flush=True)

print("Importing Kokoro...", flush=True)
from kokoro import KPipeline
import soundfile as sf

print("Initializing Kokoro Spanish pipeline (lang_code='e')...", flush=True)
pipeline = KPipeline(lang_code='e')
text = "¡Hola amiguitos! Bienvenidos a nuestra aventura para aprender a contar."
print(f"Synthesizing: '{text}'...", flush=True)

generator = pipeline(text, voice='ef_dora', speed=0.9)
os.makedirs("temp_test", exist_ok=True)
for i, (gs, ps, audio) in enumerate(generator):
    out_file = "temp_test/test_speech.wav"
    sf.write(out_file, audio, 24000)
    print(f"Saved audio chunk {i}: {len(audio)} samples to {out_file}", flush=True)
    break

print("SUCCESS: Kokoro Spanish synthesis verified!", flush=True)
