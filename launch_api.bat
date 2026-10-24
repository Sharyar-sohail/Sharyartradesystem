@echo off
setlocal
cd /d "%~dp0"
title Binance Quant REST API Service
echo ========================================================
echo   BINANCE STANDALONE REST API SERVICE (FASTAPI)
echo   High-Performance Local / Headless Server
echo ========================================================
echo.
echo [*] Starting Uvicorn API server on http://0.0.0.0:8000 ...
python -m uvicorn backend.api:api_app --host 0.0.0.0 --port 8000
pause
