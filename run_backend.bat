@echo off
echo ==========================================
echo Starting ChessIA Multiplayer Server...
echo ==========================================
set PORT=8081
call gradlew :server:run
if %errorlevel% neq 0 (
    echo [ERROR] Server failed to start.
) else (
    echo [SUCCESS] Server stopped.
)
pause
