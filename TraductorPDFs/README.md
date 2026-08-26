# Traductor de PDFs Técnicos con Gemini API 📚🤖

Este es un script robusto en Python diseñado para traducir libros técnicos de gran extensión (como diseño de sistemas, arquitectura de software, bases de datos, etc.) del inglés al español, preservando términos técnicos de la industria, manteniendo el formato en Markdown y gestionando de forma inteligente las llamadas a la API de Gemini para evitar el límite de peticiones de la capa gratuita.

---

## ✨ Características Principales

1. **Lectura Eficiente y Rápida**: Utiliza la biblioteca moderna `PyMuPDF` (`fitz`) para extraer el texto página por página.
2. **Resiliencia ante Interrupciones (Autoguardado y Continuidad)**: 
   - Guarda el progreso en tiempo real en un archivo JSON temporal (`<nombre>_progress.json`).
   - Si el proceso se detiene o se cancela voluntariamente (`Ctrl+C`), se reanuda automáticamente en la última página pendiente al volver a iniciarlo.
   - Cuenta con una función de **auto-reconstrucción** que regenera el archivo Markdown final a partir de los datos almacenados en el JSON.
3. **Control de Flujo de API (Rate Limiting)**:
   - Configura un retardo base proactivo configurable para no superar las 15 solicitudes por minuto (RPM) de la cuota de uso libre de Gemini.
   - Implementa **Backoff Exponencial** automático que reintenta la petición si la API de Google retorna un error de límite excedido (`429 Too Many Requests`) o errores del servidor (`503 Service Unavailable`).
4. **Traducción Coherente y Técnica**:
   - Envía el contexto de la página anterior como entrada para garantizar la consistencia en el vocabulario técnico, la gramática y el tono de traducción.
   - Configura un Prompt del Sistema especializado que previene la traducción errónea de términos estándares de diseño de sistemas (por ejemplo, *throughput*, *latency*, *caching*, *sharding*, *failover*, etc.).
   - Utiliza configuraciones de seguridad para desactivar falsos positivos de bloqueo causados por términos de sistemas como *"kill child process"* o *"master-slave architecture"*.
5. **Barra de Progreso y Logs**: Información visual clara en la terminal utilizando `tqdm` y logs informativos sobre caracteres procesados y estado de conexión.

---

## 🛠️ Instalación y Requisitos

### 1. Clonar o acceder a la carpeta del proyecto
Asegúrate de estar en el directorio `TraductorPDFs`:
```bash
cd TraductorPDFs
```

### 2. Crear y activar un Entorno Virtual (Recomendado)
Para mantener tus dependencias aisladas de tu sistema global:

**En Windows (PowerShell):**
```powershell
python -m venv venv
.\venv\Scripts\Activate.ps1
```

**En Linux / macOS:**
```bash
python3 -m venv venv
source venv/bin/activate
```

### 3. Instalar Dependencias
Instala los paquetes necesarios definidos en `requirements.txt`:
```bash
pip install -r requirements.txt
```

---

## ⚙️ Configuración

1. Duplica el archivo `.env.example` y renómbralo a `.env`:
   ```bash
   cp .env.example .env
   ```
2. Abre el archivo `.env` en tu editor de texto favorito y agrega tu clave de API de Gemini:
   ```env
   GEMINI_API_KEY=AIzaSy...TuAPIKeyAqui
   GEMINI_MODEL=gemini-3.5-flash
   DELAY_SECONDS=4
   ```
   *Nota: Puedes obtener una clave de API gratuita en [Google AI Studio](https://aistudio.google.com/).*

---

## 🚀 Uso del Traductor

### Ejecución básica
Para iniciar la traducción de un PDF utilizando la configuración predeterminada de tu archivo `.env`:
```bash
python translator.py --pdf "ruta/de/tu/libro.pdf"
```

El script creará de forma progresiva un archivo Markdown en la misma ruta con el nombre: `libro_traducido.md`.

### Opciones de ejecución personalizadas

El script acepta varios argumentos para tener mayor control desde la terminal:

```bash
# Cambiar el modelo de traducción en tiempo de ejecución (ej. a gemini-2.5-flash)
python translator.py --pdf "diseño_sistemas.pdf" --model gemini-2.5-flash

# Definir una ruta o nombre personalizado para el archivo Markdown de salida
python translator.py --pdf "diseño_sistemas.pdf" -o "traducciones/mi_libro.md"

# Modificar el retardo base entre llamadas a la API (ej. a 5 segundos)
python translator.py --pdf "diseño_sistemas.pdf" --delay 5.0

# Forzar al script a descartar el progreso previo y volver a empezar desde la página 1
python translator.py --pdf "diseño_sistemas.pdf" --reset
```

### 🔄 Cómo reanudar una traducción
Si tu traducción fue interrumpida a la mitad (por ejemplo, en la página 125):
1. **No borres** el archivo `<nombre_pdf>_progress.json` ni el `.md`.
2. Simplemente vuelve a ejecutar el mismo comando básico:
   ```bash
   python translator.py --pdf "diseño_sistemas.pdf"
   ```
3. El script leerá el archivo JSON, reconstruirá el archivo Markdown actual y continuará traduciendo desde la página 126 sin perder nada del progreso anterior.

---

## 🛑 Respuestas a Errores Comunes

- **Error: GEMINI_API_KEY no encontrada**: Asegúrate de haber renombrado correctamente `.env.example` a `.env` y de colocar una API key válida.
- **Error: Rate Limit (429) o Quota Exceeded**: Esto ocurre con el tier gratuito si haces demasiadas peticiones al mismo tiempo. El script lo manejará de forma autónoma haciendo pausas exponenciales. Si el problema persiste de manera constante, incrementa el valor de `--delay` a `5.0` o `6.0`.
- **Texto con marcas de seguridad o bloqueado**: La API de Gemini a veces bloquea contenido por políticas de seguridad estrictas ante vocabulario malinterpretado. Hemos configurado los umbrales de seguridad en `BLOCK_NONE` para minimizar este problema en libros técnicos, pero si ocurre, el script insertará una advertencia en la página correspondiente y continuará con las siguientes de forma segura.
