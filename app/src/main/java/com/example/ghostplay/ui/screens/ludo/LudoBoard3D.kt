package com.example.ghostplay.ui.screens.ludo

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.ghostplay.ui.screens.ludo.components.LudoPawn
import com.example.ghostplay.ui.screens.ludo.components.PawnAnimationState
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.launch
import kotlin.math.abs

// Map LudoColor to premium neon colors
fun LudoColor.toNeonColor(): Color {
    return when (this) {
        LudoColor.RED -> Color(0xFFFF2A7A)    // Hot Pink
        LudoColor.GREEN -> Color(0xFF00FF9D)  // Toxic Green
        LudoColor.YELLOW -> Color(0xFFFFE500) // Laser Yellow
        LudoColor.BLUE -> Color(0xFF00E5FF)   // Neon Cyan
    }
}

@Composable
fun LudoBoard2D(
    boardState: LudoBoardState,
    onTokenClick: (LudoToken) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    
    // Shared Animations
    val infiniteTransition = rememberInfiniteTransition(label = "board_fx")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f, targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "alpha"
    )
    val sharedFloat by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 3.dp.value,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse), label = "float"
    )
    val sharedArms by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 6.dp.value,
        animationSpec = infiniteRepeatable(tween(400), RepeatMode.Reverse), label = "victory"
    )
    val sharedDanger by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "danger"
    )
    val sharedRing by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.25f,
        animationSpec = infiniteRepeatable(tween(850), RepeatMode.Restart), label = "ring"
    )

    // Keep track of animated positions of tokens
    val tokenAnimations = remember { mutableMapOf<String, AnimatableToken>() }
    var capturedTokenKey by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(boardState.tokens) {
        boardState.tokens.forEach { token ->
            val key = "${token.color}_${token.id}"
            val animToken = tokenAnimations.getOrPut(key) {
                val cell = LudoCoordinates.getCell(token)
                AnimatableToken(cell.col.toFloat(), cell.row.toFloat())
            }

            val targetCell = LudoCoordinates.getCell(token)
            val currentTargetCol = targetCell.col.toFloat()
            val currentTargetRow = targetCell.row.toFloat()

            if (animToken.targetCol != currentTargetCol || (animToken.targetRow != currentTargetRow)) {
                val isCaptured = animToken.wasOnTrack && token.positionType == TokenPositionType.BASE
                scope.launch {
                    if (isCaptured) {
                        capturedTokenKey = key
                        animToken.animateCapture(currentTargetCol, currentTargetRow)
                        capturedTokenKey = null
                    } else {
                        animToken.animateTo(currentTargetCol, currentTargetRow)
                    }
                    animToken.wasOnTrack = token.positionType != TokenPositionType.BASE
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(Color.Transparent)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF040A18)) // Sleek dark game space
                .pointerInput(boardState) {
                    detectTapGestures { offset ->
                        val boardSize = size.width
                        val cellSize = boardSize / 15f
                        val col = (offset.x / cellSize).toInt().coerceIn(0, 14)
                        val row = (offset.y / cellSize).toInt().coerceIn(0, 14)

                        val clickedToken = boardState.tokens.firstOrNull { token ->
                            token.color == boardState.currentPlayer &&
                                    token.positionType != TokenPositionType.BASE &&
                                    LudoCoordinates.getCell(token) == LudoCell(col, row)
                        }

                        val finalToken = clickedToken ?: run {
                            val isClickInCurrentBase = when (boardState.currentPlayer) {
                                LudoColor.GREEN -> col in 0..5 && row in 0..5
                                LudoColor.YELLOW -> col in 9..14 && row in 0..5
                                LudoColor.RED -> col in 0..5 && row in 9..14
                                LudoColor.BLUE -> col in 9..14 && row in 9..14
                            }
                            if (isClickInCurrentBase) {
                                boardState.tokens.firstOrNull { it.color == boardState.currentPlayer && it.positionType == TokenPositionType.BASE }
                            } else null
                        }
                        if (finalToken != null) onTokenClick(finalToken)
                    }
                }
        ) {
            val boardSize = size.width
            val cellSize = boardSize / 15f

            // Draw full board bottom plate edge (for floating 3D effect)
            drawBoard3DDepth(boardSize)

            drawLudoGrid(cellSize)
            LudoColor.entries.forEach { drawPlayerBase(it, cellSize) }
            drawHomeAreas(cellSize)
            drawSafeZones(cellSize)
            drawDome(cellSize, pulseAlpha)

            // Draw Tokens
            boardState.tokens.forEach { token ->
                val key = "${token.color}_${token.id}"
                val anim = tokenAnimations[key] ?: return@forEach
                
                val col = anim.col.value
                val row = anim.row.value
                val hop = anim.hop.value
                val scale = anim.scale.value
                val rotation = anim.rotation.value
                
                val isDizzy = capturedTokenKey == key
                val isVictory = token.positionType == TokenPositionType.FINISHED
                
                val animState = when {
                    isDizzy -> PawnAnimationState.CAPTURED
                    isVictory -> PawnAnimationState.VICTORY
                    hop > 0.1f -> PawnAnimationState.MOVING
                    else -> PawnAnimationState.IDLE
                }

                val isTurn = token.color == boardState.currentPlayer && boardState.diceRolled
                val isClickable = isTurn && (token.positionType != TokenPositionType.BASE || boardState.diceValue == 6)

                LudoPawn.draw(
                    drawScope = this,
                    col = col, row = row, hop = hop, scale = scale, rotation = rotation,
                    color = token.color.toNeonColor(),
                    cellSize = cellSize,
                    isClickable = isClickable,
                    animState = animState,
                    pawnColor = token.color,
                    floatOffset = if (animState == PawnAnimationState.IDLE) sharedFloat else 0f,
                    armsOffset = if (animState == PawnAnimationState.VICTORY) sharedArms else 0f,
                    dangerPulse = sharedDanger,
                    ringPulse = sharedRing
                )
            }
        }
    }
}

