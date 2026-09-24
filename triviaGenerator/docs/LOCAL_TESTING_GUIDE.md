# Guía para Probar Todo en Local (Windows 11 + Podman)

Esta guía explica paso a paso cómo levantar y probar **toda la plataforma en tu propia máquina local**, desde la generación con IA hasta la descarga de las imágenes PNG y la consulta interactiva con Swagger UI.

---

## 1. Paso Previo: Iniciar la Máquina de Podman

Como estás en Windows, Podman ejecuta los contenedores dentro de una máquina virtual ligera WSL2. Si la máquina está apagada, iníciala con:

```powershell
podman machine start
```

*(Para verificar que está corriendo, ejecuta `podman ps`).*

---

## 2. Configurar Variables de Entorno (.env)

Copia la plantilla `.env.example` a `.env`:

```powershell
Copy-Item .env.example .env
```

Abre `.env` con tu editor (Bloc de notas o VS Code) y ajusta lo siguiente:

```ini
# Base de datos local en Podman
DATABASE_URL=jdbc:postgresql://postgres:5432/triviadb
DATABASE_USERNAME=trivia_user
DATABASE_PASSWORD=trivia_pass

# Tu clave de Google Gemini (imprescindible para generar preguntas reales con IA)
# Si no tienes una, consíguela gratis en: https://aistudio.google.com/app/apikey
AI_API_KEY=tu_api_key_aqui
AI_MODEL=gemini-3.5-flash-lite

# Seguridad para pruebas locales
# Si dejas API_KEY vacía, no necesitarás enviar cabecera X-API-KEY en local
API_KEY=
```

---

## 3. Levantar Todo con Podman Compose

Ejecuta en la raíz del proyecto:

```powershell
podman compose up -d
```

Este comando levantará dos contenedores:
1. `trivia-postgres`: Base de datos PostgreSQL 16 con el catálogo y las tablas de trivias.
2. `trivia-api`: La API REST Spring Boot empaquetada con Java 21 y soporte de renderizado gráfico AWT.

Para ver los logs en tiempo real y comprobar que arrancó correctamente:

```powershell
podman compose logs -f api
```

*(Cuando veas `Started TriviaApiApplication in ... seconds`, la API está lista en el puerto 8080).*

---

## 4. Métodos para Probar la Plataforma

### Opción A: Probar con Interfaz Gráfica (Swagger UI) — *Recomendado*

Abre tu navegador web en:
👉 **[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)**

Desde allí podrás probar cada endpoint con un clic usando el botón **"Try it out"**:
* `POST /api/v1/trivias`: Generar preguntas con IA.
* `POST /api/v1/trivias/async`: Generación en segundo plano (HTTP 202).
* `GET /api/v1/trivias/generations/{id}/status`: Polling de estado en vivo.
* `GET /api/v1/trivias/random`: Obtener una pregunta al azar.
* `GET /api/v1/trivias/{id}`: Consultar una trivia específica con sus opciones y URLs de imágenes.
* `GET /api/v1/trivias/stats`: Ver métricas globales del sistema.
* `GET /api/v1/catalogos/tipos-trivia`: Listar las categorías disponibles.

---

### Opción B: Probar desde PowerShell con comandos `curl`

#### 1. Probar que el servidor está vivo:
```powershell
curl http://localhost:8080/api/v1/health
```

#### 2. Consultar las categorías del catálogo:
```powershell
curl http://localhost:8080/api/v1/catalogos/tipos-trivia
```

#### 3. Generar trivias nuevas mediante IA (Síncrono — espera el resultado):
```powershell
curl -X POST http://localhost:8080/api/v1/trivias `
  -H "Content-Type: application/json" `
  -d '{
    "tipoTrivia": "ASTRONOMIA",
    "subtema": "Planetas del sistema solar",
    "cantidad": 2,
    "numeroOpciones": 4,
    "dificultad": "FACIL",
    "idioma": "es-MX"
  }'
```

