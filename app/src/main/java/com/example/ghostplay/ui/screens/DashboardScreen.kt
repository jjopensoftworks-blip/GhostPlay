package com.example.ghostplay.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ghostplay.data.repository.UserPreferencesRepository
import com.example.ghostplay.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    val viewModel: DashboardViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(
                userPreferencesRepository = UserPreferencesRepository(context)
            ) as T
        }
    })
    
    val uiState by viewModel.uiState.collectAsState()
    val userName by viewModel.userName.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "profile_glow")
    val avatarGlowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Scaffold(
        containerColor = Surface,
        topBar = {
            TopAppBar(
                title = { Text("OPERATOR_STATS", style = MaterialTheme.typography.labelMedium, letterSpacing = 2.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Secondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // User Profile Header Card
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(SurfaceContainerLow, SurfaceContainerLowest)
                                )
                            )
                            .border(1.dp, Secondary.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                            .padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // Pulsing Avatar Ring
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .scale(avatarGlowScale)
                                    .clip(CircleShape)
                                    .background(Secondary.copy(alpha = 0.15f))
                                    .border(2.dp, Secondary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = null,
                                    tint = Secondary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = userName,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = OnSurface,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 24.sp
                                )
                                Text(
                                    text = "GHOSTPLAY_OPERATOR",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OnSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }

                // Win/Loss Graphical Segment Bar
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceContainer)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "PERFORMANCE_RATIO",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                        ) {
                            val winsWeight = if (uiState.totalGamesPlayed > 0) uiState.totalWins.toFloat() / uiState.totalGamesPlayed.toFloat() else 0.5f
                            val lossesWeight = 1f - winsWeight
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(winsWeight.coerceAtLeast(0.01f))
                                    .background(Color(0xFF00FF9D)) // Glowing Green for wins
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(lossesWeight.coerceAtLeast(0.01f))
                                    .background(Color(0xFFFF2A7A)) // Glowing Pink/Red for losses
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF00FF9D)))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("${uiState.totalWins} WINS", style = MaterialTheme.typography.labelSmall, color = OnSurface)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFF2A7A)))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("${uiState.totalLosses} LOSSES", style = MaterialTheme.typography.labelSmall, color = OnSurface)
                            }
                        }
                    }
                }

                // Grid of Stats
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        StatCard(
                            title = "MATCHES",
                            value = uiState.totalGamesPlayed.toString(),
                            icon = Icons.Rounded.Casino,
                            color = Primary,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "WIN_RATE",
                            value = String.format("%.1f%%", uiState.winRatio),
                            icon = Icons.Rounded.EmojiEvents,
                            color = Tertiary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        StatCard(
                            title = "PLAY_TIME",
                            value = formatDuration(uiState.totalPlaytime),
                            icon = Icons.Rounded.Schedule,
                            color = Secondary,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "COMPLETED",
                            value = uiState.mostPlayedGames.sumOf { it.sessionCount }.toString(),
                            icon = Icons.Rounded.CheckCircle,
                            color = Color(0xFF00FF9D),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Game breakdown title
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "GAME_BREAKDOWN",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                }

                if (uiState.mostPlayedGames.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceContainerLow),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("NO_GAME_DATA", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant.copy(alpha = 0.3f))
                        }
                    }
                } else {
                    items(uiState.mostPlayedGames) { stat ->
                        GameStatRow(stat)
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLow)
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.weight(1f))
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant, fontSize = 9.sp)
            Text(text = value, style = MaterialTheme.typography.headlineSmall, color = OnSurface, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun GameStatRow(stat: GameStat) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainerLow)
            .border(1.dp, OutlineVariant.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(4.dp, 24.dp)
                .background(Primary)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stat.gameName.uppercase(), style = MaterialTheme.typography.labelMedium, color = OnSurface, fontSize = 12.sp)
            Text(text = "SESSIONS: ${stat.sessionCount}", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant, fontSize = 9.sp)
        }
        Text(
            text = formatDuration(stat.totalPlaytime),
            style = MaterialTheme.typography.bodyMedium,
            color = Secondary,
            fontWeight = FontWeight.Bold
        )
    }
}
