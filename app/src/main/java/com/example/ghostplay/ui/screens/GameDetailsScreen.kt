package com.example.ghostplay.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ghostplay.data.model.Session
import com.example.ghostplay.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailsScreen(
    gameId: String,
    onBack: () -> Unit,
    viewModel: GameDetailsViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return GameDetailsViewModel(gameId) as T
        }
    })
) {
    val game by viewModel.game.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val activeSessionId by viewModel.activeSessionId.collectAsState()
    val elapsedTime by viewModel.elapsedTime.collectAsState()
    val totalPlaytime by viewModel.totalPlaytime.collectAsState()

    Scaffold(
        containerColor = Surface,
        topBar = {
            TopAppBar(
                title = { Text(text = game?.name?.uppercase() ?: "NODE_SYNCING...", style = MaterialTheme.typography.labelMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main Stat Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceContainer)
                    .border(1.dp, Primary.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (activeSessionId != null) "PULSE_ACTIVE" else "CUMULATIVE_UPTIME",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (activeSessionId != null) Tertiary else OnSurfaceVariant
                    )
                    Text(
                        text = if (activeSessionId != null) formatDuration(elapsedTime) else formatDuration(totalPlaytime),
                        style = MaterialTheme.typography.displayLarge,
                        color = if (activeSessionId != null) Secondary else OnSurface
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            if (activeSessionId == null) viewModel.startTracking() else viewModel.stopTracking()
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeSessionId != null) Error.copy(alpha = 0.2f) else Primary.copy(alpha = 0.2f),
                            contentColor = if (activeSessionId != null) Error else Primary
                        ),
                        border = borderStroke(activeSessionId != null)
                    ) {
                        Icon(
                            imageVector = if (activeSessionId == null) Icons.Rounded.PlayArrow else Icons.Rounded.Stop,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (activeSessionId == null) "INITIATE_PULSE" else "TERMINATE_PULSE")
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "HISTORICAL_LOGS",
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant,
                modifier = Modifier.align(Alignment.Start),
                letterSpacing = 1.sp
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sessions.sortedByDescending { it.startTime }) { session ->
                    SessionLogItem(session)
                }
            }
        }
    }
}

@Composable
fun borderStroke(active: Boolean) = androidx.compose.foundation.BorderStroke(
    1.dp, 
    if (active) Error.copy(alpha = 0.5f) else Primary.copy(alpha = 0.5f)
)

@Composable
fun SessionLogItem(session: Session) {
    val dateFormat = remember { SimpleDateFormat("HH:mm // dd.MM.yy", Locale.getDefault()) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainerLow)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = dateFormat.format(Date(session.startTime)),
                style = MaterialTheme.typography.labelSmall,
                color = OnSurface
            )
            Text(
                text = "STABLE_CONNECTION",
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant.copy(alpha = 0.5f),
                fontSize = 8.sp
            )
        }
        Text(
            text = formatDuration(session.duration),
            style = MaterialTheme.typography.bodyMedium,
            color = Secondary,
            fontWeight = FontWeight.Bold
        )
    }
}

fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        String.format("%02d:%02d:%02d", h, m, s)
    } else {
        String.format("%02d:%02d", m, s)
    }
}
