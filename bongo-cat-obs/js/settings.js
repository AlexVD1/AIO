/**
 * Bongo Cat Settings Manager
 * Soporta selección de personaje, múltiples accesorios simultáneos y micrófono para voz
 */

const DEFAULT_SETTINGS = {
  character: 'cat',         // 'cat' (Gato), 'dog' (Shiba Inu), 'frog' (Rana), 'capybara' (Capibara)
  skin: 'classic',          // 'classic', 'orange', 'tuxedo', 'siamese', 'pink'
  accessories: [],          // Array de múltiples accesorios activos al mismo tiempo
  keyboard: 'rgb',          // 'rgb', 'cyberpunk', 'minimal', 'dark'
  swapPeripherals: false,   // false = Estándar (Teclado Izquierda / Ratón Derecha), true = Zurdo
  hud: true,                // Mostrar KPS y total de pulsaciones
  combo: true,              // Efecto de racha de fuego
  eco: false,               // Modo eco 30 fps
  smoothing: 7,             // 1 a 10 (factor de lerp del ratón)
  micEnabled: false,        // Reactividad de voz / micrófono
  micSensitivity: 6         // 1 a 10 (sensibilidad del micrófono)
};

class SettingsManager {
  constructor() {
    this.settings = { ...DEFAULT_SETTINGS, accessories: [...DEFAULT_SETTINGS.accessories] };
    this.listeners = [];
    this.load();
    this.initUI();
  }

  load() {
    try {
      const saved = localStorage.getItem('bongo_cat_settings');
      if (saved) {
        const parsed = JSON.parse(saved);
        // Compatibilidad hacia atrás si antes era string
        if (!Array.isArray(parsed.accessories)) {
          parsed.accessories = [];
        }
        if (parsed.version !== 2) {
          parsed.swapPeripherals = false;
          parsed.version = 2;
        }
        this.settings = { ...DEFAULT_SETTINGS, ...parsed };
      }
    } catch (e) {
      console.warn('No se pudieron cargar los ajustes de localStorage', e);
    }

    // Parámetros de URL tienen máxima prioridad (ej: ?char=dog&acc=headphones,sunglasses,coffee)
    try {
      const urlParams = new URLSearchParams(window.location.search);
      
      // Personaje
      if (urlParams.has('character')) this.settings.character = urlParams.get('character');
      if (urlParams.has('char')) this.settings.character = urlParams.get('char');

      // Skin
      if (urlParams.has('skin')) this.settings.skin = urlParams.get('skin');

      // Múltiples Accesorios separados por coma: ?acc=headphones,sunglasses,coffee
      if (urlParams.has('accessories') || urlParams.has('acc')) {
        const accParam = urlParams.get('accessories') || urlParams.get('acc');
        if (accParam === 'none' || accParam === '') {
          this.settings.accessories = [];
        } else {
          this.settings.accessories = accParam.split(',').map(s => s.trim()).filter(Boolean);
        }
      }

      // Teclado
      if (urlParams.has('keyboard')) this.settings.keyboard = urlParams.get('keyboard');
      if (urlParams.has('kb')) this.settings.keyboard = urlParams.get('kb');

      // Micrófono
      if (urlParams.has('mic')) this.settings.micEnabled = urlParams.get('mic') === 'true';

      // Posición horizontal de periféricos (swap)
      if (urlParams.has('swap')) this.settings.swapPeripherals = urlParams.get('swap') === 'true';

      // HUD y Rendimiento
      if (urlParams.has('hud')) this.settings.hud = urlParams.get('hud') !== 'false';
      if (urlParams.has('eco')) this.settings.eco = urlParams.get('eco') === 'true';
    } catch (e) {
      // Ignorar entornos sin URLSearchParams
    }
  }

  save() {
    try {
      localStorage.setItem('bongo_cat_settings', JSON.stringify(this.settings));
      this.notify();
    } catch (e) {
      console.warn('Error al guardar ajustes', e);
    }
  }

  onChange(cb) {
    this.listeners.push(cb);
  }

  notify() {
    this.listeners.forEach(cb => cb(this.settings));
  }

  get(key) {
    return this.settings[key];
  }

  set(key, val) {
    this.settings[key] = val;
    this.save();
  }

  toggleAccessory(accName) {
    if (!this.settings.accessories) this.settings.accessories = [];
    const idx = this.settings.accessories.indexOf(accName);
    if (idx >= 0) {
      this.settings.accessories.splice(idx, 1);
    } else {
      this.settings.accessories.push(accName);
    }
    this.save();
  }

  hasAccessory(accName) {
    return this.settings.accessories && this.settings.accessories.includes(accName);
  }

