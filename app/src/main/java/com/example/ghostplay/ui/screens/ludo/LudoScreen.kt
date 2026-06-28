package com.example.ghostplay.ui.screens.ludo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ghostplay.ui.screens.ludo.components.EmojiChannel
import com.example.ghostplay.ui.theme.*
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import kotlin.math.roundToInt



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LudoScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: LudoViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return LudoViewModel(
                userPrefs = com.example.ghostplay.data.repository.UserPreferencesRepository(context)
            ) as T
        }
    })

    val lobbyState by viewModel.lobbyState.collectAsState()
    var activeSubTab by remember { mutableStateOf("PLAY") } // "PLAY", "INVITES", "HISTORY", "SAFETY"

    // Floating challenge notification overlay
    var showChallengeNotification by remember { mutableStateOf(false) }
    var challengerName by remember { mutableStateOf("") }
    
    // Automatically trigger invite challenge mockup after 5 seconds in INVITES tab
    LaunchedEffect(activeSubTab) {
        if (activeSubTab == "INVITES" && !showChallengeNotification) {
            delay(5000.milliseconds)
            challengerName = "GhostMaster99"
            showChallengeNotification = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF060E20))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                when (activeSubTab) {
                    "PLAY" -> {
                        val lobby = lobbyState
                        if (lobby == null) {
                            LudoSetupView(
                                viewModel = viewModel,
                                onBack = onBack
                            )
                        } else {
                            when (lobby.status) {
                                "LOBBY" -> {
                                    LudoOnlineLobbyView(
                                        lobby = lobby,
                                        viewModel = viewModel,
                                        onLeave = { viewModel.cleanupLobby() }
                                    )
                                }
                                "PLAYING" -> {
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
                                    LudoVictoryView(
                                        lobby = lobby,
                                        onRestart = { viewModel.cleanupLobby() }
                                    )
                                }
                            }
                        }
                    }
                    "INVITES" -> {
                        LudoInvitesView(
                            viewModel = viewModel,
                            onLaunchGame = { activeSubTab = "PLAY" }
                        )
                    }
                    "HISTORY" -> {
                        LudoHistoryView()
                    }
                    "SAFETY" -> {
                        LudoSafetyView()
                    }
                }
            }

            LudoBottomSubTabBar(
                activeTab = activeSubTab,
                onTabSelected = { activeSubTab = it }
            )
        }

        // Incoming challenge overlay
        if (showChallengeNotification) {
            AlertDialog(
                onDismissRequest = { showChallengeNotification = false },
                containerColor = Color(0xFF0F172A),
                modifier = Modifier.border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(24.dp)),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = Color(0xFF00E5FF))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Challenge challenge", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    }
                },
                text = {
                    Text(
                        text = "$challengerName has challenged you to a 3D Neon Ludo Match!",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showChallengeNotification = false
                            // Configures PvP
                            val players = listOf(
                                LudoPlayer(id = viewModel.currentUserId, name = viewModel.currentUserName.value, color = LudoColor.RED, isHost = true),
                                LudoPlayer(id = "CHALLENGER_GP", name = challengerName, color = LudoColor.BLUE, isBot = false)
                            )
                            viewModel.isOnlineMode.value = true
                            viewModel.lobbyCode.value = "LUDO-Challenge"
                            val lobby = LudoLobby(
                                lobbyId = "LUDO-Challenge",
                                status = "PLAYING",
                                hostId = viewModel.currentUserId,
                                players = players,
                                boardState = LudoBoardState(
                                    currentPlayer = LudoColor.RED,
                                    logs = listOf("CHALLENGE_LINK_ESTABLISHED", "RED_PLAYER_TURN")
                                )
                            )
                            viewModel.setLobby(lobby)
                            activeSubTab = "PLAY"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9D))
                    ) {
                        Text("ACCEPT & LAUNCH", color = Color.Black, fontWeight = FontWeight.Black)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showChallengeNotification = false }) {
                        Text("DECLINE", color = Color.White.copy(alpha = 0.6f))
                    }
                }
            )
        }
    }
}

