package com.example.ghostplay.ui.screens.ludo

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.ghostplay.ui.screens.ludo.components.LudoPawn
import com.example.ghostplay.ui.screens.ludo.components.PawnAnimationState
import com.example.ghostplay.ui.screens.ludo.components.PawnCharacterType
import com.example.ghostplay.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.abs

// Map LudoColor to premium neon colors
fun LudoColor.toNeonColor(): Color {
    return when (this) {
        LudoColor.RED -> Color(0xFFFF2A7A)    // Pink
        LudoColor.GREEN -> Color(0xFF00FF9D)  // Neon Green
        LudoColor.YELLOW -> Color(0xFFFFE500) // Bright Yellow
        LudoColor.BLUE -> Color(0xFF00E5FF)   // Cyan
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
        initialValue = 0.1f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "alpha"
    )
    val sharedFloat by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 4.dp.value,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Reverse), label = "float"
    )
    val sharedArms by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 8.dp.value,
        animationSpec = infiniteRepeatable(tween(400), RepeatMode.Reverse), label = "victory"
    )
    val sharedDanger by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "danger"
    )
    val sharedRing by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Restart), label = "ring"
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
            .padding(8.dp)
            .background(Color.Transparent)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1E2433)) // Darker matte background like image
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
                
                // Character styling
                val charType = when (token.id) {
                    0 -> PawnCharacterType.ROBOT
                    1 -> PawnCharacterType.ASTRONAUT
                    2 -> PawnCharacterType.KID
                    else -> PawnCharacterType.ROBOT
                }
                
                val animState = when {
                    isDizzy -> PawnAnimationState.CAPTURED
                    isVictory -> PawnAnimationState.VICTORY
                    hop > 0.1f -> PawnAnimationState.MOVING
                    else -> PawnAnimationState.IDLE
                }

                val isTurn = token.color == boardState.currentPlayer && boardState.diceRolled
                val isClickable = isTurn && (token.positionType != TokenPositionType.BASE || boardState.diceValue == 6)

                // Increased Pawn scale to match board cells
                LudoPawn.draw(
                    drawScope = this,
                    col = col, row = row, hop = hop, scale = scale * 1.1f, rotation = rotation,
                    color = token.color.toNeonColor(),
                    cellSize = cellSize,
                    isClickable = isClickable,
                    animState = animState,
                    characterType = charType,
                    floatOffset = if (animState == PawnAnimationState.IDLE) sharedFloat else 0f,
                    armsOffset = if (animState == PawnAnimationState.VICTORY) sharedArms else 0f,
                    dangerPulse = sharedDanger,
                    ringPulse = sharedRing
                )
            }
        }
    }
}

// Rename 3D call to 2D in other files if needed, but for now let's just make it 2D
@Composable
fun LudoBoard3D(
    boardState: LudoBoardState,
    onTokenClick: (LudoToken) -> Unit,
    modifier: Modifier = Modifier
) {
    LudoBoard2D(boardState, onTokenClick, modifier)
}

// Draw basic grid backgrounds
private fun DrawScope.drawLudoGrid(cellSize: Float) {
    for (i in 0..15) {
        // Vertical lines
        drawLine(
            color = Color.White.copy(alpha = 0.05f),
            start = Offset(i * cellSize, 0f),
            end = Offset(i * cellSize, size.height),
            strokeWidth = 1.dp.toPx()
        )
        // Horizontal lines
        drawLine(
            color = Color.White.copy(alpha = 0.05f),
            start = Offset(0f, i * cellSize),
            end = Offset(size.width, i * cellSize),
            strokeWidth = 1.dp.toPx()
        )
    }
}

// Draw the 6x6 starting bases
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

    // House background (matte dark with border as in image)
    drawRoundRect(
        color = Color(0xFF141926),
        topLeft = Offset(x + 4.dp.toPx(), y + 4.dp.toPx()),
        size = Size(baseSize - 8.dp.toPx(), baseSize - 8.dp.toPx()),
        cornerRadius = CornerRadius(12.dp.toPx())
    )

    // Neon Glow Border for House
    drawRoundRect(
        color = neon.copy(alpha = 0.6f),
        topLeft = Offset(x + 4.dp.toPx(), y + 4.dp.toPx()),
        size = Size(baseSize - 8.dp.toPx(), baseSize - 8.dp.toPx()),
        cornerRadius = CornerRadius(12.dp.toPx()),
        style = Stroke(width = 2.dp.toPx())
    )

    // Inner Platforms (The raised squares for pawns in image)
    val baseCoords = LudoCoordinates.BASES[color]!!
    baseCoords.forEach { cell ->
        val px = cell.col * cellSize + 4.dp.toPx()
        val py = cell.row * cellSize + 4.dp.toPx()
        val pSize = cellSize - 8.dp.toPx()
        
        // Raised Platform
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(neon.copy(alpha = 0.4f), neon.copy(alpha = 0.2f))),
            topLeft = Offset(px, py),
            size = Size(pSize, pSize),
            cornerRadius = CornerRadius(8.dp.toPx())
        )
        // Platform Border
        drawRoundRect(
            color = neon.copy(alpha = 0.8f),
            topLeft = Offset(px, py),
            size = Size(pSize, pSize),
            cornerRadius = CornerRadius(8.dp.toPx()),
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}

