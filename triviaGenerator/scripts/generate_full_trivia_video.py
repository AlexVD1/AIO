import os
import sys
import json
import argparse
import asyncio
import subprocess
import tempfile
import shutil

ASSETS_DIR = r"src/main/resources/video-assets"
AUDIO_DIR = os.path.join(ASSETS_DIR, "audio")
FONT_FILE = os.path.join(ASSETS_DIR, "fonts", "font.ttf").replace("\\", "/")

# Calibrated durations for comfortable human reading without TTS
DEFAULT_QUESTION_READ_DURATION = 7.0   # Time to comfortably read question and 4 choices
DEFAULT_COUNTDOWN_DURATION = 5.0       # Thinking time (5, 4, 3, 2, 1 with ticks)
DEFAULT_ANSWER_DURATION = 6.5          # Time to comfortably read the correct answer & explanation
DEFAULT_TOTAL_SEGMENT_DURATION = DEFAULT_QUESTION_READ_DURATION + DEFAULT_COUNTDOWN_DURATION + DEFAULT_ANSWER_DURATION # 18.5s

def get_audio_paths():
    return {
        "tick": os.path.join(AUDIO_DIR, "tick.wav").replace("\\", "/"),
        "correct": os.path.join(AUDIO_DIR, "correct.wav").replace("\\", "/"),
        "whoosh": os.path.join(AUDIO_DIR, "whoosh.wav").replace("\\", "/"),
        "bgm": os.path.join(AUDIO_DIR, "bgm_loop.wav").replace("\\", "/")
    }

def get_audio_duration(file_path):
    cmd = [
        "ffprobe", "-v", "error",
        "-show_entries", "format=duration",
        "-of", "default=noprint_wrappers=1:nokey=1",
        file_path
    ]
    res = subprocess.run(cmd, capture_output=True, text=True, check=True)
    return float(res.stdout.strip())

async def synthesize_tts(text, voice, out_path, rate="+5%"):
    import edge_tts
    comm = edge_tts.Communicate(text, voice, rate=rate)
    await comm.save(out_path)
    return get_audio_duration(out_path)

