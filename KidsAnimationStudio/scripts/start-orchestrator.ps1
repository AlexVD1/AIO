# ============================================================
# KidsAnimationStudio - Paso 5: Iniciar Orquestador Spring Boot
# Puerto 8082 - API REST, Flyway, Pipeline Executor y Dashboard
# ============================================================
Set-Location (Join-Path $PSScriptRoot "..\orchestrator")

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  KidsAnimationStudio - Orquestador Spring Boot 3.3" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# Resolver IP de la base de datos si corre en Podman WSL
$dbHost = "127.0.0.1"
try {
    $ipOutput = wsl -d podman-machine-default ip -4 addr show eth0 2>$null
    if (($ipOutput -join "`n") -match 'inet\s+([0-9]+\.[0-9]+\.[0-9]+\.[0-9]+)') {
        $dbHost = $matches[1]
    }
} catch {}

$dbUrl = "jdbc:postgresql://${dbHost}:5434/kidsdb"
$env:DATABASE_URL = $dbUrl
[System.Environment]::SetEnvironmentVariable("DATABASE_URL", $dbUrl, "Process")

Write-Host "Conectando a base de datos en: $dbUrl" -ForegroundColor Gray
Write-Host "  Dashboard:  http://localhost:8082/" -ForegroundColor Green
Write-Host "  Health:     http://localhost:8082/api/v1/health" -ForegroundColor Green
Write-Host "  Swagger:    http://localhost:8082/swagger-ui.html" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Cyan

# Abrir el navegador en el dashboard
Start-Process "http://localhost:8082/"

mvn spring-boot:run
