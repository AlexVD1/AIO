/**
 * Bongo Cat Procedural Canvas 2D Renderer
 * Soporta múltiples personajes (Gato, Shiba Inu, Ranita, Capibara),
 * apertura de boca por voz (Lipsync) y renderizado de múltiples accesorios simultáneos.
 */

class BongoCatRenderer {
  constructor(canvas) {
    this.canvas = canvas;
    this.ctx = canvas.getContext('2d', { alpha: true, desynchronized: true });
    
    // Dimensiones lógicas
    this.width = canvas.width;
    this.height = canvas.height;

    // Estado del teclado
    this.activeKeys = new Set();
    this.kbPawTarget = { x: 560, y: 380 };
    this.kbPawPos = { x: 560, y: 380 };
    this.kbPawDown = false;
    this.lastKeyPressTime = 0;

    // Estado del ratón
    this.mouseTarget = { x: 0.5, y: 0.5 };
    this.mouseCurrent = { x: 0.5, y: 0.5 };
    this.mouseButtons = { left: false, right: false };

    // Estado de la cara y expresiones
    this.blinkTimer = 0;
    this.isBlinking = false;
    this.isHappy = false;
    this.headBob = 0;
    this.mouthOpen = 0.0; // Modulado por audio-reactive.js

    // RGB wave timer
    this.rgbPhase = 0;

    // Inversión horizontal de periféricos: false = estándar (Teclado izq, Ratón der)
    this.isSwapped = false;
    this.initKeyboardLayout(false);
  }

  getPalette(settings) {
    const char = settings.character || 'cat';
    const skin = settings.skin || 'classic';

    if (char === 'dog') {
      // Paleta Shiba Inu
      return {
        fur: '#e69138',
        furDark: '#c27420',
        whiteFur: '#fff8ee',
        innerEar: '#f4a896',
        pads: '#3d2314',
        outline: '#381e0d',
        shading: '#d47f25',
        nose: '#1e140a'
      };
    } else if (char === 'frog') {
      // Paleta Ranita Kawaii
      return {
        fur: '#86efac',
        furDark: '#4ade80',
        belly: '#dcfce7',
        innerEar: '#bbf7d0',
        pads: '#4ade80',
        outline: '#14532d',
        shading: '#22c55e',
        blush: '#f472b6'
      };
    } else if (char === 'capybara') {
      // Paleta Capibara Chill
      return {
        fur: '#b47b48',
        furDark: '#8b562a',
        snout: '#78441f',
        innerEar: '#d29665',
        pads: '#5c3315',
        outline: '#2e1808',
        shading: '#966133'
      };
    }

    // Gato (Bongo Cat)
    switch (skin) {
      case 'orange':
        return {
          fur: '#f6ad55',
          furDark: '#dd6b20',
          innerEar: '#feb2b2',
          pads: '#f687b3',
          outline: '#7b341e',
          shading: '#ea8c35',
          stripes: '#c05621'
        };
      case 'tuxedo':
        return {
          fur: '#1e293b',
          furDark: '#0f172a',
          innerEar: '#f472b6',
          pads: '#f472b6',
          outline: '#020617',
          shading: '#090d16',
          chestWhite: '#f8fafc'
        };
      case 'siamese':
        return {
          fur: '#fed7aa',
          furDark: '#78350f',
          innerEar: '#fca5a5',
          pads: '#78350f',
          outline: '#451a03',
          shading: '#fcd34d',
          points: '#451a03'
        };
      case 'pink':
        return {
          fur: '#fbcfe8',
          furDark: '#f472b6',
          innerEar: '#fb7185',
          pads: '#f43f5e',
          outline: '#831843',
          shading: '#f9a8d4'
        };
      case 'classic':
      default:
        return {
          fur: '#ffffff',
          furDark: '#e2e8f0',
          innerEar: '#fbcfe8',
          pads: '#f472b6',
          outline: '#1e293b',
          shading: '#cbd5e1'
        };
    }
  }

  initKeyboardLayout(swap = false) {
    this.isSwapped = swap;
    if (swap) {
      // Ratón a la izquierda, Teclado a la derecha
      this.mousepad = { x: 75, y: 340, w: 260, h: 155, r: 16 };
      this.kbBox = { x: 425, y: 350, w: 290, h: 140 };
      this.kbPawTarget = { x: 560, y: 380 };
      this.kbPawPos = { x: 560, y: 380 };
    } else {
      // Teclado a la izquierda, Ratón a la derecha (Estándar)
      this.kbBox = { x: 90, y: 350, w: 290, h: 140 };
      this.mousepad = { x: 460, y: 340, w: 260, h: 155, r: 16 };
      this.kbPawTarget = { x: 235, y: 380 };
      this.kbPawPos = { x: 235, y: 380 };
    }

    this.keys = [];
    const rows = 4;
    const cols = 9;
    const keyW = 26;
    const keyH = 22;
    const gap = 4;

    for (let r = 0; r < rows; r++) {
      // La barra espaciadora se ubica en la fila 0 (más cercana al personaje)
      const isSpaceRow = (r === 0);
      const rowOffset = (r % 2) * 8;

      for (let c = 0; c < cols; c++) {
        if (isSpaceRow && c > 1 && c < 7) {
          if (c === 2) {
            this.keys.push({
              id: 'Space',
              x: this.kbBox.x + 18 + c * (keyW + gap) + rowOffset,
              y: this.kbBox.y + 16 + r * (keyH + gap),
              w: (keyW + gap) * 5 - gap,
              h: keyH,
              pressed: false
            });
          }
          continue;
        }

        this.keys.push({
          id: `k_${r}_${c}`,
          x: this.kbBox.x + 18 + c * (keyW + gap) + rowOffset,
          y: this.kbBox.y + 16 + r * (keyH + gap),
          w: keyW,
          h: keyH,
          pressed: false
        });
      }
    }
  }

