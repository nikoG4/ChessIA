# ChessIA — Contexto del Proyecto

## Descripción General

**ChessIA** es una aplicación de ajedrez multijugador e IA desarrollada con **Kotlin Multiplatform (KMP)** y **Compose Multiplatform**. Permite jugar partidas de ajedrez en múltiples plataformas, incluyendo Android, Desktop (JVM) e iOS, con soporte experimental para Web (Kotlin/Wasm). El punto diferenciador es la integración con proveedores de IA externos (Gemini, Groq, DeepSeek) que sirven como oponentes virtuales.

---

## Arquitectura del Proyecto

### Módulos Principales

```
ChessIA/
├── composeApp/          ← Aplicación principal Compose Multiplatform
│   └── src/
│       ├── commonMain/  ← Código compartido entre todas las plataformas
│       ├── androidMain/ ← Código específico de Android
│       ├── desktopMain/ ← Código específico de Desktop (JVM)
│       ├── iosMain/     ← Código específico de iOS
│       └── wasmJsMain/  ← Código específico de Web (Wasm)
├── chesskt/             ← Motor de ajedrez embebido (biblioteca externa chess.kt)
│   └── shared/src/
│       └── commonMain/  ← Lógica de ajedrez multiplataforma
├── iosApp/              ← Entry point nativo iOS (SwiftUI)
└── gradle/              ← Configuración de versiones (libs.versions.toml)
```

### Stack Tecnológico

| Tecnología | Versión | Rol |
|---|---|---|
| Kotlin Multiplatform | 2.1.10 | Framework base |
| Compose Multiplatform | 1.7.3 | UI declarativa multiplataforma |
| Ktor Client | 3.1.2 | HTTP client multiplataforma para APIs IA |
| kotlinx.serialization | 1.7.3 / 1.8.1 | Serialización JSON |
| kotlinx.coroutines | 1.10.1 | Programación asíncrona |
| Android Gradle Plugin | 8.5.2 | Build Android |
| Compose Compiler Plugin | 2.1.10 | Plugin compilador Compose |

---

## Módulo `composeApp` — Aplicación Principal

### Package base: `org.nko.chessia`

### Estructura de packages

```
org.nko.chessia/
├── App.kt                  ← Composable raíz, maneja la navegación entre pantallas
├── Platform.kt             ← Interface expect/actual para info de plataforma
├── Greeting.kt             ← Greeting helper (boilerplate KMP)
├── models/
│   ├── AIProvider.kt       ← Data class que representa un proveedor de IA
│   └── Extractor.kt        ← Data class para estrategia de extracción de respuesta AI
├── services/
│   └── GameService.kt      ← Servicio que conecta con APIs de IA externas
└── ui/
    ├── Main.kt             ← Pantallas: GameModeSelector, GameSettingsScreen, DropdownMenuBox
    ├── ChessGameScreen.kt  ← Pantalla principal del juego (ChessBoardScreen)
    ├── ChessBoard.kt       ← Componente tablero + lógica de turno IA
    ├── ChessCell.kt        ← Celda individual del tablero
    └── CapturedPieces.kt   ← Visualización de piezas capturadas
```

---

## Flujo de Pantallas (Navegación)

La navegación es manejada manualmente con un `var currentScreen: String` en el Composable `App`:

```
"selector"  →  GameModeSelector      (Selección de modo de juego)
     ↓
"settings"  →  GameSettingsScreen    (Configuración: IA, dificultad, timer)
     ↓
"board"     →  ChessBoardScreen      (Partida activa)
```

No se usa un framework de navegación (como NavController). La navegación es por estados simples de string.

---

## Modos de Juego

Definidos como strings en `GameModeSelector`:

| Modo | Descripción |
|---|---|
| `🧑 Player vs 🧑 Player` | Dos jugadores humanos en el mismo dispositivo |
| `🧑 Player vs 🤖 IA` | Humano contra IA |
| `🤖 IA vs 🤖 IA` | IA automática contra sí misma |
| `🤖 IA vs 🧑🤖 Asistido` | IA vs jugador asistido por IA |
| `🧑🤖 Asistido vs 🧑🤖 Asistido` | Ambos jugadores asistidos |

> ⚠️ **Nota**: Solo el modo `Player vs IA` está completamente implementado con llamadas a la API. Los modos con "Asistido" e "IA vs IA" están definidos en la UI pero su lógica puede requerir desarrollo adicional.

