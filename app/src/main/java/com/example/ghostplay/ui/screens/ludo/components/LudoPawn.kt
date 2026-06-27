package com.example.ghostplay.ui.screens.ludo.components

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp

enum class PawnAnimationState {
    IDLE, MOVING, DANGER, CAPTURED
}

object LudoPawn {
    @Composable
    fun draw(
        drawScope: DrawScope,
        col: Float,
        row: Float,
        hop: Float,
        scale: Float,
        rotation: Float,
        color: Color,
        cellSize: Float,
        isClickable: Boolean,
        animState: PawnAnimationState = PawnAnimationState.IDLE
    ) {
        val cx = col * cellSize + cellSize / 2f
        val cy = row * cellSize + cellSize / 2f
        val hopOffset = hop * cellSize * 0.8f
        
        // Idle floating effect
        val infiniteTransition = rememberInfiniteTransition(label = "pawn_idle")
        val floatOffset by if (animState == PawnAnimationState.IDLE) {
            infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 4.dp.value,
                animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Reverse),
                label = "float"
            )
        } else remember { mutableStateOf(0f) }

        val tx = cx
        val ty = cy - hopOffset - floatOffset

        with(drawScope) {
            // Shadow
            val shadowRadius = (cellSize * 0.3f) * (1f - hop * 0.4f) * scale
            drawOval(
                color = Color.Black.copy(alpha = 0.4f * (1f - hop * 0.6f)),
                topLeft = Offset(cx - shadowRadius, cy - shadowRadius * 0.5f),
                size = Size(shadowRadius * 2f, shadowRadius * 1f)
            )

            withTransform({
                translate(tx, ty)
                scale(scale, scale, Offset.Zero)
                rotate(rotation, Offset.Zero)
                translate(-tx, -ty)
            }) {
                val radius = cellSize * 0.35f
                
                // Danger aura
                if (animState == PawnAnimationState.DANGER) {
                    val dangerPulse by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 0.8f,
                        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
                        label = "danger"
                    )
                    drawCircle(
                        color = Color.Red.copy(alpha = dangerPulse),
                        center = Offset(tx, ty),
                        radius = radius * 1.6f,
                        style = Stroke(2.dp.toPx())
                    )
                }

                // Main body (Premium Capsule shape)
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(color, color.copy(alpha = 0.6f))
                    ),
                    topLeft = Offset(tx - radius * 0.8f, ty - radius * 1.2f),
                    size = Size(radius * 1.6f, radius * 2f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
                )

                // Face / Visor
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.8f),
                    topLeft = Offset(tx - radius * 0.5f, ty - radius * 0.8f),
                    size = Size(radius * 1f, radius * 0.6f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                )

                // Glowing Eyes
                val eyeColor = if (animState == PawnAnimationState.CAPTURED) Color.Red else Color.Cyan
                drawCircle(eyeColor, radius * 0.1f, Offset(tx - radius * 0.2f, ty - radius * 0.5f))
                drawCircle(eyeColor, radius * 0.1f, Offset(tx + radius * 0.2f, ty - radius * 0.5f))
            }
            
            // Interaction Ring
            if (isClickable) {
                val ringPulse by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.3f,
                    animationSpec = infiniteRepeatable(tween(800), RepeatMode.Restart),
                    label = "ring"
                )
                drawCircle(
                    color = color.copy(alpha = 0.4f),
                    center = Offset(tx, ty),
                    radius = cellSize * 0.5f * ringPulse,
                    style = Stroke(1.5.dp.toPx())
                )
            }
        }
    }
}
