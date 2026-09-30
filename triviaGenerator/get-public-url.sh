#!/usr/bin/env bash
# ============================================================
# get-public-url.sh — Obtener URL pública de trivia-api en Linux/WSL
# ============================================================

FOUND=0

# 1. Verificar Ngrok
if podman ps --filter "name=trivia-tunnel-ngrok" --format "{{.Status}}" | grep -q "Up"; then
    FOUND=1
    DOMAIN="either-provided-figment.ngrok-free.dev"
    if [ -f ".env" ]; then
        VAL=$(grep -E '^NGROK_DOMAIN=' .env | cut -d '=' -f2 | tr -d ' "\r')
        if [ -n "$VAL" ]; then
            DOMAIN="$VAL"
        fi
    fi
    URL="https://${DOMAIN}"

    echo ""
    echo -e "\033[36m============================================================\033[0m"
    echo -e "\033[32m  trivia-api -- DOMINIO FIJO Y PERSISTENTE (NGROK)\033[0m"
    echo -e "\033[36m============================================================\033[0m"
    echo -e "  URL Fija    : \033[1m${URL}\033[0m"
    echo -e "  Web SPA     : ${URL}/"
    echo -e "  API Docs    : ${URL}/swagger-ui.html"
    echo -e "  Health      : ${URL}/api/v1/health"
    echo -e "\033[36m============================================================\033[0m"

    echo -n "[*] Verificando conectividad con tu dominio persistente... "
    STATUS=$(curl -s -m 10 -H "ngrok-skip-browser-warning: true" "${URL}/api/v1/health" | grep -o '"status":"UP"')
    if [ -n "$STATUS" ]; then
        echo -e "\033[32m[OK] En linea\033[0m"
    else
        echo -e "\033[33m[ADVERTENCIA] No respondio status UP\033[0m"
    fi
fi

# 2. Verificar Cloudflare
if podman ps --filter "name=trivia-tunnel-cloudflare" --format "{{.Status}}" | grep -q "Up"; then
    FOUND=1
    CF_URL=$(podman logs trivia-tunnel-cloudflare 2>&1 | grep -o 'https://[a-zA-Z0-9.-]*\.trycloudflare\.com' | tail -n 1)
    if [ -n "$CF_URL" ]; then
        echo ""
        echo -e "\033[36m============================================================\033[0m"
        echo -e "\033[33m  trivia-api -- TUNEL SECUNDARIO (CLOUDFLARE - SIN AVISO)\033[0m"
        echo -e "\033[36m============================================================\033[0m"
        echo -e "  URL         : \033[1m${CF_URL}\033[0m"
        echo -e "  Web SPA     : ${CF_URL}/"
        echo -e "  Health      : ${CF_URL}/api/v1/health"
        echo -e "\033[36m============================================================\033[0m"
    fi
fi

if [ $FOUND -eq 0 ]; then
    echo -e "\033[33m[!] Ningun tunel de acceso publico esta en ejecucion.\033[0m"
    echo -e "\033[36m    Inicia los servicios con: podman compose up -d\033[0m"
    exit 1
fi
