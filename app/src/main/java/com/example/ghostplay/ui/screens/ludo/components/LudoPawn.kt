package com.example.ghostplay.ui.screens.ludo.components
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

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
        ringPulse: Float = 0f,
    ) {
        val cx = (col * cellSize) + (cellSize / 2f)
        val cy = (row * cellSize) + (cellSize / 2f)
        val hopOffset = hop * cellSize * 0.8f
        
        val ty = cy - hopOffset - floatOffset
        val radius = cellSize * 0.42f

        with(drawScope) {
            // 1. Dust/Sparkle Trail for Moving + Neon Shockwave Ripple
            if (animState == PawnAnimationState.MOVING) {
                // Expanding neon shockwave ripple ring
                val rippleRadius = cellSize * 0.7f * (hop % 1f)
                drawCircle(
                    color = color.copy(alpha = 1f - (hop % 1f)),
                    radius = rippleRadius,
                    center = Offset(cx, cy),
                    style = Stroke(1.5.dp.toPx())
                )
                
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

            // 3. Comedic Dizzy Stars over head when captured
            if (animState == PawnAnimationState.CAPTURED) {
                val time = System.currentTimeMillis() / 150f
                val orbitRadius = radius * 0.5f
                for (i in 0..2) {
                    val angle = time + (i * 2 * Math.PI / 3)
                    val starX = cx + cos(angle).toFloat() * orbitRadius
                    val starY = ty - radius * 1.5f + sin(angle * 0.5).toFloat() * (orbitRadius * 0.3f)
                    
                    // Draw a tiny yellow star
                    drawCircle(
                        color = Color(0xFFFFE500),
                        radius = 3.dp.toPx(),
                        center = Offset(starX, starY)
                    )
                }
            }

            // 4. Comedic Oversized Hammer Swing on Victory/Elimination
            if (animState == PawnAnimationState.VICTORY) {
                val swingTime = System.currentTimeMillis() / 100f
                val angle = 30f + sin(swingTime.toDouble()).toFloat() * 30f
                
                withTransform({
                    translate(cx, ty - radius * 1.2f)
                    rotate(angle, Offset.Zero)
                    translate(-cx, -(ty - radius * 1.2f))
                }) {
                    // Hammer Handle (Brown Wood)
                    drawLine(
                        color = Color(0xFF8B5A2B),
                        start = Offset(cx, ty - radius * 1.2f),
                        end = Offset(cx, ty - radius * 2.5f),
                        strokeWidth = 3.5.dp.toPx()
                    )
                    // Hammer Head (Grey Comedic Block)
                    drawRoundRect(
                        color = Color(0xFF64748B),
                        topLeft = Offset(cx - radius * 0.7f, ty - radius * 2.8f),
                        size = Size(radius * 1.4f, radius * 0.6f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                    )
                }
            }

            withTransform({
                translate(cx, ty)
                scale(scale, scale, Offset.Zero)
                rotate(rotation, Offset.Zero)
                translate(-cx, -ty)
            }) {
                // Danger aura
                if (animState == PawnAnimationState.DANGER) {
                    drawCircle(
                        color = Color.Red.copy(alpha = dangerPulse),
                        center = Offset(cx, ty),
                        radius = radius * 1.6f,
                        style = Stroke(2.dp.toPx())
                    )
                }

                // --- Character Rendering ---
                when (characterType) {
                    PawnCharacterType.ROBOT -> drawRobot(cx, ty, radius, color, animState, armsOffset)
                    PawnCharacterType.ASTRONAUT -> drawAstronaut(cx, ty, radius, color, animState, armsOffset)
                    PawnCharacterType.KID -> drawKid(cx, ty, radius, color, animState, armsOffset)
                }
            }
            
            // Interaction Ring
            if (isClickable) {
                drawCircle(
                    color = color.copy(alpha = 0.4f),
                    center = Offset(cx, ty),
                    radius = cellSize * 0.55f * ringPulse,
                    style = Stroke(2.dp.toPx())
                )
            }
        }
    }

    private fun DrawScope.drawRobot(cx: Float, ty: Float, radius: Float, color: Color, state: PawnAnimationState, arms: Float) {
        val time = System.currentTimeMillis() / 150f
        
        // Comical Foot Tapping when waiting idle
        val leftTap = if (state == PawnAnimationState.IDLE && sin(time.toDouble()).toFloat() > 0.6f) 3.dp.toPx() else 0f
        val rightTap = if (state == PawnAnimationState.IDLE && sin(time.toDouble() + 1.5).toFloat() > 0.6f) 3.dp.toPx() else 0f

        // Draw Feet
        drawCircle(Color.Black.copy(alpha = 0.5f), radius * 0.2f, Offset(cx - radius * 0.4f, ty + radius * 0.9f - leftTap))
        drawCircle(Color.Black.copy(alpha = 0.5f), radius * 0.2f, Offset(cx + radius * 0.4f, ty + radius * 0.9f - rightTap))

        // Body
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(color, color.copy(alpha = 0.7f))),
            topLeft = Offset(cx - radius * 0.8f, ty - radius * 1.2f),
            size = Size(radius * 1.6f, radius * 2f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius * 0.4f)
        )
        // Visor
        drawRoundRect(
            color = Color.Black,
            topLeft = Offset(cx - radius * 0.6f, ty - radius * 0.9f),
            size = Size(radius * 1.2f, radius * 0.5f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
        )
        // Eyes
        val eyeColor = if (state == PawnAnimationState.CAPTURED) Color.Red else Color.Cyan
        drawCircle(eyeColor, radius * 0.15f, Offset(cx - radius * 0.25f, ty - radius * 0.65f))
        drawCircle(eyeColor, radius * 0.15f, Offset(cx + radius * 0.25f, ty - radius * 0.65f))
        
        // Arms
        if (state == PawnAnimationState.VICTORY) {
            drawLine(color, Offset(cx - radius * 0.8f, ty), Offset(cx - radius * 1.2f, ty - arms - radius), 4.dp.toPx())
            drawLine(color, Offset(cx + radius * 0.8f, ty), Offset(cx + radius * 1.2f, ty - arms - radius), 4.dp.toPx())
        }
    }

    private fun DrawScope.drawAstronaut(cx: Float, ty: Float, radius: Float, color: Color, state: PawnAnimationState, arms: Float) {
        val time = System.currentTimeMillis() / 150f
        val leftTap = if (state == PawnAnimationState.IDLE && sin(time.toDouble()).toFloat() > 0.6f) 3.dp.toPx() else 0f
        val rightTap = if (state == PawnAnimationState.IDLE && sin(time.toDouble() + 1.5).toFloat() > 0.6f) 3.dp.toPx() else 0f

        // Draw Boots
        drawCircle(Color.LightGray, radius * 0.2f, Offset(cx - radius * 0.35f, ty + radius * 0.9f - leftTap))
        drawCircle(Color.LightGray, radius * 0.2f, Offset(cx + radius * 0.35f, ty + radius * 0.9f - rightTap))

        // Suit
        drawCircle(Color.White, radius * 0.9f, Offset(cx, ty - radius * 0.3f))
        drawRoundRect(Color.White, Offset(cx - radius * 0.7f, ty), Size(radius * 1.4f, radius * 1.2f), androidx.compose.ui.geometry.CornerRadius(radius * 0.5f))
        // Helmet Visor
        drawCircle(Color(0xFF1A1F26), radius * 0.6f, Offset(cx, ty - radius * 0.4f))
        drawCircle(color.copy(alpha = 0.4f), radius * 0.5f, Offset(cx, ty - radius * 0.4f))
        
        if (state == PawnAnimationState.VICTORY) {
            drawRoundRect(Color.White, Offset(cx - radius * 1.1f, ty - arms - radius * 0.5f), Size(radius * 0.4f, radius * 1f))
            drawRoundRect(Color.White, Offset(cx + radius * 0.7f, ty - arms - radius * 0.5f), Size(radius * 0.4f, radius * 1f))
        }
    }

    private fun DrawScope.drawKid(cx: Float, ty: Float, radius: Float, color: Color, state: PawnAnimationState, arms: Float) {
        val time = System.currentTimeMillis() / 150f
        val leftTap = if (state == PawnAnimationState.IDLE && sin(time.toDouble()).toFloat() > 0.6f) 3.dp.toPx() else 0f
        val rightTap = if (state == PawnAnimationState.IDLE && sin(time.toDouble() + 1.5).toFloat() > 0.6f) 3.dp.toPx() else 0f

        // Draw Shoes
        drawCircle(Color.Black, radius * 0.18f, Offset(cx - radius * 0.3f, ty + radius * 0.9f - leftTap))
        drawCircle(Color.Black, radius * 0.18f, Offset(cx + radius * 0.3f, ty + radius * 0.9f - rightTap))

        // Suit
        drawCircle(color, radius, Offset(cx, ty))
        // Face
        drawCircle(Color(0xFFFFDBAC), radius * 0.7f, Offset(cx, ty - radius * 0.2f))
        // Eyes
        val eyeColor = if (state == PawnAnimationState.CAPTURED) Color.Red else Color.Black
        drawCircle(eyeColor, radius * 0.1f, Offset(cx - radius * 0.2f, ty - radius * 0.3f))
        drawCircle(eyeColor, radius * 0.1f, Offset(cx + radius * 0.2f, ty - radius * 0.3f))
        
        if (state == PawnAnimationState.VICTORY) {
            drawLine(color, Offset(cx, ty), Offset(cx - radius * 1.2f, ty - arms - radius), 6.dp.toPx())
            drawLine(color, Offset(cx, ty), Offset(cx + radius * 1.2f, ty - arms - radius), 6.dp.toPx())
        }
    }
}
