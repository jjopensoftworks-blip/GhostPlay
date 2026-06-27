package com.example.ghostplay.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ghostplay.data.model.Game
import com.example.ghostplay.data.model.Session
import com.example.ghostplay.data.repository.FirebaseGameRepository
import com.example.ghostplay.data.repository.FirebaseSessionRepository
import com.example.ghostplay.data.repository.GameRepository
import com.example.ghostplay.data.repository.SessionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class GameStat(
    val gameName: String,
    val platform: String,
    val totalPlaytime: Long,
    val sessionCount: Int
)

data class DashboardUiState(
    val totalPlaytime: Long = 0L,
    val mostPlayedGames: List<GameStat> = emptyList(),
    val totalGames: Int = 0,
    val globalPlayerCount: Int = 0,
    val groupStats: Map<String, Int> = emptyMap(),
    val isLoading: Boolean = true
)

class DashboardViewModel(
    private val gameRepository: GameRepository = FirebaseGameRepository(),
    private val sessionRepository: SessionRepository = FirebaseSessionRepository()
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        gameRepository.getGames(),
        sessionRepository.getAllSessions()
    ) { games, sessions ->
        // Aggregate by game name/type for ludo/chess/etc.
        val sessionGroups = sessions.groupBy { it.gameId }
        
        val gameStats = sessionGroups.map { (gameId, sessionList) ->
            val game = games.find { it.id == gameId }
            GameStat(
                gameName = game?.name ?: gameId,
                platform = game?.platform ?: "SIM",
                totalPlaytime = sessionList.sumOf { it.duration },
                sessionCount = sessionList.size
            )
        }.sortedByDescending { it.totalPlaytime }

        // Simulated global counts (in real app, fetch from Firestore global doc)
        val globalCount = 42000 + (sessions.size * 12)
        val groups = mapOf(
            "ELITE_SQUAD" to 120,
            "NEON_VANGUARD" to 85,
            "PULSE_CHAMPIONS" to 210
        )

        DashboardUiState(
            totalPlaytime = sessions.sumOf { it.duration },
            mostPlayedGames = gameStats,
            totalGames = games.size,
            globalPlayerCount = globalCount,
            groupStats = groups,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())
}
