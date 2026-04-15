@echo off
echo ==========================================
echo Starting ChessIA Desktop...
echo ==========================================
call gradlew :composeApp:run
if %errorlevel% neq 0 (
    echo [ERROR] Desktop failed to run.
) else (
    echo [SUCCESS] Desktop run finished.
)
pause
