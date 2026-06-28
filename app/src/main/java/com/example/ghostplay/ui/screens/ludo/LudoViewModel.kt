package com.example.ghostplay.ui.screens.ludo

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

import com.example.ghostplay.data.repository.UserPreferencesRepository
import com.example.ghostplay.ui.screens.ludo.network.LudoWebSocketService
import com.example.ghostplay.ui.screens.ludo.network.LudoWebSocketMessage
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

@Serializable
data class TokenMovePayload(val color: String, val id: Int)

@Serializable
data class EmojiPayload(val sender: String, val emoji: String)


class LudoViewModel(private val userPrefs: UserPreferencesRepository) : ViewModel() {

    private val _lobbyState = MutableStateFlow<LudoLobby?>(null)
    val lobbyState = _lobbyState.asStateFlow()

    fun setLobby(lobby: LudoLobby?) {
        _lobbyState.value = lobby
    }

    val isOnlineMode = mutableStateOf(false)
    val lobbyCode = mutableStateOf("")
    val myPlayerColor = mutableStateOf(LudoColor.RED)
    
    // Unique ID for local client player
    val currentUserId = "USER_${Random.nextInt(1000, 9999)}"
    val currentUserName = mutableStateOf("Player_$currentUserId")

    // Disconnect & Auto-Bot Takeover states
    val disconnectCountdown = mutableStateOf<Int?>(null)
    val disconnectedPlayerColor = mutableStateOf<LudoColor?>(null)

    private var firestore: FirebaseFirestore? = null
    private var lobbyListenerRegistration: ListenerRegistration? = null
    private var botJob: Job? = null
    private var timerJob: Job? = null
    private var disconnectJob: Job? = null

    private val webSocketService = LudoWebSocketService()

    init {
        // Attempt Firestore initialization
        try {
            firestore = FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e("LudoViewModel", "Firestore not available: ${e.message}")
            firestore = null
        }

        // Load the locked username from preferences repository
        viewModelScope.launch {
            userPrefs.userName.collect { name ->
                if (name != null) {
                    currentUserName.value = name
                }
            }
        }

        // Listen to WebSocket incoming messages
        viewModelScope.launch {
            webSocketService.incomingMessages.collect { message ->
                handleWebSocketMessage(message)
            }
        }
    }

    private fun handleWebSocketMessage(message: LudoWebSocketMessage) {
        val lobby = _lobbyState.value ?: return
        when (message.action) {
            "ROLL" -> {
                val rollVal = message.payload.toIntOrNull() ?: 1
                val board = lobby.boardState
                if (!board.diceRolled) {
                    val updatedLogs = board.logs + "${board.currentPlayer.name}_ROLLED_$rollVal (Synced)"
                    val nextBoardState = board.copy(
                        diceValue = rollVal,
                        diceRolled = true,
                        logs = updatedLogs
                    )
                    _lobbyState.value = lobby.copy(boardState = nextBoardState)
                }
            }
            "MOVE" -> {
                val tokenPayload = Json.decodeFromString<TokenMovePayload>(message.payload)
                val board = lobby.boardState
                val tokenToMove = board.tokens.find { it.color.name == tokenPayload.color && it.id == tokenPayload.id }
                if (tokenToMove != null && board.diceRolled) {
                    executeMove(lobby, tokenToMove, board.diceValue ?: 1)
                }
            }
            "EMOJI" -> {
                val emojiPayload = Json.decodeFromString<EmojiPayload>(message.payload)
                val currentEmojis = lobby.boardState.emojis.toMutableList()
                currentEmojis.add(Pair(emojiPayload.sender, emojiPayload.emoji))
                if (currentEmojis.size > 3) currentEmojis.removeAt(0)
                _lobbyState.value = lobby.copy(
                    boardState = lobby.boardState.copy(emojis = currentEmojis)
                )
            }
        }
    }

    // --- Lobby Creation & Matchmaking ---

