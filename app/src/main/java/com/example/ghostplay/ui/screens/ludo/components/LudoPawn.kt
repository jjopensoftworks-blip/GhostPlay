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
    IDLE, MOVING, DANGER, CAPTURED, VICTORY
}

enum class PawnCharacterType {
    ROBOT, ASTRONAUT, KID
}

object LudoPawn {
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
        animState: PawnAnimationState = PawnAnimationState.IDLE,
        characterType: PawnCharacterType = PawnCharacterType.ROBOT,
        floatOffset: Float = 0f,
        armsOffset: Float = 0f,
        dangerPulse: Float = 0f,
        ringPulse: Float = 0f
    ) {
        val cx = col * cellSize + cellSize / 2f
        val cy = row * cellSize + cellSize / 2f
        val hopOffset = hop * cellSize * 0.8f
        
        val tx = cx
        val ty = cy - hopOffset - floatOffset

        with(drawScope) {
            // 1. Dust/Sparkle Trail for Moving
            if (animState == PawnAnimationState.MOVING && hop > 0.5f) {
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.4f), Color.Transparent)),
                    radius = cellSize * 0.4f * hop,
                    center = Offset(cx, cy)
                )
            }

            // 2. Shadow
            val shadowRadius = (cellSize * 0.35f) * (1f - hop * 0.4f) * scale
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
                val radius = cellSize * 0.42f // Scaled up to fit the larger cells
                
                // Danger aura
                if (animState == PawnAnimationState.DANGER) {
                    drawCircle(
                        color = Color.Red.copy(alpha = dangerPulse),
                        center = Offset(tx, ty),
                        radius = radius * 1.6f,
                        style = Stroke(2.dp.toPx())
                    )
                }

                // --- Character Rendering ---
                when (characterType) {
                    PawnCharacterType.ROBOT -> drawRobot(tx, ty, radius, color, animState, armsOffset)
                    PawnCharacterType.ASTRONAUT -> drawAstronaut(tx, ty, radius, color, animState, armsOffset)
                    PawnCharacterType.KID -> drawKid(tx, ty, radius, color, animState, armsOffset)
                }
            }
            
            // Interaction Ring
            if (isClickable) {
                drawCircle(
                    color = color.copy(alpha = 0.4f),
                    center = Offset(tx, ty),
                    radius = cellSize * 0.55f * ringPulse,
                    style = Stroke(2.dp.toPx())
                )
            }
        }
    }

    private fun DrawScope.drawRobot(tx: Float, ty: Float, radius: Float, color: Color, state: PawnAnimationState, arms: Float) {
        // Body
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(color, color.copy(alpha = 0.7f))),
            topLeft = Offset(tx - radius * 0.8f, ty - radius * 1.2f),
            size = Size(radius * 1.6f, radius * 2f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius * 0.4f)
        )
        // Visor
        drawRoundRect(
            color = Color.Black,
            topLeft = Offset(tx - radius * 0.6f, ty - radius * 0.9f),
            size = Size(radius * 1.2f, radius * 0.5f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
        )
        // Eyes
        val eyeColor = if (state == PawnAnimationState.CAPTURED) Color.Red else Color.Cyan
        drawCircle(eyeColor, radius * 0.15f, Offset(tx - radius * 0.25f, ty - radius * 0.65f))
        drawCircle(eyeColor, radius * 0.15f, Offset(tx + radius * 0.25f, ty - radius * 0.65f))
        
        // Arms
        if (state == PawnAnimationState.VICTORY) {
            drawLine(color, Offset(tx - radius * 0.8f, ty), Offset(tx - radius * 1.2f, ty - arms - radius), 4.dp.toPx())
            drawLine(color, Offset(tx + radius * 0.8f, ty), Offset(tx + radius * 1.2f, ty - arms - radius), 4.dp.toPx())
        }
    }

    private fun DrawScope.drawAstronaut(tx: Float, ty: Float, radius: Float, color: Color, state: PawnAnimationState, arms: Float) {
        // Suit
        drawCircle(Color.White, radius * 0.9f, Offset(tx, ty - radius * 0.3f))
        drawRoundRect(Color.White, Offset(tx - radius * 0.7f, ty), Size(radius * 1.4f, radius * 1.2f), androidx.compose.ui.geometry.CornerRadius(radius * 0.5f))
        // Helmet Visor
        drawCircle(Color(0xFF1A1F26), radius * 0.6f, Offset(tx, ty - radius * 0.4f))
        drawCircle(color.copy(alpha = 0.4f), radius * 0.5f, Offset(tx, ty - radius * 0.4f))
        
        if (state == PawnAnimationState.VICTORY) {
            drawRoundRect(Color.White, Offset(tx - radius * 1.1f, ty - arms - radius * 0.5f), Size(radius * 0.4f, radius * 1f))
            drawRoundRect(Color.White, Offset(tx + radius * 0.7f, ty - arms - radius * 0.5f), Size(radius * 0.4f, radius * 1f))
        }
    }

    private fun DrawScope.drawKid(tx: Float, ty: Float, radius: Float, color: Color, state: PawnAnimationState, arms: Float) {
        // Suit
        drawCircle(color, radius, Offset(tx, ty))
        // Face
        drawCircle(Color(0xFFFFDBAC), radius * 0.7f, Offset(tx, ty - radius * 0.2f))
        // Eyes
        val eyeColor = if (state == PawnAnimationState.CAPTURED) Color.Red else Color.Black
        drawCircle(eyeColor, radius * 0.1f, Offset(tx - radius * 0.2f, ty - radius * 0.3f))
        drawCircle(eyeColor, radius * 0.1f, Offset(tx + radius * 0.2f, ty - radius * 0.3f))
        
        if (state == PawnAnimationState.VICTORY) {
            drawLine(color, Offset(tx, ty), Offset(tx - radius * 1.2f, ty - arms - radius), 6.dp.toPx())
            drawLine(color, Offset(tx, ty), Offset(tx + radius * 1.2f, ty - arms - radius), 6.dp.toPx())
        }
    }
}
