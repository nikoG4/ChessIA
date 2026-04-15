# Plan Multiplayer Online - ChessIA

## 1. Estado Actual y Respaldo
- **Rama de respaldo:** `backup/pre-multiplayer-functional`
- **Estado preservado:** La aplicación funciona en Desktop, Android y Web (Wasm) de forma consistente. Tiene modos de juego local e integración con IA (Gemini, Groq, DeepSeek) con sistema de reintentos y configuración dinámica. 
- **Cómo volver a ella:** `git checkout backup/pre-multiplayer-functional`

## 2. Arquitectura de Multiplayer (Objetivo: Free Tier y Simplicidad)
Implementaremos una arquitectura Cliente-Servidor utilizando **WebSockets** para sincronización en tiempo real. 

### Componentes:
- **Backend (Server):** Aplicación Ktor ligera independiente (embebida en el mismo repositorio bajo el módulo `:server`). 
  - **Transporte:** WebSockets (`io.ktor:ktor-server-websockets`).
  - **Lógica:** Salas en memoria usando `ConcurrentHashMap` o `StateFlow` / Actores de Corrutinas para gestionar las jugadas concurrentes.
  - **Despliegue:** Preparado para desplegarse como contenedor Docker en **Google Cloud Run**, que soporta WebSockets y es ideal para proyectos *Serverless / Free Tier*.
- **Frontend (Cliente KMP):**
  - **Transporte:** Cliente Ktor WebSockets (`io.ktor:ktor-client-websockets`). Funciona correctamente en Android, Desktop y JVM. (WasmJs también soporta WebSockets nativos a través de Ktor).

### Flujo de Datos:
1. **Crear Partida:** Cliente envía mensaje `CREATE_ROOM`, servidor devuelve un `roomId` aleatorio.
2. **Unirse:** Cliente envía `JOIN_ROOM { roomId }`, servidor asocia y notifica a ambos.
3. **Sincronización:** Cada vez que el cliente A mueve, envía `MOVE { uci }`. El servidor lo recibe, verifica que sea el turno correcto de la sala, y reenvía el `MOVE { uci }` al cliente B.
4. **Validación Legal:** Dado que el cliente ya tiene el engine de validación incorporado en `commonMain`, el servidor hará pasarela para evitar infraestructura pesada de ajedrez, pero cada cliente valida si el movimiento recibido es legal.

## 3. Estructura de Módulos a Añadir
- Crear subproyecto `:server` en Gradle.
- Dependencias: `ktor-server-netty`, `ktor-server-websockets`, `ktor-serialization-kotlinx-json`, `logback-classic`.

## 4. Estrategia de Prueba
- Se proveerán scripts para levantar el backend en local (`run_backend.bat`).
- Se probará usando la aplicación Desktop (`run_desktop.bat`) como Cliente 1 y otra instancia u otra plataforma (Web/Android) como Cliente 2.
