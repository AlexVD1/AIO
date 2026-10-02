#!/usr/bin/env python3
"""
generate_60_videos.py — Generador automatizado de 60 videos de trivias (5 preguntas c/u).
Temas:
  - 30 videos de Historia y Mitología ⚔️
  - 30 videos de Ciencia, Espacio y Naturaleza Extrema 🌌
"""

import json
import os
import sys
import time
import urllib.request
import urllib.error

# Forzar codificación UTF-8 en salida estándar para compatibilidad con Windows
if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
if hasattr(sys.stderr, 'reconfigure'):
    sys.stderr.reconfigure(encoding='utf-8', errors='replace')

BASE_URL = "http://172.23.240.83:8080"
API_KEY = "sxLIYr5xlb1Xuhvm60UQg8ZRZI6kwqArYSHzPTXD9DkTuniZP39o3v4iZ5WkOI1c"
STATE_FILE = "generation_60_state.json"

HEADERS = {
    "Content-Type": "application/json",
    "X-API-KEY": API_KEY
}

# 60 temas estructurados
VIDEOS_CONFIG = [
    # --- HISTORIA Y MITOLOGÍA (1 al 30) ---
    {"id": 1,  "tipo": "HISTORIA", "subtema": "Mitología Griega: Dioses del Olimpo y sus Poderes", "titulo": "Mitología Griega: Dioses del Olimpo y sus Poderes", "voice": "es-MX-JorgeNeural"},
    {"id": 2,  "tipo": "HISTORIA", "subtema": "Mitología Nórdica: Odín, Thor y el Ragnarök", "titulo": "Mitología Nórdica: Odín, Thor y el Fin del Mundo", "voice": "es-MX-JorgeNeural"},
    {"id": 3,  "tipo": "HISTORIA", "subtema": "Mitología Egipcia: Faraones, Anubis y el Más Allá", "titulo": "Mitología Egipcia: Anubis y el Juicio de los Muertos", "voice": "es-MX-DaliaNeural"},
    {"id": 4,  "tipo": "HISTORIA", "subtema": "Imperio Romano: Emperadores, Traición y Conquistas", "titulo": "Imperio Romano: Secretos y Traición en el Senado", "voice": "es-MX-JorgeNeural"},
    {"id": 5,  "tipo": "HISTORIA", "subtema": "Gladiadores de Roma: Mitos y Brutal Realidad", "titulo": "Gladiadores de Roma: Mitos y Brutal Realidad", "voice": "es-MX-JorgeNeural"},
    {"id": 6,  "tipo": "HISTORIA", "subtema": "Antiguo Egipto: Secretos y Misterios de las Pirámides", "titulo": "Antiguo Egipto: Misterios Ocultos de las Pirámides", "voice": "es-MX-DaliaNeural"},
    {"id": 7,  "tipo": "HISTORIA", "subtema": "Civilización Maya: Profecías, Templos y Rituales", "titulo": "Civilización Maya: Profecías y Dioses de la Selva", "voice": "es-MX-JorgeNeural"},
    {"id": 8,  "tipo": "HISTORIA", "subtema": "Los Vikingos: Guerreros del Norte y Exploradores", "titulo": "Los Vikingos: Guerreros Implacables del Norte", "voice": "es-MX-JorgeNeural"},
    {"id": 9,  "tipo": "HISTORIA", "subtema": "Edad Media: Caballeros, Castillos y Cruzadas", "titulo": "Edad Media: Caballeros, Asedios y Cruzadas", "voice": "es-MX-JorgeNeural"},
    {"id": 10, "tipo": "HISTORIA", "subtema": "Guerra de Troya: Héroes, Aquiles y el Caballo Maldito", "titulo": "Guerra de Troya: Aquiles y el Caballo Maldito", "voice": "es-MX-DaliaNeural"},
    {"id": 11, "tipo": "HISTORIA", "subtema": "Esparta: Los 300 y el Ejército Más Temido", "titulo": "Esparta: Los 300 y el Ejército Más Temido", "voice": "es-MX-JorgeNeural"},
    {"id": 12, "tipo": "HISTORIA", "subtema": "Mitología Japonesa: Samuráis, Yokais y Dioses", "titulo": "Mitología Japonesa: Samuráis y Criaturas Yokai", "voice": "es-MX-DaliaNeural"},
    {"id": 13, "tipo": "HISTORIA", "subtema": "Imperio Azteca: Guerreros Águila y Dioses de Sangre", "titulo": "Imperio Azteca: Guerreros Águila y Dioses del Sol", "voice": "es-MX-JorgeNeural"},
    {"id": 14, "tipo": "HISTORIA", "subtema": "Misterios Históricos sin Resolver del Mundo Antiguo", "titulo": "Misterios de la Historia que Nadie ha Podido Explicar", "voice": "es-MX-JorgeNeural"},
    {"id": 15, "tipo": "HISTORIA", "subtema": "Alejandro Magno: Conquistador de Imperios Imposibles", "titulo": "Alejandro Magno: El Conquistador del Mundo Antiguo", "voice": "es-MX-JorgeNeural"},
    {"id": 16, "tipo": "HISTORIA", "subtema": "Monstruos y Criaturas de la Mitología Universal", "titulo": "Monstruos Legendarios de la Mitología Universal", "voice": "es-MX-DaliaNeural"},
    {"id": 17, "tipo": "HISTORIA", "subtema": "Juana de Arco y la Guerra de los Cien Años", "titulo": "Juana de Arco: La Heroína Legendaria de Francia", "voice": "es-MX-DaliaNeural"},
    {"id": 18, "tipo": "HISTORIA", "subtema": "Imperio Otomano: Conquista de Constantinopla", "titulo": "Imperio Otomano: La Caída de Constantinopla", "voice": "es-MX-JorgeNeural"},
    {"id": 19, "tipo": "HISTORIA", "subtema": "Reyes Malditos y Dinastías Trágicas de la Historia", "titulo": "Reyes Malditos y Dinastías Trágicas de la Historia", "voice": "es-MX-JorgeNeural"},
    {"id": 20, "tipo": "HISTORIA", "subtema": "Piratas Legendarios: Barbanegra y las Reglas del Mar", "titulo": "Piratas Legendarios: La Verdadera Vida en Alta Mar", "voice": "es-MX-JorgeNeural"},
    {"id": 21, "tipo": "HISTORIA", "subtema": "Las Siete Maravillas del Mundo Antiguo", "titulo": "Las Siete Maravillas del Mundo Antiguo y su Destino", "voice": "es-MX-DaliaNeural"},
    {"id": 22, "tipo": "HISTORIA", "subtema": "Guerras Púnicas: Aníbal Barca y los Elefantes Alpinos", "titulo": "Aníbal Barca: El General que Puso a Temblar a Roma", "voice": "es-MX-JorgeNeural"},
    {"id": 23, "tipo": "HISTORIA", "subtema": "Filósofos Griegos: Sócrates, Platón y la Sabiduría", "titulo": "Filósofos de la Antigüedad: Sócrates y Platón", "voice": "es-MX-JorgeNeural"},
    {"id": 24, "tipo": "HISTORIA", "subtema": "Grandes Batallas que Cambiaron la Historia del Mundo", "titulo": "Batallas Épicas que Cambiaron el Rumbo del Mundo", "voice": "es-MX-JorgeNeural"},
    {"id": 25, "tipo": "HISTORIA", "subtema": "Mitología Celta: Druidas, Runas y Dioses del Bosque", "titulo": "Mitología Celta: Misterios de los Druidas y Dioses", "voice": "es-MX-DaliaNeural"},
    {"id": 26, "tipo": "HISTORIA", "subtema": "La Peste Negra: La Epidemia Más Letal de la Historia", "titulo": "La Peste Negra: La Epidemia Más Letal de la Historia", "voice": "es-MX-JorgeNeural"},
    {"id": 27, "tipo": "HISTORIA", "subtema": "El Coliseo Romano y los Espectáculos Sangrientos", "titulo": "El Coliseo Romano: Sangre, Fieras y Espectáculos", "voice": "es-MX-JorgeNeural"},
    {"id": 28, "tipo": "HISTORIA", "subtema": "Armas Legendarias de la Historia y la Mitología", "titulo": "Armas Legendarias de la Historia y la Mitología", "voice": "es-MX-JorgeNeural"},
    {"id": 29, "tipo": "HISTORIA", "subtema": "El Renacimiento: Genios, Arte e Inventos Asombrosos", "titulo": "El Renacimiento: Da Vinci y los Secretos del Arte", "voice": "es-MX-DaliaNeural"},
    {"id": 30, "tipo": "HISTORIA", "subtema": "Civilización Inca: Machu Picchu y el Imperio del Sol", "titulo": "Imperio Inca: El Secreto Sagrado de Machu Picchu", "voice": "es-MX-JorgeNeural"},

    # --- CIENCIA, ESPACIO Y NATURALEZA EXTREMA (31 al 60) ---
    {"id": 31, "tipo": "ASTRONOMIA", "subtema": "Agujeros Negros: La Fuerza Más Temible del Cosmos", "titulo": "Agujeros Negros: La Fuerza Más Temible del Cosmos", "voice": "es-MX-JorgeNeural"},
    {"id": 32, "tipo": "ASTRONOMIA", "subtema": "Planetas Extremos y Mortales del Universo", "titulo": "Planetas Extremos Donde la Vida es Imposible", "voice": "es-MX-JorgeNeural"},
    {"id": 33, "tipo": "ANIMALES", "subtema": "Animales Más Letales y Venenosos del Planeta", "titulo": "Los Animales Más Letales y Venenosos del Planeta", "voice": "es-MX-JorgeNeural"},
    {"id": 34, "tipo": "CIENCIA_NATURAL", "subtema": "Fosas Abisales: Criaturas de las Profundidades Oscuras", "titulo": "Fosas Abisales: Monstruos de las Profundidades", "voice": "es-MX-DaliaNeural"},
    {"id": 35, "tipo": "GEOLOGIA", "subtema": "Volcanes Extremos: Supererupciones que Cambiaron la Tierra", "titulo": "Supervolcanes: El Poder Destructivo de la Tierra", "voice": "es-MX-JorgeNeural"},
    {"id": 36, "tipo": "ASTRONOMIA", "subtema": "El Sistema Solar: Misterios Ocultos de Nuestros Vecinos", "titulo": "El Sistema Solar: Misterios Asombrosos del Cosmos", "voice": "es-MX-DaliaNeural"},
    {"id": 37, "tipo": "CIENCIA_NATURAL", "subtema": "La Mente Humana: Curiosidades Fascinantes del Cerebro", "titulo": "El Cerebro Humano: Secretos de la Mente que no Sabías", "voice": "es-MX-JorgeNeural"},
    {"id": 38, "tipo": "CIENCIA_NATURAL", "subtema": "Criaturas del Reino Fúngico: Hongos Más Extraños del Mundo", "titulo": "El Reino Fúngico: Hongos Zombie y Naturaleza Bizarra", "voice": "es-MX-JorgeNeural"},
    {"id": 39, "tipo": "ASTRONOMIA", "subtema": "Estrellas de Neutrones: Densidad Inconcebible del Espacio", "titulo": "Estrellas de Neutrones: La Materia Más Densa del Espacio", "voice": "es-MX-JorgeNeural"},
    {"id": 40, "tipo": "CIENCIA_NATURAL", "subtema": "Fenómenos Meteorológicos Más Extremos del Mundo", "titulo": "Clima Extremo: Los Fenómenos Más Salvajes de la Tierra", "voice": "es-MX-DaliaNeural"},
    {"id": 41, "tipo": "CIENCIA_NATURAL", "subtema": "Dinosaurios Gigantes: Monstruos del Jurásico Real", "titulo": "Dinosaurios Gigantes: Los Verdaderos Reyes del Jurásico", "voice": "es-MX-JorgeNeural"},
    {"id": 42, "tipo": "CIENCIA_NATURAL", "subtema": "Velocidad de la Luz y Paradojas de la Relatividad", "titulo": "Velocidad de la Luz y Paradojas de Einstein", "voice": "es-MX-JorgeNeural"},
    {"id": 43, "tipo": "ANIMALES", "subtema": "Animales con Superpoderes Biológicos Asombrosos", "titulo": "Animales con Superpoderes que Desafían la Ciencia", "voice": "es-MX-DaliaNeural"},
    {"id": 44, "tipo": "ASTRONOMIA", "subtema": "El Origen del Universo: Del Big Bang al Infinito", "titulo": "El Big Bang: Cómo Nació el Universo", "voice": "es-MX-JorgeNeural"},
    {"id": 45, "tipo": "ASTRONOMIA", "subtema": "La Luna y sus Misterios: Lo que no Sabías de Nuestro Satélite", "titulo": "La Luna: Secretos Ocultos de Nuestro Satélite", "voice": "es-MX-DaliaNeural"},
    {"id": 46, "tipo": "CIENCIA_NATURAL", "subtema": "Ecosistemas Extremos: Lugares donde la Vida no Debería Existir", "titulo": "Vida en Lugares Extremos: ¿Cómo Sobreviven?", "voice": "es-MX-JorgeNeural"},
    {"id": 47, "tipo": "CIENCIA_NATURAL", "subtema": "Física Cuántica: Fenómenos que Desafían la Realidad", "titulo": "Física Cuántica: ¿Qué es la Realidad Realmente?", "voice": "es-MX-JorgeNeural"},
    {"id": 48, "tipo": "CIENCIA_NATURAL", "subtema": "Parásitos Espeluznantes y Control Mental en la Naturaleza", "titulo": "Parásitos Zombie: Control Mental en la Naturaleza", "voice": "es-MX-JorgeNeural"},
    {"id": 49, "tipo": "ASTRONOMIA", "subtema": "Supernovas y Nebulosas: Fábricas Cósmicas de la Vida", "titulo": "Supernovas: Las Mayores Explosiones del Cosmos", "voice": "es-MX-DaliaNeural"},
    {"id": 50, "tipo": "GEOLOGIA", "subtema": "Gemas y Minerales Más Raros y Letales de la Tierra", "titulo": "Minerales Mortales y Gemas Asombrosas de la Tierra", "voice": "es-MX-JorgeNeural"},
    {"id": 51, "tipo": "ASTRONOMIA", "subtema": "Telescopios Espaciales: Los Ojos de la Humanidad en el Cosmos", "titulo": "Telescopio James Webb: Lo Más Lejano del Cosmos", "voice": "es-MX-JorgeNeural"},
    {"id": 52, "tipo": "CIENCIA_NATURAL", "subtema": "Tardígrados y Criaturas que Sobreviven en el Espacio", "titulo": "Tardígrados: Los Seres que Sobreviven al Espacio", "voice": "es-MX-DaliaNeural"},
    {"id": 53, "tipo": "CIENCIA_NATURAL", "subtema": "Fosa de las Marianas: Secretos del Abismo Oceánico", "titulo": "Fosa de las Marianas: Misterios del Fondo del Océano", "voice": "es-MX-JorgeNeural"},
    {"id": 54, "tipo": "CIENCIA_NATURAL", "subtema": "Misterios del ADN: El Código Secreto de la Vida", "titulo": "El Código Genético: Misterios Asombrosos del ADN", "voice": "es-MX-JorgeNeural"},
    {"id": 55, "tipo": "ASTRONOMIA", "subtema": "Asteroides Asesinos: Las Mayores Amenazas para la Tierra", "titulo": "Asteroides Asesinos: Amenazas Espaciales a la Tierra", "voice": "es-MX-JorgeNeural"},
    {"id": 56, "tipo": "CIENCIA_NATURAL", "subtema": "Leyes de la Física: Por qué el Universo Funciona Así", "titulo": "Leyes del Universo: Física que te Hará Pensar", "voice": "es-MX-JorgeNeural"},
    {"id": 57, "tipo": "ANIMALES", "subtema": "Insectos Monstruosos: Depredadores del Micromundo", "titulo": "Insectos Gigantes y Depredadores del Micromundo", "voice": "es-MX-JorgeNeural"},
    {"id": 58, "tipo": "ASTRONOMIA", "subtema": "Planeta Marte: ¿Hubo Vida en el Planeta Rojo?", "titulo": "Planeta Marte: Misterios y la Búsqueda de Vida", "voice": "es-MX-DaliaNeural"},
    {"id": 59, "tipo": "CIENCIA_NATURAL", "subtema": "Plantas Carnívoras y Defensas Vegetales Asombrosas", "titulo": "Plantas Carnívoras y Defensas Vegetales Letales", "voice": "es-MX-JorgeNeural"},
    {"id": 60, "tipo": "CIENCIA_NATURAL", "subtema": "La Era de Hielo: Mamuts y la Supervivencia Extrema", "titulo": "La Era de Hielo: Secretos de los Grandes Mamuts", "voice": "es-MX-JorgeNeural"}
]

