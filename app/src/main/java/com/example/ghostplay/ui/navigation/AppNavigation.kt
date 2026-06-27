package com.example.ghostplay.ui.navigation

import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.ghostplay.data.repository.UserPreferencesRepository
import com.example.ghostplay.ui.screens.*
import kotlinx.coroutines.launch

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val userPrefs = remember { UserPreferencesRepository(context) }
    val userName by userPrefs.userName.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    
    // Use an empty backstack initially, then decide start destination
    val backStack = rememberNavBackStack(Route.Library)
    var currentDestination by remember { mutableStateOf<Route>(Route.Library) }

    LaunchedEffect(userName) {
        if (userName == null) {
            currentDestination = Route.Onboarding
            backStack.clear()
            backStack.add(Route.Onboarding)
        } else if (currentDestination is Route.Onboarding) {
            currentDestination = Route.Library
            backStack.clear()
            backStack.add(Route.Library)
        }
    }

    if (currentDestination is Route.Onboarding) {
        OnboardingScreen(onNameSet = { name ->
            scope.launch {
                userPrefs.saveUserName(name)
            }
        })
    } else {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                item(
                    selected = currentDestination is Route.Library,
                    onClick = { 
                        currentDestination = Route.Library
                        backStack.clear()
                        backStack.add(Route.Library)
                    },
                    icon = { Icon(Icons.AutoMirrored.Rounded.LibraryBooks, contentDescription = "GRID") },
                    label = { Text("GRID") }
                )
                item(
                    selected = false,
                    onClick = { },
                    icon = { Icon(Icons.Rounded.Hub, contentDescription = "NETWORK") },
                    label = { Text("NETWORK") }
                )
                item(
                    selected = currentDestination is Route.Dashboard,
                    onClick = { 
                        currentDestination = Route.Dashboard
                        backStack.clear()
                        backStack.add(Route.Dashboard)
                    },
                    icon = { Icon(Icons.Rounded.BarChart, contentDescription = "LOGS") },
                    label = { Text("LOGS") }
                )
                item(
                    selected = false,
                    onClick = { },
                    icon = { Icon(Icons.Rounded.SettingsInputComponent, contentDescription = "CONFIG") },
                    label = { Text("CONFIG") }
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
                                onGameClick = { gameId -> backStack.add(Route.GameDetails(gameId)) },
                                onInstantGameClick = { type -> backStack.add(Route.GamePlay(type)) }
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
                        is Route.GamePlay -> NavEntry(key) {
                            GamePlayScreen(
                                gameType = key.gameType,
                                onBack = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) }
                            )
                        }
                        else -> NavEntry(Route.Library as NavKey) { }
                    }
                }
            )
        }
    }
}