#### 4. Generar trivias en segundo plano (Asíncrono — retorna HTTP 202 inmediatamente):
```powershell
curl -X POST http://localhost:8080/api/v1/trivias/async `
  -H "Content-Type: application/json" `
  -d '{
    "tipoTrivia": "HISTORIA",
    "subtema": "Segunda Guerra Mundial",
    "cantidad": 3,
    "numeroOpciones": 4,
    "dificultad": "MEDIA",
    "idioma": "es-MX"
  }'
```
*Copia el `id` que devuelve este comando para consultar su progreso.*

#### 5. Consultar el estado en vivo de una generación asíncrona:
```powershell
curl http://localhost:8080/api/v1/trivias/generations/<ID_OBTENIDO>/status
```

#### 6. Obtener una trivia aleatoria guardada en base de datos:
```powershell
curl http://localhost:8080/api/v1/trivias/random
```

#### 7. Ver y descargar las imágenes PNG generadas:
En la respuesta de la trivia verás un bloque `assets` con URLs como:
* `http://localhost:8080/assets/<generationId>/<triviaId>-pregunta.png`
* `http://localhost:8080/assets/<generationId>/<triviaId>-respuesta.png`

¡Ábrelas directamente en tu navegador para ver las tarjetas gráficas de 1080x1080 renderizadas!

#### 8. Ver estadísticas globales:
```powershell
curl http://localhost:8080/api/v1/trivias/stats
```

---

### Opción C: Interfaz Web Local (Studio & Quiz Interactivo) 🌟 (Recomendada)

La plataforma incluye una aplicación web interactiva completa (SPA) diseñada para probar todas las capacidades de la API de forma visual y amigable:

#### ¿Cómo acceder a la interfaz web?

1. Con los contenedores o el backend arriba en el puerto 8080, abre tu navegador web en:
   👉 **`http://localhost:8080/`** (o `http://localhost:8080/index.html`)

   *(Alternativa sin servidor web: También puedes hacer doble clic directamente sobre el archivo [`test-client.html`](file:///c:/Users/villa/GIT%20DESKTOP/AIO/triviaGenerator/test-client.html) en la carpeta raíz del proyecto y se abrirá en tu navegador favorito gracias a la configuración CORS).*

#### Funcionalidades incluidas en la Web:

1. **⚡ Generador con IA (`Gemini 3.5 Flash-Lite`)**:
   * Selección de categoría desde el catálogo dinámico.
   * Sugerencias de subtemas rápidos (Agujeros Negros, Revolución Francesa, F1, etc.).
   * Selector visual de dificultad (Fácil, Media, Difícil).
   * Generación en modo **Síncrono** o **Asíncrono** con barra de progreso y sondeo de estado en vivo.
   * Visualización inmediata de las preguntas creadas, opciones y explicaciones.
   * **Visor de imágenes PNG 1080x1080**: previsualiza `question.png` y `answer.png` y ábrelas en un modal a resolución completa.

2. **🎮 Modo Quiz (Juego Interactivo)**:
   * Juega respondiendo trivias reales generadas y guardadas en la base de datos.
   * Marcador de aciertos, fallos y racha actual (🔥).
   * Al responder, revela la explicación didáctica y la tarjeta gráfica `answer.png`.
   * Botón para cargar automáticamente preguntas aleatorias continuas.

3. **🔍 Explorador y Búsqueda**:
   * Busca trivias por coincidencia de texto en la pregunta o explicación.
   * Revisa todas las trivias guardadas en PostgreSQL con sus hashes SHA-256.

4. **📊 Métricas y Catálogo**:
   * Consulta en tiempo real las estadísticas: trivias activas, generaciones, promedio de intentos y tasa de descarte de duplicados.
   * Tabla completa del catálogo de las 9 categorías oficiales.

---

## 5. Detener los Contenedores al Finalizar

Cuando termines tus pruebas:

```powershell
podman compose down
```

*(Si deseas apagar también la máquina virtual de Podman: `podman machine stop`).*
