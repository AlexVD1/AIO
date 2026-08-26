#!/usr/bin/env python3
import os
import sys
import json
import time
import argparse
from pathlib import Path
from dotenv import load_dotenv
import fitz  # PyMuPDF
import google.generativeai as genai
from google.generativeai.types import HarmCategory, HarmBlockThreshold
from tqdm import tqdm

# Cargar variables de entorno
load_dotenv()

# Configuración del Prompt del Sistema para Gemini
SYSTEM_INSTRUCTION = """
Eres un traductor experto en tecnología, informática y diseño de sistemas (System Design), con años de experiencia traduciendo de inglés a español de manera profesional y natural.

Tu misión es traducir el fragmento de texto técnico provisto respetando las siguientes directrices:
1. **Precisión Técnica**: Conserva los términos en inglés que son el estándar absoluto en la industria hispanohablante de desarrollo de software y diseño de sistemas (por ejemplo: 'load balancer', 'throughput', 'latency', 'sharding', 'failover', 'middleware', 'broker de mensajería', 'rate limiting', 'caching', 'deadlock', 'thread pool', 'pipeline', 'deployment', etc.). Si es oportuno, la primera vez que aparezca un término complejo, puedes traducirlo e incluir el término en inglés entre paréntesis; en las apariciones subsecuentes usa el término en inglés o el traducido según sea más natural en el día a día profesional.
2. **Naturalidad y Fluidez**: Evita traducciones literales tipo "palabra por palabra". Adapta las frases para que tengan sentido técnico y gramatical natural en español técnico/profesional.
3. **Preservación del Formato**: Mantén intactas las marcas de formato en Markdown (como negritas, cursivas, listas con viñetas, enlaces, tablas, bloques de código, etc.). No alteres el código fuente dentro de los bloques de código, solo traduce los comentarios del código si es necesario.
4. **Formato de Salida Limpio**: Devuelve ÚNICAMENTE la traducción del fragmento solicitado. No agregues introducciones ("Aquí está la traducción...", "Traducido por..."), notas al pie personales, ni explicaciones adicionales sobre tus decisiones de traducción.
"""

def setup_gemini(api_key, model_name):
    """Configura e inicializa la API de Gemini."""
    if not api_key:
        print("[❌ Error] Clave de API de Gemini (GEMINI_API_KEY) no encontrada.")
        print("Por favor, crea un archivo .env basado en .env.example y añade tu clave de API.")
        print("Puedes obtener una clave gratuita en: https://aistudio.google.com/")
        sys.exit(1)
        
    genai.configure(api_key=api_key)
    
    # Configuración de seguridad permisiva para terminología técnica (evitar falsos positivos en "kill", "slave", etc.)
    safety_settings = {
        HarmCategory.HARM_CATEGORY_HARASSMENT: HarmBlockThreshold.BLOCK_NONE,
        HarmCategory.HARM_CATEGORY_HATE_SPEECH: HarmBlockThreshold.BLOCK_NONE,
        HarmCategory.HARM_CATEGORY_SEXUALLY_EXPLICIT: HarmBlockThreshold.BLOCK_NONE,
        HarmCategory.HARM_CATEGORY_DANGEROUS_CONTENT: HarmBlockThreshold.BLOCK_NONE,
    }
    
    try:
        model = genai.GenerativeModel(
            model_name=model_name,
            system_instruction=SYSTEM_INSTRUCTION,
            safety_settings=safety_settings
        )
        return model
    except Exception as e:
        print(f"[❌ Error] Error al inicializar el modelo de Gemini '{model_name}': {e}")
        sys.exit(1)

def extract_pdf_pages(pdf_path):
    """Extrae el texto de cada página del archivo PDF utilizando PyMuPDF."""
    print(f"📖 Cargando el PDF: {pdf_path}...")
    try:
        doc = fitz.open(pdf_path)
        pages_text = []
        for page_num in range(len(doc)):
            page = doc[page_num]
            text = page.get_text("text")
            pages_text.append(text.strip())
        print(f"✅ Se han extraído {len(pages_text)} páginas del PDF.")
        return pages_text
    except Exception as e:
        print(f"[❌ Error] No se pudo abrir o procesar el archivo PDF: {e}")
        sys.exit(1)