@Composable
fun LudoBoard3D(
    boardState: LudoBoardState,
    onTokenClick: (LudoToken) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .graphicsLayer {
                // Perfect immersive 3D isometric tilt projection
                rotationX = 54f
                rotationZ = -45f
                cameraDistance = 14f * density.density
                scaleX = 0.9f
                scaleY = 0.9f
            },
        contentAlignment = Alignment.Center
    ) {
        LudoBoard2D(
            boardState = boardState,
            onTokenClick = onTokenClick,
            modifier = Modifier.fillMaxSize()
        )
    }
}

// Draw the bottom depth edge of the entire Ludo board to give it height/thickness
private fun DrawScope.drawBoard3DDepth(boardSize: Float) {
    val baseThickness = 12.dp.toPx()
    // Dark grey side depth extrusion
    for (i in 1..baseThickness.toInt()) {
        drawRoundRect(
            color = Color(0xFF0F172A),
            topLeft = Offset(0f, i.toFloat()),
            size = Size(boardSize, boardSize),
            cornerRadius = CornerRadius(16.dp.toPx())
        )
    }
    // Neon Cyan rim highlight on side
    drawRoundRect(
        color = Color(0xFF00E5FF).copy(alpha = 0.4f),
        topLeft = Offset(0f, baseThickness),
        size = Size(boardSize, boardSize),
        cornerRadius = CornerRadius(16.dp.toPx()),
        style = Stroke(width = 1.5.dp.toPx())
    )
}

// Draw base subtle lines
private fun DrawScope.drawLudoGrid(cellSize: Float) {
    for (i in 0..15) {
        drawLine(
            color = Color.White.copy(alpha = 0.04f),
            start = Offset(i * cellSize, 0f),
            end = Offset(i * cellSize, size.height),
            strokeWidth = 0.5.dp.toPx()
        )
        drawLine(
            color = Color.White.copy(alpha = 0.04f),
            start = Offset(0f, i * cellSize),
            end = Offset(size.width, i * cellSize),
            strokeWidth = 0.5.dp.toPx()
        )
    }
}

