# 🎬 Guía de Configuración en OBS Studio

Esta guía te explica paso a paso cómo añadir tu **Bongo Cat Overlay** a tus escenas de **OBS Studio** o **Streamlabs OBS** con fondo 100% transparente y respuesta en tiempo real a tu teclado y ratón.

---

## ⚡ Paso Previo: Iniciar el Capturador Global

1. Haz doble clic en **[`INICIAR_BONGO_CAT.bat`](INICIAR_BONGO_CAT.bat)** (o en **[`INICIAR_SILENCIOSO.vbs`](INICIAR_SILENCIOSO.vbs)** si prefieres que se ejecute sin ventana de consola).
2. Verás que se inician:
   - **Servidor Web HTTP**: `http://127.0.0.1:8080`
   - **Canal WebSocket**: `ws://127.0.0.1:8765`

---

## 🚀 Método 1 (El más recomendado: Archivo Local)

Este método es el más rápido, directo y nunca tiene conflictos de puertos en Windows:

1. Abre **OBS Studio**.
2. En el panel inferior **Fuentes** (*Sources*), haz clic en el botón **`+`**.
3. Selecciona **Navegador** (*Browser*).
4. Nómbralo: `Bongo Cat Overlay`.
5. En la ventana de configuración:
   - ✅ Marca la casilla **Archivo local** (*Local file*).
   - Haz clic en **Examinar** (*Browse*) y selecciona el archivo:
     ```text
     c:\Users\villa\GIT DESKTOP\bongo-cat-obs\index.html
     ```
   - **Ancho (Width)**: `800`
   - **Alto (Height)**: `520`
   - **FPS**: `60` (o `30` si tienes activado el Modo Eco).
6. Haz clic en **Aceptar**. ¡Listo! El gato se conectará automáticamente a tu teclado y ratón.

---

## 🌐 Método 2 (Por URL de Servidor Web)

Si prefieres usar una dirección URL en lugar de archivo local:

1. En OBS Studio, añade una fuente **Navegador** (*Browser*).
2. Desmarca "Archivo local".
3. En **URL**, escribe:
   ```text
   http://127.0.0.1:8080
   ```
   *(⚠️ Importante: Usa el puerto **8080** para la página web. El puerto 8765 es el canal interno de datos WebSocket).*
4. **Ancho**: `800` | **Alto**: `520` | **FPS**: `60`.
5. Haz clic en **Aceptar**.

---

## ⚙️ Personalización en Vivo (Skins y Accesorios)

1. En OBS, haz clic derecho en la fuente `Bongo Cat Overlay` y selecciona **Interactuar** (*Interact*) (o simplemente abre `index.html` en Chrome).
2. Presiona la tecla **`F2`** o haz clic en el engranaje `⚙️` en la esquina superior derecha.
3. Elige tu aspecto favorito:
   - **Skins**: Blanco Clásico, Naranja Atigrado, Esmoquin Negro, Siamés, Rosa Pastel.
   - **Accesorios**: 🎧 Auriculares Gamer, 🕶️ Gafas de Sol píxel, 🎉 Sombrero de fiesta, ☕ Taza de café humeante.
   - **Estilo de Teclado**: RGB Rainbow, Cyberpunk Neón, Minimalista, Stealth.
   - **Modo Eco**: Actívalo si necesitas reducir el consumo de GPU al mínimo en juegos ultra-pesados.
