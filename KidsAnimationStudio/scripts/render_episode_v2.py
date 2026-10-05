import os
import sys
import subprocess
from pathlib import Path

# Configuración calculada exactamente para cada plano del episodio:
# scene, shot, duration_s, action, apple_count, show_star, text
SHOTS_V2_CONFIG = [
    # Escena 1: Introducción
    (1, 1, 4.32, "wave", 0, False),         # "¡Hola niños! Soy Tito, el zorrito."
    (1, 2, 3.80, "talk", 0, False),         # "Hoy vamos a contar manzanas."
    
    # Escena 2: Primera manzana
    (2, 1, 3.20, "talk", 1, False),         # "Una manzana." -> 1 manzana + badge '1'
    (2, 2, 2.80, "point", 1, False),        # "¿Cuántas manzanas ves?" -> 1 manzana + badge '1'
    
    # Escena 3: Dos manzanas
    (3, 1, 2.60, "talk", 2, False),         # "¡Dos manzanas!" -> 2 manzanas + badge '2'
    (3, 2, 2.04, "point", 2, False),        # "Dos manzanas." -> 2 manzanas + badge '2'
    
    # Escena 4: Tres manzanas
    (4, 1, 2.12, "talk", 3, False),         # "¡Tres manzanas!" -> 3 manzanas + badge '3'
    (4, 2, 2.56, "count", 3, False),        # "Tres manzanas." -> 3 manzanas + badge '3'
    
    # Escena 5: Celebración y repaso
    (5, 1, 1.92, "celebrate", 3, True),     # "¡Una, dos, tres!" -> 3 manzanas + estrella dorada
    (5, 2, 1.84, "jump", 0, True),          # "¿Cuántas manzanas?" -> Estrella dorada
    (5, 3, 2.48, "talk", 3, False),         # "¡Tres manzanas!" -> 3 manzanas + badge '3'
    
    # Escena 6: Repetición pedagógica dinámica
    (6, 1, 3.00, "talk", 0, False),         # "Vamos a contar de nuevo."
    (6, 2, 2.44, "count", 1, False),        # "¡Una manzana!" -> 1 manzana + badge '1'
    (6, 3, 2.24, "count", 2, False),        # "¡Dos manzanas!" -> 2 manzanas + badge '2'
    (6, 4, 2.72, "celebrate", 3, True),     # "Tres manzanas." -> 3 manzanas + badge '3' + estrella
    
    # Escena 7: Despedida afectiva
    (7, 1, 3.52, "wave", 0, False),         # "¡Fue divertido, verdad! ¡Nos vemos la próxima vez!"
]

def render_episode_v2():
    root = Path(r"c:\Users\villa\GIT DESKTOP\AIO\KidsAnimationStudio")
    sys.path.insert(0, str(root / "scripts"))
    from puppet_renderer import PuppetTimelineRenderer

    char_dir = str(root / "assets" / "characters" / "tito")
    loc_dir = str(root / "assets" / "locations" / "huerto")
    props_dir = str(root / "assets" / "props")

    ep_dir = root / "orchestrator" / "storage" / "episodes" / "3d29bcd5-ee1e-4803-bdbb-bf34244a6669_v2"
    out_clips_dir = ep_dir / "clips"
    audio_cloned_dir = ep_dir / "audio_cloned"
    out_clips_dir.mkdir(parents=True, exist_ok=True)

    renderer = PuppetTimelineRenderer(char_dir, loc_dir, props_dir)
    clip_paths = []
    audio_clip_paths = []

    print("=== FASE 1: Renderizado de los 16 planos con conteo pedagógico y lip-sync visible ===")
    for sc, sh, dur, action, apples, star in SHOTS_V2_CONFIG:
        clip_name = f"shot_{sc}_{sh}_v2.mp4"
        out_path = str(out_clips_dir / clip_name)
        renderer.render_shot_video(
            output_mp4=out_path,
            duration_s=dur,
            action=action,
            apple_count=apples,
            show_star=star,
            fps=30
        )
        clip_paths.append(out_path)

        # Clip de audio correspondiente
        audio_clip = audio_cloned_dir / f"shot_{sc}_{sh}_cloned_master.wav"
        audio_clip_paths.append(str(audio_clip))

    # Crear concat list para FFmpeg de video
    concat_video_file = out_clips_dir / "concat_video_list.txt"
    with open(concat_video_file, "w", encoding="utf-8") as f:
        for p in clip_paths:
            f.write(f"file '{p}'\n")

    # Concatenar video
    raw_concat_mp4 = str(out_clips_dir / "raw_concatenated.mp4")
    print("Concatenando planos de video...")
    cmd_concat_v = [
        "ffmpeg", "-y", "-loglevel", "error",
        "-f", "concat", "-safe", "0",
        "-i", str(concat_video_file),
        "-c:v", "libx264", "-pix_fmt", "yuv420p", "-preset", "ultrafast",
        raw_concat_mp4
    ]
    subprocess.run(cmd_concat_v, check=True)

    # Crear concat list para FFmpeg de audio clonado
    concat_audio_file = out_clips_dir / "concat_audio_list.txt"
    with open(concat_audio_file, "w", encoding="utf-8") as f:
        for p in audio_clip_paths:
            f.write(f"file '{p}'\n")

    # Concatenar audio clonado de Alex
    cloned_full_voice = ep_dir / "audio" / "cloned_voice_full.wav"
    cloned_full_voice.parent.mkdir(parents=True, exist_ok=True)
    print("Concatenando pista de voz de Alex.wav...")
    cmd_concat_a = [
        "ffmpeg", "-y", "-loglevel", "error",
        "-f", "concat", "-safe", "0",
        "-i", str(concat_audio_file),
        "-c:a", "pcm_s16le",
        str(cloned_full_voice)
    ]
    subprocess.run(cmd_concat_a, check=True)

    # Masterizar audio final a -16 LUFS
    mastered_episode_audio = ep_dir / "audio" / "episode_mastered_audio.wav"
    cmd_audio_master = [
        "ffmpeg", "-y", "-loglevel", "error",
        "-i", str(cloned_full_voice),
        "-af", "loudnorm=I=-16:TP=-1.5:LRA=11",
        "-ar", "44100",
        str(mastered_episode_audio)
    ]
    subprocess.run(cmd_audio_master, check=True)

    # Mux final del Episodio v2: Video de alta fidelidad + Voz clonada masterizada
    final_video = ep_dir / "video" / "Contando_con_Tito_v2_final.mp4"
    final_video.parent.mkdir(parents=True, exist_ok=True)

    print("Ensamblando entrega final de Episodio v2 en Full HD...")
    cmd_mux = [
        "ffmpeg", "-y", "-loglevel", "error",
        "-i", raw_concat_mp4,
        "-i", str(mastered_episode_audio),
        "-map", "0:v:0",
        "-map", "1:a:0",
        "-c:v", "libx264",
        "-preset", "veryfast",
        "-crf", "18",
        "-c:a", "aac",
        "-b:a", "192k",
        "-shortest",
        str(final_video)
    ]
    subprocess.run(cmd_mux, check=True)
    print(f"=== ¡Episodio v2 final generado con éxito!: {final_video} ===")

if __name__ == "__main__":
    render_episode_v2()
