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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
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

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                if (isRolling) {
                    rotationZ = rotation
                    scaleX = scale
                    scaleY = scale
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Shadow
        Box(
            modifier = Modifier
                .size(size * 0.9f)
                .offset(y = 4.dp)
                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
        )

        // Dice Body
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, color.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
        ) {
            val diceSize = this.size.width
            
            // Background with gradient for 3D look
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        color.copy(alpha = 0.2f),
                        color.copy(alpha = 0.05f),
                        color.copy(alpha = 0.2f)
                    ),
                    start = Offset.Zero,
                    end = Offset(diceSize, diceSize)
                ),
                size = this.size,
                cornerRadius = CornerRadius(12.dp.toPx())
            )

            // Draw Dots
            drawDiceDots(number, diceSize, color)
        }
    }
}

private fun DrawScope.drawDiceDots(number: Int, size: Float, color: Color) {
    val dotRadius = size * 0.08f
    val margin = size * 0.25f
    val center = size / 2f
    
    val dotColor = Color.White
    val glowColor = color.copy(alpha = 0.8f)

    fun drawDot(x: Float, y: Float) {
        // Outer glow
        drawCircle(
            color = glowColor,
            radius = dotRadius * 1.5f,
            center = Offset(x, y)
        )
        // Inner dot
        drawCircle(
            color = dotColor,
            radius = dotRadius,
            center = Offset(x, y)
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
