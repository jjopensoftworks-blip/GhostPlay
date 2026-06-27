package com.example.ghostplay.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface Route : NavKey {
    @Serializable
    data object Library : Route
    
    @Serializable
    data object Dashboard : Route

    @Serializable
    data object AddGame : Route

    @Serializable
    data class GameDetails(val gameId: String) : Route
}
