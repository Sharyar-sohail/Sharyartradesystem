@echo off
title Binance Pro Trading Terminal Launcher
color 0A
cd /d "%~dp0"

echo ======================================================================
echo           BINANCE PRO TRADING ^& STRATEGY BACKTESTING TERMINAL
echo ======================================================================
echo.
echo [*] Checking Python runtime...
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [!] ERROR: Python is not detected in your PATH.
    echo Please run install.bat first or install Python from https://www.python.org/
    pause
    exit /b 1
)

echo [*] Starting Streamlit GUI Dashboard on http://localhost:8501...
echo [*] Opening your default web browser...

:: Open browser after a brief delay
start "" http://localhost:8501

:: Launch Streamlit server
python -m streamlit run app.py --server.port 8501 --server.headless false --browser.gatherUsageStats false

pause
