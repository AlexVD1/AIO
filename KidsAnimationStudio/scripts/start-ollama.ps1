# ============================================================
# KidsAnimationStudio - Paso 2: Iniciar Ollama
# Puerto 11434 con modelo qwen2.5:7b-instruct
# ============================================================
Write-Host "Verificando Ollama en http://127.0.0.1:11434..." -ForegroundColor Yellow

$isRunning = $false
try {
    $res = Invoke-RestMethod -Uri "http://127.0.0.1:11434/api/tags" -TimeoutSec 2 -ErrorAction Stop
    $isRunning = $true
} catch {}

if ($isRunning) {
    Write-Host "[OK] Ollama ya está activo y respondiendo." -ForegroundColor Green
} else {
    Write-Host "Iniciando 'ollama serve' en segundo plano..." -ForegroundColor Cyan
    Start-Process -FilePath "ollama" -ArgumentList "serve" -WindowStyle Minimized -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 3
}

try {
    $models = (Invoke-RestMethod -Uri "http://127.0.0.1:11434/api/tags" -TimeoutSec 3).models.name
    Write-Host "Modelos disponibles en Ollama:" -ForegroundColor Cyan
    $models | ForEach-Object { Write-Host "  - $_" -ForegroundColor White }
} catch {
    Write-Host "[AVISO] No se pudo verificar la lista de modelos. Inicia la aplicación Ollama desde el menú inicio." -ForegroundColor DarkYellow
}
