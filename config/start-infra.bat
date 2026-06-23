@echo off
setlocal EnableDelayedExpansion

set "SCRIPT_DIR=%~dp0"
set "NACOS_HOME=%SCRIPT_DIR%nacos"
set "SENTINEL_JAR=%SCRIPT_DIR%sentinel-dashboard-1.7.1.jar"

echo ========================================
echo   Start Nacos + Sentinel
echo ========================================
echo.

:: ========== Cleanup old processes ==========
echo [0] Cleaning up old processes ...

for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":8848 " ^| findstr "LISTENING"') do (
    taskkill /pid %%a /f >nul 2>&1
    echo     Killed old Nacos (PID: %%a)
)
for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":8080 " ^| findstr "LISTENING"') do (
    taskkill /pid %%a /f >nul 2>&1
    echo     Killed old process on 8080 (PID: %%a)
)
for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":8999 " ^| findstr "LISTENING"') do (
    taskkill /pid %%a /f >nul 2>&1
    echo     Killed old Sentinel (PID: %%a)
)
echo     Cleanup done.
echo.

:: ========== Start Nacos ==========
echo [1/2] Starting Nacos (standalone) ...
if not exist "%NACOS_HOME%\bin\startup.cmd" (
    echo [ERROR] Nacos not found: %NACOS_HOME%\bin\startup.cmd
    goto sentinel
)

pushd "%NACOS_HOME%\bin"
start "Nacos Server" cmd /c startup.cmd -m standalone
popd
echo [OK] Nacos started (standalone)
echo      Console: http://localhost:8848/nacos
echo.

:: ========== Start Sentinel ==========
:sentinel
echo [2/2] Starting Sentinel Dashboard (port=8999) ...
if not exist "%SENTINEL_JAR%" (
    echo [ERROR] Sentinel JAR not found: %SENTINEL_JAR%
    goto end
)

pushd "%SCRIPT_DIR%"
start "Sentinel Dashboard" cmd /c java --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED -jar sentinel-dashboard-1.7.1.jar --server.port=8999
popd
echo [OK] Sentinel started
echo      Console: http://localhost:8999
echo.

:: ========== Done ==========
:end
echo ========================================
echo   Done!
echo   Nacos    : http://localhost:8848/nacos
echo   Sentinel : http://localhost:8999
echo ========================================
echo.
pause