  initUI() {
    const modal = document.getElementById('settings-modal');
    const btnOpen = document.getElementById('btn-open-settings');
    const btnClose = document.getElementById('btn-close-settings');
    const btnSave = document.getElementById('btn-save-settings');

    const toggleModal = () => {
      if (modal.classList.contains('hidden')) {
        this.syncUIFromSettings();
        modal.classList.remove('hidden');
      } else {
        modal.classList.add('hidden');
      }
    };

    btnOpen?.addEventListener('click', toggleModal);
    btnClose?.addEventListener('click', () => modal.classList.add('hidden'));
    btnSave?.addEventListener('click', () => modal.classList.add('hidden'));

    window.addEventListener('keydown', (e) => {
      if (e.key === 'F2') {
        e.preventDefault();
        toggleModal();
      } else if (e.key === 'Escape' && !modal.classList.contains('hidden')) {
        modal.classList.add('hidden');
      }
    });

    // 1. Selector de Personajes (Cat, Dog, Frog, Capybara)
    document.querySelectorAll('.char-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        const charName = btn.dataset.character;
        if (!charName) return;
        document.querySelectorAll('.char-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        this.set('character', charName);
      });
    });

    // 2. Selector de Skin / Pelaje
    document.querySelectorAll('.skin-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        const val = btn.dataset.val;
        if (!val) return;
        document.querySelectorAll('.skin-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        this.set('skin', val);
      });
    });

    // 3. Multi-selección de Accesorios (Toggle con click)
    document.querySelectorAll('.acc-chip').forEach(chip => {
      chip.addEventListener('click', () => {
        const acc = chip.dataset.acc;
        if (!acc) return;
        if (acc === 'none') {
          this.settings.accessories = [];
          this.save();
          this.syncUIFromSettings();
          return;
        }
        this.toggleAccessory(acc);
        chip.classList.toggle('active', this.hasAccessory(acc));
      });
    });

    // 4. Selector de Teclado
    document.querySelectorAll('.kb-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        const val = btn.dataset.val;
        if (!val) return;
        document.querySelectorAll('.kb-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        this.set('keyboard', val);
      });
    });

    // 5. Micrófono / Lipsync
    const chkMic = document.getElementById('chk-mic');
    chkMic?.addEventListener('change', async (e) => {
      const enabled = e.target.checked;
      this.set('micEnabled', enabled);
      if (enabled && window.AudioReactive) {
        await window.AudioReactive.start();
      } else if (!enabled && window.AudioReactive) {
        window.AudioReactive.stop();
      }
    });

    const rngMicSens = document.getElementById('rng-mic-sens');
    rngMicSens?.addEventListener('input', (e) => {
      this.set('micSensitivity', parseInt(e.target.value, 10));
    });

    // Conectar medidor visual de volumen en ajustes
    if (window.AudioReactive) {
      const micMeterBar = document.getElementById('mic-level-bar');
      window.AudioReactive.onVolumeChange = (vol) => {
        if (micMeterBar) {
          micMeterBar.style.width = `${Math.round(vol * 100)}%`;
        }
      };
    }

    // 6. Opciones de Rendimiento y HUD
    const chkHud = document.getElementById('chk-hud');
    chkHud?.addEventListener('change', (e) => {
      this.set('hud', e.target.checked);
      const hudEl = document.getElementById('streamer-hud');
      if (hudEl) hudEl.classList.toggle('hidden', !e.target.checked);
    });

    const chkCombo = document.getElementById('chk-combo');
    chkCombo?.addEventListener('change', (e) => this.set('combo', e.target.checked));

    const chkEco = document.getElementById('chk-eco');
    chkEco?.addEventListener('change', (e) => this.set('eco', e.target.checked));

    const chkSwap = document.getElementById('chk-swap');
    chkSwap?.addEventListener('change', (e) => this.set('swapPeripherals', e.target.checked));

    const rngSmoothing = document.getElementById('rng-smoothing');
    rngSmoothing?.addEventListener('input', (e) => this.set('smoothing', parseInt(e.target.value, 10)));

    this.syncUIFromSettings();
  }

  syncUIFromSettings() {
    // Personaje activo
    document.querySelectorAll('.char-btn').forEach(btn => {
      btn.classList.toggle('active', btn.dataset.character === this.settings.character);
    });

    // Skin activa
    document.querySelectorAll('.skin-btn').forEach(btn => {
      btn.classList.toggle('active', btn.dataset.val === this.settings.skin);
    });

    // Accesorios activos (Multi-select)
    document.querySelectorAll('.acc-chip').forEach(chip => {
      const acc = chip.dataset.acc;
      if (acc === 'none') {
        chip.classList.toggle('active', !this.settings.accessories || this.settings.accessories.length === 0);
      } else {
        chip.classList.toggle('active', this.hasAccessory(acc));
      }
    });

    // Estilo de teclado activo
    document.querySelectorAll('.kb-btn').forEach(btn => {
      btn.classList.toggle('active', btn.dataset.val === this.settings.keyboard);
    });

    // Micrófono
    const chkMic = document.getElementById('chk-mic');
    if (chkMic) chkMic.checked = !!this.settings.micEnabled;

    const rngMicSens = document.getElementById('rng-mic-sens');
    if (rngMicSens) rngMicSens.value = this.settings.micSensitivity || 6;

    // Posición horizontal (Zurdo)
    const chkSwap = document.getElementById('chk-swap');
    if (chkSwap) chkSwap.checked = !!this.settings.swapPeripherals;

    // Checkboxes
    const chkHud = document.getElementById('chk-hud');
    if (chkHud) chkHud.checked = this.settings.hud;

    const chkCombo = document.getElementById('chk-combo');
    if (chkCombo) chkCombo.checked = this.settings.combo;

    const chkEco = document.getElementById('chk-eco');
    if (chkEco) chkEco.checked = this.settings.eco;

    const rngSmoothing = document.getElementById('rng-smoothing');
    if (rngSmoothing) rngSmoothing.value = this.settings.smoothing;

    const hudEl = document.getElementById('streamer-hud');
    if (hudEl) hudEl.classList.toggle('hidden', !this.settings.hud);
  }
}

window.Settings = new SettingsManager();
