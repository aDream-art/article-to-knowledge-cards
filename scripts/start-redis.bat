@echo off
REM Start the portable Redis used by this project (idempotent).
REM Called manually or by the Startup shortcut; no admin rights required.
setlocal
set "ROOT=%~dp0.."
set "REDIS_DIR=%ROOT%\.tools\redis"

if not exist "%REDIS_DIR%\redis-server.exe" (
    echo [chat] redis-server.exe not found at "%REDIS_DIR%"
    exit /b 1
)

REM Already running? Nothing to do.
netstat -ano | findstr ":6379" | findstr "LISTENING" >nul 2>&1
if %errorlevel%==0 (
    echo [chat] redis already listening on 6379
    exit /b 0
)

start "" /b "%REDIS_DIR%\redis-server.exe" "%REDIS_DIR%\redis.windows.conf" --dir "%REDIS_DIR%" --logfile "%REDIS_DIR%\redis.log"
echo [chat] redis started ^(log: %REDIS_DIR%\redis.log^)
exit /b 0
