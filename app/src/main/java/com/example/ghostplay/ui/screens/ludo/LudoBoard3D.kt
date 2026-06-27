package com.example.ghostplay.ui.screens.ludo

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.ghostplay.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

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
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    
    // Animation for board pulse
    val infiniteTransition = rememberInfiniteTransition(label = "board_glow")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    // Keep track of animated positions of tokens
    val tokenAnimations = remember { mutableMapOf<String, AnimatableToken>() }

    // Track captured tokens for special animations
    var capturedTokenKey by remember { mutableStateOf<String?>(null) }

    // Initialize/Update token animations when boardState changes
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

            // If the token position in the state doesn't match the animation target
            if (animToken.targetCol != currentTargetCol || animToken.targetRow != currentTargetRow) {
                
                // Detect if it was a capture (going back to base from track/stretch)
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
        // Main Board surface
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0A0E1A))
                .border(2.dp, Primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
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

                val isTurn = token.color == boardState.currentPlayer && boardState.diceRolled
                val isClickable = isTurn && (token.positionType != TokenPositionType.BASE || boardState.diceValue == 6)

                drawAnimatedToken(
                    col = col,
                    row = row,
                    hop = hop,
                    scale = scale,
                    rotation = rotation,
                    isDizzy = isDizzy,
                    color = token.color.toNeonColor(),
                    cellSize = cellSize,
                    isClickable = isClickable
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

    // Draw main base container (cyber-glassmorphic style)
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(neon.copy(alpha = 0.15f), Color.Transparent),
            center = Offset(x + baseSize/2, y + baseSize/2),
            radius = baseSize * 0.7f
        ),
        topLeft = Offset(x, y),
        size = Size(baseSize, baseSize)
    )

    // Glowing Neon corners/borders
    drawRect(
        color = neon.copy(alpha = 0.4f),
        topLeft = Offset(x, y),
        size = Size(baseSize, baseSize),
        style = Stroke(width = 2.dp.toPx())
    )
    
    // Corner accents
    val accentLen = cellSize
    // Top-Left
    drawLine(neon, Offset(x, y), Offset(x + accentLen, y), strokeWidth = 4.dp.toPx())
    drawLine(neon, Offset(x, y), Offset(x, y + accentLen), strokeWidth = 4.dp.toPx())
    // Bottom-Right
    drawLine(neon, Offset(x + baseSize, y + baseSize), Offset(x + baseSize - accentLen, y + baseSize), strokeWidth = 4.dp.toPx())
    drawLine(neon, Offset(x + baseSize, y + baseSize), Offset(x + baseSize, y + baseSize - accentLen), strokeWidth = 4.dp.toPx())

    // Draw inner home circles / target nests for 4 tokens
    val baseCoords = LudoCoordinates.BASES[color]!!
    baseCoords.forEach { cell ->
        val cx = cell.col * cellSize + cellSize / 2f
        val cy = cell.row * cellSize + cellSize / 2f
        
        // Nest shadow
        drawCircle(
            color = Color.Black.copy(alpha = 0.5f),
            center = Offset(cx, cy),
            radius = cellSize * 0.4f
        )
        // Glowing target ring
        drawCircle(
            color = neon.copy(alpha = 0.3f),
            center = Offset(cx, cy),
            radius = cellSize * 0.35f,
            style = Stroke(width = 1.dp.toPx())
        )
        drawCircle(
            color = neon,
            center = Offset(cx, cy),
            radius = cellSize * 0.15f
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
            drawRect(
                color = neon.copy(alpha = 0.2f),
                topLeft = Offset(cell.col * cellSize + 2.dp.toPx(), cell.row * cellSize + 2.dp.toPx()),
                size = Size(cellSize - 4.dp.toPx(), cellSize - 4.dp.toPx())
            )
        }

        // Color starting cell
        val startCell = LudoCoordinates.TRACK[LudoCoordinates.START_INDEXES[color]!!]
        drawRect(
            color = neon.copy(alpha = 0.6f),
            topLeft = Offset(startCell.col * cellSize, startCell.row * cellSize),
            size = Size(cellSize, cellSize)
        )
    }

    // 2. Draw Center home area
    val cx = 7.5f * cellSize
    val cy = 7.5f * cellSize

    // Center background
    drawRect(
        color = Color(0xFF1A1F26),
        topLeft = Offset(6 * cellSize, 6 * cellSize),
        size = Size(3 * cellSize, 3 * cellSize)
    )
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
}

