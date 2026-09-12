/**
 * Bongo Cat Stream Overlay - Main Application
 * Conexión WebSocket ligera + Fallback para pruebas directas en navegador
 */

(function () {
  'use strict';

  const canvas = document.getElementById('bongoCanvas');
  const renderer = new BongoCatRenderer(canvas);

  // Elementos del DOM
  const kpsEl = document.getElementById('kps-counter');
  const totalEl = document.getElementById('total-counter');
  const comboEl = document.getElementById('combo-counter');
  const comboFlame = document.getElementById('combo-flame');
  const statusBadge = document.getElementById('connection-status');
  const statusText = document.getElementById('status-text');

  // Estadísticas del streamer
  let totalKeystrokes = 0;
  const keyTimestamps = [];
  let currentKPS = 0;
  let comboCount = 0;
  let lastKeyTime = 0;

  // Variables para bucle de animación a 60fps o 30fps (Modo Eco)
  let lastFrameTime = performance.now();
  let frameInterval = 1000 / 60; // 60 FPS por defecto

  // ==========================================================
  // 1. WebSocket Client para Captura Global de Windows
  // ==========================================================
  const WS_URL = 'ws://127.0.0.1:8765';
  let socket = null;
  let isConnected = false;
  let reconnectTimer = null;
  let statusFadeTimer = null;

  function updateStatus(state, message) {
    statusBadge.className = 'status-badge ' + state;
    statusText.textContent = message;

    // Atenuar status después de unos segundos conectado para que no estorbe en stream
    clearTimeout(statusFadeTimer);
    statusBadge.classList.remove('auto-fade');
    if (state === 'connected') {
      statusFadeTimer = setTimeout(() => {
        statusBadge.classList.add('auto-fade');
      }, 7000);
    }
  }

  function connectWebSocket() {
    if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) {
      return;
    }

    updateStatus('connecting', 'Buscando servidor...');

    try {
      socket = new WebSocket(WS_URL);
    } catch (e) {
      handleSocketFail();
      return;
    }

    socket.onopen = () => {
      isConnected = true;
      updateStatus('connected', 'Global Hook Activo');
      console.log('✅ Conectado al servidor de captura global (server.py)');
    };

    socket.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        handleInputEvent(data);
      } catch (err) {
        console.error('Error parseando evento:', err);
      }
    };

    socket.onclose = () => {
      isConnected = false;
      handleSocketFail();
    };

    socket.onerror = () => {
      isConnected = false;
      handleSocketFail();
    };
  }

  function handleSocketFail() {
    updateStatus('demo', 'Modo Demo Web');
    if (!reconnectTimer) {
      // Reintentar cada 4 segundos sin saturar
      reconnectTimer = setTimeout(() => {
        reconnectTimer = null;
        connectWebSocket();
      }, 4000);
    }
  }

  // ==========================================================
  // 2. Procesamiento de Eventos de Entrada
  // ==========================================================
  function handleInputEvent(data) {
    if (data.type === 'keydown') {
      registerKeyStroke(data.key);
      renderer.onKeyDown(data.key);
    } else if (data.type === 'keyup') {
      renderer.onKeyUp();
    } else if (data.type === 'mousemove') {
      // Coordenadas normalizadas [0..1]
      renderer.onMouseMove(data.x, data.y);
    } else if (data.type === 'mousedown') {
      renderer.onMouseDown(data.button);
    } else if (data.type === 'mouseup') {
      renderer.onMouseUp(data.button);
    }
  }

  function registerKeyStroke(keyName) {
    const now = performance.now();
    totalKeystrokes++;
    keyTimestamps.push(now);

    // Actualizar racha / combo
    if (now - lastKeyTime < 450) {
      comboCount++;
    } else {
      comboCount = 1;
    }
    lastKeyTime = now;

    // Actualizar elementos DOM solo cuando cambia
    totalEl.textContent = totalKeystrokes;

    if (window.Settings.get('combo') && comboCount >= 10) {
      comboFlame.classList.remove('hidden');
      comboEl.textContent = comboCount;
    } else {
      comboFlame.classList.add('hidden');
    }
  }

  // ==========================================================
  // 3. Fallback: Escucha directa en navegador (Modo de prueba)
  // ==========================================================
  window.addEventListener('keydown', (e) => {
    // Si no estamos en el modal de ajustes y se presiona una tecla
    if (e.key === 'F2' || e.key === 'Escape') return;
    
    // Si WebSocket no está activo, usar eventos locales
    if (!isConnected) {
      registerKeyStroke(e.key);
      renderer.onKeyDown(e.key);
    }
  });

  window.addEventListener('keyup', (e) => {
    if (!isConnected) {
      renderer.onKeyUp();
    }
  });

  window.addEventListener('mousemove', (e) => {
    if (!isConnected) {
      const normX = e.clientX / window.innerWidth;
      const normY = e.clientY / window.innerHeight;
      renderer.onMouseMove(normX, normY);
    }
  });

  window.addEventListener('mousedown', (e) => {
    if (!isConnected) {
      renderer.onMouseDown(e.button);
    }
  });

  window.addEventListener('mouseup', (e) => {
    if (!isConnected) {
      renderer.onMouseUp(e.button);
    }
  });

  // ==========================================================
  // 4. Bucle Principal de Renderizado (Optimizado a 60/30 FPS)
  // ==========================================================
  function gameLoop(now) {
    requestAnimationFrame(gameLoop);

    // Respetar límite de framerate si está activo el Modo Eco
    const isEco = window.Settings.get('eco');
    frameInterval = isEco ? (1000 / 30) : (1000 / 60);

    const elapsed = now - lastFrameTime;
    if (elapsed < frameInterval) return;

    const dt = Math.min(elapsed / 1000, 0.1); // Clamp para evitar saltos
    lastFrameTime = now - (elapsed % frameInterval);

    // Calcular KPS (teclas en el último segundo)
    const oneSecAgo = now - 1000;
    while (keyTimestamps.length > 0 && keyTimestamps[0] < oneSecAgo) {
      keyTimestamps.shift();
    }
    currentKPS = keyTimestamps.length;
    kpsEl.textContent = currentKPS;

    // Ocultar combo si pasó tiempo sin teclear
    if (now - lastKeyTime > 900) {
      comboFlame.classList.add('hidden');
      comboCount = 0;
    }

    // Actualizar audio reactivo si el micrófono está habilitado
    let mouthVolume = 0.0;
    if (window.AudioReactive && window.Settings.get('micEnabled')) {
      mouthVolume = window.AudioReactive.update(window.Settings.settings);
    }

    // Actualizar y renderizar gato
    renderer.update(dt, window.Settings.settings, mouthVolume);
    renderer.render(window.Settings.settings, currentKPS);
  }

  // ==========================================================
  // 5. Inicialización
  // ==========================================================
  connectWebSocket();

  // Si el micrófono estaba guardado como activado, solicitar inicio
  if (window.Settings.get('micEnabled') && window.AudioReactive) {
    window.AudioReactive.start();
  }

  requestAnimationFrame(gameLoop);

})();
