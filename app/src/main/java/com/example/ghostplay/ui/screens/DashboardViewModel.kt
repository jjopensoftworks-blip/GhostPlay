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
    val game: Game,
    val totalPlaytime: Long
)

data class DashboardUiState(
    val totalPlaytime: Long = 0L,
    val mostPlayedGames: List<GameStat> = emptyList(),
    val totalGames: Int = 0,
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
        val gameStats = games.map { game ->
            val playtime = sessions.filter { it.gameId == game.id }.sumOf { it.duration }
            GameStat(game, playtime)
        }.sortedByDescending { it.totalPlaytime }

        DashboardUiState(
            totalPlaytime = sessions.sumOf { it.duration },
            mostPlayedGames = gameStats.take(5),
            totalGames = games.size,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())
}
