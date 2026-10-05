# ============================================================
# KidsAnimationStudio - Paso 3: Iniciar ComfyUI NVIDIA Portable
# C:\ComfyUI_NV (RTX 5070 Blackwell, CUDA 13.0, Puerto 8188)
# ============================================================
Write-Host "Verificando ComfyUI en http://127.0.0.1:8188..." -ForegroundColor Yellow

$isRunning = $false
try {
    $res = Invoke-RestMethod -Uri "http://127.0.0.1:8188/system_stats" -TimeoutSec 2 -ErrorAction Stop
    $isRunning = $true
} catch {}

if ($isRunning) {
    Write-Host "[OK] ComfyUI ya está activo y respondiendo en el puerto 8188." -ForegroundColor Green
} else {
    $comfyBat = "C:\ComfyUI_NV\ComfyUI_windows_portable\run_nvidia_gpu.bat"
    if (Test-Path $comfyBat) {
        Write-Host "Iniciando ComfyUI en una nueva ventana..." -ForegroundColor Cyan
        Start-Process -FilePath "cmd.exe" -ArgumentList "/c run_nvidia_gpu.bat" `
            -WorkingDirectory "C:\ComfyUI_NV\ComfyUI_windows_portable"
        Write-Host "Esperando arranque (10-15 segundos)..." -ForegroundColor Gray
        Start-Sleep -Seconds 10
        try {
            $stats = Invoke-RestMethod -Uri "http://127.0.0.1:8188/system_stats" -TimeoutSec 5
            Write-Host "[OK] ComfyUI iniciado exitosamente." -ForegroundColor Green
        } catch {
            Write-Host "[INFO] ComfyUI sigue inicializando modelos en su ventana de consola." -ForegroundColor Gray
        }
    } else {
        Write-Host "[ERROR] No se encontró $comfyBat. Consulta docs/SETUP_GPU_WINDOWS.md." -ForegroundColor Red
    }
}
