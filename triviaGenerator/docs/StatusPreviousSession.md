# Estado de Sesión — triviaGenerator

> **Fecha y Hora:** 2026-09-24  
> **Proyecto:** Generador de Trivias con Inteligencia Artificial (`triviaGenerator`)  
> **Tecnologías:** Java 21 LTS, Spring Boot 3.3.0 / 3.4.1, PostgreSQL 16 con pgvector, Flyway (V1–V6), Podman & Docker Compose v5, Google Gemini 3.5 Flash-Lite, Ngrok (Dominio Fijo Gratuito), Cloudflare Tunnel (`cloudflared`), HTML5/CSS3/Vanilla JS SPA.  
> **Servidor local:** VM WSL2 en `http://172.23.240.83:8080` (puerto expuesto `8080:8080`).  
> **Acceso público simultáneo (Dual Tunnel):**
> - **Dominio fijo permanente (Ngrok):** `https://either-provided-figment.ngrok-free.dev`
> - **Túnel secundario (Cloudflare):** URL activa consultable en cualquier momento con `.\get-public-url.ps1` (sin pantalla de cortesía/aviso).

---

## 1. Objetivo Actual

Proveer a la plataforma `triviaGenerator` de acceso dual y concurrente desde Internet **de forma 100% gratuita**: un dominio fijo permanente (Ngrok) y un túnel secundario sin pantallas de aviso (Cloudflare), sin abrir puertos y orquestado con Podman Compose.

---

## 2. Diagnóstico de la Arquitectura y Selección de Solución Persistente

1. **Despliegue actual:** Orquestado con Podman Compose en WSL2 (`podman-machine-default`). Contenedores `trivia-postgres` (PostgreSQL 16 + pgvector) y `trivia-api` (Spring Boot 3 + Java 21 + SPA integrada).
2. **Componentes expuestos a Internet:** Únicamente el puerto `8080` de `trivia-api` (SPA web `/`, API REST `/api/v1/**`, documentación Swagger `/swagger-ui.html` y assets PNG `/assets/**`). PostgreSQL permanece aislado internamente en la red de contenedores.
3. **Coexistencia Dual de Túneles:** Ambos túneles (`trivia-tunnel-ngrok` y `trivia-tunnel-cloudflare`) operan como conexiones salientes independientes desde la red de Podman hacia `http://api:8080`. No compiten por puertos locales y permiten combinar las ventajas de ambos:
   - **Ngrok:** Dominio reservado fijo y permanente (`either-provided-figment.ngrok-free.dev`), ideal para compartir una sola URL que nunca caduque.
   - **Cloudflare:** Túnel sin pantalla de advertencia intermedia, ideal para acceso directo sin clics de confirmación.

---

## 3. Trabajo Realizado en Esta Sesión

1. **Integración de `trivia-tunnel-ngrok` en `compose.yaml`:**
   - Servicio configurado con la imagen oficial `docker.io/ngrok/ngrok:latest`.
   - Conectado a la red interna de Podman Compose enrutando el tráfico directamente a `http://api:8080`.
   - Parametrizado mediante `${NGROK_AUTHTOKEN}` y `${NGROK_DOMAIN:-either-provided-figment.ngrok-free.dev}`.

2. **Configuración de Variables de Entorno (`.env` y `.env.example`):**
   - Registrado el authtoken de Ngrok del usuario y el dominio fijo asignado.

3. **Bypass de Pantalla Intersticial de Ngrok (`ngrok-skip-browser-warning`):**
   - En planes gratuitos, Ngrok intercepta llamadas HTTP para mostrar una advertencia antispam/antiphishing si no se envía la cabecera correspondiente.
   - En `WebCorsConfig.java`: Se añadió `ngrok-skip-browser-warning` a la lista de cabeceras permitidas en las peticiones CORS preflight (`OPTIONS`).
   - En `index.html` y `test-client.html`: Se integró la cabecera `ngrok-skip-browser-warning: true` en todas las funciones cliente de la SPA (`getHeaders()`, `checkApiHealth()`, `loadCatalog()`, `loadRecentTrivias()`, `fetchNextQuizQuestion()`, `loadStats()`, `triggerZipDownload()` y `markTriviaAsDownloaded()`).

4. **Soporte de Método HTTP `HEAD` en `ApiKeyFilter.java`:**
   - Se habilitó el método `HEAD` como lectura pública para permitir inspecciones y sondeos de salud de proxies y CDNs sin requerir `X-API-KEY`.
   - Se añadió prueba unitaria en `ApiKeyFilterTest.java`.

5. **Scripts Utilitarios `get-public-url.ps1` y `get-public-url.sh`:**
   - Detectan automáticamente el túnel persistente activo de Ngrok y ejecutan un healthcheck con medición de latencia.
   - Soportan la detección combinada de Cloudflare si se levanta el perfil alternativo.

