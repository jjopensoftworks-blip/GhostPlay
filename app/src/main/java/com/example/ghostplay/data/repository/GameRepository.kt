package com.example.ghostplay.data.repository

import com.example.ghostplay.data.model.Game
import kotlinx.coroutines.flow.Flow

interface GameRepository {
    fun getGames(): Flow<List<Game>>
    suspend fun addGame(game: Game)
    suspend fun deleteGame(gameId: String)
}
