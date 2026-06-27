package com.example.ghostplay.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ghostplay.data.model.Game
import com.example.ghostplay.data.repository.FirebaseGameRepository
import com.example.ghostplay.data.repository.GameRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val repository: GameRepository = FirebaseGameRepository()
) : ViewModel() {

    val games: StateFlow<List<Game>> = repository.getGames()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteGame(game: Game) {
        viewModelScope.launch {
            repository.deleteGame(game.id)
        }
    }
}
