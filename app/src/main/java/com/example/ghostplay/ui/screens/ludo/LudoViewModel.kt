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

class LudoViewModel : ViewModel() {

    private val _lobbyState = MutableStateFlow<LudoLobby?>(null)
    val lobbyState = _lobbyState.asStateFlow()

    val isOnlineMode = mutableStateOf(false)
    val lobbyCode = mutableStateOf("")
    val myPlayerColor = mutableStateOf(LudoColor.RED)
    
    // Unique ID for local client player
    val currentUserId = "USER_${Random.nextInt(1000, 9999)}"
    val currentUserName = mutableStateOf("Player_$currentUserId")

    private var firestore: FirebaseFirestore? = null
    private var lobbyListenerRegistration: ListenerRegistration? = null
    private var botJob: Job? = null
    private var timerJob: Job? = null
    
    // Stats for rules
    private var consecutiveSixes = 0

    init {
        // Attempt Firestore initialization
        try {
            firestore = FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e("LudoViewModel", "Firestore not available: ${e.message}")
            firestore = null
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
        
        // Clear after delay
        viewModelScope.launch {
            delay(4000)
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
        return lobby.copy(
            endTime = System.currentTimeMillis(),
            firstWinner = winners.getOrNull(0),
            secondWinner = winners.getOrNull(1),
            status = "FINISHED"
        )
    }

    fun setupLocalGame(playerCount: Int, botCount: Int) {
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
        for (i in 2..playerCount) {
            if (colorIdx < colors.size) {
                val color = colors[colorIdx++]
                playersList.add(LudoPlayer(id = "LOCAL_$i", name = "Player_$i", color = color, isBot = false))
            }
        }

        // Add bot players for the rest of slots up to 4
        for (i in 1..botCount) {
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
            delay(20000) // 20 second turn timer
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
                        
                        // If turn changed, restart timer and reset 6s
                        if (oldPlayer != lobby.boardState.currentPlayer) {
                            consecutiveSixes = 0
                            startTurnTimer()
                        }

                        // Check if it is a bot's turn and handle bot play
                        checkAndRunBot()
                    }
                }
            }
    }

    private fun updateLobbyOnServer(lobby: LudoLobby) {
        _lobbyState.value = lobby
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

        // Rule: 3 consecutive 6s skip turn
        if (roll == 6) {
            consecutiveSixes++
            if (consecutiveSixes == 3) {
                updatedLogs.add("[PENALTY] THREE_CONSECUTIVE_6S! TURN_SKIPPED.")
                consecutiveSixes = 0
                viewModelScope.launch {
                    delay(1000)
                    passTurn(lobby)
                }
                return
            }
        } else {
            consecutiveSixes = 0
        }

        val nextBoardState = board.copy(
            diceValue = roll,
            diceRolled = true,
            logs = updatedLogs
        )

        val updatedLobby = lobby.copy(boardState = nextBoardState)
        updateLobbyOnServer(updatedLobby)

        // Post roll check: if no moves are possible, skip turn automatically
        checkMovePossibility(updatedLobby, roll)
    }

    private fun checkMovePossibility(lobby: LudoLobby, roll: Int) {
        val board = lobby.boardState
        val playerColor = board.currentPlayer

        // Find if any token of the current player has a valid move
        val validTokens = board.tokens.filter { token ->
            token.color == playerColor && isValidMove(token, roll, board.tokens)
        }

        if (validTokens.isEmpty()) {
            // No moves possible! Pass turn after a short delay so user can see the dice roll
            viewModelScope.launch {
                delay(1500)
                passTurn(lobby)
            }
        }
    }

    private fun isValidMove(token: LudoToken, roll: Int, allTokens: List<LudoToken>): Boolean {
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
        if (!isValidMove(token, roll, board.tokens)) return

        viewModelScope.launch {
            executeMove(lobby, token, roll)
        }
    }

    private suspend fun executeMove(lobby: LudoLobby, token: LudoToken, roll: Int) {
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
            val trackCell = LudoCoordinates.TRACK[movedToken.positionIndex]
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
        val finalLobby = lobby.copy(
            status = if (isGameFinished) "FINISHED" else "PLAYING",
            boardState = nextBoardState,
            lastUpdateTime = System.currentTimeMillis()
        )

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
        val startIdx = LudoCoordinates.START_INDEXES[token.color]!!

        for (i in 1..roll) {
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
                delay(1200) // Thinking delay
                
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
        val validTokens = botTokens.filter { isValidMove(it, roll, board.tokens) }

        if (validTokens.isEmpty()) {
            // No moves possible, already passed in post-roll check
            return
        }

        // AI decision making
        val selectedToken = selectBestBotToken(validTokens, board.tokens, roll)
        
        delay(1000) // Movement delay
        executeMove(lobby, selectedToken, roll)
    }

    private fun selectBestBotToken(
        validTokens: List<LudoToken>,
        allTokens: List<LudoToken>,
        roll: Int
    ): LudoToken {
        // Priority 1: Captures an opponent
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
        val furthestToken = validTokens.maxByOrNull { token ->
            when (token.positionType) {
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
        if (furthestToken != null) return furthestToken

        // Fallback: Random choice
        return validTokens.random()
    }

    fun cleanupLobby() {
        lobbyListenerRegistration?.remove()
        botJob?.cancel()
        _lobbyState.value = null
    }

    override fun onCleared() {
        super.onCleared()
        cleanupLobby()
    }
}