// Stone Tile drawing with 3D thickness and glowing boundary lines
private fun DrawScope.drawStoneTile(col: Int, row: Int, cellSize: Float, glowColor: Color) {
    val x = col * cellSize
    val y = row * cellSize
    val margin = 1.dp.toPx()
    val tileSize = cellSize - 2 * margin
    
    // Draw 3D side depth (extrusion)
    val thickness = 3.dp.toPx()
    for (i in 1..thickness.toInt()) {
        drawRoundRect(
            color = Color(0xFF0D121C), // Dark slate side shadow
            topLeft = Offset(x + margin, y + margin + i),
            size = Size(tileSize, tileSize),
            cornerRadius = CornerRadius(4.dp.toPx())
        )
    }
    
    // Grey/slate stone texture gradient fill for top face
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF475569), Color(0xFF1E293B))
        ),
        topLeft = Offset(x + margin, y + margin),
        size = Size(tileSize, tileSize),
        cornerRadius = CornerRadius(4.dp.toPx())
    )
    
    // Organic crack lines for stone appearance
    drawLine(
        color = Color(0xFF0F172A).copy(alpha = 0.5f),
        start = Offset(x + margin + tileSize * 0.2f, y + margin),
        end = Offset(x + margin + tileSize * 0.45f, y + margin + tileSize * 0.4f),
        strokeWidth = 1.dp.toPx()
    )
    drawLine(
        color = Color(0xFF0F172A).copy(alpha = 0.5f),
        start = Offset(x + margin + tileSize * 0.45f, y + margin + tileSize * 0.4f),
        end = Offset(x + margin + tileSize * 0.15f, y + margin + tileSize * 0.85f),
        strokeWidth = 1.dp.toPx()
    )
    
    // Glowing neon boundary border
    drawRoundRect(
        color = glowColor.copy(alpha = 0.75f),
        topLeft = Offset(x + margin, y + margin),
        size = Size(tileSize, tileSize),
        cornerRadius = CornerRadius(4.dp.toPx()),
        style = Stroke(width = 1.5.dp.toPx())
    )
    
    // Soft outer neon glow
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(glowColor.copy(alpha = 0.14f), Color.Transparent),
            center = Offset(x + cellSize/2f, y + cellSize/2f),
            radius = cellSize * 0.75f
        ),
        topLeft = Offset(x - margin, y - margin),
        size = Size(cellSize + 2 * margin, cellSize + 2 * margin),
        cornerRadius = CornerRadius(6.dp.toPx())
    )
}

// Draw the starting bases (neon platform slabs with 3D thickness)
private fun DrawScope.drawPlayerBase(color: LudoColor, cellSize: Float) {
    val neon = color.toNeonColor()
    val (colOffset, rowOffset) = when (color) {
        LudoColor.GREEN -> Pair(0, 0)
        LudoColor.YELLOW -> Pair(9, 0)
        LudoColor.RED -> Pair(0, 9)
        LudoColor.BLUE -> Pair(9, 9)
    }

    val x = colOffset * cellSize
    val y = rowOffset * cellSize
    val baseSize = 6 * cellSize

    // 1. Raised 3D platform slab effect
    val baseThickness = 6.dp.toPx()
    val darkNeon = Color(
        red = neon.red * 0.25f + 0.01f,
        green = neon.green * 0.25f + 0.01f,
        blue = neon.blue * 0.25f + 0.01f
    )
    for (i in 1..baseThickness.toInt()) {
        drawRoundRect(
            color = darkNeon,
            topLeft = Offset(x + 2.dp.toPx(), y + 2.dp.toPx() + i),
            size = Size(baseSize - 4.dp.toPx(), baseSize - 4.dp.toPx()),
            cornerRadius = CornerRadius(12.dp.toPx())
        )
    }

    // 2. Top Platform Face
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(neon.copy(alpha = 0.25f), neon.copy(alpha = 0.06f))),
        topLeft = Offset(x + 2.dp.toPx(), y + 2.dp.toPx()),
        size = Size(baseSize - 4.dp.toPx(), baseSize - 4.dp.toPx()),
        cornerRadius = CornerRadius(12.dp.toPx())
    )

    // 3. Glowing Top Neon Border
    drawRoundRect(
        color = neon.copy(alpha = 0.75f),
        topLeft = Offset(x + 2.dp.toPx(), y + 2.dp.toPx()),
        size = Size(baseSize - 4.dp.toPx(), baseSize - 4.dp.toPx()),
        cornerRadius = CornerRadius(12.dp.toPx()),
        style = Stroke(width = 2.5.dp.toPx())
    )

    // Grid lines inside base
    for (i in 1..5) {
        drawLine(neon.copy(alpha = 0.15f), Offset(x + i * cellSize, y), Offset(x + i * cellSize, y + baseSize), 1.dp.toPx())
        drawLine(neon.copy(alpha = 0.15f), Offset(x, y + i * cellSize), Offset(x + baseSize, y + i * cellSize), 1.dp.toPx())
    }

    // 4. Launch Pods (Glowing rings for pawns)
    val baseCoords = LudoCoordinates.BASES[color]!!
    baseCoords.forEach { cell ->
        val cx = cell.col * cellSize + cellSize / 2f
        val cy = cell.row * cellSize + cellSize / 2f
        val radius = cellSize * 0.38f
        
        drawCircle(
            brush = Brush.radialGradient(listOf(neon.copy(alpha = 0.45f), Color.Transparent)),
            center = Offset(cx, cy),
            radius = radius * 1.25f
        )
        drawCircle(
            color = neon,
            center = Offset(cx, cy),
            radius = radius,
            style = Stroke(2.dp.toPx())
        )
    }
}