def load_progress(progress_file_path):
    """Carga el estado de progreso previo si existe."""
    if progress_file_path.exists():
        try:
            with open(progress_file_path, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"[⚠️ Advertencia] No se pudo leer el archivo de progreso {progress_file_path}: {e}")
            print("Se iniciará una nueva traducción desde el principio.")
    return None

def save_progress(progress_file_path, progress_data):
    """Guarda el estado de progreso en un archivo JSON."""
    try:
        with open(progress_file_path, "w", encoding="utf-8") as f:
            json.dump(progress_data, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"[⚠️ Error] No se pudo guardar el archivo de progreso {progress_file_path}: {e}")

def reconstruct_markdown(output_md_path, progress_data, pdf_name):
    """Reconstruye el archivo Markdown final en base a la información de progreso guardada."""
    try:
        completed_pages = progress_data.get("completed_pages", {})
        sorted_pages = sorted([int(k) for k in completed_pages.keys()])
        
        with open(output_md_path, "w", encoding="utf-8") as f:
            f.write(f"# Traducción de: {pdf_name}\n\n")
            f.write(f"*Traducción técnica generada automáticamente con la API de Gemini ({progress_data.get('model_used', 'gemini-3.1-flash-lite')})*\n")
            f.write(f"*Fecha de creación/reanudación: {time.strftime('%Y-%m-%d %H:%M:%S')}*\n\n")
            f.write("---\n\n")
            
            for page_num in sorted_pages:
                translation = completed_pages[str(page_num)]
                f.write(f"## Página {page_num + 1}\n\n")
                f.write(translation)
                f.write("\n\n---\n\n")
    except Exception as e:
        print(f"[⚠️ Error] No se pudo reconstruir el archivo Markdown de salida: {e}")

def clean_response_text(response):
    """Extrae el texto traducido de forma segura, manejando posibles bloqueos de seguridad."""
    try:
        return response.text
    except Exception as e:
        # En caso de que response.text falle debido a filtros de seguridad o respuestas vacías
        try:
            if hasattr(response, 'candidates') and response.candidates:
                candidate = response.candidates[0]
                if hasattr(candidate, 'finish_reason') and str(candidate.finish_reason) == 'SAFETY':
                    return "\n* [Traducción bloqueada por políticas de seguridad de la API de Gemini] *\n"
                
                parts = candidate.content.parts
                if parts:
                    return "".join(part.text for part in parts if hasattr(part, 'text'))
        except Exception:
            pass
        return f"\n* [Error al recuperar la traducción de la página: {str(e)}] *\n"

def translate_page_with_retry(model, prompt, delay_seconds, max_retries=5):
    """Intenta traducir una página con manejo de errores y backoff exponencial en caso de rate limits."""
    retries = 0
    current_delay = delay_seconds
    
    while retries < max_retries:
        try:
            # Retardo proactivo inicial para evitar golpear límites de llamadas gratuitas (ej. 15 RPM)
            time.sleep(current_delay)
            
            response = model.generate_content(prompt)
            return clean_response_text(response)
            
        except Exception as e:
            # Capturar errores de API de Google (rate limit 429, error 500, etc.)
            err_str = str(e).lower()
            is_rate_limit = "429" in err_str or "resource_exhausted" in err_str or "quota" in err_str
            
            if is_rate_limit:
                wait_time = (2 ** retries) * 15  # Backoff: 15s, 30s, 60s, 120s, 240s
                print(f"\n[⚠️ Rate Limit (429)] Límite de cuota alcanzado. Esperando {wait_time}s antes de reintentar...")
                time.sleep(wait_time)
                retries += 1
            elif "503" in err_str or "unavailable" in err_str:
                wait_time = (2 ** retries) * 5
                print(f"\n[⚠️ Error de Servidor (503)] Servicio temporalmente no disponible. Esperando {wait_time}s...")
                time.sleep(wait_time)
                retries += 1
            else:
                # Otros errores de conexión de red o HTTP
                wait_time = (2 ** retries) * 5
                print(f"\n[⚠️ Error de Conexión] {e}. Esperando {wait_time}s antes de reintentar...")
                time.sleep(wait_time)
                retries += 1
                
    raise RuntimeError(f"Fallo crítico: No se pudo traducir la página tras {max_retries} intentos debido a errores de red o límites de la API.")