def render_trivia_segment(trivia, index, total, input_dir, temp_dir, video_format="vertical", tts_info=None):
    """
    Renders an individual trivia segment with optional TTS narration.
    """
    tid = trivia["id"]
    q_img = os.path.join(input_dir, f"{tid}_question.png").replace("\\", "/")
    a_img = os.path.join(input_dir, f"{tid}_answer.png").replace("\\", "/")
    
    if not os.path.exists(q_img) or not os.path.exists(a_img):
        raise FileNotFoundError(f"Missing images for trivia {tid}: {q_img} or {a_img}")
        
    sfx = get_audio_paths()
    out_segment = os.path.join(temp_dir, f"segment_{index:02d}.mp4").replace("\\", "/")
    
    header_text = f"PREGUNTA {index} DE {total}"
    
    # Calculate scene timings: dynamic if TTS is present, calibrated fixed if not
    if tts_info:
        dur_q_audio = tts_info["questionDuration"]
        dur_a_audio = tts_info["answerDuration"]
        q_read_dur = max(6.0, dur_q_audio + 1.2)
        countdown_dur = DEFAULT_COUNTDOWN_DURATION # 5.0s
        a_read_dur = max(6.0, dur_a_audio + 1.5)
    else:
        q_read_dur = DEFAULT_QUESTION_READ_DURATION # 7.0s
        countdown_dur = DEFAULT_COUNTDOWN_DURATION # 5.0s
        a_read_dur = DEFAULT_ANSWER_DURATION # 6.5s
        
    q_total_dur = q_read_dur + countdown_dur
    total_dur = q_total_dur + a_read_dur
    
    countdown_start = q_read_dur
    countdown_end = q_total_dur - 0.01
    
    # FFmpeg dynamic countdown formula based on countdown_end
    countdown_expr = f"%{{eif\\:{q_total_dur:.0f}-t\\:d}}"
    
    if video_format == "vertical":
        # 1080x1920 (9:16) format:
        video_filters = (
            f"color=c=0x0F172A:s=1080x1920:d={q_total_dur:.2f}[bg0];"
            f"[0:v]scale=1080:1080[qv0];"
            f"[bg0][qv0]overlay=0:420[qbase];"
            f"[qbase]drawtext=fontfile='{FONT_FILE}':text='{header_text}':"
            f"fontsize=42:fontcolor=0x38BDF8:x=(w-text_w)/2:y=180[qhead];"
            f"[qhead]drawtext=fontfile='{FONT_FILE}':text='TIEMPO  {countdown_expr}':"
            f"fontsize=52:fontcolor=white:box=1:boxcolor=0x1E293BEE:boxborderw=16:"
            f"x=(w-text_w)/2:y=280:enable='between(t,{countdown_start:.2f},{countdown_end:.2f})'[v0];"
            
            f"color=c=0x0F172A:s=1080x1920:d={a_read_dur:.2f}[bg1];"
            f"[1:v]scale=1080:1080[av0];"
            f"[bg1][av0]overlay=0:420[abase];"
            f"[abase]drawtext=fontfile='{FONT_FILE}':text='{header_text}':"
            f"fontsize=42:fontcolor=0x38BDF8:x=(w-text_w)/2:y=180[v1];"
            
            f"[v0][v1]concat=n=2:v=1:a=0[vfinal];"
        )
    else:
        # 1080x1080 (Square 1:1) format:
        video_filters = (
            f"[0:v]scale=1080:1080,setsar=1[v0_raw];"
            f"[1:v]scale=1080:1080,setsar=1[v1_raw];"
            f"[v0_raw]drawtext=fontfile='{FONT_FILE}':text='{countdown_expr}':"
            f"fontsize=68:fontcolor=white:box=1:boxcolor=0x0F172ACC:boxborderw=18:"
            f"x=(w-text_w)/2:y=820:enable='between(t,{countdown_start:.2f},{countdown_end:.2f})'[v0];"
            f"[v1_raw]null[v1];"
            f"[v0][v1]concat=n=2:v=1:a=0[vfinal];"
        )
        
    # Audio filters
    t1_ms = int(countdown_start * 1000)
    t2_ms = t1_ms + 1000
    t3_ms = t1_ms + 2000
    t4_ms = t1_ms + 3000
    t5_ms = t1_ms + 4000
    chime_ms = int(q_total_dur * 1000)
    
    if tts_info:
        q_speech_ms = 400
        a_speech_ms = chime_ms + 400
        
        audio_filters = (
            f"aevalsrc=0:d={total_dur:.2f}[asilence];"
            f"[2:a]volume=0.7,adelay=0|0[a_whoosh];"
            f"[5:a]adelay={q_speech_ms}|{q_speech_ms},volume=1.0[a_voice_q];"
            f"[3:a]volume=0.6,asplit=5[t1][t2][t3][t4][t5];"
            f"[t1]adelay={t1_ms}|{t1_ms}[at1];"
            f"[t2]adelay={t2_ms}|{t2_ms}[at2];"
            f"[t3]adelay={t3_ms}|{t3_ms}[at3];"
            f"[t4]adelay={t4_ms}|{t4_ms}[at4];"
            f"[t5]adelay={t5_ms}|{t5_ms}[at5];"
            f"[4:a]volume=0.75,adelay={chime_ms}|{chime_ms}[a_correct];"
            f"[6:a]adelay={a_speech_ms}|{a_speech_ms},volume=1.0[a_voice_a];"
            f"[asilence][a_whoosh][a_voice_q][at1][at2][at3][at4][at5][a_correct][a_voice_a]"
            f"amix=inputs=10:duration=first:dropout_transition=0:normalize=0,alimiter=limit=0.95[afinal]"
        )
        
        extra_inputs = [
            "-i", tts_info["questionAudioPath"],
            "-i", tts_info["answerAudioPath"]
        ]
    else:
        audio_filters = (
            f"aevalsrc=0:d={total_dur:.2f}[asilence];"
            f"[2:a]volume=0.7,adelay=0|0[a_whoosh];"
            f"[3:a]volume=0.6,asplit=5[t1][t2][t3][t4][t5];"
            f"[t1]adelay={t1_ms}|{t1_ms}[at1];"
            f"[t2]adelay={t2_ms}|{t2_ms}[at2];"
            f"[t3]adelay={t3_ms}|{t3_ms}[at3];"
            f"[t4]adelay={t4_ms}|{t4_ms}[at4];"
            f"[t5]adelay={t5_ms}|{t5_ms}[at5];"
            f"[4:a]volume=0.75,adelay={chime_ms}|{chime_ms}[a_correct];"
            f"[asilence][a_whoosh][at1][at2][at3][at4][at5][a_correct]"
            f"amix=inputs=8:duration=first:dropout_transition=0:normalize=0,alimiter=limit=0.95[afinal]"
        )
        extra_inputs = []
    
    cmd = [
        "ffmpeg", "-y",
        "-loop", "1", "-t", f"{q_total_dur:.2f}", "-i", q_img,
        "-loop", "1", "-t", f"{a_read_dur:.2f}", "-i", a_img,
        "-i", sfx["whoosh"],
        "-i", sfx["tick"],
        "-i", sfx["correct"],
        *extra_inputs,
        "-filter_complex", f"{video_filters}{audio_filters}",
        "-map", "[vfinal]",
        "-map", "[afinal]",
        "-c:v", "libx264",
        "-preset", "veryfast",
        "-pix_fmt", "yuv420p",
        "-r", "30",
        "-c:a", "aac",
        "-b:a", "192k",
        "-t", f"{total_dur:.2f}",
        out_segment
    ]
    
    res = subprocess.run(cmd, capture_output=True, text=True)
    if res.returncode != 0:
        raise RuntimeError(f"FFmpeg error on segment {index}: {res.stderr}")
        
    return out_segment, total_dur