  onKeyDown(key) {
    this.lastKeyPressTime = performance.now();
    this.kbPawDown = true;
    const randIdx = Math.floor(Math.random() * this.keys.length);
    const targetKey = this.keys[randIdx];
    if (targetKey) {
      targetKey.pressed = true;
      setTimeout(() => { targetKey.pressed = false; }, 90);
      this.kbPawTarget.x = targetKey.x + targetKey.w / 2;
      this.kbPawTarget.y = targetKey.y + targetKey.h / 2;
    }
  }

  onKeyUp() {
    this.kbPawDown = false;
  }

  onMouseMove(normX, normY) {
    this.mouseTarget.x = Math.max(0.05, Math.min(0.95, normX));
    this.mouseTarget.y = Math.max(0.05, Math.min(0.95, normY));
  }

  onMouseDown(button) {
    if (button === 0 || button === 'left') this.mouseButtons.left = true;
    if (button === 2 || button === 'right') this.mouseButtons.right = true;
  }

  onMouseUp(button) {
    if (button === 0 || button === 'left') this.mouseButtons.left = false;
    if (button === 2 || button === 'right') this.mouseButtons.right = false;
  }

  update(dt, settings, mouthVolume = 0.0) {
    // Detectar si el usuario cambió la opción de swap horizontal
    const targetSwap = !!settings.swapPeripherals;
    if (this.isSwapped !== targetSwap) {
      this.initKeyboardLayout(targetSwap);
    }

    this.palette = this.getPalette(settings);
    this.rgbPhase += dt * 3;
    this.mouthOpen = mouthVolume;

    // Suavizado de pata del teclado
    const pLerp = this.kbPawDown ? 0.45 : 0.22;
    this.kbPawPos.x += (this.kbPawTarget.x - this.kbPawPos.x) * pLerp;
    this.kbPawPos.y += (this.kbPawTarget.y - this.kbPawPos.y) * pLerp;

    const now = performance.now();
    if (now - this.lastKeyPressTime > 160) {
      this.kbPawDown = false;
      this.kbPawTarget.x = this.kbBox.x + this.kbBox.w * 0.55;
      this.kbPawTarget.y = this.kbBox.y + this.kbBox.h * 0.45;
    }

    // Suavizado del ratón
    const smoothFactor = (settings.smoothing || 7) * 0.035;
    this.mouseCurrent.x += (this.mouseTarget.x - this.mouseCurrent.x) * smoothFactor;
    this.mouseCurrent.y += (this.mouseTarget.y - this.mouseCurrent.y) * smoothFactor;

    // Parpadeo natural
    this.blinkTimer += dt;
    if (this.blinkTimer > 3.8) {
      this.isBlinking = true;
      if (this.blinkTimer > 4.0) {
        this.isBlinking = false;
        this.blinkTimer = Math.random() * 1.5;
      }
    }

    // Balanceo suave con la respiración/tecleo
    this.headBob = Math.sin(now * 0.003) * 2;
  }

  render(settings, kps) {
    const ctx = this.ctx;
    ctx.clearRect(0, 0, this.width, this.height);

    this.isHappy = kps >= 4;
    const char = settings.character || 'cat';
    const accList = settings.accessories || [];

    // 1. CUERPO Y CABEZA DEL PERSONAJE (Se dibuja al fondo, sentado detrás de la mesa)
    if (char === 'dog') {
      this.drawDogBody(ctx, settings);
    } else if (char === 'frog') {
      this.drawFrogBody(ctx, settings);
    } else if (char === 'capybara') {
      this.drawCapybaraBody(ctx, settings);
    } else {
      this.drawCatBody(ctx, settings);
    }

    // 2. ACCESORIOS DE CABEZA / CARA / CUELLO (Gafas, gorros, auriculares, bufanda)
    this.drawHeadAccessories(ctx, accList);

    // 3. SUPERFICIE DEL ESCRITORIO (Cubre limpiamente el torso inferior para dar profundidad 3D)
    this.drawDesk(ctx);

    // 4. BEBIDAS EN LA MESA (Café humeante, Boba tea)
    this.drawDeskAccessories(ctx, accList);

    // 5. ALFOMBRILLA Y RATÓN (Sobre el escritorio)
    this.drawMousepadAndMouse(ctx, settings);

    // 6. TECLADO MECÁNICO (Sobre el escritorio)
    this.drawKeyboard(ctx, settings);

    // 7. PATAS INTERACTIVAS (Salen de los hombros y se apoyan sobre el teclado y el ratón)
    if (this.isSwapped) {
      this.drawMousePaw(ctx, 300, 315, this.currentMousePos);
      this.drawKeyboardPaw(ctx, 500, 315, this.kbPawPos);
    } else {
      this.drawKeyboardPaw(ctx, 300, 315, this.kbPawPos);
      this.drawMousePaw(ctx, 500, 315, this.currentMousePos);
    }
  }

  drawDesk(ctx) {
    // 1. Superficie principal del escritorio
    ctx.fillStyle = '#151922';
    ctx.beginPath();
    ctx.roundRect(40, 310, 720, 195, 20);
    ctx.fill();

    // 2. Borde exterior
    ctx.strokeStyle = '#262c3b';
    ctx.lineWidth = 4;
    ctx.stroke();

    // 3. Luz cenital sutil
    const grad = ctx.createLinearGradient(0, 310, 0, 500);
    grad.addColorStop(0, 'rgba(255, 255, 255, 0.05)');
    grad.addColorStop(1, 'rgba(0, 0, 0, 0.35)');
    ctx.fillStyle = grad;
    ctx.fill();

    // 4. Bisel superior que define el horizonte de la mesa frente al personaje
    ctx.strokeStyle = '#374151';
    ctx.lineWidth = 3;
    ctx.beginPath();
    ctx.moveTo(55, 312);
    ctx.lineTo(745, 312);
    ctx.stroke();
  }

