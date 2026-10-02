# ============================================================
# sync-to-drive.ps1 — Sincronizador Automático hacia Google Drive
# ============================================================
# Vigila la carpeta local 'export_videos' y copia los videos recién
# generados a tu Google Drive ('H:\Mi unidad\VideosQuizazos')
# ============================================================

$sourceDir = Join-Path $PSScriptRoot "export_videos"
$driveDir = "H:\Mi unidad\VideosQuizazos"

if (-not (Test-Path $sourceDir)) {
    New-Item -ItemType Directory -Force -Path $sourceDir | Out-Null
}

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  SINCRONIZADOR TRIVIA -> GOOGLE DRIVE ACTIVO" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  Origen  : $sourceDir" -ForegroundColor White
Write-Host "  Destino : $driveDir" -ForegroundColor White
Write-Host "============================================================" -ForegroundColor Cyan

# Función para copiar archivos pendientes
function Sync-PendingFiles {
    if (-not (Test-Path $driveDir)) {
        Write-Warning "No se encuentra la unidad Google Drive en: $driveDir"
        return
    }
    Get-ChildItem -Path $sourceDir -Filter *.mp4 -File | ForEach-Object {
        $destPath = Join-Path $driveDir $_.Name
        if (-not (Test-Path $destPath)) {
            try {
                Copy-Item -Path $_.FullName -Destination $destPath -Force
                Write-Host "[+] Video enviado a Google Drive: $($_.Name)" -ForegroundColor Green
            } catch {
                Write-Warning "Error al copiar $($_.Name): $_"
            }
        }
    }
}

# Función para limpiar videos locales con más de 48 horas de antigüedad (mantiene limpio el disco local)
# NOTA: NO se limpia driveDir para que los videos en cola de Google Drive se mantengan listos para Make.com
function Cleanup-OldVideos {
    $cutoff = (Get-Date).AddHours(-48)
    $folder = $sourceDir
    if (Test-Path $folder) {
        Get-ChildItem -Path $folder -Filter *.mp4 -File -ErrorAction SilentlyContinue | Where-Object { $_.LastWriteTime -lt $cutoff } | ForEach-Object {
            try {
                Remove-Item $_.FullName -Force
                Write-Host "[-] Video antiguo eliminado (>48h): $($_.Name)" -ForegroundColor DarkGray
            } catch {
                Write-Warning "No se pudo eliminar $($_.Name): $_"
            }
        }
    }
}

# 1. Limpieza y sincronización de archivos existentes
Cleanup-OldVideos
Sync-PendingFiles

# 2. Configurar vigilante en tiempo real
$watcher = New-Object System.IO.FileSystemWatcher
$watcher.Path = $sourceDir
$watcher.Filter = "*.mp4"
$watcher.NotifyFilter = [System.IO.NotifyFilters]::FileName -bor [System.IO.NotifyFilters]::LastWrite
$watcher.EnableRaisingEvents = $true

$action = {
    param($sender, $e)
    $fileName = $e.Name
    $filePath = $e.FullPath
    
    # Pequeña pausa para permitir que el contenedor termine de escribir el archivo completo
    Start-Sleep -Seconds 2
    
    $driveTarget = Join-Path "H:\Mi unidad\VideosQuizazos" $fileName
    try {
        Copy-Item -Path $filePath -Destination $driveTarget -Force
        Write-Host "`n[+] ¡NUEVO VIDEO ENVIADO A GOOGLE DRIVE!" -ForegroundColor Green
        Write-Host "    Archivo: $fileName" -ForegroundColor White
    } catch {
        Write-Warning "Error al sincronizar $($fileName): $_"
    }
}

$createdEvent = Register-ObjectEvent -InputObject $watcher -EventName "Created" -Action $action

Write-Host "`nVigilando carpeta en segundo plano. Puedes minimizar esta ventana." -ForegroundColor Gray
Write-Host "Presiona Ctrl + C para detener.`n" -ForegroundColor Gray

try {
    $lastClean = Get-Date
    while ($true) {
        Start-Sleep -Seconds 3
        Sync-PendingFiles
        if ((Get-Date) - $lastClean -gt (New-TimeSpan -Hours 1)) {
            Cleanup-OldVideos
            $lastClean = Get-Date
        }
    }
} finally {
    Unregister-Event -SourceIdentifier $createdEvent.Name -ErrorAction SilentlyContinue
    $watcher.Dispose()
}
