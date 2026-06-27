package com.example.ghostplay.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ghostplay.data.model.Game
import com.example.ghostplay.data.model.Session
import com.example.ghostplay.data.repository.FirebaseGameRepository
import com.example.ghostplay.data.repository.FirebaseSessionRepository
import com.example.ghostplay.data.repository.GameRepository
import com.example.ghostplay.data.repository.SessionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GameDetailsViewModel(
    private val gameId: String,
    private val gameRepository: GameRepository = FirebaseGameRepository(),
    private val sessionRepository: SessionRepository = FirebaseSessionRepository()
) : ViewModel() {

    private val _activeSessionId = MutableStateFlow<String?>(null)
    val activeSessionId = _activeSessionId.asStateFlow()

    private val _elapsedTime = MutableStateFlow(0L)
    val elapsedTime = _elapsedTime.asStateFlow()

    private var timerJob: Job? = null
    private var startTimeMillis: Long = 0L

    val game: StateFlow<Game?> = gameRepository.getGames()
        .map { games -> games.find { it.id == gameId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val sessions: StateFlow<List<Session>> = sessionRepository.getSessionsForGame(gameId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalPlaytime: StateFlow<Long> = sessions.map { list ->
        list.sumOf { it.duration }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    fun startTracking() {
        viewModelScope.launch {
            val id = sessionRepository.startSession(gameId)
            _activeSessionId.value = id
            startTimeMillis = System.currentTimeMillis()
            startTimer()
        }
    }

    fun stopTracking() {
        val sessionId = _activeSessionId.value ?: return
        val endTime = System.currentTimeMillis()
        val duration = (endTime - startTimeMillis) / 1000
        
        viewModelScope.launch {
            sessionRepository.endSession(sessionId, endTime, duration)
            _activeSessionId.value = null
            stopTimer()
            _elapsedTime.value = 0L
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                _elapsedTime.value = (System.currentTimeMillis() - startTimeMillis) / 1000
                delay(1000)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        if (_activeSessionId.value != null) {
            stopTracking()
        }
    }
}
