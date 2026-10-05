# Manual de Operación — Kids Animation Studio

Guía operativa exhaustiva para la configuración, arranque paso a paso, diagnóstico y producción automatizada de animación educativa preescolar 3D Cartoon (audiencia objetivo: 2 a 6 años).

---

## 1. Arquitectura del Sistema y Puertos

Kids Animation Studio es una plataforma modular local diseñada para hardware NVIDIA (RTX 5070 Blackwell, 12 GB VRAM):

| Componente | Directorio | Puerto / URL | Función Principal |
| :--- | :--- | :--- | :--- |
| **Dashboard Web & Orquestador** | `orchestrator/` | `http://127.0.0.1:8082` | Gestión de series, personajes, máquina de estados, lotes y streaming. |
| **AI Gateway** | `ai-gateway/` | `http://127.0.0.1:8090` | TTS Kokoro-82M, alineación Whisper, control ComfyUI y `GpuGuard`. |
| **Base de Datos (kidsdb)** | `compose.yaml` | `172.x.x.x:5434` / `5434` | PostgreSQL 16 con esquemas versionados por Flyway. |
| **ComfyUI NV** | `C:\ComfyUI_NV` | `http://127.0.0.1:8188` | Generación SDXL Turbo, IP-Adapter Plus, LTX-Video 2B y Wan 2.2. |
| **Ollama Local** | Sistema | `http://127.0.0.1:11434` | Generador y validador de guiones (`qwen2.5:7b-instruct`). |

> [!NOTE]
> En Windows 11 se recomienda utilizar `127.0.0.1` en lugar de `localhost`. Windows intenta resolver `localhost` a IPv6 (`::1`) primero; dado que ComfyUI y Ollama escuchan en IPv4 loopback, usar `127.0.0.1` reduce la latencia de 2,000 ms a menos de 15 ms por petición.

---

## 2. Arranque del Entorno: Opciones y Paso a Paso

### Opción A: Arranque Automatizado en 1 Solo Clic (Recomendado)

Abre PowerShell en la raíz del proyecto y ejecuta:

```powershell
cd "c:\Users\villa\GIT DESKTOP\AIO\KidsAnimationStudio"
.\start-dev.ps1
```

Este script automatizado realiza la secuencia completa:
1. Inicia el contenedor `kids-postgres` en Podman WSL y resuelve automáticamente la IP interna de red.
2. Comprueba Ollama; si no responde, lo inicia en segundo plano con `ollama serve`.
3. Comprueba ComfyUI; si no está activo, lanza la instancia portable NVIDIA en `C:\ComfyUI_NV`.
4. Verifica si `ai-gateway` ya está corriendo (evitando errores de puerto duplicado 10048); si no, lo inicia en segundo plano.
5. Abre el navegador en `http://localhost:8082/` y arranca Spring Boot con Maven.

---

### Opción B: Arranque Modular Paso a Paso (Terminales Separadas)

Si prefieres supervisar cada servicio en su propia consola o necesitas depurar un componente específico, utiliza los scripts modulares de la carpeta `scripts/`:

#### Paso 1: Base de Datos PostgreSQL
```powershell
cd "c:\Users\villa\GIT DESKTOP\AIO\KidsAnimationStudio"
.\scripts\start-db.ps1
```
* **Qué hace**: Asegura el contenedor Podman `kids-postgres`, lo mapea al puerto 5434 y detecta la IP de la máquina WSL (`eth0`).
* **Verificación**: Muestra `[OK] Contenedor kids-postgres iniciado` y la URL JDBC correspondiente.

#### Paso 2: Modelo de Lenguaje (Ollama)
```powershell
.\scripts\start-ollama.ps1
```
* **Qué hace**: Comprueba si `http://127.0.0.1:11434/api/tags` responde. Si no, arranca `ollama serve` y enumera los modelos instalados (debe aparecer `qwen2.5:7b-instruct`).

