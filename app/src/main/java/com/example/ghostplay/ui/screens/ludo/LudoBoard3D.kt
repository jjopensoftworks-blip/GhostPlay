package com.example.ghostplay.ui.screens.ludo

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
        LudoColor.RED -> Color(0xFFFF2A7A)
        LudoColor.GREEN -> Color(0xFF98DA27) // Tertiary neon green
        LudoColor.YELLOW -> Color(0xFFFFD600)
        LudoColor.BLUE -> Color(0xFF00E5FF)  // Secondary neon cyan
    }
}

@Composable
fun LudoBoard3D(
    boardState: LudoBoardState,
    onTokenClick: (LudoToken) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    // Keep track of animated positions of tokens to make them hop
    // Key: "color_id", Value: animatable offset from their base/current spot
    val tokenAnimations = remember { mutableMapOf<String, AnimatableToken>() }

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

            // If the token position in the state doesn't match the animation target,
            // we animate it step-by-step
            if (animToken.targetCol != currentTargetCol || animToken.targetRow != currentTargetRow) {
                // If it was in base and got released, or moved by a lot, we animate the path.
                // For simplicity, we can animate it cell-by-cell.
                scope.launch {
                    animToken.animateTo(currentTargetCol, currentTargetRow)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .graphicsLayer {
                // 3D Isometric projection tilt
                rotationX = 50f
                rotationZ = -45f
                cameraDistance = 16f * density.density
            }
            .background(Color.Transparent)
    ) {
        // Render 3D Slab Thickness Shadow underneath
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = 12.dp) // creates depth layer
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.8f),
                            Color(0xFF060E20)
                        )
                    )
                )
                .border(2.dp, Primary.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
        )

        // Main Board surface
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceContainerLowest)
                .border(2.dp, Primary.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                .pointerInput(boardState) {
                    detectTapGestures { offset ->
                        // Calculate which grid cell was clicked
                        val boardSize = size.width
                        val cellSize = boardSize / 15f
                        val col = (offset.x / cellSize).toInt().coerceIn(0, 14)
                        val row = (offset.y / cellSize).toInt().coerceIn(0, 14)

                        // Find if there is a clickable token belonging to the current player on this cell
                        val clickedToken = boardState.tokens.firstOrNull { token ->
                            token.color == boardState.currentPlayer &&
                                    LudoCoordinates.getCell(token) == LudoCell(col, row)
                        }

                        // Also check base clicks if current player rolled a 6
                        val baseToken = if (clickedToken == null && boardState.diceValue == 6) {
                            boardState.tokens.firstOrNull { token ->
                                token.color == boardState.currentPlayer &&
                                        token.positionType == TokenPositionType.BASE &&
                                        LudoCoordinates.getCell(token) == LudoCell(col, row)
                            }
                        } else null

                        val finalToken = clickedToken ?: baseToken
                        if (finalToken != null) {
                            onTokenClick(finalToken)
                        }
                    }
                }
        ) {
            val boardSize = size.width
            val cellSize = boardSize / 15f

            // 1. Draw Grid Lines and Cells
            drawLudoGrid(cellSize)

            // 2. Draw 4 Player Bases
            LudoColor.entries.forEach { color ->
                drawPlayerBase(color, cellSize)
            }

            // 3. Draw Home Stretches and Triangles
            drawHomeAreas(cellSize)

            // 4. Draw Safe Zones (Stars)
            drawSafeZones(cellSize)

            // 5. Draw Glowing Cyber Token pegs
            boardState.tokens.forEach { token ->
                val key = "${token.color}_${token.id}"
                val anim = tokenAnimations[key]
                val col = anim?.col?.value ?: LudoCoordinates.getCell(token).col.toFloat()
                val row = anim?.row?.value ?: LudoCoordinates.getCell(token).row.toFloat()
                val hop = anim?.hop?.value ?: 0f

                // Glow size multiplier if it's this player's turn and token can move
                val isTurn = token.color == boardState.currentPlayer && boardState.diceRolled
                val isClickable = isTurn && (
                        token.positionType != TokenPositionType.BASE || boardState.diceValue == 6
                        )

                draw3DToken(
                    col = col,
                    row = row,
                    hop = hop,
                    color = token.color.toNeonColor(),
                    cellSize = cellSize,
                    isClickable = isClickable
                )
            }
        }
    }
}