  drawMousepadAndMouse(ctx, settings) {
    const mp = this.mousepad;

    ctx.save();
    ctx.fillStyle = '#0f131a';
    ctx.beginPath();
    ctx.roundRect(mp.x, mp.y, mp.w, mp.h, mp.r);
    ctx.fill();

    ctx.strokeStyle = settings.keyboard === 'cyberpunk' ? '#ec4899' : 'rgba(99, 102, 241, 0.4)';
    ctx.lineWidth = 3;
    ctx.stroke();

    const mousePadInnerW = mp.w - 80;
    const mousePadInnerH = mp.h - 80;
    const mx = mp.x + 40 + this.mouseCurrent.x * mousePadInnerW;
    const my = mp.y + 40 + this.mouseCurrent.y * mousePadInnerH;
    this.currentMousePos = { x: mx, y: my };

    // Sombra del ratón
    ctx.fillStyle = 'rgba(0, 0, 0, 0.35)';
    ctx.beginPath();
    ctx.ellipse(mx, my + 6, 22, 32, 0, 0, Math.PI * 2);
    ctx.fill();

    // Cuerpo ergonómico del ratón (palma arriba, botones abajo apuntando al frente)
    ctx.fillStyle = '#222834';
    ctx.strokeStyle = '#3a4456';
    ctx.lineWidth = 2.5;
    ctx.beginPath();
    ctx.roundRect(mx - 20, my - 30, 40, 60, [22, 22, 14, 14]);
    ctx.fill();
    ctx.stroke();

    // Botones del ratón
    const leftDown = this.mouseButtons.left;
    const rightDown = this.mouseButtons.right;

    ctx.fillStyle = leftDown ? '#6366f1' : '#2b3445';
    ctx.beginPath();
    ctx.roundRect(mx - 18, my + 6, 17, 24, [2, 2, 2, 12]);
    ctx.fill();

    ctx.fillStyle = rightDown ? '#6366f1' : '#2b3445';
    ctx.beginPath();
    ctx.roundRect(mx + 1, my + 6, 17, 24, [2, 2, 12, 2]);
    ctx.fill();

    // Rueda scroll
    ctx.fillStyle = (leftDown || rightDown) ? '#38bdf8' : '#64748b';
    ctx.beginPath();
    ctx.roundRect(mx - 3, my + 10, 6, 12, 3);
    ctx.fill();

    // Relieve suave en zona de apoyo de la palma
    ctx.fillStyle = 'rgba(255, 255, 255, 0.05)';
    ctx.beginPath();
    ctx.ellipse(mx, my - 12, 12, 9, 0, 0, Math.PI * 2);
    ctx.fill();

    ctx.restore();
  }

  drawKeyboard(ctx, settings) {
    const kb = this.kbBox;

    ctx.save();
    ctx.fillStyle = '#11151d';
    ctx.beginPath();
    ctx.roundRect(kb.x, kb.y, kb.w, kb.h, 12);
    ctx.fill();

    ctx.strokeStyle = '#2b3342';
    ctx.lineWidth = 3;
    ctx.stroke();

    for (let i = 0; i < this.keys.length; i++) {
      const k = this.keys[i];
      const isPressed = k.pressed;
      const yOffset = isPressed ? 3 : 0;

      let keyColor = isPressed ? '#4338ca' : '#1e2532';
      if (settings.keyboard === 'rgb') {
        const hue = (this.rgbPhase * 25 + k.x * 0.7 + k.y * 0.7) % 360;
        keyColor = isPressed ? `hsl(${hue}, 90%, 65%)` : `hsl(${hue}, 60%, 25%)`;
      } else if (settings.keyboard === 'cyberpunk') {
        keyColor = isPressed ? '#f43f5e' : '#1e1b4b';
      }

      ctx.fillStyle = 'rgba(0, 0, 0, 0.4)';
      ctx.beginPath();
      ctx.roundRect(k.x, k.y + 3, k.w, k.h, 4);
      ctx.fill();

      ctx.fillStyle = keyColor;
      ctx.beginPath();
      ctx.roundRect(k.x, k.y + yOffset, k.w, k.h, 4);
      ctx.fill();

      ctx.strokeStyle = isPressed ? 'rgba(255,255,255,0.4)' : 'rgba(255,255,255,0.06)';
      ctx.lineWidth = 1;
      ctx.stroke();
    }

    ctx.restore();
  }

  // ==========================================================
  // PERSONAJES
  // ==========================================================

