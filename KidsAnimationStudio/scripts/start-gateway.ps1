# ============================================================
# KidsAnimationStudio - Paso 4: Iniciar AI Gateway FastAPI
# Puerto 8090 con GpuGuard, Kokoro-82M TTS y proxy a ComfyUI
# ============================================================
Set-Location (Join-Path $PSScriptRoot "..\ai-gateway")
Write-Host "Verificando ai-gateway en http://localhost:8090/health..." -ForegroundColor Yellow

$isRunning = $false
try {
    $res = Invoke-RestMethod -Uri "http://127.0.0.1:8090/health" -TimeoutSec 3 -ErrorAction Stop
    if ($res.status -eq "UP") { $isRunning = $true }
} catch {}

if ($isRunning) {
    Write-Host "[OK] ai-gateway ya está respondiendo en el puerto 8090." -ForegroundColor Green
} else {
    $py = ".\.venv\Scripts\python.exe"
    if (Test-Path $py) {
        Write-Host "Iniciando FastAPI Uvicorn en segundo plano..." -ForegroundColor Cyan
        Start-Process -FilePath $py -ArgumentList "-m uvicorn app.main:app --host 127.0.0.1 --port 8090" -WindowStyle Minimized
        Start-Sleep -Seconds 3
        try {
            $health = Invoke-RestMethod -Uri "http://127.0.0.1:8090/health" -TimeoutSec 5
            Write-Host "[OK] ai-gateway iniciado exitosamente (GPU: $($health.gpu.name))." -ForegroundColor Green
        } catch {
            Write-Host "[AVISO] ai-gateway arrancando..." -ForegroundColor Gray
        }
    } else {
        Write-Host "[ERROR] Falta el entorno virtual en ai-gateway\.venv." -ForegroundColor Red
        Write-Host "Ejecuta: cd ai-gateway; python -m venv .venv; .\.venv\Scripts\pip install -r requirements.txt" -ForegroundColor Yellow
    }
}
