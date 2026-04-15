@echo off
echo ==========================================
echo Building and running ChessIA Android...
echo ==========================================
echo 1. Building APK
call gradlew :composeApp:assembleDebug
if %errorlevel% neq 0 (
    echo [ERROR] Android build failed.
    pause
    exit /b %errorlevel%
)

echo 2. Installing on default connected device/emulator
adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
if %errorlevel% neq 0 (
    echo [ERROR] Failed to install APK via ADB. Is a device connected?
    pause
    exit /b %errorlevel%
)

echo 3. Launching App
adb shell am start -n org.nko.chessia/org.nko.chessia.MainActivity
echo [SUCCESS] Android app launched. Check device screen and adb logcat.
pause
