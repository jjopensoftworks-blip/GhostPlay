package com.example.ghostplay.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable
    data object Library : Route
    
    @Serializable
    data object Statistics : Route
    
    @Serializable
    data object AddGame : Route
}
