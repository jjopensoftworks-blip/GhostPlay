package com.example.ghostplay.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ghostplay.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = Surface,
        topBar = {
            TopAppBar(
                title = { Text("NETWORK_LOGS", style = MaterialTheme.typography.labelMedium, letterSpacing = 2.sp) },
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
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    SummarySection(uiState)
                }

                item {
                    GlobalPulseSection(uiState)
                }
                
                item {
                    Text(
                        text = "SESSION_ANALYTICS",
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
                                .height(100.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceContainerLowest),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("NO_DATA_STREAM_FOUND", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant.copy(alpha = 0.3f))
                        }
                    }
                } else {
                    items(uiState.mostPlayedGames) { stat ->
                        LogEntryItem(stat)
                    }
                }
            }
        }
    }
}

@Composable
fun SummarySection(uiState: DashboardUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatPanel(
            modifier = Modifier.weight(1.5f),
            title = "TOTAL_UPTIME",
            value = formatDuration(uiState.totalPlaytime),
            icon = Icons.Rounded.Timeline,
            tint = Secondary
        )
        StatPanel(
            modifier = Modifier.weight(1f),
            title = "ACTIVE_NODES",
            value = uiState.totalGames.toString(),
            icon = Icons.Rounded.Lan,
            tint = Tertiary
        )
    }
}

@Composable
fun GlobalPulseSection(uiState: DashboardUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLow)
            .border(1.dp, Tertiary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Public, contentDescription = null, tint = Tertiary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("GLOBAL_PULSE_NETWORK", style = MaterialTheme.typography.labelSmall, color = Tertiary)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "${uiState.globalPlayerCount} ACTIVE_PLAYERS", style = MaterialTheme.typography.headlineSmall, color = OnSurface, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        
        uiState.groupStats.forEach { (group, count) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = group, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant, fontSize = 10.sp)
                Text(text = "$count", style = MaterialTheme.typography.labelSmall, color = OnSurface, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun StatPanel(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    tint: Color
) {
    Box(
        modifier = modifier
            .height(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainer)
            .border(1.dp, tint.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.weight(1f))
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant, fontSize = 9.sp)
            Text(text = value, style = MaterialTheme.typography.headlineMedium, color = OnSurface, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LogEntryItem(stat: GameStat) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainerLow)
            .border(1.dp, OutlineVariant.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(4.dp, 32.dp)
                .background(Primary)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stat.gameName.uppercase(), style = MaterialTheme.typography.labelMedium, color = OnSurface)
            Text(text = "SESSION_COUNT: ${stat.sessionCount}", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant, fontSize = 10.sp)
        }
        Text(
            text = formatDuration(stat.totalPlaytime),
            style = MaterialTheme.typography.bodyLarge,
            color = Secondary,
            fontWeight = FontWeight.Bold
        )
    }
}
