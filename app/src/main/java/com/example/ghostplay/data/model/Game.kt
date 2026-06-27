package com.example.ghostplay.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Game(
    val id: String = "",
    val name: String = "",
    val platform: String = "",
    val iconUrl: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)
