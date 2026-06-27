package com.example.ghostplay.data.repository

import com.example.ghostplay.data.model.Session
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun getSessionsForGame(gameId: String): Flow<List<Session>>
    fun getAllSessions(): Flow<List<Session>>
    suspend fun startSession(gameId: String): String
    suspend fun endSession(sessionId: String, endTime: Long, duration: Long)
    suspend fun deleteSession(sessionId: String)
}