def main():
    parser = argparse.ArgumentParser(description="Traductor de libros técnicos PDF al español utilizando la API de Gemini.")
    parser.add_argument("--pdf", required=True, type=str, help="Ruta al archivo PDF a traducir.")
    parser.add_argument("-o", "--output", type=str, help="Ruta de salida para el archivo Markdown (.md).")
    parser.add_argument("--model", type=str, help="Modelo de Gemini a utilizar (ej. gemini-1.5-flash, gemini-1.5-pro).")
    parser.add_argument("--delay", type=float, help="Retardo en segundos entre páginas para evitar límites de API (ej. 4.0).")
    parser.add_argument("--reset", action="store_true", help="Ignorar progreso guardado y comenzar traducción desde cero.")
    args = parser.parse_args()

    # Validar que el archivo PDF existe
    pdf_path = Path(args.pdf)
    if not pdf_path.exists():
        print(f"[❌ Error] El archivo PDF no existe en la ruta provista: {pdf_path}")
        sys.exit(1)

    # Determinar rutas por defecto
    pdf_name = pdf_path.stem
    output_md_path = Path(args.output) if args.output else pdf_path.with_name(f"{pdf_name}_traducido.md")
    progress_file_path = pdf_path.with_name(f"{pdf_name}_progress.json")

    # Configuración de variables de entorno con fallback
    api_key = os.getenv("GEMINI_API_KEY")
    model_name = args.model or os.getenv("GEMINI_MODEL", "gemini-3.1-flash-lite")
    
    # Delay: si el usuario no especifica, lee de .env, si no, usa por defecto 4.0 (ideal para tier gratuito)
    env_delay = os.getenv("DELAY_SECONDS")
    delay_seconds = args.delay if args.delay is not None else (float(env_delay) if env_delay else 4.0)

    # Inicializar Gemini
    model = setup_gemini(api_key, model_name)

    # Extraer texto de todas las páginas del PDF
    pages_text = extract_pdf_pages(pdf_path)
    total_pages = len(pages_text)

    # Cargar progreso o inicializar
    progress_data = None
    if not args.reset:
        progress_data = load_progress(progress_file_path)

    if progress_data:
        # Verificar que el progreso cargado coincida con el PDF actual
        if progress_data.get("pdf_name") != pdf_name or progress_data.get("total_pages") != total_pages:
            print("[⚠️ Advertencia] El archivo de progreso no parece coincidir con el PDF proporcionado.")
            confirm = input("¿Deseas reiniciar la traducción? (s/n): ").strip().lower()
            if confirm == 's':
                progress_data = {
                    "pdf_name": pdf_name,
                    "total_pages": total_pages,
                    "model_used": model_name,
                    "completed_pages": {}
                }
                print("Iniciando traducción desde cero...")
            else:
                print("Continuando con el progreso existente bajo tu propio riesgo.")
        else:
            print(f"🔄 Progreso encontrado. Se han traducido {len(progress_data['completed_pages'])} de {total_pages} páginas.")
            # Reconstruir Markdown inicial con lo que ya está traducido
            reconstruct_markdown(output_md_path, progress_data, pdf_name)
    else:
        progress_data = {
            "pdf_name": pdf_name,
            "total_pages": total_pages,
            "model_used": model_name,
            "completed_pages": {}
        }
        # Crear archivo Markdown nuevo/limpio
        with open(output_md_path, "w", encoding="utf-8") as f:
            f.write(f"# Traducción de: {pdf_name}\n\n")
            f.write(f"*Traducción técnica generada automáticamente con la API de Gemini ({model_name})*\n")
            f.write(f"*Fecha de inicio: {time.strftime('%Y-%m-%d %H:%M:%S')}*\n\n")
            f.write("---\n\n")

    completed_pages = progress_data["completed_pages"]

    # Iniciar ciclo de traducción
    print("\n🚀 Iniciando proceso de traducción...")
    print(f"Modo: {model_name} | Retardo base: {delay_seconds}s | Archivo salida: {output_md_path}")
    print("Presiona Ctrl+C en cualquier momento para detener de manera segura y guardar el progreso.\n")

    # Barra de progreso visual
    pbar = tqdm(total=total_pages, desc="Progreso", unit="pág")
    
    # Sincronizar barra de progreso con el número de páginas ya traducidas
    pbar.update(len(completed_pages))

    try:
        for i in range(total_pages):
            page_str = str(i)
            
            # Si la página ya está traducida, saltarla
            if page_str in completed_pages:
                continue

            current_text = pages_text[i]
            
            # Manejar páginas vacías o con muy poco contenido (ej. logos, firmas)
            if not current_text or len(current_text.strip()) < 15:
                tqdm.write(f"📝 Página {i+1}/{total_pages}: Vacía o sin texto relevante. Saltando API...")
                translation_result = "* [Página vacía o sin texto legible extraído] *"
            else:
                tqdm.write(f"⏳ Traduciendo Página {i+1}/{total_pages} (aprox. {len(current_text)} caracteres)...")
                
                # Contexto de coherencia técnico (enviar últimas 3000 caracteres de la página anterior si existe)
                previous_context = ""
                if i > 0 and str(i - 1) in completed_pages:
                    prev_translation = completed_pages[str(i - 1)]
                    previous_context = prev_translation[-3000:] if len(prev_translation) > 3000 else prev_translation

                # Construir Prompt con contexto
                if previous_context:
                    prompt = f"""
Estás traduciendo un libro técnico de diseño de sistemas de inglés a español.
Mantén coherencia en el estilo de redacción, la gramática y el vocabulario con el contexto de la página anterior.

---
[CONTEXTO DE LA PÁGINA ANTERIOR (Solo para referencia de estilo, coherencia y oraciones continuas. ¡NO LA TRADUZCAS DE NUEVO!)]
{previous_context}
---

[TEXTO DE LA PÁGINA ACTUAL A TRADUCIR (Página {i + 1})]
{current_text}
"""
                else:
                    prompt = f"""
Estás traduciendo un libro técnico de diseño de sistemas de inglés a español.

[TEXTO DE LA PÁGINA ACTUAL A TRADUCIR (Página {i + 1})]
{current_text}
"""

                # Llamar a la API con reintentos
                translation_result = translate_page_with_retry(model, prompt, delay_seconds)

            # Actualizar diccionario de progreso y guardar
            completed_pages[page_str] = translation_result
            save_progress(progress_file_path, progress_data)

            # Escribir en el archivo Markdown (Añadir progresivamente)
            with open(output_md_path, "a", encoding="utf-8") as f:
                f.write(f"## Página {i + 1}\n\n")
                f.write(translation_result)
                f.write("\n\n---\n\n")

            pbar.update(1)

        pbar.close()
        total_original_chars = sum(len(txt) for txt in pages_text)
        total_translated_chars = sum(len(txt) for txt in completed_pages.values())
        
        print(f"\n🎉 ¡Traducción finalizada con éxito!")
        print(f"📄 Documento traducido guardado en: {output_md_path}")
        print(f"📊 Estadísticas finales:")
        print(f"   - Páginas procesadas: {total_pages}")
        print(f"   - Caracteres originales (PDF): {total_original_chars:,}")
        print(f"   - Caracteres traducidos (Markdown): {total_translated_chars:,}")
        
        # Eliminar archivo de progreso temporal una vez completada la traducción con éxito
        if progress_file_path.exists():
            try:
                progress_file_path.unlink()
                print("🧹 Archivo temporal de progreso eliminado.")
            except Exception:
                pass

    except KeyboardInterrupt:
        pbar.close()
        print("\n\n👋 [Proceso Interrumpido] Se detectó una interrupción por teclado (Ctrl+C).")
        print(f"💾 El progreso se ha guardado de forma segura.")
        print(f"👉 Páginas traducidas hasta ahora: {len(completed_pages)} / {total_pages}")
        print(f"🔍 Puedes reanudar la traducción en cualquier momento volviendo a ejecutar el script.")
        sys.exit(0)
    except Exception as e:
        pbar.close()
        print(f"\n\n❌ [Fallo Inesperado] Ocurrió un error inesperado durante el procesamiento: {e}")
        print(f"💾 El progreso previo se conserva en '{progress_file_path}'.")
        sys.exit(1)

if __name__ == "__main__":
    main()