    fun sendEmoji(emoji: String) {
        val lobby = _lobbyState.value ?: return
        val currentEmojis = lobby.boardState.emojis.toMutableList()
        currentEmojis.add(Pair(currentUserName.value, emoji))
        
        // Keep only last 3 emojis to avoid clutter
        if (currentEmojis.size > 3) currentEmojis.removeAt(0)
        
        val updatedLobby = lobby.copy(
            boardState = lobby.boardState.copy(emojis = currentEmojis)
        )
        updateLobbyOnServer(updatedLobby)

        if (isOnlineMode.value) {
            val payload = Json.encodeToString(EmojiPayload(currentUserName.value, emoji))
            webSocketService.sendAction("EMOJI", currentUserId, payload)
        }
        
        // Clear after delay
        viewModelScope.launch {
            delay(4000.milliseconds)
            val l = _lobbyState.value ?: return@launch
            val list = l.boardState.emojis.toMutableList()
            list.removeIf { it.second == emoji }
            updateLobbyOnServer(l.copy(boardState = l.boardState.copy(emojis = list)))
        }
    }

    private fun startGameTracking(lobby: LudoLobby): LudoLobby {
        return lobby.copy(startTime = System.currentTimeMillis())
    }

    private fun endGameTracking(lobby: LudoLobby, winners: List<LudoColor>): LudoLobby {
        saveStructuredGameSession(lobby, winners)
        return lobby.copy(
            endTime = System.currentTimeMillis(),
            firstWinner = winners.getOrNull(0),
            secondWinner = winners.getOrNull(1),
            status = "FINISHED"
        )
    }

    private fun saveStructuredGameSession(lobby: LudoLobby, winners: List<LudoColor>) {
        val db = firestore ?: return
        val sessionId = "GP_LUDO_2026_${Random.nextInt(10000, 99999)}A"
        
        val mode = if (isOnlineMode.value) "PVP" else "BOT"
        val difficulty = if (mode == "BOT") lobby.aiDifficulty else null
        
        val startMillis = lobby.startTime ?: System.currentTimeMillis()
        val endMillis = System.currentTimeMillis()
        val durationSecs = (endMillis - startMillis) / 1000
        
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        val startTimeStr = sdf.format(java.util.Date(startMillis))
        val endTimeStr = sdf.format(java.util.Date(endMillis))
        
        val playersData = lobby.players.map { player ->
            val status = when (player.color) {
                winners.getOrNull(0) -> "Winner"
                winners.getOrNull(1) -> "Runner_Up"
                disconnectedPlayerColor.value -> "Forfeited"
                else -> "Defeated"
            }
            mapOf(
                "username" to player.name,
                "color" to player.color.name,
                "status" to status
            )
        }
        
        val sessionData = mapOf(
            "game_session" to mapOf(
                "session_id" to sessionId,
                "game_type" to "Ludo",
                "matchmaking_mode" to mode,
                "ai_difficulty" to difficulty,
                "timestamps" to mapOf(
                    "start_time" to startTimeStr,
                    "end_time" to endTimeStr,
                    "total_duration_seconds" to durationSecs
                ),
                "players" to playersData,
                "session_status" to if (disconnectedPlayerColor.value != null) "Forfeited" else "Completed"
            )
        )
        
        viewModelScope.launch {
            try {
                db.collection("game_sessions").document(sessionId).set(sessionData)
            } catch (e: Exception) {
                Log.e("LudoViewModel", "Failed to log structured game session: ${e.message}")
            }
        }
    }

    fun simulateNetworkDisconnect() {
        val lobby = _lobbyState.value ?: return
        if (lobby.status != "PLAYING") return
        val humanPlayers = lobby.players.filter { !it.isBot && it.id != currentUserId }
        if (humanPlayers.isEmpty()) return
        
        val target = humanPlayers.random()
        disconnectedPlayerColor.value = target.color
        
        disconnectJob?.cancel()
        disconnectJob = viewModelScope.launch {
            for (seconds in 45 downTo 0) {
                disconnectCountdown.value = seconds
                delay(1000)
            }
            // Timeout: takeover with bot
            disconnectCountdown.value = null
            triggerBotTakeover(target.color)
        }
    }

    private fun triggerBotTakeover(color: LudoColor) {
        val lobby = _lobbyState.value ?: return
        val updatedPlayers = lobby.players.map { player ->
            if (player.color == color) {
                player.copy(isBot = true, name = "BOT_${color.name} (Medium)")
            } else player
        }
        val logs = lobby.boardState.logs + "[DISCONNECT] ${color.name} CONNECTIVITY TIMEOUT. BOT TOOK OVER CONTROL."
        val updatedLobby = lobby.copy(
            players = updatedPlayers,
            boardState = lobby.boardState.copy(logs = logs),
            aiDifficulty = "Medium"
        )
        updateLobbyOnServer(updatedLobby)
        disconnectedPlayerColor.value = null
    }