// --- Redesigned PLAY Setup/Launcher Dashboard View ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LudoSetupView(
    viewModel: LudoViewModel,
    onBack: () -> Unit
) {
    var botDifficultySliderValue by remember { mutableStateOf(1f) } // 0=Easy, 1=Medium, 2=Hard
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(Color(0xFF040A18))
    ) {
        // --- 1. Immersive Top 65% Viewport (Zero Padding) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(440.dp)
                .background(Color(0xFF040A18))
        ) {
            val initialMockState = remember {
                LudoBoardState(
                    tokens = LudoBoardState.initialTokens(),
                    currentPlayer = LudoColor.RED,
                    diceRolled = false
                )
            }
            LudoBoard3D(
                boardState = initialMockState,
                onTokenClick = {},
                modifier = Modifier.fillMaxSize()
            )

            // Overlays: Latency & Turn
            // Latency Indicator (Top-Left)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00FF9D))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "10ms",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            // Turn Indicator (Top-Right)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFF2A7A).copy(alpha = 0.15f))
                    .border(1.dp, Color(0xFFFF2A7A).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "RED'S TURN",
                    color = Color(0xFFFF2A7A),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black
                )
            }
            
            // Back button
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        }

        // --- 2. Translucent Glassmorphism Launcher Container (35%) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(185.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.22f), RoundedCornerShape(24.dp))
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card A: PVP (Left)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF080F20).copy(alpha = 0.75f))
                        .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .clickable { viewModel.hostOnlineGame() }
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "PVP",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "(actual person)",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 10.sp
                        )
                    }
                }

                // Card B: BOT (Right)
                Box(
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF080F20).copy(alpha = 0.75f))
                        .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .clickable {
                            val diff = when (botDifficultySliderValue.roundToInt()) {
                                0 -> "Easy"
                                1 -> "Medium"
                                else -> "Hard"
                            }
                            viewModel.setupLocalGame(playerCount = 1, botCount = 3, difficulty = diff)
                        }
                        .padding(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "BOT",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(AI)",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                imageVector = Icons.Rounded.SmartToy,
                                contentDescription = null,
                                tint = Color(0xFFFF2A7A),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Slider(
                                value = botDifficultySliderValue,
                                onValueChange = { botDifficultySliderValue = it },
                                valueRange = 0f..2f,
                                steps = 1,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF00E5FF),
                                    activeTrackColor = Color(0xFF00E5FF),
                                    inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.height(28.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Easy",
                                    color = if (botDifficultySliderValue.roundToInt() == 0) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.4f),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Medium",
                                    color = if (botDifficultySliderValue.roundToInt() == 1) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.4f),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Hard",
                                    color = if (botDifficultySliderValue.roundToInt() == 2) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.4f),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Floating Refractive Glass Dice
            LudoDiceView(
                number = 4,
                isRolling = false,
                size = 64.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // --- 3. Scrollable columns underneath (INVITES, HISTORY, SAFETY) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InviteCardMock()
            HistoryCardMock()
            SafetyCardMock()
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteCardMock() {
    Column(
        modifier = Modifier
            .width(260.dp)
            .height(290.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF080F20))
            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Rounded.ChevronLeft,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "INVITES",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF040A18))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "SEARCH USERNAME",
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Recent Players",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        
        val players = listOf(
            Pair("@PlayerOne", "Pending"),
            Pair("@PlayerTwo", "Ready"),
            Pair("@PlayerThree", "Ready"),
            Pair("@PlayerFour", "Ready")
        )
        
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            players.forEach { (name, status) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = name,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Username",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 8.sp
                        )
                    }
                    val isPending = status == "Pending"
                    Text(
                        text = if (isPending) "• Pending" else "• Ready",
                        color = if (isPending) Color(0xFFFFE500) else Color(0xFF00FF9D),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryCardMock() {
    Column(
        modifier = Modifier
            .width(260.dp)
            .height(290.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF080F20))
            .border(1.dp, Color(0xFFFF2A7A).copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Rounded.ChevronLeft,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "HISTORY",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF040A18))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "SEARCH USERNAME",
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0B1226))
                        .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ludo - Match 345 | Green vs.",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Yellow | Winner:",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Green",
                                color = Color(0xFF00FF9D),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "Stats (Cancelled)",
                        color = Color(0xFFFF2A7A),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SafetyCardMock() {
    Column(
        modifier = Modifier
            .width(260.dp)
            .height(290.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF080F20))
            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Rounded.ChevronLeft,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "SAFETY",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF040A18))
                .padding(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF0B1226)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Data",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Safety",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Data Privacy",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Secure connections match security, so your data and privacy are protected.",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 7.5.sp,
                        lineHeight = 10.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF00FF9D),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Secure Connections",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Secure connections are matching secure connections, ensuring credibility.",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 7.5.sp,
                        lineHeight = 10.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "General Safety",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "General security and game safety settings in close online safety.",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 7.5.sp,
                        lineHeight = 10.sp
                    )
                }
            }
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
            title = { Text("LUDO_LOBBY", style = MaterialTheme.typography.labelMedium, letterSpacing = 2.sp) },
            navigationIcon = {
                IconButton(onClick = onLeave) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "LEAVE")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceContainer)
                .border(1.dp, Primary.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Text("SIGNAL_CODE", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = lobby.lobbyId,
                style = MaterialTheme.typography.displayMedium,
                color = Primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

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
                    Text(text = "[HOST]", color = Primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                if (player.id == viewModel.currentUserId) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(text = "YOU", color = Secondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

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
    
    var isDiceRolling by remember { mutableStateOf(false) }
    var displayedRollNumber by remember { mutableStateOf(1) }

    // Disconnect countdown alerts
    val disconnectTimer by viewModel.disconnectCountdown
    val disconnectedPlayerColor by viewModel.disconnectedPlayerColor

    LaunchedEffect(board.diceValue, board.diceRolled) {
        if (board.diceRolled && board.diceValue != null && !isDiceRolling) {
            isDiceRolling = true
            repeat(13) {
                displayedRollNumber = Random.nextInt(1, 7)
                delay(60.milliseconds)
            }
            displayedRollNumber = board.diceValue
            isDiceRolling = false
        }
    }

    if (disconnectedPlayerColor != null && disconnectTimer != null) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = Color(0xFF0F172A),
            title = {
                Text(
                    text = "DISCONNECT DETECTED",
                    color = Color(0xFFFF2A7A),
                    fontWeight = FontWeight.Black
                )
            },
            text = {
                Text(
                    text = "Operator ${disconnectedPlayerColor?.name} disconnected. Takeover countdown: ${disconnectTimer}s. Bot will take control.",
                    color = Color.White
                )
            },
            confirmButton = {},
            dismissButton = {}
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF060E20))
    ) {
        // 1. Header (GHOSTPLAY)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Casino,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "GHOSTPLAY",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
            
            // Profile indicator
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SurfaceContainerLow)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }

        // 2. Status Row (Latency & Turn)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp),
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
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF00FF9D)))
                Spacer(modifier = Modifier.width(8.dp))
                Text("10ms", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
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
                .weight(1.3f)
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
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

        // 4. Pedestal + floating dice
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .padding(bottom = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            // Glowing Pedestal Base
            Box(
                modifier = Modifier
                    .size(90.dp, 36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A))
                    .border(2.dp, Color(0xFF00E5FF).copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                    .clickable {
                        if (!board.diceRolled && isMyTurn && !isDiceRolling) {
                            viewModel.rollDice()
                        }
                    }
            )
            
            // Floating 3D White Dice with black dots
            LudoDiceView(
                number = displayedRollNumber,
                isRolling = isDiceRolling,
                color = board.currentPlayer.toNeonColor(),
                size = 64.dp,
                modifier = Modifier
                    .offset(y = (-18).dp)
                    .clickable {
                        if (!board.diceRolled && isMyTurn && !isDiceRolling) {
                            viewModel.rollDice()
                        }
                    }
            )
        }

        // 5. Control HUD & Emojis
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                EmojiChannel(
                    onEmojiSelected = { viewModel.sendEmoji(it) },
                    activeEmojis = board.emojis,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val canRoll = !board.diceRolled && isMyTurn
                Button(
                    onClick = { viewModel.rollDice() },
                    enabled = canRoll,
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(48.dp)
                        .alpha(if (canRoll) 1f else 0.5f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00E5FF).copy(alpha = 0.2f),
                        disabledContainerColor = Color.Gray.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(
                        width = 2.dp,
                        brush = if (canRoll) {
                            Brush.linearGradient(listOf(Color(0xFF00E5FF), Color(0xFF00FF9D)))
                        } else {
                            SolidColor(Color.Gray.copy(alpha = 0.3f))
                        }
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Casino, 
                            contentDescription = null, 
                            tint = if (canRoll) Color.White else Color.Gray, 
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "ROLL", 
                            style = MaterialTheme.typography.titleMedium, 
                            color = if (canRoll) Color.White else Color.Gray, 
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                if (!canRoll) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val statusText = if (isMyTurn) "SELECT PIECE" else "WAITING..."
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
            text = "MATCH COMPLETE",
            style = MaterialTheme.typography.displaySmall,
            color = OnSurface,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceContainerLow)
                .border(1.dp, Tertiary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Text(
                text = "RANKINGS",
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
                        text = "RANK 0${index + 1}",
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
            Text("RETURN", color = Tertiary, fontWeight = FontWeight.Bold)
        }
    }
}

// --- Working Bottom Sub-Tabs Layout ---
@Composable
fun LudoBottomSubTabBar(activeTab: String, onTabSelected: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color(0xFF0B0F19))
            .border(1.dp, Color.White.copy(alpha = 0.05f)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        SubTabItem("PLAY", Icons.Rounded.Casino, activeTab == "PLAY", onTabSelected)
        SubTabItem("INVITES", Icons.Rounded.Group, activeTab == "INVITES", onTabSelected)
        SubTabItem("HISTORY", Icons.Rounded.History, activeTab == "HISTORY", onTabSelected)
        SubTabItem("SAFETY", Icons.Rounded.Shield, activeTab == "SAFETY", onTabSelected)
    }
}

@Composable
fun RowScope.SubTabItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onTabSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable { onTabSelected(label) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.4f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// --- Sub-Tab Invite View ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LudoInvitesView(
    viewModel: LudoViewModel,
    onLaunchGame: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    val friendStates = remember {
        mutableStateMapOf(
            "GhostMaster99" to "ONLINE",
            "NeonRider" to "ONLINE",
            "ShadowHex" to "OFFLINE",
            "PixelBot_Hard" to "ONLINE"
        )
    }
    var challengeSentPlayer by remember { mutableStateOf<String?>(null) }
    var challengeStatusText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "CHALLENGE_NETWORK",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search operator username...", color = Color.White.copy(alpha = 0.6f)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF00E5FF),
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "OPERATOR_NODES",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.align(Alignment.Start).padding(bottom = 12.dp)
        )

        val filteredFriends = friendStates.filter { it.key.contains(searchQuery, ignoreCase = true) }

        if (filteredFriends.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.02f)),
                contentAlignment = Alignment.Center
            ) {
                Text("NO_NODES_FOUND", color = Color.White.copy(alpha = 0.3f), style = MaterialTheme.typography.bodySmall)
            }
        } else {
            filteredFriends.forEach { (name, status) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, if (status == "ONLINE") Color(0xFF00FF9D).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (status == "ONLINE") Color(0xFF00FF9D) else Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (status == "ONLINE") {
                        val isSent = challengeSentPlayer == name
                        Button(
                            onClick = {
                                challengeSentPlayer = name
                                challengeStatusText = "Challenging..."
                                coroutineScope.launch {
                                    delay(2000.milliseconds)
                                    challengeStatusText = "Accepted!"
                                    delay(500.milliseconds)
                                    val players = listOf(
                                        LudoPlayer(id = viewModel.currentUserId, name = viewModel.currentUserName.value, color = LudoColor.RED, isHost = true),
                                        LudoPlayer(id = "FRIEND_99", name = name, color = LudoColor.BLUE, isBot = false)
                                    )
                                    viewModel.isOnlineMode.value = true
                                    viewModel.lobbyCode.value = "LUDO-PVP"
                                    val lobby = LudoLobby(
                                        lobbyId = "LUDO-PVP",
                                        status = "PLAYING",
                                        hostId = viewModel.currentUserId,
                                        players = players,
                                        boardState = LudoBoardState(
                                            currentPlayer = LudoColor.RED,
                                            logs = listOf("PVP_HANDSHAKE_ESTABLISHED", "RED_PLAYER_TURN")
                                        )
                                    )
                                    viewModel.setLobby(lobby)
                                    onLaunchGame()
                                    challengeSentPlayer = null
                                }
                            },
                            enabled = challengeSentPlayer == null,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF).copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, Color(0xFF00E5FF)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isSent) challengeStatusText else "CHALLENGE",
                                color = Color(0xFF00E5FF),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black
                            )
                        }
                    } else {
                        Text(
                            text = "OFFLINE",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.3f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// --- Sub-Tab History View ---
@Composable
fun LudoHistoryView() {
    val historySessions = remember {
        listOf(
            mapOf("id" to "GP_LUDO_2026_98231A", "mode" to "PVP", "result" to "Winner", "duration" to "17m 45s", "color" to Color(0xFFFF2A7A), "date" to "2026-06-28 22:54"),
            mapOf("id" to "GP_LUDO_2026_94301B", "mode" to "BOT (Hard)", "result" to "Defeated", "duration" to "12m 30s", "color" to Color(0xFFFFE500), "date" to "2026-06-27 18:15"),
            mapOf("id" to "GP_LUDO_2026_91288C", "mode" to "PVP", "result" to "Forfeited", "duration" to "3m 15s", "color" to Color(0xFF00FF9D), "date" to "2026-06-25 14:02")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "MATCH_CHRONOLOGY",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(historySessions) { session ->
                val resultColor = when(session["result"] as String) {
                    "Winner" -> Color(0xFF00FF9D)
                    "Forfeited" -> Color(0xFFFF2A7A)
                    else -> Color(0xFF00E5FF)
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, resultColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = session["id"] as String,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = session["date"] as String,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.3f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "MODE: " + session["mode"] as String,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "DURATION: " + session["duration"] as String,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(resultColor.copy(alpha = 0.1f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = (session["result"] as String).uppercase(),
                                color = resultColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- Sub-Tab Safety View ---
@Composable
fun LudoSafetyView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "SECURITY_STATUS",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color(0xFF00FF9D).copy(alpha = 0.1f))
                .border(2.dp, Color(0xFF00FF9D), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Shield,
                contentDescription = null,
                tint = Color(0xFF00FF9D),
                modifier = Modifier.size(54.dp)
            )
        }

        Text(
            text = "ANTI_CHEAT_ACTIVE",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF00FF9D),
            fontWeight = FontWeight.Bold
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0F172A))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SafetyMetricsRow("CONNECTION_INTEGRITY", "SECURE", Color(0xFF00FF9D))
            SafetyMetricsRow("PACKET_ENCRYPTION", "AES-256", Color(0xFF00E5FF))
            SafetyMetricsRow("CLIENT_LATENCY", "10 ms", Color(0xFF00FF9D))
            SafetyMetricsRow("MEMORY_SCAN_STATUS", "CLEAN", Color(0xFF00FF9D))
        }
    }
}

@Composable
fun SafetyMetricsRow(label: String, value: String, tint: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.5f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = tint,
            fontWeight = FontWeight.Bold
        )
    }
}