  // 1. Gato Clásico (Bongo Cat)
  drawCatBody(ctx, settings) {
    const p = this.palette;
    const cx = 400;
    const cy = 250 + this.headBob;

    ctx.save();
    ctx.lineWidth = 5;
    ctx.strokeStyle = p.outline;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';

    // Oreja Izquierda
    ctx.fillStyle = p.fur;
    ctx.beginPath();
    ctx.moveTo(310, 175 + this.headBob);
    ctx.quadraticCurveTo(285, 95 + this.headBob, 295, 80 + this.headBob);
    ctx.quadraticCurveTo(345, 110 + this.headBob, 360, 160 + this.headBob);
    ctx.closePath();
    ctx.fill();
    ctx.stroke();

    ctx.fillStyle = p.innerEar;
    ctx.beginPath();
    ctx.moveTo(318, 165 + this.headBob);
    ctx.quadraticCurveTo(300, 110 + this.headBob, 305, 98 + this.headBob);
    ctx.quadraticCurveTo(340, 120 + this.headBob, 350, 155 + this.headBob);
    ctx.closePath();
    ctx.fill();

    // Oreja Derecha
    ctx.fillStyle = p.fur;
    ctx.beginPath();
    ctx.moveTo(440, 160 + this.headBob);
    ctx.quadraticCurveTo(455, 110 + this.headBob, 505, 80 + this.headBob);
    ctx.quadraticCurveTo(515, 95 + this.headBob, 490, 175 + this.headBob);
    ctx.closePath();
    ctx.fill();
    ctx.stroke();

    ctx.fillStyle = p.innerEar;
    ctx.beginPath();
    ctx.moveTo(450, 155 + this.headBob);
    ctx.quadraticCurveTo(460, 120 + this.headBob, 495, 98 + this.headBob);
    ctx.quadraticCurveTo(500, 110 + this.headBob, 482, 165 + this.headBob);
    ctx.closePath();
    ctx.fill();

    // Cuerpo
    ctx.fillStyle = p.fur;
    ctx.beginPath();
    ctx.moveTo(270, 350);
    ctx.quadraticCurveTo(260, 220 + this.headBob, 310, 175 + this.headBob);
    ctx.quadraticCurveTo(400, 140 + this.headBob, 490, 175 + this.headBob);
    ctx.quadraticCurveTo(540, 220 + this.headBob, 530, 350);
    ctx.closePath();
    ctx.fill();
    ctx.stroke();

    if (settings.skin === 'siamese') {
      ctx.fillStyle = p.points;
      ctx.beginPath();
      ctx.ellipse(400, 250 + this.headBob, 45, 32, 0, 0, Math.PI * 2);
      ctx.fill();
    } else if (settings.skin === 'orange') {
      ctx.fillStyle = p.stripes;
      this.drawTriangle(ctx, 400, 165 + this.headBob, 14, 25);
      this.drawTriangle(ctx, 370, 175 + this.headBob, 10, 20);
      this.drawTriangle(ctx, 430, 175 + this.headBob, 10, 20);
    } else if (settings.skin === 'tuxedo') {
      ctx.fillStyle = p.chestWhite;
      ctx.beginPath();
      ctx.moveTo(370, 350);
      ctx.quadraticCurveTo(400, 275 + this.headBob, 430, 350);
      ctx.closePath();
      ctx.fill();
    }

    this.drawFace(ctx, cx, cy, 'cat');
    ctx.restore();
  }

  // 2. Perrito Shiba Inu
  drawDogBody(ctx, settings) {
    const p = this.palette;
    const cx = 400;
    const cy = 250 + this.headBob;

    ctx.save();
    ctx.lineWidth = 5;
    ctx.strokeStyle = p.outline;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';

    // Orejas de Shiba Inu (triangulares ligeramente inclinadas)
    // Oreja Izq
    ctx.fillStyle = p.fur;
    ctx.beginPath();
    ctx.moveTo(300, 180 + this.headBob);
    ctx.lineTo(275, 95 + this.headBob);
    ctx.lineTo(350, 150 + this.headBob);
    ctx.closePath();
    ctx.fill();
    ctx.stroke();

    ctx.fillStyle = p.innerEar;
    ctx.beginPath();
    ctx.moveTo(304, 168 + this.headBob);
    ctx.lineTo(285, 110 + this.headBob);
    ctx.lineTo(340, 148 + this.headBob);
    ctx.closePath();
    ctx.fill();

    // Oreja Der
    ctx.fillStyle = p.fur;
    ctx.beginPath();
    ctx.moveTo(450, 150 + this.headBob);
    ctx.lineTo(525, 95 + this.headBob);
    ctx.lineTo(500, 180 + this.headBob);
    ctx.closePath();
    ctx.fill();
    ctx.stroke();

    ctx.fillStyle = p.innerEar;
    ctx.beginPath();
    ctx.moveTo(460, 148 + this.headBob);
    ctx.lineTo(515, 110 + this.headBob);
    ctx.lineTo(496, 168 + this.headBob);
    ctx.closePath();
    ctx.fill();

    // Cuerpo base
    ctx.fillStyle = p.fur;
    ctx.beginPath();
    ctx.moveTo(270, 350);
    ctx.quadraticCurveTo(255, 215 + this.headBob, 315, 165 + this.headBob);
    ctx.quadraticCurveTo(400, 135 + this.headBob, 485, 165 + this.headBob);
    ctx.quadraticCurveTo(545, 215 + this.headBob, 530, 350);
    ctx.closePath();
    ctx.fill();
    ctx.stroke();

    // Pecho y mejillas blancas de Shiba
    ctx.fillStyle = p.whiteFur;
    ctx.beginPath();
    ctx.moveTo(330, 240 + this.headBob);
    ctx.quadraticCurveTo(310, 290 + this.headBob, 340, 350);
    ctx.lineTo(460, 350);
    ctx.quadraticCurveTo(490, 290 + this.headBob, 470, 240 + this.headBob);
    ctx.quadraticCurveTo(400, 275 + this.headBob, 330, 240 + this.headBob);
    ctx.closePath();
    ctx.fill();

    // Cejas redondas blancas icónicas de Shiba Inu
    ctx.fillStyle = p.whiteFur;
    ctx.beginPath();
    ctx.ellipse(cx - 50, cy - 44, 9, 6, -0.15, 0, Math.PI * 2);
    ctx.ellipse(cx + 50, cy - 44, 9, 6, 0.15, 0, Math.PI * 2);
    ctx.fill();

    this.drawFace(ctx, cx, cy, 'dog');
    ctx.restore();
  }

  // 3. Ranita Kawaii
  drawFrogBody(ctx, settings) {
    const p = this.palette;
    const cx = 400;
    const cy = 250 + this.headBob;

    ctx.save();
    ctx.lineWidth = 5;
    ctx.strokeStyle = p.outline;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';

    // Ojos saltones de rana en la parte superior
    // Ojo Izq
    ctx.fillStyle = p.fur;
    ctx.beginPath();
    ctx.arc(330, 140 + this.headBob, 36, 0, Math.PI * 2);
    ctx.fill();
    ctx.stroke();

    // Ojo Der
    ctx.beginPath();
    ctx.arc(470, 140 + this.headBob, 36, 0, Math.PI * 2);
    ctx.fill();
    ctx.stroke();

    // Cuerpo redondeado de la ranita
    ctx.beginPath();
    ctx.moveTo(260, 350);
    ctx.quadraticCurveTo(240, 220 + this.headBob, 310, 160 + this.headBob);
    ctx.quadraticCurveTo(400, 145 + this.headBob, 490, 160 + this.headBob);
    ctx.quadraticCurveTo(560, 220 + this.headBob, 540, 350);
    ctx.closePath();
    ctx.fill();
    ctx.stroke();

    // Barriga verde clara
    ctx.fillStyle = p.belly;
    ctx.beginPath();
    ctx.ellipse(cx, 330, 75, 45, 0, 0, Math.PI * 2);
    ctx.fill();

    // Globos oculares blancos dentro de los salientes
    ctx.fillStyle = '#ffffff';
    ctx.beginPath();
    ctx.arc(330, 140 + this.headBob, 25, 0, Math.PI * 2);
    ctx.arc(470, 140 + this.headBob, 25, 0, Math.PI * 2);
    ctx.fill();

    this.drawFace(ctx, cx, cy, 'frog');
    ctx.restore();
  }