    fun setupLocalGame(playerCount: Int, botCount: Int, difficulty: String = "Medium") {
        isOnlineMode.value = false
        lobbyCode.value = "LOCAL"
        
        val playersList = mutableListOf<LudoPlayer>()
        
        // Add human player (Player 1 - RED)
        playersList.add(
            LudoPlayer(
                id = currentUserId,
                name = currentUserName.value,
                color = LudoColor.RED,
                isBot = false,
                isHost = true
            )
        )

        // Remaining colors sequence: GREEN, YELLOW, BLUE
        val colors = listOf(LudoColor.GREEN, LudoColor.YELLOW, LudoColor.BLUE)
        var colorIdx = 0

        // Add extra human players if selected
        for (_i in 2..playerCount) {
            if (colorIdx < colors.size) {
                val color = colors[colorIdx++]
                playersList.add(LudoPlayer(id = "LOCAL_$_i", name = "Player_$_i", color = color, isBot = false))
            }
        }

        // Add bot players for the rest of slots up to 4
        for (_i in 1..botCount) {
            if (colorIdx < colors.size) {
                val color = colors[colorIdx++]
                playersList.add(LudoPlayer(id = "BOT_$color", name = "BOT_${color.name}", color = color, isBot = true))
            }
        }

        val lobby = LudoLobby(
            lobbyId = "LOCAL",
            status = "PLAYING",
            hostId = currentUserId,
            players = playersList,
            aiDifficulty = difficulty,
            boardState = LudoBoardState(
                currentPlayer = LudoColor.RED,
                logs = listOf("LOCAL_MATCH_INITIATED", "RED_PLAYER_TURN")
            )
        )
        
        _lobbyState.value = startGameTracking(lobby)
    }

    fun hostOnlineGame() {
        val db = firestore
        if (db == null) {
            _lobbyState.value = null
            return
        }

        isOnlineMode.value = true
        val newCode = "LUDO-${Random.nextInt(1000, 9999)}"
        lobbyCode.value = newCode
        myPlayerColor.value = LudoColor.RED

        val hostPlayer = LudoPlayer(
            id = currentUserId,
            name = currentUserName.value,
            color = LudoColor.RED,
            isBot = false,
            isHost = true
        )

        val lobby = LudoLobby(
            lobbyId = newCode,
            status = "LOBBY",
            hostId = currentUserId,
            players = listOf(hostPlayer),
            boardState = LudoBoardState()
        )

        _lobbyState.value = lobby

        viewModelScope.launch {
            try {
                db.collection("ludo_lobbies").document(newCode).set(lobby)
                listenToLobby(newCode)
                webSocketService.connect(newCode, currentUserId)
            } catch (e: Exception) {
                Log.e("LudoViewModel", "Error hosting game: ${e.message}")
            }
        }
    }

    fun joinOnlineGame(code: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val db = firestore
        if (db == null) {
            onError("FIRESTORE_NOT_INITIALIZED")
            return
        }

        viewModelScope.launch {
            try {
                val doc = db.collection("ludo_lobbies").document(code).get().await()
                if (!doc.exists()) {
                    onError("LOBBY_NOT_FOUND")
                    return@launch
                }
                
                val lobby = doc.toObject(LudoLobby::class.java)
                if (lobby == null) {
                    onError("CORRUPTED_LOBBY_DATA")
                    return@launch
                }

                if (lobby.status != "LOBBY") {
                    onError("GAME_ALREADY_STARTED")
                    return@launch
                }

                if (lobby.players.size >= 4) {
                    onError("LOBBY_FULL")
                    return@launch
                }

                // Determine next color
                val assignedColor = when (lobby.players.size) {
                    1 -> LudoColor.GREEN
                    2 -> LudoColor.YELLOW
                    3 -> LudoColor.BLUE
                    else -> LudoColor.BLUE
                }
                
                myPlayerColor.value = assignedColor

                val newPlayer = LudoPlayer(
                    id = currentUserId,
                    name = currentUserName.value,
                    color = assignedColor,
                    isBot = false
                )

                val updatedPlayers = lobby.players + newPlayer
                val updatedLobby = lobby.copy(
                    players = updatedPlayers,
                    lastUpdateTime = System.currentTimeMillis()
                )

                db.collection("ludo_lobbies").document(code).set(updatedLobby)
                isOnlineMode.value = true
                lobbyCode.value = code
                _lobbyState.value = updatedLobby
                
                listenToLobby(code)
                webSocketService.connect(code, currentUserId)
                onSuccess()

            } catch (e: Exception) {
                Log.e("LudoViewModel", "Error joining: ${e.message}")
                onError(e.message ?: "UNKNOWN_ERROR")
            }
        }
    }