#### Paso 3: Motor de Renderizado Generativo (ComfyUI NV)
```powershell
.\scripts\start-comfyui.ps1
```
* **Qué hace**: Inicia `C:\ComfyUI_NV\ComfyUI_windows_portable\run_nvidia_gpu.bat` en su propia ventana con soporte CUDA 13.0 para RTX 5070 Blackwell.
* **Tiempo estimado**: 10 a 15 segundos mientras valida los paths de modelos y custom nodes.
* **Verificación**: `http://127.0.0.1:8188/system_stats` debe responder con HTTP 200 y reportar ~11.5 GB de VRAM libre.

#### Paso 4: AI Gateway (FastAPI)
```powershell
.\scripts\start-gateway.ps1
```
* **Qué hace**: Verifica si el puerto 8090 ya está en uso; si no, activa el entorno virtual `.venv` y arranca Uvicorn en `127.0.0.1:8090`.
* **Verificación**: Consulta `http://127.0.0.1:8090/health` y comprueba que `gpu.meets_min_free` sea `true`.

#### Paso 5: Orquestador Spring Boot & Dashboard Web
```powershell
.\scripts\start-orchestrator.ps1
```
* **Qué hace**: Resuelve la IP interna de WSL para la base de datos, ejecuta las migraciones Flyway (V1 y V2), levanta el servidor web en el puerto 8082 y abre el navegador automáticamente.
* **Acceso**:
  - **Dashboard Web**: `http://localhost:8082/` o `http://localhost:8082/index.html`
  - **API Health**: `http://localhost:8082/api/v1/health`
  - **Swagger UI**: `http://localhost:8082/swagger-ui.html`

---

### Diagnóstico Rápido de Estado (`check-status.ps1`)

En cualquier momento puedes verificar el estado de los 5 componentes ejecutando:

```powershell
.\scripts\check-status.ps1
```

Ejemplo de salida exitosa con todas las luces en verde:
```text
==========================================================
  KidsAnimationStudio - Diagnóstico de Servicios Locales
==========================================================
  [UP  ] PostgreSQL (kids-postgres)     | 172.23.240.83:5434                     | 14 ms
  [UP  ] Ollama LLM                     | http://127.0.0.1:11434/api/tags        | 35 ms
  [UP  ] ComfyUI (NVIDIA)               | http://127.0.0.1:8188/system_stats     | 9 ms
  [UP  ] AI Gateway (FastAPI)           | http://127.0.0.1:8090/health           | 575 ms
  [UP  ] Orquestador (Spring Boot)      | http://127.0.0.1:8082/api/v1/health    | 640 ms
==========================================================
```

---

## 3. Diagnóstico y Solución de Errores Comunes

### Error 1: `Connection to localhost:5434 refused` en Spring Boot
* **Causa**: En Windows con WSL2/Podman, los puertos publicados dentro de la máquina virtual Linux no siempre se reflejan automáticamente en el adaptador loopback `127.0.0.1` de Windows sin privilegios elevados de administrador (`netsh`).
* **Solución Implementada**:
  - La clase `KidsAnimationApiApplication` cuenta con auto-detección dinámica: al arrancar, comprueba si `127.0.0.1:5434` responde. Si está cerrado, consulta la IP de `eth0` en la máquina WSL (`wsl -d podman-machine-default ip -4 addr show eth0`) y redirige la conexión JDBC automáticamente a `jdbc:postgresql://<IP_WSL>:5434/kidsdb` sin intervención manual.

### Error 2: `[Errno 10048] solo se permite un uso de cada dirección de socket`
* **Causa**: Se intentó arrancar `ai-gateway` cuando ya había un proceso Uvicorn en segundo plano escuchando en el puerto 8090.
* **Solución**:
  - `start-dev.ps1` y `scripts/start-gateway.ps1` comprueban previamente si el puerto 8090 ya está respondiendo.
  - Para cerrar cualquier proceso colgado en el puerto 8090 manualmente:
    ```powershell
    $proc = (Get-NetTCPConnection -LocalPort 8090 -ErrorAction SilentlyContinue).OwningProcess
    if ($proc) { Stop-Process -Id $proc -Force }
    ```