  // 4. Capibara Chill
  drawCapybaraBody(ctx, settings) {
    const p = this.palette;
    const cx = 400;
    const cy = 250 + this.headBob;

    ctx.save();
    ctx.lineWidth = 5;
    ctx.strokeStyle = p.outline;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';

    // Orejitas redondas pequeñas de capibara
    ctx.fillStyle = p.innerEar;
    ctx.beginPath();
    ctx.arc(295, 175 + this.headBob, 16, 0, Math.PI * 2);
    ctx.arc(505, 175 + this.headBob, 16, 0, Math.PI * 2);
    ctx.fill();
    ctx.stroke();

    // Cabeza / cuerpo ancho y rectangular característico
    ctx.fillStyle = p.fur;
    ctx.beginPath();
    ctx.roundRect(280, 160 + this.headBob, 240, 190, [45, 45, 10, 10]);
    ctx.fill();
    ctx.stroke();

    // Hocico cuadrado alargado hacia abajo
    ctx.fillStyle = p.snout;
    ctx.beginPath();
    ctx.roundRect(335, 235 + this.headBob, 130, 90, [18, 18, 25, 25]);
    ctx.fill();
    ctx.stroke();

    this.drawFace(ctx, cx, cy, 'capybara');
    ctx.restore();
  }

  drawTriangle(ctx, x, y, w, h) {
    ctx.beginPath();
    ctx.moveTo(x - w / 2, y);
    ctx.lineTo(x + w / 2, y);
    ctx.lineTo(x, y + h);
    ctx.closePath();
    ctx.fill();
  }

  // ==========================================================
  // EXPRESIONES FACIALES & BOCA AUDIO-REACTIVA (LIPSYNC)
  // ==========================================================
  drawFace(ctx, cx, cy, charType) {
    const eyeSpacing = charType === 'frog' ? 70 : 50;
    const eyeY = charType === 'frog' ? 140 + this.headBob : cy - 25;

    // Sonrojo en mejillas
    ctx.fillStyle = 'rgba(251, 113, 133, 0.45)';
    ctx.beginPath();
    ctx.ellipse(cx - eyeSpacing - 18, cy + 8, 14, 9, 0, 0, Math.PI * 2);
    ctx.ellipse(cx + eyeSpacing + 18, cy + 8, 14, 9, 0, 0, Math.PI * 2);
    ctx.fill();

    // 1. Ojos
    ctx.strokeStyle = this.palette.outline;
    ctx.lineWidth = 4.5;

    if (this.isBlinking) {
      ctx.beginPath();
      ctx.arc(cx - eyeSpacing, eyeY, 10, 0.2 * Math.PI, 0.8 * Math.PI, false);
      ctx.arc(cx + eyeSpacing, eyeY, 10, 0.2 * Math.PI, 0.8 * Math.PI, false);
      ctx.stroke();
    } else if (this.isHappy) {
      ctx.beginPath();
      ctx.arc(cx - eyeSpacing, eyeY + 4, 12, 1.1 * Math.PI, 1.9 * Math.PI, false);
      ctx.arc(cx + eyeSpacing, eyeY + 4, 12, 1.1 * Math.PI, 1.9 * Math.PI, false);
      ctx.stroke();
    } else {
      // Ojos normales
      ctx.fillStyle = '#0f172a';
      ctx.beginPath();
      const eyeR = charType === 'capybara' ? 7 : (charType === 'frog' ? 12 : 10);
      ctx.arc(cx - eyeSpacing, eyeY, eyeR, 0, Math.PI * 2);
      ctx.arc(cx + eyeSpacing, eyeY, eyeR, 0, Math.PI * 2);
      ctx.fill();

      // Brillos de ojos
      ctx.fillStyle = '#ffffff';
      ctx.beginPath();
      ctx.arc(cx - eyeSpacing - 3, eyeY - 3, 3.5, 0, Math.PI * 2);
      ctx.arc(cx + eyeSpacing - 3, eyeY - 3, 3.5, 0, Math.PI * 2);
      ctx.fill();
    }

    // 2. Nariz
    const noseY = charType === 'capybara' ? cy - 2 : (charType === 'frog' ? cy - 10 : cy - 13);
    ctx.fillStyle = this.palette.nose || this.palette.pads || '#f472b6';
    ctx.beginPath();
    if (charType === 'capybara') {
      // Dos fosas nasales oscuras de capibara
      ctx.fillStyle = '#1f1309';
      ctx.ellipse(cx - 10, noseY, 4, 6, -0.15, 0, Math.PI * 2);
      ctx.ellipse(cx + 10, noseY, 4, 6, 0.15, 0, Math.PI * 2);
      ctx.fill();
    } else if (charType === 'frog') {
      ctx.fillStyle = '#14532d';
      ctx.arc(cx - 6, noseY, 2.5, 0, Math.PI * 2);
      ctx.arc(cx + 6, noseY, 2.5, 0, Math.PI * 2);
      ctx.fill();
    } else {
      // Nariz de gato o perro
      ctx.ellipse(cx, noseY, 5, 4, 0, 0, Math.PI * 2);
      ctx.fill();
    }

    // 3. Boca (Reactiva a la voz con Lipsync)
    const mouthY = charType === 'capybara' ? cy + 18 : cy + 6;
    const openAmount = this.mouthOpen || 0.0;

    if (openAmount > 0.08) {
      // BOCA ABIERTA (HABLANDO POR MICRÓFONO)
      const openHeight = 10 + openAmount * 22; // Dinámica según volumen
      const openWidth = 14 + openAmount * 12;

      ctx.save();
      ctx.fillStyle = '#261214';
      ctx.strokeStyle = this.palette.outline;
      ctx.lineWidth = 3.5;

      ctx.beginPath();
      ctx.ellipse(cx, mouthY + openHeight / 2, openWidth, openHeight / 2, 0, 0, Math.PI * 2);
      ctx.fill();
      ctx.stroke();

      // Lengüita rosa dentro de la boca
      ctx.fillStyle = '#fb7185';
      ctx.beginPath();
      ctx.ellipse(cx, mouthY + openHeight * 0.7, openWidth * 0.65, openHeight * 0.35, 0, 0, Math.PI * 2);
      ctx.fill();

      ctx.restore();
    } else {
      // BOCA CERRADA (:3 o sonrisa)
      ctx.strokeStyle = this.palette.outline;
      ctx.lineWidth = 3.5;
      ctx.beginPath();
      if (charType === 'frog' || charType === 'capybara') {
        // Sonrisa sutil de ranita / capibara
        ctx.arc(cx, mouthY - 3, 16, 0.2 * Math.PI, 0.8 * Math.PI, false);
      } else {
        // Boca curva clásica de gato/perro :3
        ctx.arc(cx - 7, mouthY, 7, 0.1 * Math.PI, 0.9 * Math.PI, false);
        ctx.arc(cx + 7, mouthY, 7, 0.1 * Math.PI, 0.9 * Math.PI, false);
      }
      ctx.stroke();
    }
  }