// Draw home stretch stone tiles and center obelisk
private fun DrawScope.drawHomeAreas(cellSize: Float) {
    // 1. Draw Home Stretches with stone tile texture and custom colors
    LudoColor.entries.forEach { color ->
        val neon = color.toNeonColor()
        val cells = LudoCoordinates.HOME_STRETCHES[color]!!
        cells.forEach { cell ->
            drawStoneTile(cell.col, cell.row, cellSize, neon)
        }

        // Starting cell highlights
        val startCell = LudoCoordinates.TRACK[LudoCoordinates.START_INDEXES[color]!!]
        drawCircle(
            brush = Brush.radialGradient(listOf(neon.copy(alpha = 0.5f), Color.Transparent)),
            center = Offset(startCell.col * cellSize + cellSize/2f, startCell.row * cellSize + cellSize/2f),
            radius = cellSize * 0.7f
        )
    }

    // 2. Draw center final triangle base
    val basePos = 6 * cellSize
    val centerSize = 3 * cellSize
    
    drawRect(
        color = Color(0xFF040A18),
        topLeft = Offset(basePos, basePos),
        size = Size(centerSize, centerSize)
    )
    
    LudoColor.entries.forEach { color ->
        val neon = color.toNeonColor()
        val path = Path().apply {
            when (color) {
                LudoColor.GREEN -> {
                    moveTo(basePos, basePos)
                    lineTo(basePos + cellSize * 1.5f, basePos + cellSize * 1.5f)
                    lineTo(basePos, basePos + cellSize * 3f)
                }
                LudoColor.YELLOW -> {
                    moveTo(basePos, basePos)
                    lineTo(basePos + cellSize * 1.5f, basePos + cellSize * 1.5f)
                    lineTo(basePos + cellSize * 3f, basePos)
                }
                LudoColor.RED -> {
                    moveTo(basePos, basePos + cellSize * 3f)
                    lineTo(basePos + cellSize * 1.5f, basePos + cellSize * 1.5f)
                    lineTo(basePos + cellSize * 3f, basePos + cellSize * 3f)
                }
                LudoColor.BLUE -> {
                    moveTo(basePos + cellSize * 3f, basePos)
                    lineTo(basePos + cellSize * 1.5f, basePos + cellSize * 1.5f)
                    lineTo(basePos + cellSize * 3f, basePos + cellSize * 3f)
                }
            }
            close()
        }
        drawPath(path, neon.copy(alpha = 0.35f))
        drawPath(path, neon.copy(alpha = 0.6f), style = Stroke(1.5.dp.toPx()))
    }
}

// Draw center dome obelisk (Refractive gem floating in center)
private fun DrawScope.drawDome(cellSize: Float, pulseAlpha: Float) {
    val cx = 7.5f * cellSize
    val cy = 7.5f * cellSize
    val radius = 1.25f * cellSize

    // Metallic outer base ring
    drawCircle(
        color = Color(0xFF0F172A),
        center = Offset(cx, cy),
        radius = radius
    )
    drawCircle(
        color = Color(0xFF00E5FF).copy(alpha = 0.6f),
        center = Offset(cx, cy),
        radius = radius,
        style = Stroke(2.dp.toPx())
    )
    
    // Core glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.3f), Color.Transparent),
            center = Offset(cx, cy),
            radius = radius * 1.6f
        ),
        center = Offset(cx, cy),
        radius = radius * 1.6f
    )

    // Vertical gem structure
    val cw = cellSize * 0.55f
    val ch = cellSize * 1.2f
    val time = System.currentTimeMillis() / 150f
    val pulseHeight = ch + Math.sin(time.toDouble()).toFloat() * 1.5.dp.toPx()

    val gemPath = Path().apply {
        moveTo(cx, cy - pulseHeight / 2) // Top apex
        lineTo(cx + cw / 2, cy - pulseHeight * 0.1f) // Right edge
        lineTo(cx + cw / 2, cy + pulseHeight * 0.3f) // Right base
        lineTo(cx, cy + pulseHeight / 2) // Bottom base
        lineTo(cx - cw / 2, cy + pulseHeight * 0.3f) // Left base
        lineTo(cx - cw / 2, cy - pulseHeight * 0.1f) // Left edge
        close()
    }
    
    drawPath(
        path = gemPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF00E5FF),
                Color(0xFF00FF9D).copy(alpha = 0.7f),
                Color(0xFFFF2A7A).copy(alpha = 0.5f)
            )
        )
    )

    // Core sheen line
    val shinePath = Path().apply {
        moveTo(cx, cy - pulseHeight / 2)
        lineTo(cx + cw * 0.08f, cy - pulseHeight * 0.1f)
        lineTo(cx + cw * 0.08f, cy + pulseHeight * 0.3f)
        lineTo(cx, cy + pulseHeight / 2)
        close()
    }
    drawPath(path = shinePath, color = Color.White.copy(alpha = 0.35f))
    drawPath(path = gemPath, color = Color.White.copy(alpha = 0.7f), style = Stroke(1.5.dp.toPx()))
    drawLine(Color.White.copy(alpha = 0.5f), Offset(cx, cy - pulseHeight / 2), Offset(cx, cy + pulseHeight / 2), 1.dp.toPx())
}