async def pregenerate_all_tts(trivias, temp_dir, voice="es-MX-JorgeNeural", rate="+5%"):
    tts_dict = {}
    print(f"\n--- Synthesizing Neural TTS Audio with {voice} ({rate}) ---")
    for idx, trivia in enumerate(trivias, 1):
        tid = trivia["id"]
        q_path = os.path.join(temp_dir, f"tts_{idx}_q.mp3").replace("\\", "/")
        a_path = os.path.join(temp_dir, f"tts_{idx}_a.mp3").replace("\\", "/")
        
        pregunta = trivia["pregunta"]
        q_text = f"Pregunta {idx}: {pregunta}"
        
        opciones = trivia.get("opciones", [])
        correcta = next((o for o in opciones if o.get("correcta")), None)
        if correcta:
            letra = correcta.get("letra", "")
            texto = correcta.get("texto", "")
            ans_intro = f"¡Respuesta correcta: letra {letra}, {texto}!"
        else:
            ans_intro = "¡Respuesta correcta!"
            
        explicacion = trivia.get("explicacion", "")
        if explicacion:
            a_text = f"{ans_intro} {explicacion}"
        else:
            a_text = ans_intro
            
        print(f"[{idx}/{len(trivias)}] Synthesizing speech for trivia {tid[:8]}...")
        dur_q = await synthesize_tts(q_text, voice, q_path, rate=rate)
        dur_a = await synthesize_tts(a_text, voice, a_path, rate=rate)
        
        tts_dict[tid] = {
            "questionAudioPath": q_path,
            "questionDuration": dur_q,
            "answerAudioPath": a_path,
            "answerDuration": dur_a
        }
    print("All TTS audio files synthesized successfully!\n")
    return tts_dict