  // ==========================================================
  // PATAS (INTERACTIVAS CON TECLADO Y RATÓN)
  drawKeyboardPaw(ctx, shoulderX, shoulderY, pawPos) {
    const p = this.palette;
    const pawX = pawPos.x;
    const pawY = pawPos.y;
    const isRight = shoulderX > 400;
    const dir = isRight ? 1 : -1;

    ctx.save();
    ctx.lineWidth = 5;
    ctx.strokeStyle = p.outline;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';

    // Sombra sutil de la pata
    ctx.fillStyle = 'rgba(0, 0, 0, 0.25)';
    ctx.beginPath();
    ctx.ellipse(pawX, pawY + 6, 26, 18, isRight ? 0.2 : -0.2, 0, Math.PI * 2);
    ctx.fill();

    // 1. Brazo que baja desde el hombro
    ctx.fillStyle = p.fur;
    ctx.beginPath();
    ctx.moveTo(shoulderX - 18 * dir, shoulderY);
    ctx.quadraticCurveTo((shoulderX + pawX) / 2 - 20 * dir, (shoulderY + pawY) / 2 + 10, pawX - 16 * dir, pawY);
    ctx.lineTo(pawX + 16 * dir, pawY);
    ctx.quadraticCurveTo((shoulderX + pawX) / 2 + 20 * dir, (shoulderY + pawY) / 2 - 20, shoulderX + 18 * dir, shoulderY - 10);
    ctx.closePath();
    ctx.fill();
    ctx.stroke();

    // 2. Patita redonda
    ctx.beginPath();
    ctx.arc(pawX, pawY, 24, 0, Math.PI * 2);
    ctx.fill();
    ctx.stroke();

    // 3. Detalle de dedos / garras pulsando teclas
    ctx.strokeStyle = p.outline;
    ctx.lineWidth = 3.5;
    ctx.beginPath();
    ctx.moveTo(pawX - 8, pawY + 6);
    ctx.lineTo(pawX - 8, pawY + 18);
    ctx.moveTo(pawX, pawY + 8);
    ctx.lineTo(pawX, pawY + 21);
    ctx.moveTo(pawX + 8, pawY + 6);
    ctx.lineTo(pawX + 8, pawY + 18);
    ctx.stroke();

    ctx.restore();
  }

  drawMousePaw(ctx, shoulderX, shoulderY, mousePos) {
    const p = this.palette;
    const isRight = shoulderX > 400;
    const pos = mousePos || { x: (isRight ? 590 : 190), y: 410 };
    const pawX = pos.x;
    const pawY = pos.y - 4;
    const dir = isRight ? 1 : -1;

    ctx.save();
    ctx.lineWidth = 5;
    ctx.strokeStyle = p.outline;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';

    // Sombra sutil de la pata
    ctx.fillStyle = 'rgba(0, 0, 0, 0.22)';
    ctx.beginPath();
    ctx.ellipse(pawX, pawY + 10, 25, 18, isRight ? 0.15 : -0.15, 0, Math.PI * 2);
    ctx.fill();

    // 1. Brazo del personaje que baja suavemente desde el hombro
    ctx.fillStyle = p.fur;
    ctx.beginPath();
    ctx.moveTo(shoulderX - 18 * dir, shoulderY);
    ctx.quadraticCurveTo((shoulderX + pawX) / 2 - 18 * dir, (shoulderY + pawY) / 2 + 10, pawX - 16 * dir, pawY);
    ctx.lineTo(pawX + 16 * dir, pawY);
    ctx.quadraticCurveTo((shoulderX + pawX) / 2 + 20 * dir, (shoulderY + pawY) / 2 - 18, shoulderX + 20 * dir, shoulderY - 8);
    ctx.closePath();
    ctx.fill();
    ctx.stroke();

    // 2. Patita redonda que descansa sobre el ratón
    ctx.beginPath();
    ctx.arc(pawX, pawY, 24, 0, Math.PI * 2);
    ctx.fill();
    ctx.stroke();

    // 3. Dedos / garras sobre los botones del ratón
    ctx.strokeStyle = p.outline;
    ctx.lineWidth = 3.5;
    ctx.beginPath();
    ctx.moveTo(pawX - 8, pawY + 8);
    ctx.lineTo(pawX - 8, pawY + 22);
    ctx.moveTo(pawX, pawY + 10);
    ctx.lineTo(pawX, pawY + 25);
    ctx.moveTo(pawX + 8, pawY + 8);
    ctx.lineTo(pawX + 8, pawY + 22);
    ctx.stroke();

    ctx.restore();
  }

