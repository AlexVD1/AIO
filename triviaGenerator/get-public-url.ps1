# ============================================================
# get-public-url.ps1 — Obtener URL pública de trivia-api
# ============================================================

$found = $false

# 1. Verificar Ngrok (Dominio Persistente)
$ngrokStatus = podman ps --filter "name=trivia-tunnel-ngrok" --format "{{.Status}}" 2>$null
if ($ngrokStatus -and ($ngrokStatus -match "Up")) {
    $found = $true
    # Obtener el dominio configurado en .env o inspeccionar contenedor
    $domain = ""
    if (Test-Path ".env") {
        $envContent = Get-Content ".env" | Out-String
        if ($envContent -match 'NGROK_DOMAIN=([^\r\n]+)') {
            $domain = $matches[1].Trim()
        }
    }
    if (-not $domain) {
        $domain = "either-provided-figment.ngrok-free.dev"
    }

    $publicUrl = "https://$domain"

    Write-Host ""
    Write-Host "============================================================" -ForegroundColor Cyan
    Write-Host "  trivia-api -- DOMINIO FIJO Y PERSISTENTE (NGROK)" -ForegroundColor Green
    Write-Host "============================================================" -ForegroundColor Cyan
    Write-Host "  URL Fija    : $publicUrl" -ForegroundColor White
    Write-Host "  Web SPA     : $publicUrl/" -ForegroundColor Gray
    Write-Host "  API Docs    : $publicUrl/swagger-ui.html" -ForegroundColor Gray
    Write-Host "  Health      : $publicUrl/api/v1/health" -ForegroundColor Gray
    Write-Host "============================================================" -ForegroundColor Cyan

    Write-Host "[*] Verificando conectividad con tu dominio persistente..." -NoNewline
    try {
        $sw = [System.Diagnostics.Stopwatch]::StartNew()
        $headers = @{ "ngrok-skip-browser-warning" = "true" }
        $res = Invoke-RestMethod -Uri "$publicUrl/api/v1/health" -Method Get -Headers $headers -TimeoutSec 10
        $sw.Stop()
        $ms = $sw.ElapsedMilliseconds
        if ($res.status -eq "UP") {
            Write-Host " [OK] En linea ($ms ms)" -ForegroundColor Green
        } else {
            Write-Host " [ADVERTENCIA] Estado: $($res.status)" -ForegroundColor Yellow
        }
    } catch {
        Write-Host " [ERROR] No se pudo conectar a $publicUrl" -ForegroundColor Red
    }
}

# 2. Verificar Cloudflare Tunnel si está activo
$cfStatus = podman ps --filter "name=trivia-tunnel-cloudflare" --format "{{.Status}}" 2>$null
if ($cfStatus -and ($cfStatus -match "Up")) {
    $found = $true
    $logs = podman logs trivia-tunnel-cloudflare 2>&1 | Out-String
    if ($logs -match 'https://[a-zA-Z0-9\.\-]+\.trycloudflare\.com') {
        $cfUrl = $matches[0]
        Write-Host ""
        Write-Host "============================================================" -ForegroundColor Cyan
        Write-Host "  trivia-api -- TUNEL SECUNDARIO (CLOUDFLARE - SIN AVISO)" -ForegroundColor Yellow
        Write-Host "============================================================" -ForegroundColor Cyan
        Write-Host "  URL         : $cfUrl" -ForegroundColor White
        Write-Host "  Web SPA     : $cfUrl/" -ForegroundColor Gray
        Write-Host "  Health      : $cfUrl/api/v1/health" -ForegroundColor Gray
        Write-Host "============================================================" -ForegroundColor Cyan
    }
}

if (-not $found) {
    Write-Host "[!] Ningún túnel de acceso público está en ejecución." -ForegroundColor Yellow
    Write-Host "    Inicia los servicios con: podman compose up -d" -ForegroundColor Cyan
    exit 1
}