// Draw a high-fidelity animated token with effects
private fun DrawScope.drawAnimatedToken(
    col: Float,
    row: Float,
    hop: Float,
    scale: Float,
    rotation: Float,
    isDizzy: Boolean,
    color: Color,
    cellSize: Float,
    isClickable: Boolean
) {
    val cx = col * cellSize + cellSize / 2f
    val cy = row * cellSize + cellSize / 2f

    // Calculate vertical offset due to "hop" height animation
    val hopOffset = hop * cellSize * 1.2f

    // Center point shifted upwards by the hop offset
    val tx = cx
    val ty = cy - hopOffset

    // 1. Draw Ground Shadow (dynamic based on hop)
    val shadowRadius = (cellSize * 0.35f) * (1f - hop * 0.3f) * scale
    drawOval(
        color = Color.Black.copy(alpha = 0.5f * (1f - hop * 0.5f)),
        topLeft = Offset(cx - shadowRadius, cy - shadowRadius * 0.5f),
        size = Size(shadowRadius * 2f, shadowRadius * 1f)
    )

    // 2. Draw Token Body with Scale and Rotation
    withTransform({
        translate(tx, ty)
        scale(scale, scale, Offset.Zero)
        rotate(rotation, Offset.Zero)
        translate(-tx, -ty)
    }) {
        val radius = cellSize * 0.35f
        
        // Outer Glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.6f), Color.Transparent),
                center = Offset(tx, ty),
                radius = radius * 1.5f
            ),
            center = Offset(tx, ty),
            radius = radius * 1.5f
        )

        // Body
        drawCircle(
            color = color,
            center = Offset(tx, ty),
            radius = radius
        )
        
        // Inner Gloss
        drawCircle(
            color = Color.White.copy(alpha = 0.4f),
            center = Offset(tx - radius * 0.3f, ty - radius * 0.3f),
            radius = radius * 0.4f
        )
        
        // Dizzy Emoticon (if captured)
        if (isDizzy) {
            val eyeSize = radius * 0.2f
            // Left Eye X
            drawLine(Color.Black, Offset(tx - radius * 0.4f, ty - eyeSize), Offset(tx - radius * 0.2f, ty + eyeSize), 2.dp.toPx())
            drawLine(Color.Black, Offset(tx - radius * 0.2f, ty - eyeSize), Offset(tx - radius * 0.4f, ty + eyeSize), 2.dp.toPx())
            // Right Eye X
            drawLine(Color.Black, Offset(tx + radius * 0.2f, ty - eyeSize), Offset(tx + radius * 0.4f, ty + eyeSize), 2.dp.toPx())
            drawLine(Color.Black, Offset(tx + radius * 0.4f, ty - eyeSize), Offset(tx + radius * 0.2f, ty + eyeSize), 2.dp.toPx())
        }
    }

    // 3. Clickable Turn Highlight (Glowing Pulsing Ring)
    if (isClickable) {
        val pulseScale = 1f + (System.currentTimeMillis() % 1000) / 1000f * 0.4f
        val pulseRadius = cellSize * 0.5f * pulseScale
        drawCircle(
            color = color.copy(alpha = 0.3f * (2f - pulseScale)),
            center = Offset(tx, ty),
            radius = pulseRadius,
            style = Stroke(width = 2.dp.toPx())
        )
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
                delay(800)
                scale.animateTo(1f, tween(200))
            }
        }
    }
}

// Coroutines support inside drawing scope
private suspend fun coroutineScope(block: suspend kotlinx.coroutines.CoroutineScope.() -> Unit) {
    kotlinx.coroutines.coroutineScope(block)
}