// Draw home stretch paths and the center triangles
private fun DrawScope.drawHomeAreas(cellSize: Float) {
    // 1. Draw Home Stretches
    LudoColor.entries.forEach { color ->
        val neon = color.toNeonColor()
        val cells = LudoCoordinates.HOME_STRETCHES[color]!!
        cells.forEach { cell ->
            // Step cells (thicker/larger as in image)
            drawRoundRect(
                color = Color(0xFF242B38),
                topLeft = Offset(cell.col * cellSize + 2.dp.toPx(), cell.row * cellSize + 2.dp.toPx()),
                size = Size(cellSize - 4.dp.toPx(), cellSize - 4.dp.toPx()),
                cornerRadius = CornerRadius(6.dp.toPx())
            )
            // Tint with player color
            drawRoundRect(
                color = neon.copy(alpha = 0.2f),
                topLeft = Offset(cell.col * cellSize + 2.dp.toPx(), cell.row * cellSize + 2.dp.toPx()),
                size = Size(cellSize - 4.dp.toPx(), cellSize - 4.dp.toPx()),
                cornerRadius = CornerRadius(6.dp.toPx())
            )
        }

        // Color starting cell
        val startCell = LudoCoordinates.TRACK[LudoCoordinates.START_INDEXES[color]!!]
        drawRoundRect(
            color = neon.copy(alpha = 0.6f),
            topLeft = Offset(startCell.col * cellSize + 2.dp.toPx(), startCell.row * cellSize + 2.dp.toPx()),
            size = Size(cellSize - 4.dp.toPx(), cellSize - 4.dp.toPx()),
            cornerRadius = CornerRadius(6.dp.toPx())
        )
    }

    // 2. Draw Center home area (The crystal structure in image)
    val basePos = 6 * cellSize
    val centerSize = 3 * cellSize
    
    drawRoundRect(
        color = Color(0xFF141926),
        topLeft = Offset(basePos, basePos),
        size = Size(centerSize, centerSize),
        cornerRadius = CornerRadius(8.dp.toPx())
    )
    
    // Crystal base accents
    LudoColor.entries.forEach { color ->
        val neon = color.toNeonColor()
        val path = Path().apply {
            when (color) {
                LudoColor.GREEN -> {
                    moveTo(basePos, basePos)
                    lineTo(basePos + cellSize * 0.5f, basePos)
                    lineTo(basePos, basePos + cellSize * 0.5f)
                }
                LudoColor.YELLOW -> {
                    moveTo(basePos + centerSize, basePos)
                    lineTo(basePos + centerSize - cellSize * 0.5f, basePos)
                    lineTo(basePos + centerSize, basePos + cellSize * 0.5f)
                }
                LudoColor.RED -> {
                    moveTo(basePos, basePos + centerSize)
                    lineTo(basePos + cellSize * 0.5f, basePos + centerSize)
                    lineTo(basePos, basePos + centerSize - cellSize * 0.5f)
                }
                LudoColor.BLUE -> {
                    moveTo(basePos + centerSize, basePos + centerSize)
                    lineTo(basePos + centerSize - cellSize * 0.5f, basePos + centerSize)
                    lineTo(basePos + centerSize, basePos + centerSize - cellSize * 0.5f)
                }
            }
            close()
        }
        drawPath(path, neon.copy(alpha = 0.6f))
    }
}

