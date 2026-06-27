package com.example.ghostplay.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.ghostplay.ui.screens.AddGameScreen
import com.example.ghostplay.ui.screens.DashboardScreen
import com.example.ghostplay.ui.screens.GameDetailsScreen
import com.example.ghostplay.ui.screens.LibraryScreen

@Composable
fun AppNavigation() {
    val backStack = rememberNavBackStack(Route.Library)
    var currentDestination by remember { mutableStateOf<Route>(Route.Library) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            item(
                selected = currentDestination is Route.Library,
                onClick = { 
                    currentDestination = Route.Library
                    backStack.clear()
                    backStack.add(Route.Library)
                },
                icon = { Icon(Icons.Rounded.LibraryBooks, contentDescription = "Library") },
                label = { Text("Library") }
            )
            item(
                selected = currentDestination is Route.Dashboard,
                onClick = { 
                    currentDestination = Route.Dashboard
                    backStack.clear()
                    backStack.add(Route.Dashboard)
                },
                icon = { Icon(Icons.Rounded.Dashboard, contentDescription = "Dashboard") },
                label = { Text("Dashboard") }
            )
        }
    ) {
        NavDisplay(
            backStack = backStack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            entryProvider = { key ->
                when (key) {
                    is Route.Library -> NavEntry(key) {
                        LibraryScreen(
                            onAddGame = { backStack.add(Route.AddGame) },
                            onViewStatistics = { 
                                currentDestination = Route.Dashboard
                                backStack.clear()
                                backStack.add(Route.Dashboard)
                            },
                            onGameClick = { gameId -> backStack.add(Route.GameDetails(gameId)) }
                        )
                    }
                    is Route.Dashboard -> NavEntry(key) {
                        DashboardScreen()
                    }
                    is Route.AddGame -> NavEntry(key) {
                        AddGameScreen(
                            onBack = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) },
                            onGameAdded = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) }
                        )
                    }
                    is Route.GameDetails -> NavEntry(key) {
                        GameDetailsScreen(
                            gameId = key.gameId,
                            onBack = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) }
                        )
                    }
                    else -> NavEntry(Route.Library as NavKey) { }
                }
            }
        )
    }
}
