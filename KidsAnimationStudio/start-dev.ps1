# ============================================================
# KidsAnimationStudio - Master Launcher de Desarrollo
# No toca nada de StoryVideoGenerator (puertos 8081/5433/7860).
# ============================================================
$ErrorActionPreference = "Continue"
Set-Location $PSScriptRoot

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  KidsAnimationStudio - Entorno de desarrollo" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# Cargar .env (sin sobreescribir variables ya definidas)
if (Test-Path ".env") {
    Get-Content ".env" | ForEach-Object {
        if ($_ -match '^\s*([^#=\s]+)\s*=\s*(.*)$') {
            $k = $matches[1].Trim(); $v = $matches[2].Trim()
            if (-not [System.Environment]::GetEnvironmentVariable($k)) {
                [System.Environment]::SetEnvironmentVariable($k, $v, "Process")
            }
        }
    }
} else {
    Write-Warning "No existe .env; se usaran valores por defecto (copia .env.example a .env)."
}

function Test-Http($url, $timeoutSec = 2) {
    try { 
        (Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec $timeoutSec -ErrorAction Stop).StatusCode -eq 200 
    } catch { 
        $false 
    }
}

# 1. Postgres propio (kids-postgres :5434)
Write-Host "[1/5] Verificando kids-postgres (puerto 5434)..." -ForegroundColor Yellow
try {
    wsl -d podman-machine-default -u root sh -c "nohup sleep 86400 >/dev/null 2>&1 &" 2>$null | Out-Null
    wsl -d podman-machine-default -u root podman --cgroup-manager=cgroupfs start kids-postgres 2>$null | Out-Null
    Write-Host "       Contenedor kids-postgres activo." -ForegroundColor Green
} catch {
    podman compose -f compose.yaml up -d postgres
    if ($LASTEXITCODE -ne 0) { Write-Warning "No se pudo levantar kids-postgres con Podman." }
}

# Resolver IP de WSL de la máquina Podman para acceso directo
$dbHost = "localhost"
try {
    $ipOutput = wsl -d podman-machine-default ip -4 addr show eth0 2>$null
    if (($ipOutput -join "`n") -match 'inet\s+([0-9]+\.[0-9]+\.[0-9]+\.[0-9]+)') { $dbHost = $matches[1] }
} catch {}

$resolvedDbUrl = "jdbc:postgresql://${dbHost}:5434/kidsdb"
$env:DATABASE_URL = $resolvedDbUrl
[System.Environment]::SetEnvironmentVariable("DATABASE_URL", $resolvedDbUrl, "Process")
Write-Host "       DATABASE_URL=$resolvedDbUrl" -ForegroundColor Gray

# 2. Ollama (puerto 11434)
Write-Host "[2/5] Verificando Ollama..." -ForegroundColor Yellow
$ollamaUrl = if ($env:OLLAMA_URL) { $env:OLLAMA_URL } else { "http://localhost:11434" }
if (Test-Http "$ollamaUrl/api/tags") {
    Write-Host "       Ollama respondiendo OK." -ForegroundColor Green
} else {
    Write-Host "       Iniciando Ollama en segundo plano..." -ForegroundColor Cyan
    Start-Process -FilePath "ollama" -ArgumentList "serve" -WindowStyle Minimized -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 2
    if (Test-Http "$ollamaUrl/api/tags") {
        Write-Host "       Ollama levantado correctamente." -ForegroundColor Green
    } else {
        Write-Host "       [AVISO] Inicia Ollama manualmente con 'ollama serve' o la app de escritorio." -ForegroundColor DarkYellow
    }
}

# 3. ComfyUI (puerto 8188 - instalacion NVIDIA Blackwell RTX 5070)
Write-Host "[3/5] Verificando ComfyUI..." -ForegroundColor Yellow
$comfyUrl = if ($env:COMFYUI_URL) { $env:COMFYUI_URL } else { "http://localhost:8188" }
if (Test-Http "$comfyUrl/system_stats") {
    Write-Host "       ComfyUI respondiendo OK en $comfyUrl." -ForegroundColor Green
} else {
    $comfyBat = "C:\ComfyUI_NV\ComfyUI_windows_portable\run_nvidia_gpu.bat"
    if (Test-Path $comfyBat) {
        Write-Host "       Iniciando ComfyUI en ventana separada..." -ForegroundColor Cyan
        Start-Process -FilePath "cmd.exe" -ArgumentList "/c run_nvidia_gpu.bat" `
            -WorkingDirectory "C:\ComfyUI_NV\ComfyUI_windows_portable" -WindowStyle Minimized
        Write-Host "       Esperando inicio de ComfyUI (puede tardar 10-15s)..." -ForegroundColor Gray
    } else {
        Write-Host "       [AVISO] No se encontro $comfyBat (ver docs/SETUP_GPU_WINDOWS.md)" -ForegroundColor DarkYellow
    }
}

# 4. ai-gateway (puerto 8090)
Write-Host "[4/5] Verificando ai-gateway (puerto 8090)..." -ForegroundColor Yellow
if (Test-Http "http://127.0.0.1:8090/health" 3) {
    Write-Host "       ai-gateway ya esta respondiendo en puerto 8090." -ForegroundColor Green
} else {
    $gatewayPy = Join-Path $PSScriptRoot "ai-gateway\.venv\Scripts\python.exe"
    if (Test-Path $gatewayPy) {
        Write-Host "       Iniciando ai-gateway..." -ForegroundColor Cyan
        Start-Process -FilePath $gatewayPy -ArgumentList "-m uvicorn app.main:app --host 127.0.0.1 --port 8090" `
            -WorkingDirectory (Join-Path $PSScriptRoot "ai-gateway") -WindowStyle Minimized
        Start-Sleep -Seconds 2
        if (Test-Http "http://127.0.0.1:8090/health" 3) {
            Write-Host "       ai-gateway levantado correctamente." -ForegroundColor Green
        } else {
            Write-Host "       ai-gateway iniciando en segundo plano." -ForegroundColor Gray
        }
    } else {
        Write-Warning "Falta ai-gateway\.venv. Crea el entorno: cd ai-gateway; python -m venv .venv; .\.venv\Scripts\pip install -r requirements.txt"
    }
}

# 5. Orquestador Spring Boot (puerto 8082)
Write-Host "[5/5] Iniciando orquestador Spring Boot..." -ForegroundColor Yellow
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  Dashboard Web: http://localhost:8082/" -ForegroundColor Green
Write-Host "  API Health:    http://localhost:8082/api/v1/health" -ForegroundColor Green
Write-Host "  Swagger UI:    http://localhost:8082/swagger-ui.html" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Cyan

# Abrir el dashboard en el navegador
Start-Process "http://localhost:8082/"

Set-Location (Join-Path $PSScriptRoot "orchestrator")
mvn spring-boot:run