def load_state():
    if os.path.exists(STATE_FILE):
        try:
            with open(STATE_FILE, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception:
            pass
    return {"completed": [], "failed": []}

def save_state(state):
    with open(STATE_FILE, "w", encoding="utf-8") as f:
        json.dump(state, f, ensure_ascii=False, indent=2)

def api_post(endpoint, data, timeout=120):
    url = f"{BASE_URL}{endpoint}"
    req = urllib.request.Request(
        url,
        data=json.dumps(data).encode("utf-8"),
        headers=HEADERS,
        method="POST"
    )
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return json.loads(resp.read().decode("utf-8"))

def api_get(endpoint, timeout=60):
    url = f"{BASE_URL}{endpoint}"
    req = urllib.request.Request(url, headers=HEADERS, method="GET")
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return json.loads(resp.read().decode("utf-8"))

def process_single_video(cfg):
    vid_id = cfg["id"]
    tipo = cfg["tipo"]
    subtema = cfg["subtema"]
    titulo = cfg["titulo"]
    voice = cfg.get("voice", "es-MX-JorgeNeural")

    print(f"\n[{vid_id}/60] >>> Generando trivias para: '{subtema}' (Cat: {tipo})...")
    
    # 1. Generar 5 trivias con IA
    gen_payload = {
        "tipoTrivia": tipo,
        "subtema": subtema,
        "cantidad": 5,
        "numeroOpciones": 4,
        "dificultad": "MEDIA",
        "idioma": "es-MX"
    }

    gen_res = api_post("/api/v1/trivias", gen_payload, timeout=180)
    created_count = gen_res.get("cantidadFinal", 0)
    print(f"      [+] Trivias creadas: {created_count}")

    # 2. Obtener las 5 trivias activas recién creadas
    trivias_page = api_get("/api/v1/trivias?page=0&size=5&sort=createdAt,desc")
    items = trivias_page.get("content", [])
    trivia_ids = [t["id"] for t in items[:5]]
    
    if len(trivia_ids) < 5:
        raise RuntimeError(f"Se esperaban 5 trivias pero se obtuvieron {len(trivia_ids)}")

    print(f"      [+] 5 IDs obtenidos. Compilando video MP4 con Edge-TTS ({voice})...")

    # 3. Compilar video con Edge-TTS, BGM y título personalizado
    video_payload = {
        "triviaIds": trivia_ids,
        "format": "VERTICAL_9_16",
        "withBgm": True,
        "withTts": True,
        "ttsVoice": voice,
        "introMode": "TEMPLATE",
        "introTemplate": "tpl_que_tanto_sabes",
        "customTitle": titulo
    }

    vid_res = api_post("/api/v1/videos/generate", video_payload, timeout=240)
    
    final_title = vid_res.get("title")
    final_filename = vid_res.get("filename")
    duration = vid_res.get("durationSeconds", 0)
    
    print(f"      [OK] VIDEO GENERADO CON EXITO:")
    print(f"          Archivo  : {final_filename}")
    print(f"          Titulo   : {final_title}")
    print(f"          Duración : {round(duration, 1)}s")
    
    return {
        "id": vid_id,
        "titulo": final_title,
        "filename": final_filename,
        "duration": duration,
        "videoId": vid_res.get("id"),
        "timestamp": time.strftime("%Y-%m-%d %H:%M:%S")
    }

def main():
    print("=" * 65)
    print("  GENERADOR MASIVO DE 60 VIDEOS TRIVIA (5 PREGUNTAS C/U)")
    print("  30 x Historia y Mitologia [Historia]")
    print("  30 x Ciencia, Espacio y Naturaleza Extrema [Ciencia]")
    print("=" * 65)

    state = load_state()
    completed_ids = {item["id"] for item in state["completed"]}
    print(f"Videos ya completados previamente: {len(completed_ids)} de 60\n")

    start_time = time.time()
    
    for cfg in VIDEOS_CONFIG:
        vid_id = cfg["id"]
        if vid_id in completed_ids:
            print(f"[-] Video #{vid_id} ya completado ({cfg['titulo']}). Saltando...")
            continue

        attempts = 0
        success = False
        while attempts < 3 and not success:
            attempts += 1
            try:
                result = process_single_video(cfg)
                state["completed"].append(result)
                save_state(state)
                completed_ids.add(vid_id)
                success = True
                # Pequeña pausa de 2 segundos para dar respiro al renderizador
                time.sleep(2)
            except Exception as e:
                print(f"      [!] Intento {attempts}/3 falló para video #{vid_id}: {e}")
                time.sleep(5)

        if not success:
            print(f"      [ERROR CRÍTICO] Video #{vid_id} no pudo ser generado.")
            state["failed"].append({"id": vid_id, "subtema": cfg["subtema"], "error": str(e)})
            save_state(state)

    elapsed = time.time() - start_time
    print("\n" + "=" * 65)
    print(f"  PROCESO COMPLETADO EN {round(elapsed / 60, 1)} MINUTOS")
    print(f"  Total videos generados: {len(state['completed'])} / 60")
    if state["failed"]:
        print(f"  Videos con error: {len(state['failed'])}")
    print("=" * 65)

if __name__ == "__main__":
    main()
