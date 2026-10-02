import os
import sys
import json
import asyncio
import argparse
import subprocess
import edge_tts

def get_audio_duration(file_path):
    cmd = [
        "ffprobe", "-v", "error",
        "-show_entries", "format=duration",
        "-of", "default=noprint_wrappers=1:nokey=1",
        file_path
    ]
    res = subprocess.run(cmd, capture_output=True, text=True, check=True)
    return float(res.stdout.strip())

async def synthesize_item(text, voice, out_path, rate="+0%"):
    communicate = edge_tts.Communicate(text, voice, rate=rate)
    await communicate.save(out_path)
    return get_audio_duration(out_path)

async def process_trivias(trivias, output_dir, voice="es-MX-JorgeNeural", rate="+0%"):
    os.makedirs(output_dir, exist_ok=True)
    results = []
    
    for idx, t in enumerate(trivias, 1):
        tid = str(t.get("id", f"trivia_{idx}"))
        q_path = os.path.join(output_dir, f"{tid}_q.mp3").replace("\\", "/")
        a_path = os.path.join(output_dir, f"{tid}_a.mp3").replace("\\", "/")
        
        pregunta = t.get("pregunta", "")
        q_text = f"Pregunta {idx}: {pregunta}"
        
        opciones = t.get("opciones", [])
        correcta = next((o for o in opciones if o.get("correcta")), None)
        if correcta:
            letra = correcta.get("letra", "")
            texto = correcta.get("texto", "")
            ans_intro = f"¡Respuesta correcta: letra {letra}, {texto}!"
        else:
            ans_intro = "¡Respuesta correcta!"
            
        explicacion = t.get("explicacion", "")
        if explicacion:
            a_text = f"{ans_intro} {explicacion}"
        else:
            a_text = ans_intro
            
        dur_q = await synthesize_item(q_text, voice, q_path, rate=rate)
        dur_a = await synthesize_item(a_text, voice, a_path, rate=rate)
        
        results.append({
            "triviaId": tid,
            "index": idx,
            "questionAudioPath": q_path,
            "questionDuration": dur_q,
            "answerAudioPath": a_path,
            "answerDuration": dur_a
        })
        
    return results

async def run_pipeline(payload, output_dir, voice="es-MX-JorgeNeural", rate="+0%"):
    os.makedirs(output_dir, exist_ok=True)

    intro_data = None
    trivias = []

    if isinstance(payload, dict):
        intro_data = payload.get("intro")
        if "trivias" in payload:
            trivias = payload["trivias"]
        else:
            trivias = [payload]
    elif isinstance(payload, list):
        trivias = payload

    intro_result = None
    if intro_data and intro_data.get("text"):
        intro_text = intro_data["text"].strip()
        if intro_text:
            intro_path = os.path.join(output_dir, "intro.mp3").replace("\\", "/")
            dur_intro = await synthesize_item(intro_text, voice, intro_path, rate=rate)
            intro_result = {
                "audioPath": intro_path,
                "duration": dur_intro
            }

    scenes_result = await process_trivias(trivias, output_dir, voice=voice, rate=rate)

    if intro_result is not None:
        return {
            "intro": intro_result,
            "scenes": scenes_result
        }
    return scenes_result

def main():
    parser = argparse.ArgumentParser(description="TTS Bridge for Trivia Narration with optional Intro")
    parser.add_argument("--input-json", required=True, help="Path to JSON file with trivias or payload")
    parser.add_argument("--output-dir", required=True, help="Output directory for audio files")
    parser.add_argument("--voice", default="es-MX-JorgeNeural", help="Edge TTS voice name")
    parser.add_argument("--rate", default="+5%", help="Speech rate adjustment (e.g. +5%%)")
    
    args = parser.parse_args()
    
    with open(args.input_json, "r", encoding="utf-8-sig") as f:
        payload = json.load(f)
        
    results = asyncio.run(run_pipeline(payload, args.output_dir, voice=args.voice, rate=args.rate))
    
    print(json.dumps(results, indent=2))

if __name__ == '__main__':
    main()
