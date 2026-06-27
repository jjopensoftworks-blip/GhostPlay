package com.example.ghostplay.data.repository

import android.util.Log
import com.example.ghostplay.data.model.Session
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseSessionRepository : SessionRepository {

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e("GhostPlay", "Firestore not initialized", e)
            null
        }
    }

    private val sessionsCollection get() = firestore?.collection("sessions")

    override fun getSessionsForGame(gameId: String): Flow<List<Session>> = callbackFlow {
        val collection = sessionsCollection ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val subscription = collection
            .whereEqualTo("gameId", gameId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val sessions = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Session::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(sessions)
            }
        awaitClose { subscription.remove() }
    }

    override fun getAllSessions(): Flow<List<Session>> = callbackFlow {
        val collection = sessionsCollection ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val subscription = collection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val sessions = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Session::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(sessions)
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun startSession(gameId: String): String {
        val collection = sessionsCollection ?: return ""
        val session = Session(
            gameId = gameId,
            startTime = System.currentTimeMillis()
        )
        val result = collection.add(session).await()
        return result.id
    }

    override suspend fun endSession(sessionId: String, endTime: Long, duration: Long) {
        sessionsCollection?.document(sessionId)?.update(
            mapOf(
                "endTime" to endTime,
                "duration" to duration
            )
        )?.await()
    }

    override suspend fun deleteSession(sessionId: String) {
        sessionsCollection?.document(sessionId)?.delete()?.await()
    }
}
