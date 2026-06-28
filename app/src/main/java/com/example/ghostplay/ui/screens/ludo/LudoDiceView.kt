package com.example.ghostplay.ui.screens.ludo

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ghostplay.ui.theme.Primary

@Composable
fun LudoDiceView(
    number: Int,
    isRolling: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 60.dp,
    color: Color = Primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dice_roll")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val bobbingAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4.dp.value,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bobbing"
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                if (isRolling) {
                    rotationZ = rotation
                    scaleX = scale
                    scaleY = scale
                } else {
                    translationY = -bobbingAnim // Floating bobbing effect
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Soft contact shadow below the floating dice
        Box(
            modifier = Modifier
                .size(size * 0.85f)
                .offset(y = 8.dp + bobbingAnim.dp)
                .graphicsLayer {
                    // Shadow shrinks as dice floats higher
                    val s = 1.0f - (bobbingAnim / 15.dp.value)
                    scaleX = s
                    scaleY = s
                    alpha = 0.35f * s
                }
                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
        )

        // Glass Dice Body Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
                .border(1.5.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
        ) {
            val diceSize = this.size.width
            
            // Refractive glass gradient base
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.28f),
                        Color.White.copy(alpha = 0.08f),
                        Color(0xFFE2E8F0).copy(alpha = 0.15f)
                    )
                ),
                size = this.size,
                cornerRadius = CornerRadius(14.dp.toPx())
            )

            // Dynamic diagonal gloss lines/refractions
            val glossPath = Path().apply {
                moveTo(0f, 0f)
                lineTo(diceSize * 0.4f, 0f)
                lineTo(0f, diceSize * 0.4f)
                close()
            }
            drawPath(
                path = glossPath,
                color = Color.White.copy(alpha = 0.25f)
            )

            val glarePath = Path().apply {
                moveTo(diceSize * 0.55f, 0f)
                lineTo(diceSize, 0f)
                lineTo(0f, diceSize * 1.0f)
                lineTo(0f, diceSize * 0.75f)
                close()
            }
            drawPath(
                path = glarePath,
                color = Color.White.copy(alpha = 0.12f)
            )

            // Inner glass rim highlight
            drawRoundRect(
                color = Color.White.copy(alpha = 0.3f),
                topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
                size = Size(diceSize - 4.dp.toPx(), diceSize - 4.dp.toPx()),
                cornerRadius = CornerRadius(12.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )

            // Draw Slate Black Dots
            drawDiceDots(number, diceSize, color)
        }
    }
}

private fun DrawScope.drawDiceDots(number: Int, size: Float, color: Color) {
    val dotRadius = size * 0.075f
    val margin = size * 0.26f
    val center = size / 2f
    
    val dotColor = Color(0xFF0F172A) // Dark Slate Black

    fun drawDot(x: Float, y: Float) {
        // Base Dot shadow
        drawCircle(
            color = Color.Black.copy(alpha = 0.2f),
            radius = dotRadius,
            center = Offset(x, y + 1.dp.toPx())
        )
        // Main crisp dot
        drawCircle(
            color = dotColor,
            radius = dotRadius,
            center = Offset(x, y)
        )
        // Dot glass sparkle specular
        drawCircle(
            color = Color.White.copy(alpha = 0.4f),
            radius = dotRadius * 0.3f,
            center = Offset(x - dotRadius * 0.3f, y - dotRadius * 0.3f)
        )
    }

    when (number) {
        1 -> {
            drawDot(center, center)
        }
        2 -> {
            drawDot(margin, margin)
            drawDot(size - margin, size - margin)
        }
        3 -> {
            drawDot(margin, margin)
            drawDot(center, center)
            drawDot(size - margin, size - margin)
        }
        4 -> {
            drawDot(margin, margin)
            drawDot(size - margin, margin)
            drawDot(margin, size - margin)
            drawDot(size - margin, size - margin)
        }
        5 -> {
            drawDot(margin, margin)
            drawDot(size - margin, margin)
            drawDot(center, center)
            drawDot(margin, size - margin)
            drawDot(size - margin, size - margin)
        }
        6 -> {
            drawDot(margin, margin)
            drawDot(size - margin, margin)
            drawDot(margin, center)
            drawDot(size - margin, center)
            drawDot(margin, size - margin)
            drawDot(size - margin, size - margin)
        }
    }
}
