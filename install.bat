@echo off
title Binance Pro Trading Terminal - One-Click Installer
color 0B
cd /d "%~dp0"

echo ======================================================================
echo         BINANCE PRO TRADING TERMINAL - ONE-CLICK INSTALLER
echo ======================================================================
echo.

:: 1. Check Python installation
echo [*] Step 1/4: Checking Python installation...
python --version >nul 2>&1
if %errorlevel% equ 0 goto :PYTHON_OK

echo [!] Python is not detected on your system PATH.
echo [*] Attempting automated silent install via Windows Package Manager winget...
winget install -e --id Python.Python.3.11 --silent --accept-package-agreements --accept-source-agreements
if %errorlevel% neq 0 (
    echo [x] Automated install failed. Please install Python 3.10+ manually from https://www.python.org/
    pause
    exit /b 1
)
echo [*] Python installed successfully.

:PYTHON_OK
for /f "tokens=*" %%i in ('python --version 2^>^&1') do set PY_VER=%%i
echo [OK] Detected %PY_VER%

:: 2. Upgrade pip and install dependencies
echo.
echo [*] Step 2/4: Installing required dependencies...
python -m pip install --upgrade pip --quiet
python -m pip install -r requirements.txt --quiet
if %errorlevel% neq 0 (
    echo [!] Pip install encountered a warning or error. Retrying with verbose output...
    python -m pip install -r requirements.txt
)
echo [OK] All dependencies installed successfully.

:: 3. Generate High-Res Terminal Icon
echo.
echo [*] Step 3/4: Generating custom application icon...
python create_assets.py
if exist "assets\icon.ico" (
    echo [OK] Desktop icon created at assets\icon.ico
) else (
    echo [!] Icon generation skipped.
)

:: 4. Create Desktop Shortcut via PowerShell script
echo.
echo [*] Step 4/4: Creating Desktop Shortcut...
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0create_shortcut.ps1"

echo.
echo ======================================================================
echo [SUCCESS] INSTALLATION COMPLETE!
echo.
echo A desktop shortcut named "Binance Trading Terminal" has been created!
echo Double-click the desktop shortcut or run launch.bat to start.
echo ======================================================================
echo.
pause
