package com.example.ghostplay.ui.screens.ludo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ghostplay.ui.screens.ludo.components.EmojiChannel
import com.example.ghostplay.ui.theme.*
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
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
            title = { Text("GhostPlay Setup", style = MaterialTheme.typography.labelMedium, letterSpacing = 2.sp) },
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
            title = { Text("GhostPlay Lobby", style = MaterialTheme.typography.labelMedium, letterSpacing = 2.sp) },
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
        repeat(4 - lobby.players.size) {
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
            repeat(13) {
                displayedRollNumber = Random.nextInt(1, 7)
                delay(60.milliseconds)
            }
            displayedRollNumber = board.diceValue
            isDiceRolling = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF060E20))
    ) {
        // 1. Header (GHOSTPLAY)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Casino,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "GHOSTPLAY",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
            
            // Profile / Settings icon placeholder
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceContainerLow)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Person, contentDescription = null, tint = Color.White)
            }
        }

        // 2. Status Row (Latency & Turn)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.3f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF00FF9D)))
                Spacer(modifier = Modifier.width(8.dp))
                Text("12ms", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
            }

            val activeColor = board.currentPlayer.toNeonColor()
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(activeColor.copy(alpha = 0.15f))
                    .border(1.dp, activeColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${board.currentPlayer.name}'S TURN".uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = activeColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 3. The 3D Render Canvas Viewport
        Box(
            modifier = Modifier
                .weight(1.2f) // Give board more weight
                .fillMaxWidth()
                .padding(horizontal = 4.dp), // Minimal padding for maximum width
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
            
            // Dice Overlay (Floating above board - shifted to side so not blocking center)
            if (board.diceRolled || isDiceRolling) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp)
                ) {
                    LudoDiceView(
                        number = displayedRollNumber,
                        isRolling = isDiceRolling,
                        color = board.currentPlayer.toNeonColor(),
                        size = 70.dp // Slightly larger dice
                    )
                }
            }
        }

        // 4. Control HUD Dashboard & Emoji Channel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Emoji Reaction Channel
                EmojiChannel(
                    onEmojiSelected = { viewModel.sendEmoji(it) },
                    activeEmojis = board.emojis,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // ROLL Button
                if (!board.diceRolled && isMyTurn) {
                    Button(
                        onClick = { viewModel.rollDice() },
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF).copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(28.dp),
                        border = BorderStroke(2.dp, Brush.linearGradient(listOf(Color(0xFF00E5FF), Color(0xFF00FF9D))))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Casino, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("ROLL", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Black)
                        }
                    }
                } else {
                    // Status Text
                    val statusText = if (isMyTurn) "SELECT A TOKEN TO MOVE" else "WAITING FOR ${board.currentPlayer.name}"
                    Text(
                        text = statusText.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isSelected: Boolean = false) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(8.dp)
            .alpha(if (isSelected) 1f else 0.5f)
    ) {
        Icon(icon, contentDescription = null, tint = if (isSelected) Color(0xFF00E5FF) else Color.White, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 10.sp)
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