// Draw basic grid backgrounds
private fun DrawScope.drawLudoGrid(cellSize: Float) {
    val gridPaint = Paint().apply {
        color = OutlineVariant.copy(alpha = 0.4f)
        style = PaintingStyle.Stroke
        strokeWidth = 1.dp.toPx()
    }

    for (i in 0..15) {
        // Vertical lines
        drawLine(
            color = OutlineVariant.copy(alpha = 0.3f),
            start = Offset(i * cellSize, 0f),
            end = Offset(i * cellSize, size.height),
            strokeWidth = 1.dp.toPx()
        )
        // Horizontal lines
        drawLine(
            color = OutlineVariant.copy(alpha = 0.3f),
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
    val size = 6 * cellSize

    // Draw main base container (cyber-glassmorphic style)
    drawRect(
        color = color.toNeonColor().copy(alpha = 0.05f),
        topLeft = Offset(x, y),
        size = Size(size, size)
    )

    // Glowing Neon corners/borders
    drawRect(
        color = neon.copy(alpha = 0.6f),
        topLeft = Offset(x, y),
        size = Size(size, size),
        style = Stroke(width = 3.dp.toPx())
    )

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
            color = neon.copy(alpha = 0.4f),
            center = Offset(cx, cy),
            radius = cellSize * 0.3f,
            style = Stroke(width = 2.dp.toPx())
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
                color = neon.copy(alpha = 0.25f),
                topLeft = Offset(cell.col * cellSize, cell.row * cellSize),
                size = Size(cellSize, cellSize)
            )
            drawRect(
                color = neon.copy(alpha = 0.6f),
                topLeft = Offset(cell.col * cellSize, cell.row * cellSize),
                size = Size(cellSize, cellSize),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Color starting cell
        val startCell = LudoCoordinates.TRACK[LudoCoordinates.START_INDEXES[color]!!]
        drawRect(
            color = neon.copy(alpha = 0.3f),
            topLeft = Offset(startCell.col * cellSize, startCell.row * cellSize),
            size = Size(cellSize, cellSize)
        )
    }

    // 2. Draw Center home triangles meeting at (7.5, 7.5)
    val cx = 7.5f * cellSize
    val cy = 7.5f * cellSize

    // RED (bottom)
    val redPath = Path().apply {
        moveTo(6f * cellSize, 9f * cellSize)
        lineTo(9f * cellSize, 9f * cellSize)
        lineTo(cx, cy)
        close()
    }
    drawPath(redPath, LudoColor.RED.toNeonColor().copy(alpha = 0.3f))
    drawPath(redPath, LudoColor.RED.toNeonColor(), style = Stroke(width = 2.dp.toPx()))

    // GREEN (left)
    val greenPath = Path().apply {
        moveTo(6f * cellSize, 6f * cellSize)
        lineTo(6f * cellSize, 9f * cellSize)
        lineTo(cx, cy)
        close()
    }
    drawPath(greenPath, LudoColor.GREEN.toNeonColor().copy(alpha = 0.3f))
    drawPath(greenPath, LudoColor.GREEN.toNeonColor(), style = Stroke(width = 2.dp.toPx()))

    // YELLOW (top)
    val yellowPath = Path().apply {
        moveTo(6f * cellSize, 6f * cellSize)
        lineTo(9f * cellSize, 6f * cellSize)
        lineTo(cx, cy)
        close()
    }
    drawPath(yellowPath, LudoColor.YELLOW.toNeonColor().copy(alpha = 0.3f))
    drawPath(yellowPath, LudoColor.YELLOW.toNeonColor(), style = Stroke(width = 2.dp.toPx()))

    // BLUE (right)
    val bluePath = Path().apply {
        moveTo(9f * cellSize, 6f * cellSize)
        lineTo(9f * cellSize, 9f * cellSize)
        lineTo(cx, cy)
        close()
    }
    drawPath(bluePath, LudoColor.BLUE.toNeonColor().copy(alpha = 0.3f))
    drawPath(bluePath, LudoColor.BLUE.toNeonColor(), style = Stroke(width = 2.dp.toPx()))
}

// Draw star markings on safe zones
private fun DrawScope.drawSafeZones(cellSize: Float) {
    LudoCoordinates.SAFE_ZONE_INDEXES.forEach { idx ->
        val cell = LudoCoordinates.TRACK[idx]
        val cx = cell.col * cellSize + cellSize / 2f
        val cy = cell.row * cellSize + cellSize / 2f

        // Draw a glowing diamond/star in safe cells
        val size = cellSize * 0.4f
        val path = Path().apply {
            moveTo(cx, cy - size)
            lineTo(cx + size * 0.4f, cy - size * 0.4f)
            lineTo(cx + size, cy)
            lineTo(cx + size * 0.4f, cy + size * 0.4f)
            lineTo(cx, cy + size)
            lineTo(cx - size * 0.4f, cy + size * 0.4f)
            lineTo(cx - size, cy)
            lineTo(cx - size * 0.4f, cy - size * 0.4f)
            close()
        }
        drawPath(path, Secondary.copy(alpha = 0.8f))
        drawPath(path, Secondary, style = Stroke(width = 1.5.dp.toPx()))
    }
}

// Draw a glowing 3D cylindrical token peg
private fun DrawScope.draw3DToken(
    col: Float,
    row: Float,
    hop: Float,
    color: Color,
    cellSize: Float,
    isClickable: Boolean
) {
    val cx = col * cellSize + cellSize / 2f
    val cy = row * cellSize + cellSize / 2f

    // Calculate vertical offset due to "hop" height animation
    val hopOffset = hop * cellSize * 0.8f
    val tokenHeight = cellSize * 0.4f

    // Center point shifted upwards by the hop offset
    val tx = cx
    val ty = cy - hopOffset

    // 1. Draw Ground Shadow underneath (larger when token is on the floor, smaller/faded when hopping)
    val shadowRadius = (cellSize * 0.3f) * (1f - hop * 0.4f)
    drawOval(
        color = Color.Black.copy(alpha = 0.6f * (1f - hop * 0.6f)),
        topLeft = Offset(cx - shadowRadius, cy - shadowRadius * 0.5f),
        size = Size(shadowRadius * 2f, shadowRadius * 1f)
    )

    // 2. Draw 3D cylinder body
    val bodyWidth = cellSize * 0.4f
    val bodyHeight = tokenHeight
    
    // Draw body cylinder (gradient fill for 3D curved lighting)
    val cylinderPath = Path().apply {
        // Left side line
        moveTo(tx - bodyWidth / 2f, ty)
        lineTo(tx - bodyWidth / 2f, ty - bodyHeight)
        // Top cap curve
        quadraticBezierTo(tx, ty - bodyHeight + bodyWidth * 0.2f, tx + bodyWidth / 2f, ty - bodyHeight)
        // Right side line
        lineTo(tx + bodyWidth / 2f, ty)
        // Bottom cap curve
        quadraticBezierTo(tx, ty + bodyWidth * 0.2f, tx - bodyWidth / 2f, ty)
    }

    val bodyBrush = Brush.linearGradient(
        colors = listOf(
            color.copy(alpha = 0.9f),
            color.copy(alpha = 0.4f),
            color.copy(alpha = 0.9f)
        ),
        start = Offset(tx - bodyWidth / 2f, 0f),
        end = Offset(tx + bodyWidth / 2f, 0f)
    )
    
    drawPath(cylinderPath, bodyBrush)
    drawPath(cylinderPath, color.copy(alpha = 0.7f), style = Stroke(width = 1.dp.toPx()))

    // 3. Draw Top Cap face
    drawOval(
        color = color.copy(alpha = 0.8f),
        topLeft = Offset(tx - bodyWidth / 2f, ty - bodyHeight - bodyWidth * 0.1f),
        size = Size(bodyWidth, bodyWidth * 0.2f)
    )
    
    // Draw inner glow on the top cap
    drawOval(
        color = Color.White,
        topLeft = Offset(tx - bodyWidth * 0.2f, ty - bodyHeight - bodyWidth * 0.05f),
        size = Size(bodyWidth * 0.4f, bodyWidth * 0.1f)
    )

    // 4. Clickable Turn Highlight (Glowing Pulsing Ring around base)
    if (isClickable) {
        val pulseScale = 1f + (System.currentTimeMillis() % 1000) / 1000f * 0.4f
        val pulseRadius = cellSize * 0.45f * pulseScale
        drawOval(
            color = color.copy(alpha = 0.4f * (2f - pulseScale)),
            topLeft = Offset(tx - pulseRadius, ty - pulseRadius * 0.5f),
            size = Size(pulseRadius * 2f, pulseRadius * 1f),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

// Class to manage step-by-step path hop animations
class AnimatableToken(startCol: Float, startRow: Float) {
    val col = Animatable(startCol)
    val row = Animatable(startRow)
    val hop = Animatable(0f)

    var targetCol = startCol
    var targetRow = startRow

    suspend fun animateTo(destCol: Float, destRow: Float) {
        targetCol = destCol
        targetRow = destRow

        // Compute step difference
        val diffCol = destCol - col.value
        val diffRow = destRow - row.value

        // If distance is small, do a direct single hop
        if (abs(diffCol) <= 1.1f && abs(diffRow) <= 1.1f) {
            // Simultaneous slide & hop
            val duration = 250
            coroutineScope {
                launch {
                    col.animateTo(destCol, tween(duration))
                }
                launch {
                    row.animateTo(destRow, tween(duration))
                }
                launch {
                    // Parabolic arc for hop (up and down)
                    hop.animateTo(1f, tween(duration / 2))
                    hop.animateTo(0f, tween(duration / 2))
                }
            }
        } else {
            // If teleporting (e.g. going back to base or loading game state), animate directly without hop
            col.snapTo(destCol)
            row.snapTo(destRow)
            hop.snapTo(0f)
        }
    }
}

// Coroutines support inside drawing scope
private suspend fun coroutineScope(block: suspend kotlinx.coroutines.CoroutineScope.() -> Unit) {
    kotlinx.coroutines.coroutineScope(block)
}