---

## 4. Detalle de Archivos Modificados y Creados

| Archivo | Acción | Descripción del Cambio |
|---|---|---|
| `compose.yaml` | Modificado | Añadido servicio `tunnel-ngrok` como túnel por defecto y `tunnel-cloudflare` como perfil opcional. |
| `.env` y `.env.example` | Modificado | Incorporadas variables `NGROK_AUTHTOKEN` y `NGROK_DOMAIN`. |
| `src/main/java/com/trivia/api/infrastructure/config/WebCorsConfig.java` | Modificado | Habilitada cabecera `ngrok-skip-browser-warning` en la configuración global de CORS. |
| `src/main/java/com/trivia/api/infrastructure/security/ApiKeyFilter.java` | Modificado | Habilitado método `HEAD` como método público de lectura. |
| `src/test/java/com/trivia/api/infrastructure/security/ApiKeyFilterTest.java` | Modificado | Test unitario verificando acceso público con método `HEAD`. |
| `src/main/resources/static/index.html` | Modificado | Inyección de cabecera `ngrok-skip-browser-warning: true` en todas las llamadas fetch. |
| `test-client.html` | Sincronizado | Copia idéntica de `index.html`. |
| `get-public-url.ps1` | Actualizado | Detección de dominio persistente Ngrok con comprobación de salud y latencia. |
| `get-public-url.sh` | Actualizado | Versión Bash para entornos Linux/WSL. |
| `README.md` y `docs/DEPLOYMENT_GUIDE.md` | Actualizado | Documentadas instrucciones para acceso público persistente. |
| `docs/StatusPreviousSession.md` | Actualizado | Documento consolidado del estado de la sesión. |

---

## 5. Verificación y Resultados de Pruebas

### A. Pruebas Automáticas (`mvn test -o`)
- **Total de pruebas:** 95
- **Fallos:** 0
- **Errores:** 0
- **Resultado:** `BUILD SUCCESS`

### B. Pruebas en Vivo sobre Dominio Persistente (`https://either-provided-figment.ngrok-free.dev`)
1. **Health Check Público (`GET /api/v1/health`):**
   - Responde `HTTP 200 OK` con payload JSON:
     `{"status":"UP","servicio":"trivia-api","version":"0.0.1-SNAPSHOT","timestamp":"2026-09-24T19:18:30.831291286Z"}`.
2. **Interfaz Web SPA (`GET /`):**
   - Responde `HTTP 200 OK` con HTML completo cargado a través del borde de Ngrok.
3. **Catálogo de Categorías (`GET /api/v1/catalogos/tipos-trivia`):**
   - Responde `HTTP 200 OK` devolviendo el listado completo de categorías desde PostgreSQL.
4. **Consulta de Trivias (`GET /api/v1/trivias`):**
   - Responde `HTTP 200 OK` con paginación y trivias registradas en la base de datos.
5. **Servidor de Assets PNG (`GET /assets/.../pregunta.png`):**
   - Responde `HTTP 200 OK` (`Content-Type: image/png`, 43.9 KB transferidos correctamente).
6. **Script PowerShell (`.\get-public-url.ps1`):**
   - Ejecutado con éxito reportando: `[*] Verificando conectividad con tu dominio persistente... [OK] En linea (399 ms)`.

---

## 6. Problemas o Limitaciones de la Solución

1. **Pantalla Inicial en Navegadores Nuevos (Free Tier Interstitial):**
   - Al abrir el enlace `https://either-provided-figment.ngrok-free.dev` por primera vez en un navegador nuevo, Ngrok muestra una página de aviso antispam con un botón *"Visit Site"*. Al hacer clic, se almacena una cookie y la SPA carga con normalidad.
   - *Mitigación:* Para llamadas API automáticas desde código o la SPA, la cabecera `ngrok-skip-browser-warning: true` ya está integrada, por lo que nunca son bloqueadas ni reciben HTML.
2. **Límites del Plan Gratuito de Ngrok:**
   - 1 dominio estático por cuenta (suficiente para este proyecto).
   - Ancho de banda de 1 GB mensual (suficiente para pruebas y demostraciones de trivias).
3. **Disponibilidad Ligada al Host Local:**
   - El contenedor y la máquina local (WSL2/Podman) deben estar encendidos para que el túnel responda peticiones.

---

## 7. Pendientes y Próximo Paso Recomendado

- **Próximo paso recomendado:** Abrir `https://either-provided-figment.ngrok-free.dev` desde un teléfono móvil fuera de la red local, hacer clic en "Visit Site" en la pantalla de bienvenida de Ngrok, y probar el flujo de juego Quiz y generación en tiempo real.
- **Opcional:** Si en el futuro se compra un dominio propio, se puede volver a habilitar Cloudflare Tunnel con token para eliminar la pantalla intersticial de bienvenida.