// Draw safe zones
private fun DrawScope.drawSafeZones(cellSize: Float) {
    // Renders all cells on the board grid as stone tiles
    LudoCoordinates.TRACK.forEachIndexed { idx, cell ->
        val quadrantColor = when {
            idx in 0..11 || idx == 51 -> LudoColor.RED.toNeonColor()
            idx in 12..24 -> LudoColor.GREEN.toNeonColor()
            idx in 25..37 -> LudoColor.YELLOW.toNeonColor()
            else -> LudoColor.BLUE.toNeonColor()
        }
        drawStoneTile(cell.col, cell.row, cellSize, quadrantColor)
    }

    // Stars on safe zones
    LudoCoordinates.SAFE_ZONE_INDEXES.forEach { idx ->
        val cell = LudoCoordinates.TRACK[idx]
        val cx = cell.col * cellSize + cellSize / 2f
        val cy = cell.row * cellSize + cellSize / 2f

        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0xFF00E5FF).copy(alpha = 0.5f), Color.Transparent)),
            center = Offset(cx, cy),
            radius = cellSize * 0.55f
        )

        val starSize = cellSize * 0.35f
        val path = Path().apply {
            moveTo(cx, cy - starSize)
            lineTo(cx + starSize * 0.25f, cy - starSize * 0.25f)
            lineTo(cx + starSize, cy)
            lineTo(cx + starSize * 0.25f, cy + starSize * 0.25f)
            lineTo(cx, cy + starSize)
            lineTo(cx - starSize * 0.25f, cy + starSize * 0.25f)
            lineTo(cx - starSize, cy)
            lineTo(cx - starSize * 0.25f, cy - starSize * 0.25f)
            close()
        }
        drawPath(path, Color(0xFF00E5FF).copy(alpha = 0.95f))
        drawPath(path, Color.White, style = Stroke(1.dp.toPx()))
    }
}

// Class to manage path animations
class AnimatableToken(startCol: Float, startRow: Float) {
    val col = Animatable(startCol)
    val row = Animatable(startRow)
    val hop = Animatable(0f)
    val scale = Animatable(1f)
    val rotation = Animatable(0f)
    
    var wasOnTrack = false
    var targetCol = startCol
    var targetRow = startRow

    suspend fun animateTo(destCol: Float, destRow: Float) {
        targetCol = destCol
        targetRow = destRow

        val diffCol = destCol - col.value
        val diffRow = destRow - row.value

        if (abs(diffCol) <= 1.1f && abs(diffRow) <= 1.1f) {
            val duration = 280
            coroutineScope {
                launch { col.animateTo(destCol, tween(duration)) }
                launch { row.animateTo(destRow, tween(duration)) }
                launch {
                    hop.animateTo(1f, tween(duration / 2, easing = FastOutSlowInEasing))
                    hop.animateTo(0f, tween(duration / 2, easing = FastOutSlowInEasing))
                }
                launch {
                    scale.animateTo(1.22f, tween(duration / 2))
                    scale.animateTo(1f, tween(duration / 2))
                }
            }
        } else {
            col.snapTo(destCol)
            row.snapTo(destRow)
        }
    }

    suspend fun animateCapture(destCol: Float, destRow: Float) {
        targetCol = destCol
        targetRow = destRow
        
        coroutineScope {
            launch {
                rotation.animateTo(720f, tween(900, easing = LinearEasing))
                rotation.snapTo(0f)
            }
            launch {
                scale.animateTo(0.45f, tween(450))
            }
            launch {
                col.animateTo(destCol, tween(750, easing = FastOutSlowInEasing))
            }
            launch {
                row.animateTo(destRow, tween(750, easing = FastOutSlowInEasing))
            }
            launch {
                kotlinx.coroutines.delay(750.milliseconds)
                scale.animateTo(1f, tween(200))
            }
        }
    }
}

private suspend fun coroutineScope(block: suspend kotlinx.coroutines.CoroutineScope.() -> Unit) {
    kotlinx.coroutines.coroutineScope(block)
}
