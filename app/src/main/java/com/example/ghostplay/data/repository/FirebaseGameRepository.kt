package com.example.ghostplay.data.repository

import android.util.Log
import com.example.ghostplay.data.model.Game
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseGameRepository : GameRepository {

    private var _firestore: FirebaseFirestore? = null
    private var _initAttempted = false

    private fun getFirestore(): FirebaseFirestore? {
        if (_initAttempted) return _firestore
        
        synchronized(this) {
            if (_initAttempted) return _firestore
            try {
                _firestore = FirebaseFirestore.getInstance()
            } catch (e: Throwable) {
                Log.e("GhostPlay", "Firestore initialization failed: ${e.message}")
                _firestore = null
            }
            _initAttempted = true
        }
        return _firestore
    }

    private val gamesCollection get() = getFirestore()?.collection("games")

    override fun getGames(): Flow<List<Game>> = callbackFlow {
        val collection = gamesCollection ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val subscription = collection.orderBy("addedAt")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val games = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Game::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(games)
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun addGame(game: Game) {
        gamesCollection?.add(game)?.await()
    }

    override suspend fun deleteGame(gameId: String) {
        gamesCollection?.document(gameId)?.delete()?.await()
    }
}
