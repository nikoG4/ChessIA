@echo off
echo ==========================================
echo Validating all platforms build...
echo ==========================================
call gradlew assemble :composeApp:desktopJar :composeApp:compileKotlinWasmJs
if %errorlevel% neq 0 (
    echo [ERROR] Build validation failed.
) else (
    echo [SUCCESS] All target platforms compiled successfully.
)
pause
