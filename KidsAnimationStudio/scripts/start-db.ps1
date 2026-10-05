# ============================================================
# KidsAnimationStudio - Paso 1: Iniciar PostgreSQL
# Contenedor dedicado kids-postgres (puerto 5434)
# ============================================================
Set-Location (Join-Path $PSScriptRoot "..")
Write-Host "Levantando contenedor kids-postgres en Podman WSL..." -ForegroundColor Yellow

try {
    wsl -d podman-machine-default -u root sh -c "nohup sleep 86400 >/dev/null 2>&1 &" 2>$null | Out-Null
    wsl -d podman-machine-default -u root podman --cgroup-manager=cgroupfs start kids-postgres 2>$null | Out-Null
    Write-Host "[OK] Contenedor kids-postgres iniciado." -ForegroundColor Green
} catch {
    podman compose -f compose.yaml up -d postgres
}

$dbHost = "127.0.0.1"
try {
    $ipOutput = wsl -d podman-machine-default ip -4 addr show eth0 2>$null
    if (($ipOutput -join "`n") -match 'inet\s+([0-9]+\.[0-9]+\.[0-9]+\.[0-9]+)') {
        $dbHost = $matches[1]
    }
} catch {}

Write-Host "PostgreSQL disponible en:" -ForegroundColor Cyan
Write-Host "  Host WSL:  ${dbHost}:5434" -ForegroundColor White
Write-Host "  Base:      kidsdb" -ForegroundColor White
Write-Host "  Usuario:   kids_user" -ForegroundColor White
Write-Host "  JDBC URL:  jdbc:postgresql://${dbHost}:5434/kidsdb" -ForegroundColor Green
