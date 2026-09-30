import math
import struct
import wave
import os

SAMPLE_RATE = 44100

def write_wav(filepath, samples, channels=1, sample_rate=SAMPLE_RATE):
    os.makedirs(os.path.dirname(filepath), exist_ok=True)
    with wave.open(filepath, 'w') as wav:
        wav.setnchannels(channels)
        wav.setsampwidth(2)  # 16-bit
        wav.setframerate(sample_rate)
        # Normalize to prevent clipping
        max_val = max(abs(s) for s in samples) if samples else 1.0
        if max_val > 0.95:
            scale = 0.95 / max_val
            samples = [s * scale for s in samples]
        
        frames = bytearray()
        for s in samples:
            int_val = int(s * 32767.0)
            int_val = max(-32768, min(32767, int_val))
            frames.extend(struct.pack('<h', int_val))
            if channels == 2:
                frames.extend(struct.pack('<h', int_val))
        wav.writeframes(frames)
    print(f"Created: {filepath} ({len(samples)/sample_rate:.2f}s)")

def generate_tick(filepath):
    # Sharp clock tick / woodblock pulse: 1000Hz fast pitch drop with exponential decay (80ms)
    duration = 0.1
    num_samples = int(SAMPLE_RATE * duration)
    samples = []
    for i in range(num_samples):
        t = i / SAMPLE_RATE
        env = math.exp(-t * 60.0)
        freq = 1200.0 - t * 4000.0
        freq = max(200.0, freq)
        val = math.sin(2 * math.pi * freq * t) * env
        samples.append(val * 0.8)
    write_wav(filepath, samples)

def generate_correct(filepath):
    # Pleasant celebratory chime: 2 ascending bell notes with harmonic overtones (C6 -> G6)
    duration = 1.4
    num_samples = int(SAMPLE_RATE * duration)
    samples = [0.0] * num_samples
    
    # Note 1: C6 (1046.5 Hz) at t = 0.0s
    # Note 2: E6 (1318.5 Hz) at t = 0.12s
    # Note 3: G6 (1567.98 Hz) at t = 0.24s
    notes = [
        (0.00, 1046.50, 0.35),
        (0.12, 1318.51, 0.45),
        (0.24, 1567.98, 0.60),
    ]
    
    for start_t, base_freq, amp in notes:
        start_idx = int(start_t * SAMPLE_RATE)
        for i in range(start_idx, num_samples):
            t = (i - start_idx) / SAMPLE_RATE
            env = math.exp(-t * 3.5)
            # Fundamental + 2nd and 3rd harmonics for bell timbre
            v = math.sin(2 * math.pi * base_freq * t)
            v += 0.4 * math.sin(2 * math.pi * (base_freq * 2.0) * t)
            v += 0.2 * math.sin(2 * math.pi * (base_freq * 3.0) * t)
            samples[i] += v * env * amp

    write_wav(filepath, samples)

def generate_whoosh(filepath):
    # Smooth dynamic swoosh / whoosh for transition (0.45s)
    # Pinkish / filtered noise with bell-shaped amplitude envelope
    import random
    duration = 0.45
    num_samples = int(SAMPLE_RATE * duration)
    samples = []
    random.seed(42)
    
    # Filter state
    prev = 0.0
    for i in range(num_samples):
        t = i / SAMPLE_RATE
        # Hanning-like window for amplitude
        amp = 0.5 * (1.0 - math.cos(2 * math.pi * t / duration))
        amp = amp ** 1.8
        
        # Frequency sweep from 250Hz up to 1800Hz then back down
        center_f = 300.0 + 1500.0 * math.sin(math.pi * t / duration)
        raw_noise = (random.random() * 2.0 - 1.0)
        # Simple IIR low-pass filter
        alpha = min(0.9, 2.0 * math.pi * center_f / SAMPLE_RATE)
        filtered = prev + alpha * (raw_noise - prev)
        prev = filtered
        
        samples.append(filtered * amp * 0.9)
    write_wav(filepath, samples)

def generate_bgm(filepath, duration_sec=16.0):
    # Subtle, rhythmic upbeat marimba / synth bass loop
    # BPM = 120 -> 2 beats per second, 0.5s per beat
    num_samples = int(SAMPLE_RATE * duration_sec)
    samples = [0.0] * num_samples
    
    # Chord progression: Am - F - C - G (4 bars, 4 beats each = 8s per cycle, 2 cycles)
    # Pentatonic cheerful marimba arpeggio notes
    bpm = 115.0
    beat_dur = 60.0 / bpm
    step_dur = beat_dur / 2.0 # 8th notes
    
    steps_total = int(duration_sec / step_dur)
    
    # Pattern of scale intervals (C major / A minor pentatonic: C, D, E, G, A)
    melody_freqs = [
        # Bar 1 (Am)
        440.0, 523.25, 659.25, 523.25, 440.0, 659.25, 523.25, 659.25,
        # Bar 2 (F)
        349.23, 440.0, 523.25, 440.0, 349.23, 523.25, 659.25, 523.25,
        # Bar 3 (C)
        261.63, 329.63, 392.00, 523.25, 392.00, 523.25, 659.25, 523.25,
        # Bar 4 (G)
        392.00, 493.88, 587.33, 493.88, 392.00, 587.33, 493.88, 392.00
    ]
    
    bass_freqs = [
        # Bar 1 (A2)
        110.0, 0, 110.0, 0, 110.0, 0, 110.0, 0,
        # Bar 2 (F2)
        87.31, 0, 87.31, 0, 87.31, 0, 87.31, 0,
        # Bar 3 (C3)
        130.81, 0, 130.81, 0, 130.81, 0, 130.81, 0,
        # Bar 4 (G2)
        98.00, 0, 98.00, 0, 98.00, 0, 98.00, 0
    ]
    
    for step in range(steps_total):
        start_t = step * step_dur
        start_idx = int(start_t * SAMPLE_RATE)
        
        # Marimba note
        m_idx = step % len(melody_freqs)
        mf = melody_freqs[m_idx]
        
        # Bass note
        b_idx = step % len(bass_freqs)
        bf = bass_freqs[b_idx]
        
        # Add marimba
        note_len = int(step_dur * 1.5 * SAMPLE_RATE)
        for i in range(note_len):
            idx = start_idx + i
            if idx >= num_samples:
                break
            t = i / SAMPLE_RATE
            env = math.exp(-t * 12.0)
            # Marimba timbre
            val = math.sin(2 * math.pi * mf * t) + 0.3 * math.sin(2 * math.pi * (mf * 3.0) * t) * math.exp(-t * 25.0)
            samples[idx] += val * env * 0.18
            
        # Add bass if active
        if bf > 0:
            bass_len = int(step_dur * 2.0 * SAMPLE_RATE)
            for i in range(bass_len):
                idx = start_idx + i
                if idx >= num_samples:
                    break
                t = i / SAMPLE_RATE
                env = math.exp(-t * 6.0)
                val = math.sin(2 * math.pi * bf * t) + 0.2 * math.sin(2 * math.pi * (bf * 2.0) * t)
                samples[idx] += val * env * 0.22
                
    write_wav(filepath, samples)

if __name__ == '__main__':
    out_dir = r"src/main/resources/video-assets/audio"
    generate_tick(os.path.join(out_dir, "tick.wav"))
    generate_correct(os.path.join(out_dir, "correct.wav"))
    generate_whoosh(os.path.join(out_dir, "whoosh.wav"))
    generate_bgm(os.path.join(out_dir, "bgm_loop.wav"), duration_sec=32.0)
    print("All audio assets generated successfully!")
