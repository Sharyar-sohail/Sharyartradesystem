@echo off
setlocal
cd /d "%~dp0"
title Binance Terminal - Wi-Fi APK Share Server
color 0E

echo ======================================================================
echo       BINANCE PRO TRADING TERMINAL - WI-FI APK SHARE SERVER
echo ======================================================================
echo.
echo [*] Checking Python installation...
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [!] Python is not detected on your system PATH.
    pause
    exit /b 1
)

echo [*] Starting Wi-Fi APK Share Server on port 8080...
python share_apk_wifi.py 8080
pause
