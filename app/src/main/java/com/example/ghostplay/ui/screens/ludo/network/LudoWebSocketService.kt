package com.example.ghostplay.ui.screens.ludo.network

import android.util.Log
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class LudoWebSocketMessage(
    val action: String, // "ROLL", "MOVE", "EMOJI", "JOIN", "DISCONNECT"
    val senderId: String,
    val payload: String
)

class LudoWebSocketService {
    private val client = HttpClient {
        install(WebSockets)
    }

    private var session: WebSocketSession? = null
    private val _incomingMessages = MutableSharedFlow<LudoWebSocketMessage>()
    val incomingMessages = _incomingMessages.asSharedFlow()

    private var connectionJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun connect(lobbyCode: String, userId: String) {
        disconnect()
        connectionJob = scope.launch {
            try {
                // Low-latency socket endpoint matching GhostPlay routing backend
                client.webSocket(method = HttpMethod.Get, host = "ghostplay-gateway.jjopensoftworks.com", port = 80, path = "/ws/ludo/$lobbyCode?user=$userId") {
                    session = this
                    Log.i("LudoWebSocket", "Established WebSocket connection node link to $lobbyCode")
                    
                    // Join announcement
                    send(Frame.Text(Json.encodeToString(LudoWebSocketMessage("JOIN", userId, lobbyCode))))

                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val text = frame.readText()
                            try {
                                val message = Json.decodeFromString<LudoWebSocketMessage>(text)
                                _incomingMessages.emit(message)
                            } catch (e: Exception) {
                                Log.e("LudoWebSocket", "Failed parsing frame: ${e.message}")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("LudoWebSocket", "Connection handshake failed: ${e.message}")
            }
        }
    }

    fun sendAction(action: String, senderId: String, payload: String) {
        val message = LudoWebSocketMessage(action, senderId, payload)
        scope.launch {
            try {
                session?.send(Frame.Text(Json.encodeToString(message)))
            } catch (e: Exception) {
                Log.e("LudoWebSocket", "Failed to broadcast action over socket: ${e.message}")
            }
        }
    }

    fun disconnect() {
        connectionJob?.cancel()
        runBlocking {
            try {
                session?.close()
            } catch (e: Exception) {
                // Ignore close errors
            }
            session = null
        }
    }
}
