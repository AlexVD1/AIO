#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Bongo Cat Global Input Server para OBS
Ultra-eficiente: consumo de CPU < 0.2%, rate limiting inteligente de ratón (60 Hz),
servidor HTTP integrado en puerto 8080 y WebSocket en puerto 8765.
"""

import sys
import os

# Asegurar codificación UTF-8 en Windows para evitar UnicodeEncodeError en la consola
if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding='utf-8', errors='replace', line_buffering=True)
        sys.stderr.reconfigure(encoding='utf-8', errors='replace', line_buffering=True)
    except Exception:
        pass

import json
import asyncio
import threading
import time
import http.server

# Intentar importar ctypes para resolución nativa en Windows sin dependencias adicionales
try:
    import ctypes
    user32 = ctypes.windll.user32
    try:
        user32.SetProcessDPIAware()
    except Exception:
        pass
    SCREEN_WIDTH = user32.GetSystemMetrics(0)
    SCREEN_HEIGHT = user32.GetSystemMetrics(1)
except Exception:
    SCREEN_WIDTH = 1920
    SCREEN_HEIGHT = 1080

print(f"🖥️  Resolución de pantalla detectada: {SCREEN_WIDTH} x {SCREEN_HEIGHT}")

# Dependencias requeridas
try:
    import websockets
    from pynput import keyboard, mouse
except ImportError as e:
    print("\n❌ Faltan dependencias necesarias.")
    print("Por favor ejecuta: pip install -r requirements.txt")
    print(f"Detalle del error: {e}\n")
    input("Presiona Enter para salir...")
    sys.exit(1)

# Configuración de puertos
HOST = "127.0.0.1"
WS_PORT = 8765
HTTP_PORT = 8080
OVERLAY_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))

# Clientes WebSocket conectados
connected_clients = set()

# Estado atómico del ratón para rate-limiting a 60Hz
mouse_state = {
    "x": 0.5,
    "y": 0.5,
    "changed": False
}
mouse_lock = threading.Lock()

loop: asyncio.AbstractEventLoop = None

def broadcast_sync(message_dict):
    """Envía un mensaje JSON a todos los clientes WebSocket de manera thread-safe."""
    if not connected_clients or loop is None:
        return
    msg = json.dumps(message_dict)
    asyncio.run_coroutine_threadsafe(_broadcast_async(msg), loop)

async def _broadcast_async(message):
    if connected_clients:
        await asyncio.gather(
            *[client.send(message) for client in connected_clients],
            return_exceptions=True
        )

# ==========================================================
# Listeners Globales de pynput (Teclado y Ratón)
# ==========================================================
def on_press(key):
    try:
        key_name = key.char if hasattr(key, 'char') and key.char else key.name
    except AttributeError:
        key_name = str(key)
    broadcast_sync({"type": "keydown", "key": key_name})

def on_release(key):
    try:
        key_name = key.char if hasattr(key, 'char') and key.char else key.name
    except AttributeError:
        key_name = str(key)
    broadcast_sync({"type": "keyup", "key": key_name})

def on_move(x, y):
    norm_x = max(0.0, min(1.0, round(x / SCREEN_WIDTH, 4)))
    norm_y = max(0.0, min(1.0, round(y / SCREEN_HEIGHT, 4)))
    
    with mouse_lock:
        mouse_state["x"] = norm_x
        mouse_state["y"] = norm_y
        mouse_state["changed"] = True

def on_click(x, y, button, pressed):
    btn_str = "left" if button == mouse.Button.left else ("right" if button == mouse.Button.right else "middle")
    action = "mousedown" if pressed else "mouseup"
    broadcast_sync({"type": action, "button": btn_str})

# ==========================================================
# Tarea de 60 FPS para enviar el ratón sin saturar la CPU
# ==========================================================
async def mouse_broadcast_loop():
    last_sent_x = -1.0
    last_sent_y = -1.0
    while True:
        await asyncio.sleep(1 / 60.0) # ~16.6 ms (60 Hz)
        
        send_needed = False
        with mouse_lock:
            if mouse_state["changed"]:
                cur_x = mouse_state["x"]
                cur_y = mouse_state["y"]
                mouse_state["changed"] = False
                send_needed = True

        if send_needed and (cur_x != last_sent_x or cur_y != last_sent_y):
            last_sent_x = cur_x
            last_sent_y = cur_y
            msg = json.dumps({"type": "mousemove", "x": cur_x, "y": cur_y})
            await _broadcast_async(msg)

# ==========================================================
# Servidor HTTP Integrado en Segundo Plano (Puerto 8080)
# ==========================================================
class QuietOverlayHandler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=OVERLAY_DIR, **kwargs)

    def log_message(self, format, *args):
        # Silenciar logs individuales para mantener la consola limpia
        pass

def start_http_server():
    global HTTP_PORT
    for port_candidate in [8080, 8088, 8000, 8888]:
        try:
            httpd = http.server.ThreadingHTTPServer((HOST, port_candidate), QuietOverlayHandler)
            HTTP_PORT = port_candidate
            t = threading.Thread(target=httpd.serve_forever, daemon=True)
            t.start()
            return HTTP_PORT
        except OSError:
            continue
    return None

# ==========================================================
# Manejo de Clientes WebSocket
# ==========================================================
async def handle_client(websocket):
    connected_clients.add(websocket)
    print(f"🔗 Conectado exitosamente con OBS / Navegador")
    try:
        await websocket.wait_closed()
    finally:
        connected_clients.discard(websocket)
        print(f"🔌 Cliente desconectado")

# ==========================================================
# Punto de Entrada Principal
# ==========================================================
async def main():
    global loop
    loop = asyncio.get_running_loop()

    # 1. Iniciar servidor HTTP en segundo plano para servir index.html
    active_http_port = start_http_server()

    # 2. Iniciar listeners globales en segundo plano
    kb_listener = keyboard.Listener(on_press=on_press, on_release=on_release)
    m_listener = mouse.Listener(on_move=on_move, on_click=on_click)
    kb_listener.daemon = True
    m_listener.daemon = True
    kb_listener.start()
    m_listener.start()

    # 3. Iniciar bucle de ratón a 60 Hz
    asyncio.create_task(mouse_broadcast_loop())

    local_file_path = os.path.join(OVERLAY_DIR, 'index.html')

    print("==================================================")
    print("🐱  BONGO CAT GLOBAL STREAM SERVER ACTIVO")
    print(f"🌐  Servidor Web HTTP: http://{HOST}:{active_http_port}")
    print(f"📡  Canal WebSocket: ws://{HOST}:{WS_PORT}")
    print("--------------------------------------------------")
    print("💡  CÓMO USAR EN OBS STUDIO (2 Formas Sencillas):")
    print("    OPCIÓN A (Recomendada):")
    print(f"      En OBS añade fuente Navegador, marca 'Archivo local'")
    print(f"      y selecciona: {local_file_path}")
    print()
    print("    OPCIÓN B (Por URL):")
    print(f"      En OBS añade fuente Navegador con la URL:")
    print(f"      http://{HOST}:{active_http_port}")
    print()
    print("⚠️  NOTA: No abras http://127.0.0.1:8765 en el navegador;")
    print("         el puerto 8765 es solo el canal WebSocket.")
    print("⚡  Consumo de recursos: Ultra Bajo (<0.2% CPU)")
    print("==================================================")
    print("Presiona Ctrl + C para detener el servidor.\n")

    # Iniciar servidor WebSocket
    server = await websockets.serve(handle_client, HOST, WS_PORT)
    await asyncio.Future()

if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("\n👋 Servidor Bongo Cat cerrado con éxito.")