def generate_video(input_dir, output_path, video_format="vertical", max_trivias=10, with_bgm=True, with_tts=False, voice="es-MX-JorgeNeural"):
    trivias_json_path = os.path.join(input_dir, "trivias.json")
    if not os.path.exists(trivias_json_path):
        raise FileNotFoundError(f"trivias.json not found in {input_dir}")
        
    with open(trivias_json_path, "r", encoding="utf-8") as f:
        trivias = json.load(f)
        
    trivias = trivias[:max_trivias]
    total = len(trivias)
    
    print(f"--- Starting Video Generation ---")
    print(f"Total trivias to process: {total}")
    print(f"Format: {video_format}")
    print(f"TTS Enabled: {with_tts} (Voice: {voice if with_tts else 'N/A'})")
    print(f"Output: {output_path}")
    
    temp_dir = tempfile.mkdtemp(prefix="trivia_video_")
    segment_files = []
    total_video_duration = 0.0
    
    try:
        # Step 0: Pre-generate TTS audio if requested
        tts_data = {}
        if with_tts:
            tts_data = asyncio.run(pregenerate_all_tts(trivias, temp_dir, voice=voice))
            
        # Step 1: Render each trivia segment
        for idx, trivia in enumerate(trivias, 1):
            tid = trivia["id"]
            tts_info = tts_data.get(tid)
            seg_file, seg_dur = render_trivia_segment(
                trivia, idx, total, input_dir, temp_dir,
                video_format=video_format,
                tts_info=tts_info
            )
            segment_files.append(seg_file)
            total_video_duration += seg_dur
            print(f"[{idx}/{total}] Segment rendered ({seg_dur:.1f}s) for {tid[:8]}: {trivia.get('subtema', 'Trivia')}")
            
        print(f"\nAll {total} segments rendered. Total duration: {total_video_duration:.1f}s ({total_video_duration/60:.1f} min)")
        print("Assembling final video...")
        
        # Step 2: Create concat list file
        concat_list_file = os.path.join(temp_dir, "concat_list.txt")
        with open(concat_list_file, "w", encoding="utf-8") as f:
            for sf in segment_files:
                clean_path = sf.replace("\\", "/")
                f.write(f"file '{clean_path}'\n")
                
        raw_concat_video = os.path.join(temp_dir, "raw_concat.mp4")
        
        concat_cmd = [
            "ffmpeg", "-y",
            "-f", "concat",
            "-safe", "0",
            "-i", concat_list_file,
            "-c", "copy",
            raw_concat_video
        ]
        res = subprocess.run(concat_cmd, capture_output=True, text=True)
        if res.returncode != 0:
            raise RuntimeError(f"Error concatenating segments: {res.stderr}")
            
        # Step 3: Add background music (BGM) loop if requested
        if with_bgm:
            sfx = get_audio_paths()
            bgm_path = sfx["bgm"]
            bgm_vol = 0.09 if with_tts else 0.15 # softer when TTS voice is present
            
            final_cmd = [
                "ffmpeg", "-y",
                "-i", raw_concat_video,
                "-stream_loop", "-1", "-i", bgm_path,
                "-filter_complex",
                f"[1:a]volume={bgm_vol:.2f}[bgm_soft];"
                f"[0:a][bgm_soft]amix=inputs=2:duration=first:dropout_transition=0:normalize=0,alimiter=limit=0.95[aout]",
                "-map", "0:v",
                "-map", "[aout]",
                "-c:v", "copy",
                "-c:a", "aac",
                "-b:a", "192k",
                "-t", f"{total_video_duration:.2f}",
                output_path
            ]
            print(f"Applying background music mix (volume={bgm_vol})...")
            res = subprocess.run(final_cmd, capture_output=True, text=True)
            if res.returncode != 0:
                raise RuntimeError(f"Error mixing BGM: {res.stderr}")
        else:
            shutil.copyfile(raw_concat_video, output_path)
            
        print(f"\n========================================================")
        print(f"SUCCESS: Video successfully generated at:\n{os.path.abspath(output_path)}")
        file_size_mb = os.path.getsize(output_path) / (1024 * 1024)
        print(f"File size: {file_size_mb:.2f} MB")
        print(f"Actual duration: {total_video_duration:.1f} seconds ({total_video_duration/60:.1f} min)")
        print(f"========================================================\n")
        
    finally:
        shutil.rmtree(temp_dir, ignore_errors=True)

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description="Trivia Video Pipeline Generator with TTS")
    parser.add_argument("--input", default=r"docs/triviasExample", help="Directory with trivias.json and PNGs")
    parser.add_argument("--output", default="scripts/trivia_video_with_tts.mp4", help="Output MP4 file path")
    parser.add_argument("--format", choices=["vertical", "square"], default="vertical", help="Video aspect format")
    parser.add_argument("--max", type=int, default=10, help="Max trivias to include")
    parser.add_argument("--no-bgm", action="store_true", help="Disable background music")
    parser.add_argument("--tts", action="store_true", help="Enable neural TTS narration for question and answer")
    parser.add_argument("--voice", default="es-MX-JorgeNeural", help="Voice for TTS (e.g. es-MX-JorgeNeural, es-MX-DaliaNeural)")
    
    args = parser.parse_args()
    generate_video(
        input_dir=args.input,
        output_path=args.output,
        video_format=args.format,
        max_trivias=args.max,
        with_bgm=not args.no_bgm,
        with_tts=args.tts,
        voice=args.voice
    )