---

## Modelos de Datos

### `AIProvider`
```kotlin
data class AIProvider(
    val name: String,           // "Gemini" | "Groq" | "DeepSeek"
    val endpoint: String,       // URL del API (soporta {{apiKey}})
    val method: String,         // "POST"
    val headers: Map<String, String>,
    val bodyTemplate: String,   // JSON template con {{fen}}, {{difficulty}}, {{apiKey}}
    val extract: Extractor,     // Estrategia para extraer el movimiento de la respuesta
    val apiKey: String = ""
)
```

### `Extractor`
```kotlin
data class Extractor(
    val type: String,           // "regex" | "jsonpath"
    val pattern: String? = null,// Para type="regex"
    val path: String? = null    // Para type="jsonpath" (e.g. "$.candidates[0].content.parts[0].text")
)
```

### `HighlightedSquare`
```kotlin
data class HighlightedSquare(
    val square: Square,
    val isCastle: Boolean = false,
    val isPromotion: Boolean = false
)
```

---

## Servicio de IA — `GameService`

El servicio centraliza la comunicación con APIs externas:

- **HTTP Client**: Ktor con engine `CIO` (solo JVM/Desktop — ⚠️ ver limitaciones Wasm)
- **Request**: Reemplaza placeholders `{{fen}}`, `{{difficulty}}`, `{{apiKey}}` en el `bodyTemplate`
- **Response Parsing**: Soporta extracción por `jsonpath` (con soporte de arrays `[n]`) o `regex`

### Proveedores Configurados

| Proveedor | Modelo | Endpoint |
|---|---|---|
| **Gemini** | `gemini-2.0-flash` | `generativelanguage.googleapis.com` |
| **Groq** | `mixtral-8x7b-32768` | `api.groq.com` |
| **DeepSeek** | `deepseek-chat` | `api.deepseek.com` |

> 🔑 **Seguridad**: Las API keys están hardcodeadas en `GameService.kt`. Esto debe cambiarse antes de producción (mover a variables de entorno o configuración segura).

---

## Motor de Ajedrez — Módulo `chesskt`

