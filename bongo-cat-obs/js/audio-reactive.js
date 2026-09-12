/**
 * Bongo Cat Audio Reactive / Lipsync Module
 * Captura ligera de micrófono con Web Audio API (consumo < 0.1% CPU)
 */

class AudioReactiveManager {
  constructor() {
    this.audioCtx = null;
    this.analyser = null;
    this.mediaStream = null;
    this.source = null;
    this.dataArray = null;
    
    this.isListening = false;
    this.volume = 0.0;
    this.smoothVolume = 0.0;
    this.mouthOpen = 0.0; // 0.0 (cerrada) a 1.0 (completamente abierta)

    this.onVolumeChange = null;
  }

  async start() {
    if (this.isListening) return true;

    try {
      const AudioContextClass = window.AudioContext || window.webkitAudioContext;
      if (!AudioContextClass) {
        console.warn('Web Audio API no está soportada en este navegador.');
        return false;
      }

      this.audioCtx = new AudioContextClass();
      
      // Pedir stream de micrófono solo de audio (mono, sin latencia)
      this.mediaStream = await navigator.mediaDevices.getUserMedia({
        audio: {
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true
        },
        video: false
      });

      this.source = this.audioCtx.createMediaStreamSource(this.mediaStream);
      this.analyser = this.audioCtx.createAnalyser();
      this.analyser.fftSize = 256; // Muestreo ultraligero
      this.analyser.smoothingTimeConstant = 0.3;

      this.source.connect(this.analyser);
      this.dataArray = new Uint8Array(this.analyser.frequencyBinCount);
      this.isListening = true;

      console.log('🎤 Micrófono activado para detección de voz (Lipsync)');
      return true;
    } catch (err) {
      console.warn('No se pudo acceder al micrófono:', err.message);
      this.isListening = false;
      return false;
    }
  }

  stop() {
    if (!this.isListening) return;

    if (this.mediaStream) {
      this.mediaStream.getTracks().forEach(track => track.stop());
      this.mediaStream = null;
    }

    if (this.audioCtx && this.audioCtx.state !== 'closed') {
      this.audioCtx.close();
      this.audioCtx = null;
    }

    this.isListening = false;
    this.volume = 0.0;
    this.smoothVolume = 0.0;
    this.mouthOpen = 0.0;
    console.log('🔇 Micrófono desactivado');
  }

  update(settings) {
    if (!this.isListening || !this.analyser || !this.dataArray) {
      this.mouthOpen *= 0.8;
      if (this.mouthOpen < 0.01) this.mouthOpen = 0.0;
      return this.mouthOpen;
    }

    // Reanudar contexto de audio si estaba suspendido por políticas del navegador
    if (this.audioCtx && this.audioCtx.state === 'suspended') {
      this.audioCtx.resume();
    }

    this.analyser.getByteTimeDomainData(this.dataArray);

    // Calcular RMS (Root Mean Square) del volumen
    let sumSquares = 0;
    for (let i = 0; i < this.dataArray.length; i++) {
      const normalized = (this.dataArray[i] - 128) / 128; // -1.0 a 1.0
      sumSquares += normalized * normalized;
    }
    const rms = Math.sqrt(sumSquares / this.dataArray.length);

    // Sensibilidad configurable (1 a 10)
    const sensMultiplier = (settings.micSensitivity || 6) * 3.5;
    const threshold = 0.02; // Umbral de silencio base

    let rawVolume = 0;
    if (rms > threshold) {
      rawVolume = Math.min(1.0, (rms - threshold) * sensMultiplier);
    }

    // Suavizado exponencial para movimiento orgánico de la boca
    const lerpSpeed = rawVolume > this.smoothVolume ? 0.45 : 0.25;
    this.smoothVolume += (rawVolume - this.smoothVolume) * lerpSpeed;

    // Apertura final de la boca
    this.mouthOpen = Math.max(0.0, Math.min(1.0, this.smoothVolume));

    if (this.onVolumeChange) {
      this.onVolumeChange(this.mouthOpen);
    }

    return this.mouthOpen;
  }
}

window.AudioReactive = new AudioReactiveManager();
