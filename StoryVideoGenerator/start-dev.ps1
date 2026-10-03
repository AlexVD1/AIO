<#
.SYNOPSIS
    Master launcher para StoryVideoGenerator en Windows.
.DESCRIPTION
    1. Asegura contenedor PostgreSQL en WSL
    2. Resuelve la IP interna de Podman WSL
    3. Inicia automáticamente Stable Diffusion Forge si no está corriendo
    4. Abre el navegador en http://localhost:8081/
    5. Arranca el servidor Spring Boot
#>

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " StoryVideoGenerator — Master Launcher (Todo en 1 clic)" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Asegurar contenedor Postgres en WSL
Write-Host "[1/4] Verificando PostgreSQL en Podman WSL..." -ForegroundColor Yellow
try {
    wsl -d podman-machine-default -u root sh -c "nohup sleep 86400 >/dev/null 2>&1 &" 2>$null | Out-Null
    wsl -d podman-machine-default -u root podman --cgroup-manager=cgroupfs start story-postgres 2>$null | Out-Null
    Write-Host "       Contenedor story-postgres activo." -ForegroundColor Green
} catch {
    Write-Warning "No se pudo comunicar con Podman WSL. Continuando..."
}

# 2. Obtener IP de eth0 de WSL
Write-Host "[2/4] Resolviendo direccion de red de la base de datos..." -ForegroundColor Yellow
$wslIp = "127.0.0.1"
try {
    $ipOutput = wsl -d podman-machine-default ip -4 addr show eth0 2>$null
    if ($ipOutput -match 'inet\s+([0-9]+\.[0-9]+\.[0-9]+\.[0-9]+)') {
        $wslIp = $matches[1]
        Write-Host "       IP detectada: $wslIp:5433" -ForegroundColor Green
    }
} catch {
    Write-Host "       Usando IP por defecto: 127.0.0.1" -ForegroundColor Gray
}

$env:DATABASE_URL = "jdbc:postgresql://${wslIp}:5433/storydb"
$env:DATABASE_USERNAME = "story_user"
$env:DATABASE_PASSWORD = "CAMBIAR_POR_PASSWORD_SEGURO"

# Cargar variables locales desde .env si existe y no estan definidas
if (Test-Path ".env") {
    Get-Content ".env" | ForEach-Object {
        if ($_ -match '^\s*([^#=\s]+)\s*=\s*(.*)$') {
            $k = $matches[1].Trim()
            $v = $matches[2].Trim()
            if (-not [System.Environment]::GetEnvironmentVariable($k)) {
                [System.Environment]::SetEnvironmentVariable($k, $v, "Process")
            }
        }
    }
}
if (-not $env:AI_MODEL) { $env:AI_MODEL = "gemini-2.5-flash" }

# 3. Iniciar Stable Diffusion Forge si no está corriendo
Write-Host "[3/5] Verificando Stable Diffusion Forge en C:\SD_Forge..." -ForegroundColor Yellow
$sdRunning = $false
try {
    $response = Invoke-WebRequest -Uri "http://localhost:7860/" -UseBasicParsing -TimeoutSec 2 -ErrorAction SilentlyContinue
    if ($response.StatusCode -eq 200) { $sdRunning = $true }
} catch {}

if (-not $sdRunning) {
    if (Test-Path "C:\SD_Forge\run.bat") {
        Write-Host "       Iniciando Stable Diffusion en ventana separada..." -ForegroundColor Cyan
        Start-Process -FilePath "cmd.exe" -ArgumentList "/c run.bat" -WorkingDirectory "C:\SD_Forge"
    } else {
        Write-Host "       [AVISO] No se encontro C:\SD_Forge\run.bat. Modo respaldo activo." -ForegroundColor Gray
    }
} else {
    Write-Host "       Stable Diffusion ya esta respondiendo en puerto 7860." -ForegroundColor Green
}

# 4. Iniciar Sincronizador con Google Drive en segundo plano
Write-Host "[4/5] Iniciando Sincronizador con Google Drive..." -ForegroundColor Yellow
if (Test-Path "$PSScriptRoot\sync-to-drive.ps1") {
    Start-Process powershell.exe -ArgumentList "-NoProfile -ExecutionPolicy Bypass -File `"$PSScriptRoot\sync-to-drive.ps1`"" -WindowStyle Minimized
    Write-Host "       Sincronizador activo hacia H:\Mi unidad\VideosStories" -ForegroundColor Green
}

# 5. Abrir navegador e iniciar Spring Boot
Write-Host "[5/5] Iniciando Servidor Web y Dashboard..." -ForegroundColor Yellow
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  Dashboard: http://localhost:8081/" -ForegroundColor Green
Write-Host "  API Health: http://localhost:8081/api/v1/health" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Cyan

Start-Process "http://localhost:8081/"

mvn spring-boot:run