Biblioteca externa embebida **chess.kt** (de [krossovochkin](https://github.com/krossovochkin/chess.kt)), adaptada como sub-proyecto local.

### Clases clave (package: `com.krossovochkin.chess`)

| Clase | Descripción |
|---|---|
| `Game` | Estado completo de la partida. Se crea con `Game.create(fen)` |
| `Board` | Tablero 8x8. Usa `board.get(square)` para obtener pieza |
| `Move` | Tipos: `GeneralMove`, `CastleMove`, `PromotionMove` |
| `Piece` | Con `Piece.Type` (Pawn, Rook, Knight, Bishop, Queen, King) y `Piece.Color` (White, Black) |
| `Square` | Coordenada `(file: Int, rank: Int)` (0-indexed) |
| `FenSerializer` | `FenSerializer.serialize(state)` convierte el estado a FEN string |
| `GameResult` | Resultados: `Draw`, Checkmate, etc. |
| `GameState` | Estado serializable del juego |

### Extensiones útiles
```kotlin
"e2e4".asMove()     // String → Move (notación UCI)
"e4".asSquare()     // String → Square (notación algebraica)
```

### Import path en composeApp
Los archivos de chess.kt son referenciados con un package re-exportado:
```kotlin
import org.nko.chessia.com.github.krossovochkin.chess.Game
import org.nko.chessia.com.github.krossovochkin.chess.fen.FenSerializer
```

---

## Lógica del Tablero (`ChessBoard.kt`)

### Turno de la IA
- Se detecta con `isOpponentTurn`: verifica si el color que debe mover es el contrario al jugador humano.
- Se usa `LaunchedEffect(isOpponentTurn, fen)` para disparar la llamada a la API cuando es el turno de la IA.
- La llamada se ejecuta en `Dispatchers.Default`.
- El movimiento UCI devuelto por la IA se convierte con `.asMove()` y se ejecuta con `game.move(move)`.

### Rotación del tablero
- El tablero se orienta según el color del jugador: blancas abajo si `playerColor == White`.
- El color del jugador se asigna aleatoriamente al inicio de cada partida con `Random.nextBoolean()`.

### Promoción de peón
- Al mover un peón a la fila de coronación (rank 0 o 7), se muestra `PromotionDialog`.
- El jugador elige entre Reina, Torre, Alfil o Caballo.

### Jaque
- Se detecta con `game.isCheck` y se resalta en rojo la casilla del rey en jaque.

---

## Configuración de Juego (`GameSettingsScreen`)

Parámetros configurables antes de iniciar la partida:

| Parámetro | Tipo | Default |
|---|---|---|
| `withTimer` | Boolean | false |
| `timerMinutes` | Int | 5 |
| `difficulty` | String | "Media" (opciones: "Fácil", "Media", "Difícil") |
| `selectedAI` | AIProvider? | Primer proveedor (Gemini) |

- El timer se maneja con `LaunchedEffect` y `delay(1000L)` (countdown por segundos).
- Al agotarse el tiempo, se declara ganador al oponente.
- El estado de jaque mate lo detecta `game.isCheckmate`.

---

## Plataformas Soportadas

| Plataforma | Estado | Entry Point |
|---|---|---|
| **Desktop (JVM)** | ✅ Funcional | `org.nko.chessia.MainKt` |
| **Android** | ✅ Configurado | `AndroidManifest.xml`, `minSdk 24`, `targetSdk 35` |
| **iOS** | ✅ Configurado | `iosApp/` (SwiftUI entry point) |
| **Web (WasmJs)** | ⚠️ Experimental | `composeApp:wasmJsBrowserDevelopmentRun` |

> ⚠️ **Limitación Wasm**: El engine HTTP `CIO` de Ktor **no es compatible** con Wasm/Browser. Si se necesita soporte web, se debe usar `ktor-client-js` o un engine compatible con el browser.

---

## Comandos de Build / Ejecución

```bash
# Desktop
./gradlew :composeApp:run

# Android
./gradlew :composeApp:assembleDebug

# Web (Wasm - desarrollo)
./gradlew :composeApp:wasmJsBrowserDevelopmentRun

# Generar distribución Desktop
./gradlew :composeApp:createDistributable
```

---

## Dependencias Clave (`libs.versions.toml`)

```toml
[plugins]
kotlinMultiplatform = { id = "org.jetbrains.kotlin.multiplatform", version = "2.1.10" }
composeMultiplatform = { id = "org.jetbrains.compose", version = "1.7.3" }
androidApplication = { id = "com.android.application", version = "8.5.2" }

[libraries]
ktor-client-core        # HTTP client base
ktor-client-cio         # Engine JVM (⚠️ no compatible con Wasm)
ktor-client-content-negotiation
ktor-serialization-kotlinx-json
kotlinx-serialization-json
kotlinx-coroutines-swing  # Coroutines para Desktop
```

---

## Aspectos Pendientes / Deuda Técnica

- [ ] **API Keys hardcodeadas** en `GameService.kt` → mover a configuración segura o BuildConfig
- [ ] **Engine Ktor CIO** no compatible con WasmJs → reemplazar por engine multiplataforma (`ktor-client-js` o mock)
- [ ] **Modos de juego incompletos**: "IA vs IA", "Asistido vs X" necesitan implementación de lógica
- [ ] **Doble serialización conflictiva**: se declaran dos versiones de `kotlinx-serialization-json` (1.7.3 y 1.8.1), potencial conflicto
- [ ] **NavController ausente**: la navegación manual con strings puede volverse frágil al crecer la app
- [ ] **Archivos `.kt~`**: hay archivos de backup en `services/` y `ui/` que deberían eliminarse del repositorio
- [ ] **`settings.gradle.kts`**: el módulo `chesskt` no está incluido con `include(":chesskt")`, revisar cómo se integra (puede ser como archivos locales copiados)
- [ ] **Firebase Crashlytics**: está como dependencia (`firebase-crashlytics-buildtools`) pero no parece usarse activamente en el código

---

## Convenciones del Código

- **Idioma**: UI y prompts de IA en **español**; código en **inglés**
- **Estado**: Manejo de estado con `remember { mutableStateOf(...) }` (sin ViewModel ni arquitectura MVI/MVVM formal)
- **Coroutines**: Se usa `LaunchedEffect` para side effects, `Dispatchers.Default` para trabajo de IA
- **Formato FEN**: Representación interna del estado del juego. Ejemplo: `rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1`
- **Movimientos UCI**: Formato para comunicación con la IA. Ejemplo: `e2e4`, `e7e8q` (promoción)
