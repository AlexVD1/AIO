# ============================================================
# sync-to-drive.ps1 — Sincronizador Automático hacia Google Drive
# ============================================================
# Vigila la carpeta local 'export_videos' y copia los videos recién
# generados y sus metadatos a tu Google Drive ('H:\Mi unidad\VideosStories')
#
# PROTECCIÓN CONTRA BUCLES:
# Si Make.com mueve los archivos a la subcarpeta 'Procesados',
# este script detecta que ya fueron procesados y NO los vuelve a subir.
# ============================================================

$sourceDir = Join-Path $PSScriptRoot "export_videos"
$driveDir = "H:\Mi unidad\VideosStories"
$processedDir = Join-Path $driveDir "Procesados"
$historyFile = Join-Path $sourceDir ".synced_history.txt"

if (-not (Test-Path $sourceDir)) {
    New-Item -ItemType Directory -Force -Path $sourceDir | Out-Null
}

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  SINCRONIZADOR STORYVIDEO -> GOOGLE DRIVE ACTIVO" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  Origen     : $sourceDir" -ForegroundColor White
Write-Host "  Destino    : $driveDir" -ForegroundColor White
Write-Host "  Procesados : $processedDir" -ForegroundColor White
Write-Host "============================================================" -ForegroundColor Cyan

# Memoria de archivos sincronizados para evitar re-subidas si Make los mueve o borra
$syncedHistory = @{}

function Load-History {
    if (Test-Path $historyFile) {
        Get-Content $historyFile -ErrorAction SilentlyContinue | ForEach-Object {
            if ($_ -match '^([^|]+)\|(.+)$') {
                $syncedHistory[$matches[1]] = $matches[2]
            }
        }
    }
}

function Save-History {
    try {
        $lines = $syncedHistory.GetEnumerator() | ForEach-Object { "$($_.Key)|$($_.Value)" }
        Set-Content -Path $historyFile -Value $lines -Encoding utf8 -Force
    } catch {}
}

# Inicializar historial
Load-History

# Determinar si un archivo necesita sincronizarse
function Should-SyncFile($fileItem) {
    $fileName = $fileItem.Name
    $fileTime = $fileItem.LastWriteTime.ToString("o")

    # 1. Ya se sincronizó esta versión exacta (según historial local)
    if ($syncedHistory.ContainsKey($fileName) -and $syncedHistory[$fileName] -eq $fileTime) {
        # Si ya está en la raíz de Drive, no hacer nada
        if (Test-Path (Join-Path $driveDir $fileName)) {
            return $false
        }
        # Si fue movido a 'Procesados' por Make.com, ¡NO volver a subir!
        if (Test-Path (Join-Path $processedDir $fileName)) {
            return $false
        }
        # Si ya no está en la raíz porque Make lo procesó/movió, tampoco re-subir
        return $false
    }

    # 2. Si no estaba en historial, pero ya existe físicamente en 'Procesados'
    if (Test-Path (Join-Path $processedDir $fileName)) {
        # Registrar en historial para futuras pasadas y no re-subir
        $syncedHistory[$fileName] = $fileTime
        Save-History
        return $false
    }

    # 3. Si ya existe en la raíz de Drive con el mismo tamaño
    $destPath = Join-Path $driveDir $fileName
    if (Test-Path $destPath) {
        $destItem = Get-Item $destPath -ErrorAction SilentlyContinue
        if ($destItem -and $destItem.Length -eq $fileItem.Length) {
            $syncedHistory[$fileName] = $fileTime
            Save-History
            return $false
        }
    }

    # Requiere sincronizarse (archivo nuevo o re-renderizado)
    return $true
}

# Función para copiar archivos pendientes (.mp4 y .json)
function Sync-PendingFiles {
    if (-not (Test-Path $driveDir)) {
        Write-Warning "No se encuentra la unidad Google Drive en: $driveDir"
        return
    }

    Get-ChildItem -Path $sourceDir -File -ErrorAction SilentlyContinue | Where-Object { $_.Extension -in '.mp4', '.json' } | ForEach-Object {
        if (Should-SyncFile $_) {
            $destPath = Join-Path $driveDir $_.Name
            try {
                Copy-Item -Path $_.FullName -Destination $destPath -Force
                $syncedHistory[$_.Name] = $_.LastWriteTime.ToString("o")
                Save-History
                Write-Host "[+] Archivo sincronizado con Google Drive: $($_.Name)" -ForegroundColor Green
            } catch {
                Write-Warning "Error al copiar $($_.Name): $_"
            }
        }
    }
}

# Función para limpiar videos locales con más de 48 horas de antigüedad
function Cleanup-OldVideos {
    $cutoff = (Get-Date).AddHours(-48)
    if (Test-Path $sourceDir) {
        Get-ChildItem -Path $sourceDir -File -ErrorAction SilentlyContinue | Where-Object { $_.LastWriteTime -lt $cutoff -and $_.Name -ne ".synced_history.txt" } | ForEach-Object {
            try {
                Remove-Item $_.FullName -Force
                $syncedHistory.Remove($_.Name)
                Write-Host "[-] Archivo local purgado (>48h): $($_.Name)" -ForegroundColor DarkGray
            } catch {
                Write-Warning "No se pudo eliminar $($_.Name): $_"
            }
        }
        Save-History
    }
}

# 1. Limpieza y sincronización de archivos existentes
Cleanup-OldVideos
Sync-PendingFiles

# 2. Configurar vigilante en tiempo real
$watcher = New-Object System.IO.FileSystemWatcher
$watcher.Path = $sourceDir
$watcher.Filter = "*.*"
$watcher.NotifyFilter = [System.IO.NotifyFilters]::FileName -bor [System.IO.NotifyFilters]::LastWrite
$watcher.EnableRaisingEvents = $true

$action = {
    param($sender, $e)
    $fileName = $e.Name
    $filePath = $e.FullPath
    $ext = [System.IO.Path]::GetExtension($fileName).ToLower()
    
    if ($ext -notin '.mp4', '.json') {
        return
    }

    # Pequeña pausa para permitir que termine de escribir el archivo completo
    Start-Sleep -Seconds 2
    
    if (-not (Test-Path $filePath)) { return }
    $fileItem = Get-Item $filePath -ErrorAction SilentlyContinue
    if (-not $fileItem) { return }

    if (Should-SyncFile $fileItem) {
        $driveTarget = Join-Path $driveDir $fileName
        try {
            Copy-Item -Path $filePath -Destination $driveTarget -Force
            $syncedHistory[$fileName] = $fileItem.LastWriteTime.ToString("o")
            Save-History
            Write-Host "`n[+] NUEVO ARCHIVO ENVIADO A GOOGLE DRIVE!" -ForegroundColor Green
            Write-Host "    Archivo: $fileName" -ForegroundColor White
        } catch {
            Write-Warning "Error al sincronizar $($fileName): $_"
        }
    }
}

$createdEvent = Register-ObjectEvent -InputObject $watcher -EventName "Created" -Action $action

Write-Host "`nVigilando carpeta en segundo plano con proteccion anti-duplicados." -ForegroundColor Gray
Write-Host "Puedes minimizar esta ventana. Presiona Ctrl + C para detener.`n" -ForegroundColor Gray

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
