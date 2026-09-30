# Guía de Despliegue en Producción — VPS + Cloudflare Tunnel

Esta guía describe el procedimiento paso a paso para desplegar **trivia-api** en un servidor VPS accesible desde Internet mediante **Cloudflare Tunnel** (sin abrir puertos en el firewall ni requerir IP pública estática).

---

## 1. Arquitectura de Despliegue

```
Internet (HTTPS) 
      │
      ▼
Cloudflare CDN / Edge (SSL automático + WAF + DDoS)
      │
      ▼ [Túnel cifrado saliente - cloudflared]
VPS (Ubuntu 22.04 / 24.04 o Debian 12)
      │
      ├── Contenedor: trivia-api (Spring Boot 3 + Java 21) :8080
      ├── Contenedor: trivia-postgres (PostgreSQL 16) :5432
      └── Volumen: /data/storage (Imágenes PNG renderizadas)
```

---

## 2. Requisitos Previos en el VPS

1. Un servidor VPS con Linux (Ubuntu/Debian) con al menos 1GB de RAM (recomendado 2GB).
2. Podman y podman-compose instalados:
   ```bash
   sudo apt update
   sudo apt install -y podman podman-compose
   ```
3. Un dominio gestionado en Cloudflare (ej. `midominio.com`).
4. Cliente `cloudflared` instalado:
   ```bash
   curl -L --output cloudflared.deb https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64.deb
   sudo dpkg -i cloudflared.deb
   ```

---

## 3. Preparación del Proyecto

1. Clonar el repositorio en el VPS:
   ```bash
   git clone <URL_DEL_REPOSITORIO> triviaGenerator
   cd triviaGenerator
   ```

2. Crear el archivo `.env` a partir de la plantilla:
   ```bash
   cp .env.example .env
   nano .env
   ```

3. Configurar los valores de producción:
   - `DATABASE_PASSWORD`: Generar una contraseña segura.
   - `AI_API_KEY`: Tu clave de Google AI Studio (Gemini).
   - `API_KEY`: Una clave secreta para proteger las operaciones POST de la API.
   - `STORAGE_BASE_URL`: La URL pública de tu dominio (ej. `https://trivia.midominio.com/assets`).

---

## 4. Construcción y Despliegue con Podman

1. Construir la imagen y levantar los contenedores en segundo plano:
   ```bash
   podman compose up -d --build
   ```

2. Verificar el estado de los contenedores:
   ```bash
   podman ps
   ```

3. Verificar logs de inicialización y migraciones Flyway:
   ```bash
   podman compose logs -f
   ```

4. Probar el endpoint local de salud:
   ```bash
   curl http://localhost:8080/api/v1/health
   ```
   Debe responder: `{"status":"UP","servicio":"trivia-api",...}`

---

---

## 5. Acceso Público por Internet con Cloudflare Tunnel

La infraestructura ya incluye el servicio `trivia-tunnel` preconfigurado en `compose.yaml` utilizando la imagen oficial `docker.io/cloudflare/cloudflared:latest`.

Existen dos modalidades compatibles:

### Opción A: Quick Tunnel (Gratuito, Sin cuenta ni dominio previo)
Esta es la opción predeterminada. Al levantar el entorno con:
```bash
podman compose up -d
```
Cloudflare genera automáticamente un túnel temporal con certificado SSL público (ej. `https://xyz-subdomain.trycloudflare.com`).

Para consultar la URL activa y verificar la salud del servicio en cualquier momento:
- En Windows (PowerShell):
  ```powershell
  .\get-public-url.ps1
  ```
- En Linux / WSL (Bash):
  ```bash
  ./get-public-url.sh
  ```

### Opción B: Túnel Persistente con Dominio Propio (Cloudflare Zero Trust)
Si posees un dominio gestionado en Cloudflare:
1. Crea un túnel en el dashboard de Cloudflare Zero Trust (**Networks > Tunnels**).
2. Obtén el token del túnel (`eyJh...`).
3. En `.env`, configura:
   ```properties
   CLOUDFLARE_TUNNEL_CMD=tunnel --no-autoupdate run --token eyJh...
   ```
4. Reinicia el contenedor del túnel:
   ```bash
   podman compose up -d tunnel
   ```
5. Tu aplicación quedará accesible permanentemente bajo tu propio subdominio (ej. `https://trivia.tudominio.com`).

---

## 6. Verificación en Producción

Accede desde cualquier navegador o terminal en Internet:

1. **Health Check**:
   ```bash
   curl https://trivia.midominio.com/api/v1/health
   ```

2. **Swagger UI**:
   Visita `https://trivia.midominio.com/swagger-ui.html` para consultar y probar todos los endpoints interactivamente.

3. **Generar Trivias**:
   ```bash
   curl -X POST https://trivia.midominio.com/api/v1/trivias \
     -H "Content-Type: application/json" \
     -H "X-API-KEY: tu_clave_secreta" \
     -d '{
       "tipoTrivia": "ASTRONOMIA",
       "cantidad": 3,
       "numeroOpciones": 4,
       "dificultad": "FACIL",
       "idioma": "es-MX"
     }'
   ```