  // ==========================================================
  // SISTEMA MULTI-ACCESORIOS
  // ==========================================================
  drawHeadAccessories(ctx, accList) {
    const cx = 400;
    const cy = 250 + this.headBob;

    ctx.save();

    // 0. Bufanda (rodea el cuello del personaje justo arriba del escritorio)
    if (accList.includes('scarf')) {
      ctx.fillStyle = '#dc2626';
      ctx.strokeStyle = '#991b1b';
      ctx.lineWidth = 4;
      ctx.beginPath();
      ctx.roundRect(cx - 95, cy + 38, 190, 34, 16);
      ctx.fill();
      ctx.stroke();
      // Flecos
      ctx.fillStyle = '#b91c1c';
      ctx.fillRect(cx + 35, cy + 62, 26, 32);
    }

    // 1. Auriculares Gamer RGB
    if (accList.includes('headphones')) {
      ctx.strokeStyle = '#0f172a';
      ctx.lineWidth = 9;
      ctx.beginPath();
      ctx.arc(cx, cy - 35, 125, 1.15 * Math.PI, 1.85 * Math.PI, false);
      ctx.stroke();

      ctx.fillStyle = '#1e293b';
      ctx.strokeStyle = '#38bdf8';
      ctx.lineWidth = 4;
      ctx.beginPath();
      ctx.roundRect(cx - 135, cy - 70, 24, 60, 10);
      ctx.fill();
      ctx.stroke();

      ctx.beginPath();
      ctx.roundRect(cx + 111, cy - 70, 24, 60, 10);
      ctx.fill();
      ctx.stroke();
    }

    // 2. Gafas de Sol Píxel (Thug Life)
    if (accList.includes('sunglasses')) {
      ctx.fillStyle = '#020617';
      ctx.strokeStyle = '#f8fafc';
      ctx.lineWidth = 2;
      ctx.fillRect(cx - 75, cy - 42, 60, 26);
      ctx.fillRect(cx + 15, cy - 42, 60, 26);
      ctx.fillRect(cx - 15, cy - 36, 30, 8);

      ctx.fillStyle = 'rgba(255, 255, 255, 0.4)';
      ctx.fillRect(cx - 68, cy - 38, 12, 18);
      ctx.fillRect(cx + 22, cy - 38, 12, 18);
    }

    // 3. Gafas Redondas Nerd
    if (accList.includes('glasses')) {
      ctx.strokeStyle = '#f59e0b';
      ctx.lineWidth = 4;
      ctx.beginPath();
      ctx.arc(cx - 45, cy - 25, 22, 0, Math.PI * 2);
      ctx.arc(cx + 45, cy - 25, 22, 0, Math.PI * 2);
      ctx.stroke();
      // Puente
      ctx.beginPath();
      ctx.moveTo(cx - 23, cy - 25);
      ctx.lineTo(cx + 23, cy - 25);
      ctx.stroke();
      // Reflejo
      ctx.fillStyle = 'rgba(255, 255, 255, 0.3)';
      ctx.beginPath();
      ctx.arc(cx - 40, cy - 30, 8, 0, Math.PI * 2);
      ctx.arc(cx + 50, cy - 30, 8, 0, Math.PI * 2);
      ctx.fill();
    }

    // 4. Sombrero de Fiesta
    if (accList.includes('partyhat')) {
      const hatX = cx - 15;
      const hatY = cy - 110;
      ctx.fillStyle = '#ec4899';
      ctx.beginPath();
      ctx.moveTo(hatX, hatY - 70);
      ctx.lineTo(hatX - 35, hatY);
      ctx.lineTo(hatX + 35, hatY);
      ctx.closePath();
      ctx.fill();

      ctx.strokeStyle = '#facc15';
      ctx.lineWidth = 7;
      ctx.beginPath();
      ctx.moveTo(hatX - 18, hatY - 30);
      ctx.lineTo(hatX + 18, hatY - 30);
      ctx.stroke();

      ctx.fillStyle = '#facc15';
      ctx.beginPath();
      ctx.arc(hatX, hatY - 72, 10, 0, Math.PI * 2);
      ctx.fill();
    }

    // 5. Corona Real de Oro
    if (accList.includes('crown')) {
      const crY = cy - 105;
      ctx.fillStyle = '#eab308';
      ctx.strokeStyle = '#a16207';
      ctx.lineWidth = 3.5;
      ctx.beginPath();
      ctx.moveTo(cx - 50, crY);
      ctx.lineTo(cx - 45, crY - 45);
      ctx.lineTo(cx - 20, crY - 20);
      ctx.lineTo(cx, crY - 55);
      ctx.lineTo(cx + 20, crY - 20);
      ctx.lineTo(cx + 45, crY - 45);
      ctx.lineTo(cx + 50, crY);
      ctx.closePath();
      ctx.fill();
      ctx.stroke();

      ctx.fillStyle = '#ef4444';
      ctx.beginPath();
      ctx.arc(cx - 45, crY - 45, 5, 0, Math.PI * 2);
      ctx.arc(cx, crY - 55, 6, 0, Math.PI * 2);
      ctx.arc(cx + 45, crY - 45, 5, 0, Math.PI * 2);
      ctx.fill();
    }

    // 6. Gorra de Béisbol
    if (accList.includes('cap')) {
      const capY = cy - 90;
      ctx.fillStyle = '#2563eb';
      ctx.strokeStyle = '#1e40af';
      ctx.lineWidth = 3;
      ctx.beginPath();
      ctx.arc(cx, capY, 55, Math.PI, 0, false);
      ctx.fill();
      ctx.stroke();
      ctx.fillStyle = '#1d4ed8';
      ctx.beginPath();
      ctx.ellipse(cx + 35, capY + 2, 45, 14, 0.15, 0, Math.PI * 2);
      ctx.fill();
      ctx.stroke();
    }

    // 7. Lazo / Moño Coquette
    if (accList.includes('bow')) {
      const bowX = cx - 45;
      const bowY = cy - 85;
      ctx.fillStyle = '#f43f5e';
      ctx.strokeStyle = '#9f1239';
      ctx.lineWidth = 3;
      ctx.beginPath();
      ctx.moveTo(bowX, bowY);
      ctx.lineTo(bowX - 25, bowY - 18);
      ctx.lineTo(bowX - 25, bowY + 18);
      ctx.closePath();
      ctx.fill();
      ctx.stroke();
      ctx.beginPath();
      ctx.moveTo(bowX, bowY);
      ctx.lineTo(bowX + 25, bowY - 18);
      ctx.lineTo(bowX + 25, bowY + 18);
      ctx.closePath();
      ctx.fill();
      ctx.stroke();
      ctx.beginPath();
      ctx.arc(bowX, bowY, 8, 0, Math.PI * 2);
      ctx.fill();
      ctx.stroke();
    }

    // 8. Flor en la Oreja
    if (accList.includes('flower')) {
      const flX = cx + 55;
      const flY = cy - 80;
      ctx.fillStyle = '#fb7185';
      for (let i = 0; i < 5; i++) {
        const angle = (i * Math.PI * 2) / 5;
        ctx.beginPath();
        ctx.arc(flX + Math.cos(angle) * 12, flY + Math.sin(angle) * 12, 9, 0, Math.PI * 2);
        ctx.fill();
      }
      ctx.fillStyle = '#fef08a';
      ctx.beginPath();
      ctx.arc(flX, flY, 8, 0, Math.PI * 2);
      ctx.fill();
    }

    // 9. Naranja en la Cabeza (Capibara Onsen)
    if (accList.includes('orange_head')) {
      const orY = cy - 100;
      ctx.fillStyle = '#f97316';
      ctx.strokeStyle = '#c2410c';
      ctx.lineWidth = 3;
      ctx.beginPath();
      ctx.arc(cx, orY, 20, 0, Math.PI * 2);
      ctx.fill();
      ctx.stroke();

      ctx.fillStyle = '#22c55e';
      ctx.beginPath();
      ctx.ellipse(cx + 8, orY - 18, 9, 5, 0.4, 0, Math.PI * 2);
      ctx.fill();
    }

    ctx.restore();
  }