### Error 3: `No default constructor found` en Beans de Spring Boot
* **Causa**: Clases componentes o de configuración con múltiples constructores (por ejemplo, para permitir inyección en tests unitarios) causan ambigüedad en Spring Boot si no se marca explícitamente el constructor de inyección.
* **Solución Implementada**:
  - `OllamaLlmProvider` y `GeminiLlmProvider` tienen `@Autowired` explícito en su constructor principal.
  - Los records de configuración (`KidsPipelineProperties`, `KidsVideoProperties`) incluyen un constructor por defecto sin argumentos para garantizar la instanciación segura bajo cualquier perfil.

### Error 4: Latencia excesiva o timeouts de conexión (2,000 ms+)
* **Causa**: El uso de la palabra `localhost` en Windows obliga al sistema operativo a consultar la interfaz IPv6 (`::1`), esperando el tiempo límite antes de intentar IPv4 (`127.0.0.1`).
* **Solución**:
  - Todos los archivos `.env`, `.env.example`, `application.yml` y scripts de PowerShell utilizan `127.0.0.1`.
  - El timeout de sondeo `probe-timeout-ms` en el orquestador se ajustó a 5,000 ms para tolerar momentos de carga pesada en GPU.

---

## 4. Flujo de Producción Operativo

### Paso 1: Configurar la Serie y la Biblia
1. En la pestaña **"📁 Series & Biblia"**, inspecciona las series existentes ("Tito el zorrito") o crea una nueva serie.
2. Cada serie define:
   - Nombre, descripción, idioma (`es-MX`) y rango de edad (`2-6 años`).
   - Relación de aspecto (`16:9` horizontal predeterminado).
   - Perfil de Estilo 3D Cartoon (Pixar/Disney infantil, colores cálidos, iluminación suave).
   - Locaciones registradas con su atmósfera y estado de ánimo.

### Paso 2: Crear Personajes y Aprobar su Hoja de Referencia
1. En la pestaña **"🎨 Personajes & Sheet"**, haz clic en **"+ Nuevo Personaje"**.
2. Ingresa el nombre (ej. "Tito"), rol (`HOST`), prompt canónico visual y voz asignada (`ef_dora` o `em_alex`).
3. Haz clic en **"✨ Generar 4 Candidatas"**. ComfyUI ejecutará el workflow `character_sheet.json` con SDXL Turbo variando los seeds.
4. En el modal emergente, revisa las 4 opciones, selecciona la que mejor represente la identidad visual y presiona **"✅ Aprobar Candidata"**.
   - *Nota:* El seed y la imagen aprobada se guardan permanentemente en la base de datos y servirán como ancla canónica para IP-Adapter en cada plano.

### Paso 3: Producir un Episodio Individual
1. Ve a la pestaña **"🎬 Nuevo Episodio / Lote"**.
2. Selecciona la serie y el tema pedagógico curricular:
   - `COUNTING`: Conteo y noción de cantidad.
   - `COLORS`: Colores primarios y secundarios.
   - `SHAPES`: Formas geométricas básicas (círculo, cuadrado, triángulo).
   - `ALPHABET`: Fonemas y vocales.
   - `ANIMALS_AND_SOUNDS`: Onomatopeyas y fauna.
   - `EMOTIONS`, `DAILY_ROUTINES`, `OPPOSITES`, `SIZES`, `NATURE`.
3. Especifica el detalle del tema (ej. *"Contar del 1 al 5 con manzanas"*) y el objetivo de aprendizaje.
4. Elige si deseas `autoApprove = true` (pipeline completamente automatizado) o `false` para revisar el guion antes de narrar.
5. Haz clic en **"🚀 Iniciar Pipeline Completo"**.

