package com.example.ghostplay.data.repository

import com.example.ghostplay.data.model.Game
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseGameRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : GameRepository {

    private val gamesCollection = firestore.collection("games")

    override fun getGames(): Flow<List<Game>> = callbackFlow {
        val subscription = gamesCollection.orderBy("addedAt")
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
        gamesCollection.add(game).await()
    }

    override suspend fun deleteGame(gameId: String) {
        gamesCollection.document(gameId).delete().await()
    }
}
