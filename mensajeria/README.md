# Sistema de Mensajería (Mattermost + Cloudflared)

Este proyecto levanta un sistema de mensajería empresarial auto-alojado utilizando Mattermost y lo expone de forma segura a internet mediante Cloudflare Tunnels (TryCloudflare).

## Instalación y Ejecución

1. Asegúrate de tener Podman (o Docker) funcionando.
2. Abre una terminal en esta carpeta y ejecuta:
   ```powershell
   podman compose up -d
   ```
   *(Si usas Docker, el comando es `docker-compose up -d` o `docker compose up -d`)*

## ¿Cómo obtener la URL pública de acceso?

Dado que estamos utilizando un túnel rápido de Cloudflare (sin cuenta), **la URL cambia cada vez que reinicias los contenedores o tu equipo**.

Para obtener tu URL actual, ejecuta este comando en tu terminal:

```powershell
podman compose logs cloudflared
```

En la salida de la consola, busca un bloque de texto como este:
```text
|  Your quick Tunnel has been created! Visit it at (it may take some time to be reachable):  |
|  https://[palabras-aleatorias].trycloudflare.com                                           |
```
Copia ese enlace y úsalo para acceder al chat.

> **💡 Consejo adicional:**
> Si utilizas una interfaz gráfica como **Podman Desktop** o **Docker Desktop**, puedes encontrar esta URL yendo a la lista de contenedores, haciendo clic en el contenedor `cloudflared` y abriendo la pestaña de **Logs** (Registros).
