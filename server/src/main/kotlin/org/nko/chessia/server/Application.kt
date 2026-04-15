package org.nko.chessia.server

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.util.concurrent.ConcurrentHashMap
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

fun main() {
    embeddedServer(Netty, port = System.getenv("PORT")?.toInt() ?: 8081, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

class Room(val id: String) {
    var player1: DefaultWebSocketSession? = null
    var player2: DefaultWebSocketSession? = null

    fun isFull() = player1 != null && player2 != null
    
    fun removePlayer(session: DefaultWebSocketSession) {
        if (player1 == session) player1 = null
        if (player2 == session) player2 = null
    }
}

val rooms = ConcurrentHashMap<String, Room>()

fun Application.module() {
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        webSocket("/ws/game") {
            var currentRoom: Room? = null
            var isPlayer1 = false

            try {
                for (frame in incoming) {
                    frame as? Frame.Text ?: continue
                    val receivedText = frame.readText()
                    println("Received: $receivedText")

                    val parts = receivedText.split(" ", limit = 2)
                    val command = parts[0]
                    val payload = parts.getOrNull(1) ?: ""

                    when (command) {
                        "CREATE" -> {
                            val roomId = UUID.randomUUID().toString().replace("-", "").take(6)
                            val room = Room(roomId)
                            room.player1 = this
                            rooms[roomId] = room
                            currentRoom = room
                            isPlayer1 = true
                            send(Frame.Text("ROOM_CREATED $roomId"))
                            send(Frame.Text("WAITING_OPPONENT"))
                        }
                        "JOIN" -> {
                            val roomId = payload.trim()
                            val room = rooms[roomId]
                            if (room == null) {
                                send(Frame.Text("ERROR Room not found"))
                            } else if (room.isFull()) {
                                send(Frame.Text("ERROR Room is full"))
                            } else {
                                if (room.player1 == null) {
                                    room.player1 = this
                                    isPlayer1 = true
                                } else {
                                    room.player2 = this
                                    isPlayer1 = false
                                }
                                currentRoom = room
                                val color = if (isPlayer1) "WHITE" else "BLACK"
                                send(Frame.Text("ROOM_JOINED $color"))
                                
                                if (room.isFull()) {
                                    room.player1?.send(Frame.Text("OPPONENT_JOINED BLACK"))
                                    room.player2?.send(Frame.Text("OPPONENT_JOINED WHITE"))
                                }
                            }
                        }
                        "MOVE" -> {
                            val room = currentRoom
                            if (room != null && room.isFull()) {
                                val opponent = if (isPlayer1) room.player2 else room.player1
                                opponent?.send(Frame.Text("OPPONENT_MOVED $payload"))
                            } else {
                                send(Frame.Text("ERROR Cannot move, opponent not connected"))
                            }
                        }
                        else -> {
                            send(Frame.Text("ERROR Unknown command"))
                        }
                    }
                }
            } catch (e: Exception) {
                println("Error in websocket: ${e.localizedMessage}")
            } finally {
                val room = currentRoom
                if (room != null) {
                    val opponent = if (isPlayer1) room.player2 else room.player1
                    room.removePlayer(this@webSocket)
                    try {
                        opponent?.send(Frame.Text("OPPONENT_DISCONNECTED"))
                    } catch (e: Exception) {
                        // Ignore if opponent is also disconnected
                    }
                    if (room.player1 == null && room.player2 == null) {
                        rooms.remove(room.id)
                    }
                }
            }
        }
    }
}