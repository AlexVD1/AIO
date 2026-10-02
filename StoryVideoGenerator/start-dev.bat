@echo off
setlocal enabledelayedexpansion
title StoryVideoGenerator — Master Launcher

echo ==========================================================
echo  StoryVideoGenerator — Iniciando Todo el Sistema
echo ==========================================================

REM 1. Asegurar WSL y contenedor PostgreSQL
echo [1/4] Verificando PostgreSQL en Podman WSL...
wsl -d podman-machine-default -u root sh -c "nohup sleep 86400 >/dev/null 2>&1 &" >nul 2>&1
wsl -d podman-machine-default -u root podman --cgroup-manager=cgroupfs start story-postgres >nul 2>&1
echo        PostgreSQL activo en contenedor story-postgres.

REM 2. Resolver IP de Podman WSL
echo [2/4] Resolviendo direccion de red de la base de datos...
set WSL_IP=127.0.0.1
for /f "tokens=2 delims= " %%a in ('wsl -d podman-machine-default ip -4 addr show eth0 2^>nul ^| findstr /i "inet "') do (
    for /f "tokens=1 delims=/" %%b in ("%%a") do set WSL_IP=%%b
)
echo        IP de base de datos: !WSL_IP!:5433

set DATABASE_URL=jdbc:postgresql://!WSL_IP!:5433/storydb
set DATABASE_USERNAME=story_user
set DATABASE_PASSWORD=CAMBIAR_POR_PASSWORD_SEGURO

if exist ".env" (
    for /f "usebackq tokens=1,* delims==" %%i in (".env") do (
        if /i "%%i"=="AI_API_KEY" set "AI_API_KEY=%%j"
        if /i "%%i"=="AI_MODEL" set "AI_MODEL=%%j"
        if /i "%%i"=="DATABASE_PASSWORD" set "DATABASE_PASSWORD=%%j"
    )
)
if not defined AI_MODEL set AI_MODEL=gemini-2.5-flash

REM 3. Iniciar Stable Diffusion Forge si no esta activo
echo [3/4] Verificando Stable Diffusion Forge en C:\SD_Forge...
curl.exe -s -o NUL -w "%%{http_code}" http://localhost:7860/ >nul 2>&1
if %errorlevel% neq 0 (
    if exist "C:\SD_Forge\run.bat" (
        echo        Iniciando Stable Diffusion Forge en ventana separada...
        start "Stable Diffusion Forge (GPU)" /d "C:\SD_Forge" cmd.exe /c run.bat
    ) else (
        echo        [AVISO] No se encontro C:\SD_Forge\run.bat. Modo respaldo activo.
    )
) else (
    echo        Stable Diffusion ya esta respondiendo en puerto 7860.
)

REM 4. Abrir navegador e iniciar Spring Boot
echo [4/4] Iniciando Backend y Dashboard Web...
echo ==========================================================
echo  Dashboard: http://localhost:8081/
echo  Health:    http://localhost:8081/api/v1/health
echo ==========================================================

start "" "http://localhost:8081/"

mvn spring-boot:run