private fun DrawScope.drawDome(cellSize: Float, pulseAlpha: Float) {
    val cx = 7.5f * cellSize
    val cy = 7.5f * cellSize
    val radius = 1.2f * cellSize

    // Dome shadow
    drawCircle(
        color = Color.Black.copy(alpha = 0.5f),
        center = Offset(cx, cy + 4.dp.toPx()),
        radius = radius
    )

    // Dome glass
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Primary.copy(alpha = 0.4f), Color(0xFF00E5FF).copy(alpha = 0.1f)),
            center = Offset(cx - radius/3, cy - radius/3),
            radius = radius
        ),
        center = Offset(cx, cy),
        radius = radius
    )
    
    // Dome border
    drawCircle(
        color = Color.White.copy(alpha = 0.4f),
        center = Offset(cx, cy),
        radius = radius,
        style = Stroke(width = 1.dp.toPx())
    )
    
    // Inner pulse
    drawCircle(
        color = Primary.copy(alpha = pulseAlpha),
        center = Offset(cx, cy),
        radius = radius * 0.8f
    )
}

// Draw star markings on safe zones
private fun DrawScope.drawSafeZones(cellSize: Float) {
    LudoCoordinates.SAFE_ZONE_INDEXES.forEach { idx ->
        val cell = LudoCoordinates.TRACK[idx]
        val cx = cell.col * cellSize + cellSize / 2f
        val cy = cell.row * cellSize + cellSize / 2f

        // Draw track cell background first
        drawRoundRect(
            color = Color(0xFF242B38),
            topLeft = Offset(cell.col * cellSize + 2.dp.toPx(), cell.row * cellSize + 2.dp.toPx()),
            size = Size(cellSize - 4.dp.toPx(), cellSize - 4.dp.toPx()),
            cornerRadius = CornerRadius(6.dp.toPx())
        )

        // Draw a glowing diamond/star in safe cells
        val starSize = cellSize * 0.3f
        val path = Path().apply {
            moveTo(cx, cy - starSize)
            lineTo(cx + starSize * 0.3f, cy - starSize * 0.3f)
            lineTo(cx + starSize, cy)
            lineTo(cx + starSize * 0.3f, cy + starSize * 0.3f)
            lineTo(cx, cy + starSize)
            lineTo(cx - starSize * 0.3f, cy + starSize * 0.3f)
            lineTo(cx - starSize, cy)
            lineTo(cx - starSize * 0.3f, cy - starSize * 0.3f)
            close()
        }
        drawPath(path, Color(0xFF00E5FF).copy(alpha = 0.6f))
        drawPath(path, Color(0xFF00E5FF), style = Stroke(width = 1.dp.toPx()))
    }
    
    // Draw remaining track cells
    LudoCoordinates.TRACK.forEachIndexed { idx, cell ->
        if (idx !in LudoCoordinates.SAFE_ZONE_INDEXES && idx !in LudoColor.entries.map { LudoCoordinates.START_INDEXES[it]!! }) {
             drawRoundRect(
                color = Color(0xFF242B38),
                topLeft = Offset(cell.col * cellSize + 2.dp.toPx(), cell.row * cellSize + 2.dp.toPx()),
                size = Size(cellSize - 4.dp.toPx(), cellSize - 4.dp.toPx()),
                cornerRadius = CornerRadius(6.dp.toPx())
            )
        }
    }
}

// Class to manage step-by-step path hop animations
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
            val duration = 300
            coroutineScope {
                launch { col.animateTo(destCol, tween(duration)) }
                launch { row.animateTo(destRow, tween(duration)) }
                launch {
                    // Parabolic arc for hop + scale at apex
                    hop.animateTo(1f, tween(duration / 2, easing = FastOutSlowInEasing))
                    hop.animateTo(0f, tween(duration / 2, easing = FastOutSlowInEasing))
                }
                launch {
                    scale.animateTo(1.2f, tween(duration / 2))
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
            // 1. Dizzy spin and shrink
            launch {
                rotation.animateTo(720f, tween(1000, easing = LinearEasing))
                rotation.snapTo(0f)
            }
            launch {
                scale.animateTo(0.5f, tween(500))
            }
            
            // 2. Slide back to base
            launch {
                col.animateTo(destCol, tween(800, easing = FastOutSlowInEasing))
            }
            launch {
                row.animateTo(destRow, tween(800, easing = FastOutSlowInEasing))
            }
            
            // 3. Restore scale
            launch {
                kotlinx.coroutines.delay(800)
                scale.animateTo(1f, tween(200))
            }
        }
    }
}

// Coroutines support inside drawing scope
private suspend fun coroutineScope(block: suspend kotlinx.coroutines.CoroutineScope.() -> Unit) {
    kotlinx.coroutines.coroutineScope(block)
}
