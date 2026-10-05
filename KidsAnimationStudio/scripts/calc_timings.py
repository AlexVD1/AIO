import subprocess
from pathlib import Path

root = Path(r"c:\Users\villa\GIT DESKTOP\AIO\KidsAnimationStudio")
audio_dir = root / "orchestrator" / "storage" / "episodes" / "3d29bcd5-ee1e-4803-bdbb-bf34244a6669_v2" / "audio_cloned"

shots = [
    (1, 1, "wave", None),
    (1, 2, "talk", None),
    (2, 1, "idle", None),
    (2, 2, "point", "apple"),
    (3, 1, "talk", "apple"),
    (3, 2, "point", "apple"),
    (4, 1, "talk", "apple"),
    (4, 2, "count", "apple"),
    (5, 1, "celebrate", "star"),
    (5, 2, "jump", "star"),
    (5, 3, "talk", "apple"),
    (6, 1, "point", "apple"),
    (6, 2, "count", "apple"),
    (6, 3, "count", "apple"),
    (6, 4, "celebrate", "star"),
    (7, 1, "wave", None),
]

config = []
for sc, sh, action, prop in shots:
    p = audio_dir / f"shot_{sc}_{sh}_cloned_master.wav"
    cmd = ["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "default=noprint_wrappers=1:nokey=1", str(p)]
    dur = float(subprocess.run(cmd, capture_output=True, text=True, check=True).stdout.strip())
    v_dur = round(dur + 0.2, 2)
    config.append((sc, sh, v_dur, action, prop, dur))

print("SHOTS_V2_CONFIG = [")
for c in config:
    print(f"    ({c[0]}, {c[1]}, {c[2]}, {repr(c[3])}, {repr(c[4])}),  # audio: {c[5]:.2f}s")
print("]")
