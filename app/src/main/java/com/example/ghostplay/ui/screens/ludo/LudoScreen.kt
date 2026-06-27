package com.example.ghostplay.ui.screens.ludo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ghostplay.ui.theme.*
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LudoScreen(
    onBack: () -> Unit,
    viewModel: LudoViewModel = viewModel()
) {
    val lobbyState by viewModel.lobbyState.collectAsState()
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
    ) {
        // Futuristic grid pattern background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Surface, Color.Black.copy(alpha = 0.6f), Surface)
                    )
                )
        )

        val lobby = lobbyState
        if (lobby == null) {
            // Setup Screen
            LudoSetupView(
                viewModel = viewModel,
                onBack = onBack
            )
        } else {
            when (lobby.status) {
                "LOBBY" -> {
                    // Multiplayer Lobby Waiting Area
                    LudoOnlineLobbyView(
                        lobby = lobby,
                        viewModel = viewModel,
                        onLeave = { viewModel.cleanupLobby() }
                    )
                }
                "PLAYING" -> {
                    // Active Game Board
                    LudoGameplayView(
                        lobby = lobby,
                        viewModel = viewModel,
                        onBack = {
                            viewModel.cleanupLobby()
                            onBack()
                        }
                    )
                }
                "FINISHED" -> {
                    // Game End / Victory Screen
                    LudoVictoryView(
                        lobby = lobby,
                        onRestart = { viewModel.cleanupLobby() }
                    )
                }
            }
        }
    }
}

