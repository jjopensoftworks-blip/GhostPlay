package com.example.ghostplay.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ghostplay.ui.theme.*
import com.example.ghostplay.ui.screens.ludo.LudoScreen
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamePlayScreen(
    gameType: String,
    onBack: () -> Unit,
    viewModel: GameDetailsViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            // Reusing GameDetailsViewModel for session tracking.
            // In a real app, we'd have a specific SessionViewModel.
            return GameDetailsViewModel(gameType) as T
        }
    })
) {
    val activeSessionId by viewModel.activeSessionId.collectAsState()
    val elapsedTime by viewModel.elapsedTime.collectAsState()

    // Start tracking as soon as we enter
    LaunchedEffect(Unit) {
        viewModel.startTracking()
    }

    if (gameType == "LUDO") {
        LudoScreen(onBack = onBack)
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Surface)
        ) {
            // High-fidelity background image (simulated with brush/placeholder)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Surface, Color.Black.copy(alpha = 0.5f), Surface)
                        )
                    )
            )

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                TopAppBar(
                    title = { 
                        Text(
                            text = if (gameType == "CHESS") "NEO-CHESS_BETA" else "MEGA-LUDO_V1",
                            style = MaterialTheme.typography.labelMedium,
                            letterSpacing = 2.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            viewModel.stopTracking()
                            onBack()
                        }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "TERMINATE")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                        .background(SurfaceContainerLow.copy(alpha = 0.8f)), // Glassmorphism
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "SIMULATING_${gameType}_ENVIRONMENT",
                            style = MaterialTheme.typography.bodySmall,
                            color = Secondary
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Simple simulated game visual
                        Text(
                            text = formatDuration(elapsedTime),
                            style = MaterialTheme.typography.displayLarge,
                            color = Primary
                        )
                    }
                }

                // Bottom Control Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .height(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceContainerHigh),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Text(
                        text = "STATUS: ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Tertiary
                    )
                    VerticalDivider(modifier = Modifier.height(20.dp), color = OutlineVariant)
                    Text(
                        text = "OPS: 24.5k",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant
                    )
                }
            }
        }
    }
}
