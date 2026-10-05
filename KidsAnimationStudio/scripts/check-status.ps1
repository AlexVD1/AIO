# ============================================================
# KidsAnimationStudio - Monitor de Estado de Servicios
# ============================================================
function Check-Endpoint($name, $url, $timeoutSec = 2) {
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        $res = Invoke-RestMethod -Uri $url -TimeoutSec $timeoutSec -ErrorAction Stop
        $sw.Stop()
        return [PSCustomObject]@{
            Servicio = $name
            Estado   = "UP"
            URL      = $url
            Latencia = "$($sw.ElapsedMilliseconds) ms"
        }
    } catch {
        $sw.Stop()
        return [PSCustomObject]@{
            Servicio = $name
            Estado   = "DOWN"
            URL      = $url
            Latencia = "N/A"
        }
    }
}

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  KidsAnimationStudio - Diagnóstico de Servicios Locales" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Base de datos
$dbHost = "127.0.0.1"
try {
    $ipOutput = wsl -d podman-machine-default ip -4 addr show eth0 2>$null
    if (($ipOutput -join "`n") -match 'inet\s+([0-9]+\.[0-9]+\.[0-9]+\.[0-9]+)') { $dbHost = $matches[1] }
} catch {}

$dbStatus = "DOWN"
$dbLatency = "N/A"
$swDb = [System.Diagnostics.Stopwatch]::StartNew()
try {
    $tcp = New-Object System.Net.Sockets.TcpClient
    $iar = $tcp.BeginConnect($dbHost, 5434, $null, $null)
    if ($iar.AsyncWaitHandle.WaitOne(1000, $false)) {
        $tcp.EndConnect($iar)
        $swDb.Stop()
        $dbStatus = "UP"
        $dbLatency = "$($swDb.ElapsedMilliseconds) ms"
    }
    $tcp.Close()
} catch {}

$dbUrlDisplay = "${dbHost}:5434"
$results = @(
    [PSCustomObject]@{ Servicio = "PostgreSQL (kids-postgres)"; Estado = $dbStatus; URL = $dbUrlDisplay; Latencia = $dbLatency },
    (Check-Endpoint "Ollama LLM" "http://127.0.0.1:11434/api/tags" 2),
    (Check-Endpoint "ComfyUI (NVIDIA)" "http://127.0.0.1:8188/system_stats" 2),
    (Check-Endpoint "AI Gateway (FastAPI)" "http://127.0.0.1:8090/health" 3),
    (Check-Endpoint "Orquestador (Spring Boot)" "http://127.0.0.1:8082/api/v1/health" 3)
)

foreach ($r in $results) {
    $color = if ($r.Estado -eq "UP") { "Green" } else { "Red" }
    Write-Host ("  [{0,-4}] {1,-30} | {2,-38} | {3}" -f $r.Estado, $r.Servicio, $r.URL, $r.Latencia) -ForegroundColor $color
}

Write-Host "==========================================================" -ForegroundColor Cyan
