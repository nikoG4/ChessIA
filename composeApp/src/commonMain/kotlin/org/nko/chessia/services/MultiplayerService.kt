package org.nko.chessia.services

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.nko.chessia.com.github.krossovochkin.chess.Piece

enum class MultiplayerState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ROOM_CREATED, // Waiting for opponent
    ROOM_JOINED, // Waiting for opponent
    PLAYING,
    ERROR
}

class MultiplayerService {

    private val client = HttpClient {
        install(WebSockets)
    }

    private var session: DefaultClientWebSocketSession? = null

    private val _state = MutableStateFlow(MultiplayerState.DISCONNECTED)
    val state: StateFlow<MultiplayerState> = _state

    private val _roomId = MutableStateFlow<String?>(null)
    val roomId: StateFlow<String?> = _roomId

    private val _playerColor = MutableStateFlow<Piece.Color?>(null)
    val playerColor: StateFlow<Piece.Color?> = _playerColor
    
    private val _opponentMoves = MutableStateFlow<String?>(null)
    val opponentMoves: StateFlow<String?> = _opponentMoves

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    fun connectAndCreateRoom(host: String = "localhost", port: Int = 8081) {
        _state.value = MultiplayerState.CONNECTING
        coroutineScope.launch {
            try {
                client.webSocket(method = HttpMethod.Get, host = host, port = port, path = "/ws/game") {
                    session = this
                    send(Frame.Text("CREATE"))
                    listenMessages()
                }
            } catch (e: Exception) {
                _state.value = MultiplayerState.ERROR
                _errorMessage.value = "Connection failed: ${e.message}"
            }
        }
    }

    fun connectAndJoinRoom(roomId: String, host: String = "localhost", port: Int = 8081) {
        _state.value = MultiplayerState.CONNECTING
        coroutineScope.launch {
            try {
                client.webSocket(method = HttpMethod.Get, host = host, port = port, path = "/ws/game") {
                    session = this
                    send(Frame.Text("JOIN $roomId"))
                    listenMessages()
                }
            } catch (e: Exception) {
                _state.value = MultiplayerState.ERROR
                _errorMessage.value = "Connection failed: ${e.message}"
            }
        }
    }

    private suspend fun DefaultClientWebSocketSession.listenMessages() {
        try {
            for (message in incoming) {
                message as? Frame.Text ?: continue
                val text = message.readText()
                println("WS Client Received: $text")

                val parts = text.split(" ", limit = 2)
                val cmd = parts[0]
                val payload = parts.getOrNull(1) ?: ""

                when (cmd) {
                    "ROOM_CREATED" -> {
                        _roomId.value = payload
                        _playerColor.value = Piece.Color.White // Creator is white
                        _state.value = MultiplayerState.ROOM_CREATED
                    }
                    "WAITING_OPPONENT" -> {
                        // State already handled
                    }
                    "ROOM_JOINED" -> {
                        _playerColor.value = if (payload == "WHITE") Piece.Color.White else Piece.Color.Black
                        _state.value = MultiplayerState.ROOM_JOINED
                    }
                    "OPPONENT_JOINED" -> {
                        _state.value = MultiplayerState.PLAYING
                    }
                    "OPPONENT_MOVED" -> {
                        _opponentMoves.value = payload
                    }
                    "OPPONENT_DISCONNECTED" -> {
                        _state.value = MultiplayerState.ERROR
                        _errorMessage.value = "El oponente se ha desconectado."
                        session?.close()
                    }
                    "ERROR" -> {
                        _state.value = MultiplayerState.ERROR
                        _errorMessage.value = payload
                        session?.close()
                    }
                }
            }
        } catch (e: Exception) {
             _state.value = MultiplayerState.ERROR
             _errorMessage.value = "Desconectado: ${e.message}"
        } finally {
             session = null
             if (_state.value != MultiplayerState.ERROR) {
                 _state.value = MultiplayerState.DISCONNECTED
             }
        }
    }

    fun sendMove(uci: String) {
        coroutineScope.launch {
            session?.send(Frame.Text("MOVE $uci"))
        }
    }

    fun disconnect() {
        coroutineScope.launch {
            session?.close()
            session = null
            _state.value = MultiplayerState.DISCONNECTED
        }
    }
}
