# DEV NOTES - ChessIA

## 🚀 Estado del Proyecto
Se han estabilizado las dependencias y la configuración de Ktor para que la aplicación sea verdaderamente **Kotlin Multiplatform (Android, Desktop, Wasm)**. 

### ✅ Validaciones Realizadas
1. **Desktop**: Compila y arranca usando `CIO` como engine Ktor de fallback o autoselección por el plugin. Eliminados warnings o colisiones de serialización y claves crudas.
2. **Android**: Compila en modo `assembleDebug` y está listo para lanzarse en emuladores/dispositivos.
3. **Web (Wasm)**: Compila y se corrigió la dependencia de `ktor-client-cio` que impedía la construcción para Web. Ahora usa el fetch por defecto del entorno browser sin problemas.
4. **IA y Robustez**: 
    - Las API keys (Gemini, DeepSeek, Groq) se han retirado de producción (ya no están en código fuente).
    - Se implementó un loop de reintentos al consultar a la IA.
    - Se agregó validación cruzada del movimiento: El engine local (`chesskt` Game object) ahora verifica explícitamente (`game.move()`) si el string UCI entregado por la IA representa un movimiento legal antes de aplicarlo visualmente. 
    - Fallback de consola/logs: Si la IA falla 3 veces en devolver algo útil o legal, el juego frena y emite un warning claro por consola en vez de crashear o quedar en estado inconsistente.
5. **Logs de Observabilidad**: Se integraron logs `println` con métricas de reintento, formato de respuesta raw (AI), y confirmación de parseo UCI/Legalidad. En un dispositivo Android aparecerán en el Logcat (tag `System.out`), en Desktop y Web en las consolas/herramientas de desarrollador correspondientes.

---

## 🔧 Cómo Ejecutar y Diagnosticar

He incluido scripts `.bat` para usuarios Windows en la raíz del proyecto para simplificar el proceso:

### 1. Ejecutar en Desktop
*Abre una consola y ejecuta:*
```bat
run_desktop.bat
```
*También puedes usar:* `./gradlew :composeApp:run`

### 2. Ejecutar Web (Navegador vía Wasm)
*Abre una consola y ejecuta:*
```bat
run_web.bat
```
*También puedes usar:* `./gradlew :composeApp:wasmJsBrowserDevelopmentRun`
*Nota: Abre tu navegador en la URL `http://localhost:8080/`.*

### 3. Ejecutar en Android
Asegúrate de tener tu emulador iniciado o un celular conectado (USB Debugging activo).
*Abre una consola y ejecuta:*
```bat
run_android.bat
```
*Este script compila el APK, lo instala usando `adb` y lo arranca automáticamente.*

### 4. Validar el Build de todas las plataformas
*Para asegurarte de no romper el soporte a ninguna plataforma antes de commitear, ejecuta:*
```bat
build_all_check.bat
```

---

## ⚙️ Configuración de API Keys (Modo Desarrollo / Diagnóstico)
1. Al seleccionar un modo que incluya "IA" (`🤖 IA vs 🤖 IA`, o `🧑 Player vs 🤖 IA`), la pantalla de `Configuración` (Settings) desplegará el tipo de IA seleccionada.
2. Justo debajo aparecerá un campo de texto: `API Key para [Proveedor]`. 
3. Pega ahí tu API Key y presiona "Iniciar partida". 
4. Estas keys **NO se guardan en código**, y desaparecerán al cerrar la app para evitar fugas.

## 🕵️ Cómo Interpretar Logs de Diagnóstico
Los logs de estado del juego y peticiones se imprimen de la siguiente manera:
- **Intento de conexión:** `Requesting move from AI (attempt X/3). FEN: <current_fen>`
- **Respuesta cruda de la IA:** `Raw AI response: <response>`
- **Validación del motor local:** 
  - `Valid move executed: <uci>` (Todo correcto).
  - `Engine rejected the move: <uci> (Illegal)` (La IA envió un movimiento prohibido, el sistema reintentará).
  - `Cannot parse matched UCI move...` (Falló el formato).
- **Fallo total:** `Critical: AI failed to provide a valid move after 3 attempts.`

(En Wasm, ve a la pestaña `Console` (F12). En Android, busca los strings en el `Logcat` de Android Studio).
