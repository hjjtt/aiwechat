@echo off
setlocal

echo ========================================
echo   Stop Nacos + Sentinel
echo ========================================
echo.

echo [1/2] Stopping Sentinel ...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":8999 " ^| findstr "LISTENING"') do (
    taskkill /pid %%a /f >nul 2>&1
    echo [OK] Sentinel stopped (PID: %%a)
)
echo.

echo [2/2] Stopping Nacos ...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":8848 " ^| findstr "LISTENING"') do (
    taskkill /pid %%a /f >nul 2>&1
    echo [OK] Nacos stopped (PID: %%a)
)
echo.

echo ========================================
echo   All infrastructure stopped
echo ========================================
echo.
pause
