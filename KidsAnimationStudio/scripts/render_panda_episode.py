import subprocess
import wave
import json
import sys
from pathlib import Path

sys.path.append(str(Path(__file__).parent))
from panda_timeline_renderer import PandaTimelineRenderer

SHOTS_CONFIG = [
    {"scene": 1, "shot": 1, "action": "wave", "vocal": None, "celebrate": False},
    {"scene": 2, "shot": 1, "action": "talk", "vocal": None, "celebrate": False},
    {"scene": 3, "shot": 1, "action": "vowel", "vocal": "a", "celebrate": False},
    {"scene": 4, "shot": 1, "action": "vowel", "vocal": "e", "celebrate": False},
    {"scene": 5, "shot": 1, "action": "vowel", "vocal": "i", "celebrate": False},
    {"scene": 6, "shot": 1, "action": "vowel", "vocal": "o", "celebrate": False},
    {"scene": 7, "shot": 1, "action": "vowel", "vocal": "u", "celebrate": False},
    {"scene": 8, "shot": 1, "action": "celebrate", "vocal": None, "celebrate": True},
]

def get_wav_duration(path: str) -> float:
    with wave.open(path, "rb") as wf:
        frames = wf.getnframes()
        rate = wf.getframerate()
        return frames / float(rate)

def render_panda_episode():
    root = Path(r"c:\Users\villa\GIT DESKTOP\AIO\KidsAnimationStudio")
    char_dir = root / "assets" / "characters" / "panda"
    loc_dir = root / "assets" / "locations" / "bambu"
    props_dir = root / "assets" / "props" / "vocales"

    ep_dir = root / "orchestrator" / "storage" / "episodes" / "panda_vocales_ep1"
    audio_dir = ep_dir / "audio"
    clips_dir = ep_dir / "clips"
    video_dir = ep_dir / "video"
    clips_dir.mkdir(parents=True, exist_ok=True)
    video_dir.mkdir(parents=True, exist_ok=True)

    renderer = PandaTimelineRenderer(
        character_dir=str(char_dir),
        location_dir=str(loc_dir),
        props_dir=str(props_dir)
    )

    rendered_clips = []
    audio_tracks = []
    
    print("=== FASE 1: Renderizado de los 8 planos de Bao el Panda y las Vocales ===")
    for cfg in SHOTS_CONFIG:
        sc = cfg["scene"]
        sh = cfg["shot"]
        wav_path = audio_dir / f"shot_{sc}_{sh}_master.wav"
        dur = get_wav_duration(str(wav_path)) + 0.35 # Padding de respiro infantil
        
        clip_path = clips_dir / f"shot_{sc}_{sh}.mp4"
        renderer.render_shot_video(
            duration_s=dur,
            fps=30,
            output_path=str(clip_path),
            action=cfg["action"],
            vocal_letter=cfg["vocal"],
            celebrate=cfg["celebrate"]
        )
        rendered_clips.append(str(clip_path))
        audio_tracks.append((str(wav_path), dur))

    # Concatenar video clips
    concat_list = ep_dir / "concat_clips.txt"
    with open(concat_list, "w", encoding="utf-8") as f:
        for c in rendered_clips:
            f.write(f"file '{c}'\n")

    video_only = ep_dir / "panda_video_only.mp4"
    subprocess.run([
        "ffmpeg", "-y", "-loglevel", "error",
        "-f", "concat", "-safe", "0",
        "-i", str(concat_list),
        "-c", "copy",
        str(video_only)
    ], check=True)

    # Masterizar audio continuo alineado con la duración de los clips
    # Cada clip tiene dur = wav_dur + 0.35s
    # Concatenar los wavs añadiendo el silencio correspondiente
    filter_complex = []
    inputs = []
    for i, (wpath, dur) in enumerate(audio_tracks):
        inputs.extend(["-i", wpath])
        wav_d = get_wav_duration(wpath)
        pad_ms = int(max(0, dur - wav_d) * 1000)
        filter_complex.append(f"[{i}:a]apad=pad_dur={pad_ms/1000.0:.3f}[a{i}];")
    
    concat_expr = "".join([f"[a{i}]" for i in range(len(audio_tracks))])
    concat_expr += f"concat=n={len(audio_tracks)}:v=0:a=1[aout]"
    filter_complex.append(concat_expr)

    full_audio = ep_dir / "panda_audio_master.wav"
    ffmpeg_audio_cmd = ["ffmpeg", "-y", "-loglevel", "error"] + inputs + [
        "-filter_complex", "".join(filter_complex),
        "-map", "[aout]",
        "-ar", "44100",
        str(full_audio)
    ]
    subprocess.run(ffmpeg_audio_cmd, check=True)

    # Mezclar video + voz con música suave (playful bgm)
    final_output = video_dir / "Aprendiendo_las_Vocales_con_Bao_final.mp4"
    bgm_path = root / "orchestrator" / "audio" / "bgm" / "playful_acoustic.wav"

    if bgm_path.exists():
        # Mezcla con BGM a -24 dB para claridad de la voz
        mux_cmd = [
            "ffmpeg", "-y", "-loglevel", "error",
            "-i", str(video_only),
            "-i", str(full_audio),
            "-i", str(bgm_path),
            "-filter_complex", "[1:a]volume=1.0[vocal];[2:a]volume=0.18,aloop=loop=-1:size=2e+09[bgm];[vocal][bgm]amix=inputs=2:duration=first:dropout_transition=2[aout]",
            "-map", "0:v",
            "-map", "[aout]",
            "-c:v", "copy",
            "-c:a", "aac",
            "-b:a", "192k",
            "-shortest",
            str(final_output)
        ]
    else:
        mux_cmd = [
            "ffmpeg", "-y", "-loglevel", "error",
            "-i", str(video_only),
            "-i", str(full_audio),
            "-c:v", "copy",
            "-c:a", "aac",
            "-b:a", "192k",
            str(final_output)
        ]

    subprocess.run(mux_cmd, check=True)
    print(f"=== ¡Episodio de Bao el Panda generado con éxito!: {final_output} ===")

if __name__ == "__main__":
    render_panda_episode()