    fun startOnlineGame() {
        val lobby = _lobbyState.value ?: return
        if (lobby.hostId != currentUserId) return // Only host can start

        // Fill remaining players with bots to make it 4 players
        val currentPlayers = lobby.players.toMutableList()
        val assignedColors = currentPlayers.map { it.color }.toSet()
        
        LudoColor.entries.forEach { color ->
            if (!assignedColors.contains(color)) {
                currentPlayers.add(
                    LudoPlayer(
                        id = "BOT_$color",
                        name = "BOT_${color.name}",
                        color = color,
                        isBot = true
                    )
                )
            }
        }

        val updatedLobby = lobby.copy(
            status = "PLAYING",
            players = currentPlayers,
            boardState = lobby.boardState.copy(
                currentPlayer = LudoColor.RED,
                logs = lobby.boardState.logs + "GAME_STARTED_BY_HOST" + "RED_PLAYER_TURN"
            ),
            lastUpdateTime = System.currentTimeMillis()
        )

        updateLobbyOnServer(updatedLobby)
    }

    private fun startTurnTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            delay(20000.milliseconds) // 20 second turn timer
            val lobby = _lobbyState.value
            if (lobby != null && lobby.status == "PLAYING") {
                passTurn(lobby)
            }
        }
    }

    private fun listenToLobby(code: String) {
        lobbyListenerRegistration?.remove()
        val db = firestore ?: return
        
        lobbyListenerRegistration = db.collection("ludo_lobbies")
            .document(code)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("LudoViewModel", "Lobby listen failed: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val lobby = snapshot.toObject(LudoLobby::class.java)
                    if (lobby != null) {
                        val oldPlayer = _lobbyState.value?.boardState?.currentPlayer
                        _lobbyState.value = lobby
                        
                        // If turn changed, restart timer
                        if (oldPlayer != lobby.boardState.currentPlayer) {
                            startTurnTimer()
                        }

                        // Check if it is a bot's turn and handle bot play
                        checkAndRunBot()
                    }
                }
            }
    }

    private fun updateLobbyOnServer(lobby: LudoLobby) {
        val oldPlayer = _lobbyState.value?.boardState?.currentPlayer
        _lobbyState.value = lobby
        
        if (oldPlayer != lobby.boardState.currentPlayer) {
            startTurnTimer()
        }

        if (isOnlineMode.value) {
            val db = firestore
            if (db != null) {
                db.collection("ludo_lobbies").document(lobby.lobbyId).set(lobby)
            }
        } else {
            // Local mode, immediately check bot play
            checkAndRunBot()
        }
    }

    // --- Ludo Game Logic Action Handlers ---

    fun rollDice() {
        val lobby = _lobbyState.value ?: return
        val board = lobby.boardState

        // Validate turn
        if (board.diceRolled) return
        
        // In online mode, only roll if it matches my assigned color
        if (isOnlineMode.value && board.currentPlayer != myPlayerColor.value) return

        val roll = Random.nextInt(1, 7)
        val updatedLogs = board.logs.toMutableList()
        updatedLogs.add("${board.currentPlayer.name}_ROLLED_$roll")

        var cSixes = board.consecutiveSixes
        // Rule: 3 consecutive 6s skip turn
        if (roll == 6) {
            cSixes++
            if (cSixes == 3) {
                updatedLogs.add("[PENALTY] THREE_CONSECUTIVE_6S! TURN_SKIPPED.")
                val nextPlayer = getNextPlayer(board.currentPlayer, board.winningPlayers, lobby.players)
                val nextBoardState = board.copy(
                    currentPlayer = nextPlayer,
                    diceValue = null,
                    diceRolled = false,
                    logs = updatedLogs,
                    consecutiveSixes = 0
                )
                updateLobbyOnServer(lobby.copy(boardState = nextBoardState))
                return
            }
        } else {
            cSixes = 0
        }

        val nextBoardState = board.copy(
            diceValue = roll,
            diceRolled = true,
            logs = updatedLogs,
            consecutiveSixes = cSixes
        )

        val updatedLobby = lobby.copy(boardState = nextBoardState)
        updateLobbyOnServer(updatedLobby)

        if (isOnlineMode.value) {
            webSocketService.sendAction("ROLL", currentUserId, roll.toString())
        }

        // Post roll check: if no moves are possible, skip turn automatically
        checkMovePossibility(updatedLobby, roll)
    }

    private fun checkMovePossibility(lobby: LudoLobby, roll: Int) {
        val board = lobby.boardState
        val playerColor = board.currentPlayer

        // Find if any token of the current player has a valid move
        val validTokens = board.tokens.filter { token ->
            token.color == playerColor && isValidMove(token, roll)
        }

        if (validTokens.isEmpty()) {
            // No moves possible! Pass turn after a short delay so user can see the dice roll
            viewModelScope.launch {
                delay(1500.milliseconds)
                passTurn(lobby)
            }
        }
    }

    private fun isValidMove(token: LudoToken, roll: Int): Boolean {
        return when (token.positionType) {
            TokenPositionType.BASE -> {
                // To get out of base, player must roll a 6
                roll == 6
            }
            TokenPositionType.TRACK -> {
                // Tokens can always move along the track, but check exit bounds
                true
            }
            TokenPositionType.HOME_STRETCH -> {
                // Must land exactly on finished (index 5)
                token.positionIndex + roll <= 5
            }
            TokenPositionType.FINISHED -> {
                // Already done
                false
            }
        }
    }

    fun moveToken(token: LudoToken) {
        val lobby = _lobbyState.value ?: return
        val board = lobby.boardState
        val roll = board.diceValue ?: return

        // Verify turn
        if (!board.diceRolled) return
        if (token.color != board.currentPlayer) return
        
        // In online mode, only move if it's my assigned color
        if (isOnlineMode.value && token.color != myPlayerColor.value) return

        // Validate move
        if (!isValidMove(token, roll)) return

        if (isOnlineMode.value) {
            val payload = Json.encodeToString(TokenMovePayload(token.color.name, token.id))
            webSocketService.sendAction("MOVE", currentUserId, payload)
        }

        viewModelScope.launch {
            executeMove(lobby, token, roll)
        }
    }

    private fun executeMove(lobby: LudoLobby, token: LudoToken, roll: Int) {
        val board = lobby.boardState
        val tokens = board.tokens.toMutableList()

        // 1. Calculate path coordinates for logging/execution
        val path = getTraversedPath(token, roll)
        val finalPosition = path.last()

        // Remove old token position and insert new
        val tokenIndex = tokens.indexOfFirst { it.color == token.color && it.id == token.id }
        if (tokenIndex == -1) return

        val movedToken = token.copy(
            positionType = finalPosition.first,
            positionIndex = finalPosition.second
        )
        tokens[tokenIndex] = movedToken

        // 2. Resolve token captures
        var capturedOpponent = false
        val logs = board.logs.toMutableList()
        logs.add("${token.color.name}_MOVED_TOKEN_${token.id}_BY_$roll")

        if (movedToken.positionType == TokenPositionType.TRACK) {
            val isSafeCell = LudoCoordinates.SAFE_ZONE_INDEXES.contains(movedToken.positionIndex)

            if (!isSafeCell) {
                // Check if any opponent token sits on this cell
                tokens.forEachIndexed { idx, t ->
                    if (t.color != movedToken.color && t.positionType == TokenPositionType.TRACK && t.positionIndex == movedToken.positionIndex) {
                        // Capture! Send back to base
                        tokens[idx] = t.copy(positionType = TokenPositionType.BASE, positionIndex = 0)
                        capturedOpponent = true
                        logs.add("[CAPTURE] ${movedToken.color.name}_CAPTURED_${t.color.name}_TOKEN_${t.id}!")
                    }
                }
            }
        }

        // 3. Check for win conditions
        val finishedCount = tokens.count { it.color == token.color && it.positionType == TokenPositionType.FINISHED }
        val winningList = board.winningPlayers.toMutableList()
        
        if (finishedCount == 4 && !winningList.contains(token.color)) {
            winningList.add(token.color)
            logs.add("[VICTORY] ${token.color.name}_HAS_FINISHED_ALL_TOKENS!")
        }

        // 4. Decide next turn
        // Rule: If player rolled a 6 or captured an opponent, they get an extra roll
        val extraTurn = (roll == 6 || capturedOpponent) && finishedCount < 4
        
        val nextPlayerColor = if (extraTurn) {
            token.color
        } else {
            getNextPlayer(token.color, winningList, lobby.players)
        }

        logs.add("${nextPlayerColor.name}_PLAYER_TURN")

        val nextBoardState = board.copy(
            tokens = tokens,
            currentPlayer = nextPlayerColor,
            diceValue = null,
            diceRolled = false,
            winningPlayers = winningList,
            logs = logs
        )

        // Check if game is completely finished
        val isGameFinished = winningList.size >= (lobby.players.size - 1).coerceAtLeast(1)
        var finalLobby = lobby.copy(
            status = if (isGameFinished) "FINISHED" else "PLAYING",
            boardState = nextBoardState,
            lastUpdateTime = System.currentTimeMillis()
        )
        
        if (isGameFinished) {
            finalLobby = endGameTracking(finalLobby, winningList)
        }

        updateLobbyOnServer(finalLobby)
    }

    private fun passTurn(lobby: LudoLobby) {
        val board = lobby.boardState
        val nextPlayerColor = getNextPlayer(board.currentPlayer, board.winningPlayers, lobby.players)
        val logs = board.logs + "${board.currentPlayer.name}_PASSES_TURN" + "${nextPlayerColor.name}_PLAYER_TURN"
        
        val nextBoardState = board.copy(
            currentPlayer = nextPlayerColor,
            diceValue = null,
            diceRolled = false,
            logs = logs
        )

        val finalLobby = lobby.copy(
            boardState = nextBoardState,
            lastUpdateTime = System.currentTimeMillis()
        )

        updateLobbyOnServer(finalLobby)
    }

    private fun getNextPlayer(
        current: LudoColor,
        winners: List<LudoColor>,
        players: List<LudoPlayer>
    ): LudoColor {
        val activeColors = players.map { it.color }
        var next = current
        
        do {
            next = when (next) {
                LudoColor.RED -> LudoColor.GREEN
                LudoColor.GREEN -> LudoColor.YELLOW
                LudoColor.YELLOW -> LudoColor.BLUE
                LudoColor.BLUE -> LudoColor.RED
            }
        } while (
            // Skip players who are not in the lobby, or have already won
            (!activeColors.contains(next) || winners.contains(next)) && next != current
        )
        
        return next
    }

    // Helper to calculate cell positions traversed (Pair<Type, Index>)
    private fun getTraversedPath(token: LudoToken, roll: Int): List<Pair<TokenPositionType, Int>> {
        val path = mutableListOf<Pair<TokenPositionType, Int>>()
        
        if (token.positionType == TokenPositionType.BASE) {
            // Getting out of base goes to Start Index
            if (roll == 6) {
                path.add(Pair(TokenPositionType.TRACK, LudoCoordinates.START_INDEXES[token.color]!!))
            }
            return path
        }

        var currentType = token.positionType
        var currentIndex = token.positionIndex

        val exitIdx = LudoCoordinates.EXIT_INDEXES[token.color]!!

        for (_i in 1..roll) {
            if (currentType == TokenPositionType.TRACK) {
                if (currentIndex == exitIdx) {
                    // Turn into home stretch
                    currentType = TokenPositionType.HOME_STRETCH
                    currentIndex = 0
                } else {
                    currentIndex = (currentIndex + 1) % 52
                }
            } else if (currentType == TokenPositionType.HOME_STRETCH) {
                currentIndex++
                if (currentIndex == 5) {
                    currentType = TokenPositionType.FINISHED
                }
            }
            path.add(Pair(currentType, currentIndex))
        }

        return path
    }

    // --- Autonomous Bot Logic ---

    private fun checkAndRunBot() {
        val lobby = _lobbyState.value ?: return
        if (lobby.status != "PLAYING") return

        val board = lobby.boardState
        val playerColor = board.currentPlayer
        val player = lobby.players.find { it.color == playerColor } ?: return

        // Only run bot logic if the current player is a bot
        // And if we are hosting/local (or we are player RED host in online lobby)
        val isMyResponsibility = !isOnlineMode.value || (lobby.hostId == currentUserId)

        if (player.isBot && isMyResponsibility) {
            botJob?.cancel()
            botJob = viewModelScope.launch {
                delay(1200.milliseconds) // Thinking delay
                
                if (!board.diceRolled) {
                    // Bot Rolls
                    triggerBotRoll(lobby)
                } else {
                    // Bot Moves
                    triggerBotMove(lobby)
                }
            }
        }
    }

    private fun triggerBotRoll(lobby: LudoLobby) {
        val board = lobby.boardState
        val roll = Random.nextInt(1, 7)
        val updatedLogs = board.logs + "BOT_${board.currentPlayer.name}_ROLLED_$roll"

        val nextBoardState = board.copy(
            diceValue = roll,
            diceRolled = true,
            logs = updatedLogs
        )

        val updatedLobby = lobby.copy(boardState = nextBoardState)
        _lobbyState.value = updatedLobby
        
        if (isOnlineMode.value) {
            firestore?.collection("ludo_lobbies")?.document(lobby.lobbyId)?.set(updatedLobby)
        }

        // Post roll check for bot moves
        checkMovePossibility(updatedLobby, roll)
    }

    private suspend fun triggerBotMove(lobby: LudoLobby) {
        val board = lobby.boardState
        val roll = board.diceValue ?: return
        val botColor = board.currentPlayer

        val botTokens = board.tokens.filter { it.color == botColor }
        val validTokens = botTokens.filter { isValidMove(it, roll) }

        if (validTokens.isEmpty()) {
            // No moves possible, already passed in post-roll check
            return
        }

        // AI decision making
        val selectedToken = selectBestBotToken(validTokens, board.tokens, roll)
        
        delay(1000.milliseconds) // Movement delay
        executeMove(lobby, selectedToken, roll)
    }

    private fun selectBestBotToken(
        validTokens: List<LudoToken>,
        allTokens: List<LudoToken>,
        roll: Int
    ): LudoToken {
        val lobby = _lobbyState.value ?: return validTokens.random()
        val diff = lobby.aiDifficulty ?: "Medium"
        
        // 1. Easy Mode: 80% chance of random moves. Never targets player pawns.
        if (diff == "Easy") {
            if (Random.nextFloat() > 0.2f) {
                return validTokens.random()
            }
            // Non-aggressive move: prioritises base release or moving furthest token
            if (roll == 6) {
                validTokens.find { it.positionType == TokenPositionType.BASE }?.let { return it }
            }
            val furthest = validTokens.maxByOrNull { getDistanceTraveled(it) }
            if (furthest != null) return furthest
            return validTokens.random()
        }

        // 2. Medium Mode: 40% chance of random moves, 60% strategic
        if (diff == "Medium") {
            if (Random.nextFloat() > 0.6f) {
                return validTokens.random()
            }
        }

        // 3. Hard Mode / Strategic Medium choice:
        // Priority 1: Captures an opponent (Highly aggressive targeting)
        validTokens.forEach { token ->
            val path = getTraversedPath(token, roll)
            if (path.isNotEmpty() && path.last().first == TokenPositionType.TRACK) {
                val destIdx = path.last().second
                val isSafe = LudoCoordinates.SAFE_ZONE_INDEXES.contains(destIdx)
                if (!isSafe) {
                    val enemyExists = allTokens.any { it.color != token.color && it.positionType == TokenPositionType.TRACK && it.positionIndex == destIdx }
                    if (enemyExists) return token
                }
            }
        }

        // Priority 2: Finishes game (lands in home triangle)
        validTokens.find { token ->
            val path = getTraversedPath(token, roll)
            path.isNotEmpty() && path.last().first == TokenPositionType.FINISHED
        }?.let { return it }

        // Priority 3: Releases token from base (if rolled a 6)
        if (roll == 6) {
            validTokens.find { it.positionType == TokenPositionType.BASE }?.let { return it }
        }

        // Priority 4: Moves token closest to finishing (furthest track or home stretch index)
        val furthestToken = validTokens.maxByOrNull { getDistanceTraveled(it) }
        if (furthestToken != null) return furthestToken

        // Fallback: Random choice
        return validTokens.random()
    }

    private fun getDistanceTraveled(token: LudoToken): Int {
        return when (token.positionType) {
            TokenPositionType.HOME_STRETCH -> 100 + token.positionIndex
            TokenPositionType.TRACK -> {
                // Calculate distance traveled relative to start index
                val start = LudoCoordinates.START_INDEXES[token.color]!!
                val current = token.positionIndex
                val distance = (current - start + 52) % 52
                distance
            }
            else -> 0
        }
    }

    fun cleanupLobby() {
        lobbyListenerRegistration?.remove()
        botJob?.cancel()
        webSocketService.disconnect()
        _lobbyState.value = null
    }

    override fun onCleared() {
        cleanupLobby()
    }
}
