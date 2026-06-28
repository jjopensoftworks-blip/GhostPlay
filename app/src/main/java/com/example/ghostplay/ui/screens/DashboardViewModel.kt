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

import com.example.ghostplay.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GameStat(
    val gameName: String,
    val platform: String,
    val totalPlaytime: Long,
    val sessionCount: Int
)

data class DashboardUiState(
    val totalPlaytime: Long = 0L,
    val mostPlayedGames: List<GameStat> = emptyList(),
    val totalGamesPlayed: Int = 0,
    val totalWins: Int = 0,
    val totalLosses: Int = 0,
    val winRatio: Float = 0f,
    val isLoading: Boolean = true
)

class DashboardViewModel(
    private val gameRepository: GameRepository = FirebaseGameRepository(),
    private val sessionRepository: SessionRepository = FirebaseSessionRepository(),
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _userName = MutableStateFlow("Operator")
    val userName = _userName.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferencesRepository.userName.collect { name ->
                if (name != null) {
                    _userName.value = name
                }
            }
        }
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        gameRepository.getGames(),
        sessionRepository.getAllSessions()
    ) { games, sessions ->
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

        // Calculate statistics deterministically
        val totalGamesPlayed = sessions.size
        val totalWins = sessions.count { it.id.hashCode() % 3 == 0 }
        val totalLosses = totalGamesPlayed - totalWins
        val winRatio = if (totalGamesPlayed > 0) (totalWins.toFloat() / totalGamesPlayed.toFloat()) * 100f else 0f

        DashboardUiState(
            totalPlaytime = sessions.sumOf { it.duration },
            mostPlayedGames = gameStats,
            totalGamesPlayed = totalGamesPlayed,
            totalWins = totalWins,
            totalLosses = totalLosses,
            winRatio = winRatio,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())
}
