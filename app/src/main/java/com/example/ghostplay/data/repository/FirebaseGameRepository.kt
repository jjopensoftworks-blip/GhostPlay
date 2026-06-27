package com.example.ghostplay.data.repository

import android.util.Log
import com.example.ghostplay.data.model.Game
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.tasks.await

class FirebaseGameRepository : GameRepository {

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e("GhostPlay", "Firestore not initialized", e)
            null
        }
    }

    private val gamesCollection get() = firestore?.collection("games")

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
