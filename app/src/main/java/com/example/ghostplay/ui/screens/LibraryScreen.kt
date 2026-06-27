package com.example.ghostplay.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ghostplay.data.model.Game
import com.example.ghostplay.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onAddGame: () -> Unit,
    onViewStatistics: () -> Unit,
    onGameClick: (String) -> Unit,
    onInstantGameClick: (String) -> Unit,
    viewModel: LibraryViewModel = viewModel()
) {
    val games by viewModel.games.collectAsState()

    Scaffold(
        containerColor = Surface,
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Shield, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CYBERPULSE", style = MaterialTheme.typography.labelMedium, letterSpacing = 2.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("OS_V_2.4", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant.copy(alpha = 0.5f))
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Rounded.Notifications, contentDescription = null, tint = OnSurface)
                    }
                    Box(modifier = Modifier.padding(end = 16.dp).size(32.dp).clip(RoundedCornerShape(4.dp)).background(SurfaceBright))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Main Banner
            item {
                MainBanner()
            }

            // Instant Social Nodes
            item {
                SectionHeader("INSTANT_SOCIAL_NODES", "GRID_DENSITY: HIGH")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { InstantGameCard("Neo-Chess", "1-2P • INSTANT", Primary, onClick = { onInstantGameClick("CHESS") }) }
                    item { InstantGameCard("Mega Ludo", "16P • CHAOS", Secondary, onClick = { onInstantGameClick("LUDO") }) }
                    item { InstantGameCard("Social Poker", "20P • EXPRESS", Tertiary, onClick = { onInstantGameClick("POKER") }) }
                }
            }

            // Global Index
            item {
                Spacer(modifier = Modifier.height(32.dp))
                SectionHeader("GLOBAL_INDEX", "") {
                    // Filter chips placeholder
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = true, onClick = {}, label = { Text("ALL") })
                        FilterChip(selected = false, onClick = {}, label = { Text("RPG") })
                        FilterChip(selected = false, onClick = {}, label = { Text("FPS") })
                    }
                }
            }

            items(games) { game ->
                GlobalIndexItem(game, onClick = { onGameClick(game.id) })
            }

            item {
                AddGameButton(onClick = onAddGame)
            }
        }
    }
}

@Composable
fun MainBanner() {
    Box(
        modifier = Modifier
            .padding(20.dp)
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainer)
            .border(1.dp, OutlineVariant.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.Red))
                Spacer(modifier = Modifier.width(8.dp))
                Text("LIVE EVENT", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                Spacer(modifier = Modifier.width(16.dp))
                Text("PING: 14ms • 24.5k OPS", style = MaterialTheme.typography.labelSmall, color = Secondary)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("NEON REBELLION\nPHASE 02_", style = MaterialTheme.typography.displayMedium, color = OnSurface)
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {},
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary.copy(alpha = 0.2f)),
                modifier = Modifier.border(1.dp, Primary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            ) {
                Text("INITIATE", color = Primary, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String, content: @Composable () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(4.dp).height(24.dp).background(Secondary))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = title, style = MaterialTheme.typography.labelMedium, color = OnSurface)
        }
        if (subtitle.isNotEmpty()) {
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant.copy(alpha = 0.5f))
        }
        content()
    }
}

@Composable
fun InstantGameCard(name: String, meta: String, tint: Color, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(160.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceContainerHigh)
                .border(1.dp, tint.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
        ) {
            // Placeholder for game art
            Icon(
                Icons.Rounded.Gamepad,
                contentDescription = null,
                modifier = Modifier.align(Alignment.Center).size(48.dp),
                tint = tint.copy(alpha = 0.2f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = meta, style = MaterialTheme.typography.labelSmall, color = tint, fontSize = 10.sp)
        Text(text = name, style = MaterialTheme.typography.bodyLarge, color = OnSurface, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun GlobalIndexItem(game: Game, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainerLow)
            .border(1.dp, OutlineVariant.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceBright))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = game.name.uppercase(), style = MaterialTheme.typography.labelMedium, color = OnSurface)
            Text(text = "${game.platform} // ENV_SIM", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant.copy(alpha = 0.5f))
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Tertiary.copy(alpha = 0.1f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text("NEW_ENTRY", color = Tertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AddGameButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(20.dp)
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Transparent)
            .border(1.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.Add, contentDescription = null, tint = OnSurfaceVariant)
            Text("REGISTER_NEW_MODULE", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
        }
    }
}
