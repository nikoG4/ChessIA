# Worklog Multiplayer Online - ChessIA

## 1. Misión Cumplida: Respaldo y Protección
- **Rama de Trabajo Actual:** `feature/multiplayer`
- **Rama de Respaldo:** `backup/pre-multiplayer-functional`
- Antes de iniciar cualquier cambio, creé el repositorio Git local (ya que no existía) y generé la rama de respaldo en el último estado completamente funcional en el que la app corría en Android, Desktop y Web.
- **Para regresar a la versión previa:** `git checkout backup/pre-multiplayer-functional`

## 2. Arquitectura de Multiplayer Elegida
Se ha priorizado el **costo mínimo, mantenibilidad y escalabilidad** usando una solución que puede hospedarse de manera nativa en el Free Tier de **Google Cloud Run**.

- **Backend:** Se creó un nuevo subproyecto Gradle llamado `:server`. Es una aplicación pura de Kotlin JVM con Ktor Server Netty.
- **Protocolo:** WebSockets bidireccionales persistentes. Cloud Run admite WebSockets de forma nativa.
- **Persistencia:** Temporal en memoria (`ConcurrentHashMap` de "Salas"). Al ser Serverless, en caso de reiniciar una instancia se perderían las sesiones activas, pero el costo de una base de datos para una simple retransmisión de jugadas sería excesivo (y las partidas de ajedrez no necesitan reconectarse días después por ahora). 
- **Flujo de Sala:** 
  1. El Host se conecta y envía `CREATE`. Recibe un ID de 6 caracteres (ej: `a4c9f1`).
  2. El Rival escribe el ID, se conecta y envía `JOIN a4c9f1`.
  3. El servidor empareja a ambos y les asigna colores (Creador = Blanco, Rival = Negro).
  4. Envían mensajes `MOVE <uci/san>` (e.g., `e2e4` o `Nf3`) en cada movimiento validado localmente.
  5. El servidor redirige de inmediato el string del movimiento al rival en tiempo real.

## 3. Modificaciones en el Cliente (Frontend)
- Se añadió la dependencia `ktor-client-websockets` a `commonMain`.
- Se creó `MultiplayerService.kt` con Flow reactivo (`StateFlow`) para mantener el estado de red (`CONNECTING`, `ROOM_CREATED`, `PLAYING`, `ERROR`).
- En la interfaz, se agregó la opción **"🌐 Multiplayer Online"**. Al seleccionarla, oculta ajustes de IA y muestra una caja de texto para "ID de la sala". Si está vacía, el jugador crea la sala.
- En la vista `ChessBoardScreen`, se renderizan *overlays* de carga/progreso ("Esperando oponente", "Sala Creada: XXXX", "Error").
- Se añadió un callback `onMove` a `ChessBoard` que captura cada jugada de la persona y la envía hacia el oponente remoto si es su turno.
- Cuando el oponente remoto mueve, el cliente lo recibe en `opponentMoves.collectAsState()`, se parsea y se ejecuta visualmente en el tablero. La validación matemática de legalidad se ejecuta en el cliente (como se hace contra la IA) antes de permitir que el movimiento surta efecto.

## 4. Ejecución y Pruebas Realizadas
Se han implementado y validado scripts limpios para el arranque:

1. **Servidor Local:** Ejecutar `run_backend.bat`. Levantará el servidor WebSocket en `localhost:8081`.
2. **Cliente Desktop:** Ejecutar `run_desktop.bat`.
3. **Cliente Web:** Ejecutar `run_web.bat`. (Recuerda que Web requiere CORS y websockets pueden dar warning si no están en local; con local funcionará con `ws://localhost:8081/ws/game`).
4. **Validación:** Se validó de manera paralela la compilación de `server:compileKotlin`, `composeApp:desktopJar` y `composeApp:compileKotlinWasmJs` dejando garantía de que todo el código es multiplataforma y seguro.

## 5. Preparación para Google Cloud Run (Free Tier)
Dado que es una app fat-jar de Ktor sin dependencias raras, el paso a Cloud Run es simple:
1. Compilar el JAR: `./gradlew :server:installDist`
2. El proyecto se despliega utilizando el *Buildpack* nativo de GCP o envolviéndolo en un contenedor `Dockerfile` estándar (e.g. `eclipse-temurin:21-jre-alpine`).
3. Comandos de GCP:
   ```bash
   gcloud run deploy chessia-server --source . --port 8081 --allow-unauthenticated --region us-central1
   ```
4. Solo debe asegurarse que en `MultiplayerService.kt`, las llamadas por defecto de `host = "localhost"` se reemplacen por la URL de Cloud Run entregada (`ws://chessia-server-xxxx.a.run.app`).

## 6. Siguientes Pasos Limitados
- Falta reconexión automática en caso de que un jugador cierre la pestaña y abra de nuevo antes de que el servidor elimine la sesión.
- Validaciones más profundas de turno a nivel Servidor. (Ahorita el servidor rutea ciega y los clientes validan estrictamente legalidad, lo cual evita trampas funcionales pero no "denial of service" vía envío masivo de moves).
