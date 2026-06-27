package com.example.ghostplay.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Session(
    val id: String = "",
    val gameId: String = "",
    val startTime: Long = 0L,
    val endTime: Long? = null,
    val duration: Long = 0L // in seconds
)