  drawDeskAccessories(ctx, accList) {
    ctx.save();

    const baseCupX = this.isSwapped ? 355 : 395;
    const baseBobaX = this.isSwapped ? 350 : 390;

    // 10. Taza de Café Humeante (Mesa)
    if (accList.includes('coffee')) {
      const cupX = baseCupX;
      const cupY = 325;
      ctx.fillStyle = 'rgba(0, 0, 0, 0.3)';
      ctx.beginPath();
      ctx.ellipse(cupX + 18, cupY + 45, 20, 8, 0, 0, Math.PI * 2);
      ctx.fill();

      ctx.fillStyle = '#ef4444';
      ctx.beginPath();
      ctx.roundRect(cupX, cupY, 36, 44, [3, 3, 10, 10]);
      ctx.fill();

      ctx.strokeStyle = '#ef4444';
      ctx.lineWidth = 4;
      ctx.beginPath();
      ctx.arc(cupX - 4, cupY + 20, 10, 0.5 * Math.PI, 1.5 * Math.PI, false);
      ctx.stroke();

      const smokeOffset = Math.sin(performance.now() * 0.004) * 5;
      ctx.strokeStyle = 'rgba(255, 255, 255, 0.35)';
      ctx.lineWidth = 2.5;
      ctx.beginPath();
      ctx.moveTo(cupX + 14, cupY - 4);
      ctx.quadraticCurveTo(cupX + 14 + smokeOffset, cupY - 16, cupX + 14, cupY - 28);
      ctx.stroke();
    }

    // 11. Vaso de Boba Tea (Mesa)
    if (accList.includes('boba')) {
      const bobaX = baseBobaX;
      const bobaY = 315;
      ctx.fillStyle = 'rgba(0, 0, 0, 0.3)';
      ctx.beginPath();
      ctx.ellipse(bobaX + 18, bobaY + 58, 20, 7, 0, 0, Math.PI * 2);
      ctx.fill();

      // Vaso transparente
      ctx.fillStyle = '#fed7aa';
      ctx.strokeStyle = 'rgba(255, 255, 255, 0.6)';
      ctx.lineWidth = 2.5;
      ctx.beginPath();
      ctx.roundRect(bobaX, bobaY, 36, 56, [4, 4, 12, 12]);
      ctx.fill();
      ctx.stroke();

      // Perlas de tapioca
      ctx.fillStyle = '#451a03';
      ctx.beginPath();
      ctx.arc(bobaX + 10, bobaY + 46, 4, 0, Math.PI * 2);
      ctx.arc(bobaX + 18, bobaY + 48, 4, 0, Math.PI * 2);
      ctx.arc(bobaX + 26, bobaY + 46, 4, 0, Math.PI * 2);
      ctx.arc(bobaX + 14, bobaY + 40, 4, 0, Math.PI * 2);
      ctx.arc(bobaX + 22, bobaY + 40, 4, 0, Math.PI * 2);
      ctx.fill();

      // Popote morado
      ctx.strokeStyle = '#a855f7';
      ctx.lineWidth = 5;
      ctx.beginPath();
      ctx.moveTo(bobaX + 18, bobaY + 30);
      ctx.lineTo(bobaX + 30, bobaY - 14);
      ctx.stroke();
    }

    ctx.restore();
  }
}

window.BongoCatRenderer = BongoCatRenderer;
