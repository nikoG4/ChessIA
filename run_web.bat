@echo off
echo ==========================================
echo Starting ChessIA Web (Wasm)...
echo ==========================================
call gradlew :composeApp:wasmJsBrowserDevelopmentRun
if %errorlevel% neq 0 (
    echo [ERROR] Web failed to run.
) else (
    echo [SUCCESS] Web run finished.
)
pause