// --- Setup/Mode Selection View ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LudoSetupView(
    viewModel: LudoViewModel,
    onBack: () -> Unit
) {
    var modeSelected by remember { mutableStateOf<String?>(null) } // "LOCAL", "JOIN", "HOST"
    var localPlayerCount by remember { mutableStateOf(2) }
    var localBotCount by remember { mutableStateOf(2) }
    var lobbyCodeInput by remember { mutableStateOf("") }
    var joinError by remember { mutableStateOf<String?>(null) }
    var isJoining by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        TopAppBar(
            title = { Text("LUDO_NODE_SETUP", style = MaterialTheme.typography.labelMedium, letterSpacing = 2.sp) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "BACK")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Username Setup Card
        OutlinedTextField(
            value = viewModel.currentUserName.value,
            onValueChange = { viewModel.currentUserName.value = it },
            label = { Text("OPERATOR_CALLSIGN", style = MaterialTheme.typography.labelSmall) },
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Primary),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = OutlineVariant,
                focusedLabelColor = Primary,
                unfocusedLabelColor = OnSurfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (modeSelected == null) {
            // Main setup buttons
            Text(
                text = "SELECT_CONNECTION_METHOD",
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            SetupOptionCard(
                title = "LOCAL_SIMULATION_OFFLINE",
                subtitle = "Play with pass-and-play or customize AI bots.",
                icon = Icons.Rounded.Computer,
                tint = Secondary,
                onClick = { modeSelected = "LOCAL" }
            )

            Spacer(modifier = Modifier.height(16.dp))

            SetupOptionCard(
                title = "HOST_MULTIPLAYER_LOBBY",
                subtitle = "Generate a Lobby Node for remote players.",
                icon = Icons.Rounded.CloudUpload,
                tint = Primary,
                onClick = { viewModel.hostOnlineGame() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            SetupOptionCard(
                title = "JOIN_EXISTING_NODE",
                subtitle = "Enter lobby code to sync over cloud.",
                icon = Icons.Rounded.Login,
                tint = Tertiary,
                onClick = { modeSelected = "JOIN" }
            )

        } else if (modeSelected == "LOCAL") {
            // Local game customization
            Text(
                text = "LOCAL_SIMULATION_PARAMETERS",
                style = MaterialTheme.typography.labelMedium,
                color = Secondary,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Text("HUMAN_PLAYERS: $localPlayerCount", style = MaterialTheme.typography.bodyMedium, color = OnSurface)
            Slider(
                value = localPlayerCount.toFloat(),
                onValueChange = { localPlayerCount = it.toInt() },
                valueRange = 1f..4f,
                steps = 2,
                colors = SliderDefaults.colors(thumbColor = Secondary, activeTrackColor = Secondary)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("AI_BOT_PLAYERS: $localBotCount", style = MaterialTheme.typography.bodyMedium, color = OnSurface)
            Slider(
                value = localBotCount.toFloat(),
                onValueChange = { localBotCount = it.toInt() },
                valueRange = 0f..3f,
                steps = 2,
                colors = SliderDefaults.colors(thumbColor = Secondary, activeTrackColor = Secondary)
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val total = localPlayerCount + localBotCount
                    val finalBots = if (total > 4) 4 - localPlayerCount else localBotCount
                    viewModel.setupLocalGame(localPlayerCount, finalBots.coerceAtLeast(0))
                },
                modifier = Modifier.fillMaxWidth().height(56.dp).border(1.dp, Secondary, RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = Secondary.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("INITIATE_SIMULATION", color = Secondary, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = { modeSelected = null },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("CANCEL", color = OnSurfaceVariant)
            }

        } else if (modeSelected == "JOIN") {
            // Join Lobby fields
            Text(
                text = "ESTABLISHING_LINK_TO_NODE",
                style = MaterialTheme.typography.labelMedium,
                color = Tertiary,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            OutlinedTextField(
                value = lobbyCodeInput,
                onValueChange = { lobbyCodeInput = it.uppercase() },
                label = { Text("LOBBY_CODE", style = MaterialTheme.typography.labelSmall) },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Tertiary,
                    unfocusedBorderColor = OutlineVariant,
                    focusedLabelColor = Tertiary,
                    unfocusedLabelColor = OnSurfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            )

            joinError?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "LINK_FAILURE: $it", color = Error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (lobbyCodeInput.isNotEmpty()) {
                        isJoining = true
                        joinError = null
                        viewModel.joinOnlineGame(
                            code = lobbyCodeInput,
                            onSuccess = { isJoining = false },
                            onError = { err ->
                                isJoining = false
                                joinError = err
                            }
                        )
                    }
                },
                enabled = !isJoining && lobbyCodeInput.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(56.dp).border(1.dp, Tertiary, RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = Tertiary.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isJoining) {
                    CircularProgressIndicator(color = Tertiary, modifier = Modifier.size(24.dp))
                } else {
                    Text("CONNECT_NODE", color = Tertiary, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = { modeSelected = null },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("CANCEL", color = OnSurfaceVariant)
            }
        }
    }
}

@Composable
fun SetupOptionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLow)
            .border(1.dp, tint.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.width(20.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.labelMedium, color = OnSurface)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
        }
    }
}

// --- Online Lobby Room View ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LudoOnlineLobbyView(
    lobby: LudoLobby,
    viewModel: LudoViewModel,
    onLeave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(
            title = { Text("MULTIPLAYER_NODE_LOBBY", style = MaterialTheme.typography.labelMedium, letterSpacing = 2.sp) },
            navigationIcon = {
                IconButton(onClick = onLeave) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "LEAVE")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Large invite code display
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceContainer)
                .border(1.dp, Primary.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Text("INVITATION_SIGNAL_CODE", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = lobby.lobbyId,
                style = MaterialTheme.typography.displayMedium,
                color = Primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Share this signal with operators to join the matrix.",
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant.copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Players connection status list
        Text(
            text = "CONNECTED_NODES (${lobby.players.size}/4)",
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceVariant,
            modifier = Modifier.align(Alignment.Start).padding(bottom = 12.dp)
        )

        lobby.players.forEach { player ->
            val colorGlow = player.color.toNeonColor()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceContainerLow)
                    .border(1.dp, colorGlow.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(colorGlow)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurface,
                    fontWeight = FontWeight.Bold
                )
                if (player.isHost) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "[HOST]",
                        color = Primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (player.id == viewModel.currentUserId) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "YOU",
                        color = Secondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Fill remaining slots
        for (i in 1..(4 - lobby.players.size)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceContainerLow.copy(alpha = 0.4f))
                    .border(1.dp, OutlineVariant.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(OnSurfaceVariant.copy(alpha = 0.2f))
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "AWAITING_SIGNAL_NODE...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant.copy(alpha = 0.3f)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Start button (only host can trigger)
        val isHost = lobby.hostId == viewModel.currentUserId
        if (isHost) {
            Button(
                onClick = { viewModel.startOnlineGame() },
                modifier = Modifier.fillMaxWidth().height(56.dp).border(1.dp, Primary, RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = Primary.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("INITIATE_MULTIPLAYER_MATRIX", color = Primary, fontWeight = FontWeight.Bold)
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceContainerLow),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "WAITING_FOR_HOST_TO_ESTABLISH_LINK...",
                    color = OnSurfaceVariant.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

// --- Gameplay View ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LudoGameplayView(
    lobby: LudoLobby,
    viewModel: LudoViewModel,
    onBack: () -> Unit
) {
    val board = lobby.boardState
    val isMyTurn = !viewModel.isOnlineMode.value || (board.currentPlayer == viewModel.myPlayerColor.value)
    
    // Dice roll animations states
    val scope = rememberCoroutineScope()
    var isDiceRolling by remember { mutableStateOf(false) }
    var displayedRollNumber by remember { mutableStateOf(1) }

    // Trigger local roll UI animation when state updates with a new dice value
    LaunchedEffect(board.diceValue, board.diceRolled) {
        if (board.diceRolled && board.diceValue != null && !isDiceRolling) {
            isDiceRolling = true
            // Run rapid flicker animation
            for (i in 0..8) {
                displayedRollNumber = Random.nextInt(1, 7)
                delay(80)
            }
            displayedRollNumber = board.diceValue
            isDiceRolling = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(Secondary))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("MEGA-LUDO_V1", style = MaterialTheme.typography.labelMedium, letterSpacing = 2.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (viewModel.isOnlineMode.value) "CLOUD_SYNCED" else "OFFLINE_SIM",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "TERMINATE")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        // 1. The 3D Render Canvas Viewport
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            LudoBoard3D(
                boardState = board,
                onTokenClick = { token ->
                    if (isMyTurn && board.diceRolled) {
                        viewModel.moveToken(token)
                    }
                }
            )
        }

        // 2. Control HUD Dashboard
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(130.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left HUD Section: Active Player Profile & Dice
            val activeColor = board.currentPlayer.toNeonColor()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceContainerLow)
                    .border(1.dp, activeColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Turn Indicator
                Text(
                    text = "ACTIVE_TURN: ${board.currentPlayer.name}",
                    color = activeColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )

                // Dice / Roll Controller
                if (!board.diceRolled) {
                    if (isMyTurn) {
                        Button(
                            onClick = { viewModel.rollDice() },
                            colors = ButtonDefaults.buttonColors(containerColor = activeColor.copy(alpha = 0.2f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .border(1.dp, activeColor, RoundedCornerShape(8.dp)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("ROLL_DICE", color = activeColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    } else {
                        // Enemy player rolling
                        Text(
                            text = "WAITING_FOR_ROLL...",
                            color = OnSurfaceVariant.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                } else {
                    // Rolled Dice indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(activeColor.copy(alpha = 0.1f))
                            .border(1.dp, activeColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            Icons.Rounded.Casino,
                            contentDescription = null,
                            tint = activeColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "VALUE: $displayedRollNumber",
                            color = activeColor,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Subtitle
                Text(
                    text = if (isMyTurn) "YOUR_ACTION_REQUIRED" else "AWAITING_SIGNAL",
                    color = if (isMyTurn) Secondary else OnSurfaceVariant.copy(alpha = 0.4f),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp
                )
            }

            // Right HUD Section: Console Log Feed
            val logListState = rememberLazyListState()
            
            // Auto scroll console to bottom on new log
            LaunchedEffect(board.logs.size) {
                if (board.logs.isNotEmpty()) {
                    logListState.animateScrollToItem(board.logs.size - 1)
                }
            }

            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceContainerLowest)
                    .border(1.dp, OutlineVariant.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(4.dp).background(Tertiary))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SYSTEM_LOG_FEED", style = MaterialTheme.typography.labelSmall, color = Tertiary, fontSize = 8.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    state = logListState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(board.logs) { log ->
                        Text(
                            text = "> $log",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = if (log.contains("VICTORY") || log.contains("CAPTURE")) Secondary else OnSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// --- Victory Screen View ---
@Composable
fun LudoVictoryView(
    lobby: LudoLobby,
    onRestart: () -> Unit
) {
    val winners = lobby.boardState.winningPlayers

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Rounded.EmojiEvents,
            contentDescription = null,
            tint = Tertiary,
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "SIMULATION_COMPLETE",
            style = MaterialTheme.typography.displaySmall,
            color = OnSurface,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Rank Board
        Column(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceContainerLow)
                .border(1.dp, Tertiary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Text(
                text = "FINAL_RANKINGS",
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            winners.forEachIndexed { index, color ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "RANK_0${index + 1}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Tertiary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = color.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = color.toNeonColor(),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onRestart,
            modifier = Modifier.fillMaxWidth(0.8f).height(56.dp).border(1.dp, Tertiary, RoundedCornerShape(12.dp)),
            colors = ButtonDefaults.buttonColors(containerColor = Tertiary.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("RETURN_TO_BASE", color = Tertiary, fontWeight = FontWeight.Bold)
        }
    }
}