### Paso 4: Producción Desatendida en Lote (Batch)
Para generar series completas de manera nocturna o desatendida:
1. En el panel **"Producción en Lote"**, escribe el nombre del lote.
2. Ingresa uno o más temas en formato:
   ```text
   COUNTING|Contar del 1 al 3|Identificar cantidades pequeñas
   COUNTING|Contar del 4 al 6|Aprender a contar hasta 6
   COLORS|Color Rojo y Amarillo|Identificar colores primarios
   ```
3. Haz clic en **"⚡ Programar y Ejecutar Lote"**.
4. El ejecutor asíncrono con concurrencia 1 procesará cada episodio uno tras otro sin saturar la VRAM de la GPU. Si un episodio llegara a fallar, el lote continuará con el siguiente sin detener la producción.

---

## 5. Supervisión y Control de Calidad en Vivo

### Monitor de Pipeline (`⚡ Monitor en Vivo`)
- Muestra el progreso en tiempo real de 0 a 100% pasando por las 10 etapas:
  `PLANNING` ➔ `SAFETY_REVIEW` ➔ `NARRATION` ➔ `KEYFRAMES` ➔ `ANIMATION` ➔ `SUBTITLES_OVERLAYS` ➔ `RENDERING` ➔ `VIDEO_QA` ➔ `EXPORTING` ➔ `COMPLETED`.
- Si el episodio se interrumpe o falla por cualquier motivo externo, presiona el botón **"🔄 Reanudar"** (`POST /api/v1/episodes/{id}/resume`). El sistema examinará los assets existentes en disco y continuará a partir del plano incompleto.

### Desglose de Planos y Regeneración (`🎞️ Planos & QA`)
- Permite ver cada plano con su keyframe, texto de locución, voz TTS y puntuación de similitud CLIP.
- **Botones de Regeneración Puntual**:
  - `🔄 Keyframe`: Genera un nuevo keyframe con un seed alternativo y revalida con IP-Adapter.
  - `🔄 Animación`: Re-renderiza el clip de video I2V.
  - `🔄 Audio`: Vuelve a sintetizar la locución con Kokoro y re-alinea las palabras con Whisper.

---

## 6. Distribución y Publicación

En la pestaña **"📺 Reproductor Final"**:
1. Reproduce directamente el video MP4 final con subtítulos karaoke y overlays quemados.
2. Descarga el video en alta definición (1080p a 30 fps, audio normalizado a -14 LUFS).
3. Descarga la miniatura oficial generada (`_thumb.png`).
4. Consulta el archivo `_metadata.json` exportado en `export_videos/<serie>/`:
   ```json
   {
     "episodeId": "...",
     "title": "Episodio 1: Contando manzanas con Tito",
     "seriesTitle": "Tito el zorrito",
     "madeForKids": true,
     "containsSyntheticMedia": true,
     "language": "es-MX",
     "targetAge": "2-6",
     "tags": ["animacion infantil", "educacion preescolar", "aprender jugando", "conteo"]
   }
   ```
   Estos metadatos garantizan el cumplimiento riguroso de las políticas de **YouTube Kids** y la normativa de protección de menores **COPPA**.

---

## 7. Mantenimiento y Memoria de GPU (`GpuGuard`)

- **GpuGuard Activo**: El gateway revisa que existan al menos 9,000 MB libres en la RTX 5070 antes de iniciar un renderizado de video pesado. Si otros programas están abiertos, entrará en espera activa hasta que se libere la VRAM.
- **Limpieza Automática**: Tras cada etapa del pipeline, el orquestador invoca automáticamente `POST /api/v1/gpu/free`, descargando los pesos del modelo de la memoria de la GPU.
- **Fallback Ken Burns**: Si ComfyUI sufriera una interrupción inesperada, el sistema genera automáticamente una animación Ken Burns (zoom suave y paneo) con `PyAV` para garantizar que la producción nunca se bloquee.
