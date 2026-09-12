# 🐾 Bongo Cat Stream Overlay para OBS Studio

Un overlay interactivo estilo **Bongo Cat** ultra-eficiente diseñado específicamente para streamers y creadores de contenido en OBS Studio, Streamlabs y navegadores web. Simula tus movimientos de ratón y pulsaciones de teclado en tiempo real mientras juegas o trabajas.

![Overlay Preview](https://raw.githubusercontent.com/kurisubrooks/bongo.cat/master/bongo.cat.png)

---

## ✨ Características Principales

- 🐱 **Bongo Cat Animado en Tiempo Real**:
  - **Pata Izquierda**: Interactúa con un teclado mecánico dinámico (las teclas se hunden y se iluminan con RGB o estilos personalizados).
  - **Pata Derecha**: Sostiene el ratón y sigue los movimientos de tu cursor en pantalla de forma suave y orgánica (LERP).
  - **Clicks del Ratón**: Los botones izquierdo y derecho del ratón se hunden e iluminan al hacer clic.
  - **Expresiones Faciales**: Parpadeo natural, ojos alegres (`^ ᵕ ^`) al escribir rápido y mejillas sonrojadas.
- ⚡ **Ultra Eficiente & Bajo Consumo**:
  - Renderizado nativo en Canvas 2D sin frameworks pesados ni dependencias innecesarias.
  - Rate limiting inteligente a 60 Hz en el servidor para ratones gamer (evita saturar la CPU con sondeos de 1000 Hz).
  - **Modo Eco conmutable** (30 FPS) para equipos que requieran el mínimo uso de GPU/CPU posible.
- 🎨 **Personalización Completa (Presiona `F2`)**:
  - **Skins**: Blanco Clásico, Naranja Atigrado, Esmoquin Negro, Siamés y Rosa Pastel.
  - **Accesorios**: Auriculares Gamer con RGB, Gafas de Sol píxel, Sombrero de Fiesta, Taza de Café humeante.
  - **Teclado**: RGB Rainbow Wave, Cyberpunk Neón, Gris Minimalista, Stealth.
- 📊 **Streamer HUD**:
  - Contador de **KPS** (*Keys Per Second*).
  - Contador de pulsaciones totales acumuladas.
  - Racha de Combo con efecto de fuego 🔥 cuando tecleas rápidamente.
- 🎬 **Listo para OBS Studio**: Fondo 100% transparente para fuentes de navegador (*Browser Source*).

---

## 🚀 Inicio Rápido en Windows

### 1. Iniciar con 1 Clic
Haz doble clic en:
👉 `INICIAR_BONGO_CAT.bat`

*(El script comprobará automáticamente tus dependencias e iniciará el servidor de captura global).*

Si prefieres que no se quede ninguna ventana negra de consola abierta en pantalla durante tus transmisiones, usa:
👉 `INICIAR_SILENCIOSO.vbs`

### 2. Probar en el Navegador
Si quieres probarlo antes de abrir OBS, simplemente abre el archivo `index.html` en Google Chrome, Edge o Firefox. Funcionará de inmediato tanto con el servidor como en modo web autónomo.

### 3. Configurar en OBS Studio
Consulta la guía completa paso a paso en [OBS_SETUP.md](OBS_SETUP.md).

Configuración básica recomendada en OBS:
- **Tipo de Fuente**: Navegador (*Browser*)
- **URL**: `http://127.0.0.1:8765`
- **Ancho / Alto**: `800 x 520`
- **FPS**: `60`

---

## 🛠️ Estructura del Proyecto

```text
bongo-cat-obs/
├── index.html                 # Página web del overlay transparente
├── style.css                  # Estilos del HUD, transparencia y modal de ajustes
├── INICIAR_BONGO_CAT.bat      # Lanzador en 1 clic para Windows
├── INICIAR_SILENCIOSO.vbs     # Lanzador silencioso en segundo plano
├── OBS_SETUP.md               # Guía paso a paso para OBS Studio
├── README.md                  # Documentación del proyecto
├── js/
│   ├── app.js                 # Lógica de conexión WebSocket y modo fallback
│   ├── cat-renderer.js        # Motor de dibujo en Canvas 2D (gato, patas, teclado, ratón)
│   └── settings.js            # Gestor de ajustes y preferencias (F2)
└── server/
    ├── server.py              # Servidor ultraligero de captura global (Python)
    ├── requirements.txt       # Dependencias mínimas (pynput, websockets)
    └── start.bat              # Script auxiliar de inicio
```

---

## ⌨️ Atajos de Teclado

- **`F2`**: Abre o cierra el panel flotante de personalización (skins, accesorios, colores, etc.).
- **`Escape`**: Cierra el panel de ajustes si está abierto.
