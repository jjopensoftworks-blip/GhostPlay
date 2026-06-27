package com.example.ghostplay.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.NavDisplay
import androidx.navigation3.rememberNavBackStack
import com.example.ghostplay.ui.screens.LibraryScreen
import com.example.ghostplay.ui.screens.StatisticsScreen
import com.example.ghostplay.ui.screens.AddGameScreen

@Composable
fun AppNavigation() {
    val backStack = rememberNavBackStack(startDestination = Route.Library)
    
    NavDisplay(
        backstack = backStack
    ) { route ->
        when (route) {
            is Route.Library -> LibraryScreen(
                onAddGame = { backStack.push(Route.AddGame) },
                onViewStatistics = { backStack.push(Route.Statistics) }
            )
            is Route.Statistics -> StatisticsScreen(
                onBack = { backStack.pop() }
            )
            is Route.AddGame -> AddGameScreen(
                onBack = { backStack.pop() },
                onGameAdded = { backStack.pop() }
            )
        }
    }
}
