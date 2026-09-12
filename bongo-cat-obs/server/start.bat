@echo off
chcp 65001 >nul
set PYTHONIOENCODING=utf-8
set PYTHONUNBUFFERED=1
setlocal enabledelayedexpansion
title Bongo Cat Global Server (OBS)
color 0b

echo ===================================================
echo     INICIANDO BONGO CAT PARA OBS STUDIO
echo ===================================================
echo.

cd /d "%~dp0"

:: 1. Verificar Python
python --version >nul 2>&1
if !errorlevel! neq 0 (
    echo [ERROR] No se encontro Python en el sistema.
    echo Por favor instala Python desde https://www.python.org/ y asegurate de marcar Add Python to PATH.
    echo.
    pause
    exit /b 1
)

:: 2. Verificar dependencias
python -c "import pynput, websockets" >nul 2>&1
if !errorlevel! neq 0 (
    echo [1/2] Instalando librerias necesarias pynput y websockets...
    python -m pip install websockets pynput
)

python -c "import pynput, websockets" >nul 2>&1
if !errorlevel! neq 0 (
    echo [ERROR] No se pudieron verificar las dependencias.
    pause
    exit /b 1
)

echo [1/2] Dependencias verificadas correctamente.
echo [2/2] Servidor de captura global iniciando...
echo.

python server.py

if !errorlevel! neq 0 (
    echo.
    echo [AVISO] El servidor se ha cerrado.
    pause
)
