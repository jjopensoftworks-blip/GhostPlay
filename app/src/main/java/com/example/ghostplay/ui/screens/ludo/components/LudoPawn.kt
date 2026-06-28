package com.example.ghostplay.ui.screens.ludo.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import com.example.ghostplay.ui.screens.ludo.LudoColor
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

enum class PawnAnimationState {
    IDLE, MOVING, DANGER, CAPTURED, VICTORY
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
        pawnColor: LudoColor,
        floatOffset: Float = 0f,
        armsOffset: Float = 0f,
        dangerPulse: Float = 0f,
        ringPulse: Float = 0f,
    ) {
        val cx = (col * cellSize) + (cellSize / 2f)
        val cy = (row * cellSize) + (cellSize / 2f)
        val hopOffset = hop * cellSize * 0.8f
        val ty = cy - hopOffset - floatOffset
        val radius = cellSize * 0.44f // Oversized character

        with(drawScope) {
            // 1. Contact shadow (on ground plane)
            val shadowRadius = (cellSize * 0.35f) * (1f - hop * 0.4f) * scale
            drawOval(
                color = Color.Black.copy(alpha = 0.5f * (1f - hop * 0.6f)),
                topLeft = Offset(cx - shadowRadius, cy - shadowRadius * 0.4f),
                size = Size(shadowRadius * 2f, shadowRadius * 0.8f)
            )

            // 2. Captured dizzy stars
            if (animState == PawnAnimationState.CAPTURED) {
                val time = System.currentTimeMillis() / 150f
                val orbitRadius = radius * 0.6f
                for (i in 0..2) {
                    val angle = time + (i * 2 * Math.PI / 3)
                    val starX = cx + cos(angle).toFloat() * orbitRadius
                    val starY = ty - radius * 1.6f + sin(angle * 0.5).toFloat() * (orbitRadius * 0.3f)
                    drawCircle(
                        color = Color(0xFFFFE500),
                        radius = 3.dp.toPx(),
                        center = Offset(starX, starY)
                    )
                }
            }

            // 3. Waving / Movement Transform
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

                val time = System.currentTimeMillis() / 1000.0 // seconds

                when (pawnColor) {
                    LudoColor.GREEN -> drawGreenJoggingPawn(cx, ty, radius, color, animState, time)
                    LudoColor.YELLOW -> drawYellowYawningPawn(cx, ty, radius, color, animState, time)
                    LudoColor.RED -> drawPinkStancePawn(cx, ty, radius, color, animState, time)
                    LudoColor.BLUE -> drawBlueWavingPawn(cx, ty, radius, color, animState, time)
                }
            }

            // 4. Clickable indicator ring
            if (isClickable) {
                drawCircle(
                    color = color.copy(alpha = 0.5f),
                    center = Offset(cx, ty),
                    radius = cellSize * 0.65f * ringPulse,
                    style = Stroke(2.5.dp.toPx())
                )
            }
        }
    }

    // GREEN: Jogging Green Robot
    private fun DrawScope.drawGreenJoggingPawn(cx: Float, ty: Float, radius: Float, color: Color, state: PawnAnimationState, time: Double) {
        var localTy = ty
        var footLiftLeft = 0f
        var footLiftRight = 0f
        var armSwing = 0f
        var leanAngle = 0f

        if (state == PawnAnimationState.IDLE) {
            val speed = 7.5
            val jogBob = sin(time * speed).toFloat() * 3.dp.toPx()
            localTy += jogBob

            footLiftLeft = if (sin(time * speed).toFloat() > 0f) sin(time * speed).toFloat() * 5.dp.toPx() else 0f
            footLiftRight = if (sin(time * speed).toFloat() <= 0f) -sin(time * speed).toFloat() * 5.dp.toPx() else 0f
            armSwing = sin(time * speed).toFloat() * 6.dp.toPx()
            leanAngle = sin(time * 2.0).toFloat() * 4f
        }

        withTransform({
            translate(cx, localTy)
            rotate(leanAngle, Offset(cx, localTy))
            translate(-cx, -localTy)
        }) {
            // Legs / Feet
            drawRoundRect(
                color = Color(0xFF0F172A),
                topLeft = Offset(cx - radius * 0.45f, localTy + radius * 0.8f - footLiftLeft),
                size = Size(radius * 0.22f, radius * 0.35f),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
            drawRoundRect(
                color = Color(0xFF0F172A),
                topLeft = Offset(cx + radius * 0.22f, localTy + radius * 0.8f - footLiftRight),
                size = Size(radius * 0.22f, radius * 0.35f),
                cornerRadius = CornerRadius(4.dp.toPx())
            )

            // Robot Body (3D cylindrical shine brush)
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        color.copy(alpha = 0.9f),
                        Color.White.copy(alpha = 0.3f),
                        color.copy(alpha = 0.6f)
                    )
                ),
                topLeft = Offset(cx - radius * 0.65f, localTy - radius * 0.9f),
                size = Size(radius * 1.3f, radius * 1.8f),
                cornerRadius = CornerRadius(radius * 0.3f)
            )

            // Tech screen pane
            drawRoundRect(
                color = Color(0xFF0F172A),
                topLeft = Offset(cx - radius * 0.45f, localTy - radius * 0.6f),
                size = Size(radius * 0.9f, radius * 0.42f),
                cornerRadius = CornerRadius(6.dp.toPx())
            )

            // Glowing Eyes (Cyan/Light Green)
            drawCircle(Color(0xFF00FFCC), radius * 0.08f, Offset(cx - radius * 0.18f, localTy - radius * 0.4f))
            drawCircle(Color(0xFF00FFCC), radius * 0.08f, Offset(cx + radius * 0.18f, localTy - radius * 0.4f))

            // Visor shine reflection
            drawLine(
                color = Color.White.copy(alpha = 0.4f),
                start = Offset(cx - radius * 0.35f, localTy - radius * 0.55f),
                end = Offset(cx - radius * 0.1f, localTy - radius * 0.35f),
                strokeWidth = 1.5.dp.toPx()
            )

            // Antennae
            drawCircle(Color.LightGray, radius * 0.1f, Offset(cx - radius * 0.7f, localTy - radius * 0.7f))
            drawCircle(Color.LightGray, radius * 0.1f, Offset(cx + radius * 0.7f, localTy - radius * 0.7f))
            drawLine(Color.LightGray, Offset(cx, localTy - radius * 0.9f), Offset(cx, localTy - radius * 1.2f), 2.dp.toPx())
            drawCircle(Color(0xFF00FF9D), radius * 0.08f, Offset(cx, localTy - radius * 1.2f))

            // Arms swinging
            drawCircle(Color.White, radius * 0.14f, Offset(cx - radius * 0.8f, localTy - radius * 0.2f + armSwing))
            drawCircle(Color.White, radius * 0.14f, Offset(cx + radius * 0.8f, localTy - radius * 0.2f - armSwing))
        }
    }

    // YELLOW: Yawning Yellow Astronaut
    private fun DrawScope.drawYellowYawningPawn(cx: Float, ty: Float, radius: Float, color: Color, state: PawnAnimationState, time: Double) {
        var localTy = ty
        var yawnCycle = 0f
        var leftArmYOffset = 0f
        var visorColor = Color(0xFF0F172A)

        if (state == PawnAnimationState.IDLE) {
            val period = 4.5
            val modTime = time % period
            if (modTime > 2.0) {
                val progress = (modTime - 2.0) / 2.5
                yawnCycle = sin(progress * Math.PI).toFloat()
            }
            
            localTy += yawnCycle * 2.dp.toPx()
            leftArmYOffset = yawnCycle * radius * 0.8f
            
            visorColor = Color(
                red = (15f / 255f + yawnCycle * 0.8f).coerceIn(0f, 1f),
                green = (23f / 255f + yawnCycle * 0.2f).coerceIn(0f, 1f),
                blue = (42f / 255f - yawnCycle * 0.15f).coerceIn(0f, 1f)
            )
        }

        // Oxygen tank backpack (3D shaded)
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(Color.Gray, Color.LightGray)),
            topLeft = Offset(cx - radius * 0.75f, localTy - radius * 0.7f),
            size = Size(radius * 1.5f, radius * 1.3f),
            cornerRadius = CornerRadius(8.dp.toPx())
        )

        // Suit Body (spherical white spacesuit with Z-depth highlight)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color(0xFFE2E8F0)),
                center = Offset(cx - radius * 0.2f, localTy + radius * 0.1f),
                radius = radius * 0.75f
            ),
            radius = radius * 0.75f,
            center = Offset(cx, localTy + radius * 0.2f)
        )
        // Yellow chest stripe
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(color, color.copy(alpha = 0.6f))),
            topLeft = Offset(cx - radius * 0.4f, localTy + radius * 0.1f),
            size = Size(radius * 0.8f, radius * 0.2f),
            cornerRadius = CornerRadius(2.dp.toPx())
        )

        // Helmet (white shaded dome)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color(0xFFE2E8F0)),
                center = Offset(cx - radius * 0.15f, localTy - radius * 0.5f),
                radius = radius * 0.65f
            ),
            radius = radius * 0.65f,
            center = Offset(cx, localTy - radius * 0.4f)
        )

        // Visor glass shield
        drawRoundRect(
            color = visorColor,
            topLeft = Offset(cx - radius * 0.45f - yawnCycle * 0.05f * radius, localTy - radius * 0.65f),
            size = Size(radius * 0.9f + yawnCycle * 0.1f * radius, radius * 0.45f),
            cornerRadius = CornerRadius(10.dp.toPx())
        )

        // Visor glare stripe
        drawLine(
            color = Color.White.copy(alpha = 0.4f),
            start = Offset(cx - radius * 0.35f, localTy - radius * 0.6f),
            end = Offset(cx - radius * 0.1f, localTy - radius * 0.35f),
            strokeWidth = 2.dp.toPx()
        )

        // Hands / Limbs
        drawCircle(
            color = Color.White,
            radius = radius * 0.16f,
            center = Offset(cx - radius * 0.75f + leftArmYOffset * 0.6f, localTy + radius * 0.1f - leftArmYOffset)
        )
        drawCircle(
            color = Color.White,
            radius = radius * 0.16f,
            center = Offset(cx + radius * 0.75f, localTy + radius * 0.1f)
        )

        // Feet
        drawCircle(Color.LightGray, radius * 0.18f, Offset(cx - radius * 0.3f, localTy + radius * 0.9f))
        drawCircle(Color.LightGray, radius * 0.18f, Offset(cx + radius * 0.3f, localTy + radius * 0.9f))
    }

    // PINK/RED: Hands on Hips Robot
    private fun DrawScope.drawPinkStancePawn(cx: Float, ty: Float, radius: Float, color: Color, state: PawnAnimationState, time: Double) {
        var localTy = ty
        var breathScaleX = 1.0f
        var breathScaleY = 1.0f

        if (state == PawnAnimationState.IDLE) {
            val speed = 2.2
            val breath = sin(time * speed).toFloat()
            breathScaleX = 1.0f + breath * 0.04f
            breathScaleY = 1.0f - breath * 0.02f
            localTy += breath * 0.5.dp.toPx()
        }

        withTransform({
            translate(cx, localTy)
            scale(breathScaleX, breathScaleY, Offset(cx, localTy + radius * 0.4f))
            translate(-cx, -localTy)
        }) {
            // Legs / Shoes
            drawCircle(Color(0xFF0F172A), radius * 0.18f, Offset(cx - radius * 0.35f, localTy + radius * 0.85f))
            drawCircle(Color(0xFF0F172A), radius * 0.18f, Offset(cx + radius * 0.35f, localTy + radius * 0.85f))

            // Body (3D metal shine gradient)
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        color.copy(alpha = 0.9f),
                        Color.White.copy(alpha = 0.35f),
                        color.copy(alpha = 0.55f)
                    )
                ),
                topLeft = Offset(cx - radius * 0.6f, localTy - radius * 0.8f),
                size = Size(radius * 1.2f, radius * 1.6f),
                cornerRadius = CornerRadius(radius * 0.4f)
            )

            // Visor screen pane
            drawRoundRect(
                color = Color(0xFF0F172A),
                topLeft = Offset(cx - radius * 0.42f, localTy - radius * 0.55f),
                size = Size(radius * 0.84f, radius * 0.4f),
                cornerRadius = CornerRadius(6.dp.toPx())
            )

            // Glowing Eyes (Pink/Magenta)
            drawCircle(Color(0xFFFF007F), radius * 0.08f, Offset(cx - radius * 0.18f, localTy - radius * 0.38f))
            drawCircle(Color(0xFFFF007F), radius * 0.08f, Offset(cx + radius * 0.18f, localTy - radius * 0.38f))

            // Visor reflection shine
            drawLine(
                color = Color.White.copy(alpha = 0.4f),
                start = Offset(cx - radius * 0.3f, localTy - radius * 0.5f),
                end = Offset(cx - radius * 0.1f, localTy - radius * 0.35f),
                strokeWidth = 1.5.dp.toPx()
            )

            // Arms on Hips (Bent Outwards)
            val leftArmPath = Path().apply {
                moveTo(cx - radius * 0.6f, localTy - radius * 0.4f)
                lineTo(cx - radius * 0.92f, localTy - radius * 0.1f)
                lineTo(cx - radius * 0.58f, localTy + radius * 0.2f)
            }
            drawPath(
                path = leftArmPath,
                color = color,
                style = Stroke(4.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )

            val rightArmPath = Path().apply {
                moveTo(cx + radius * 0.6f, localTy - radius * 0.4f)
                lineTo(cx + radius * 0.92f, localTy - radius * 0.1f)
                lineTo(cx + radius * 0.58f, localTy + radius * 0.2f)
            }
            drawPath(
                path = rightArmPath,
                color = color,
                style = Stroke(4.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )

            // Hand pods on hips
            drawCircle(Color.White, radius * 0.1f, Offset(cx - radius * 0.58f, localTy + radius * 0.2f))
            drawCircle(Color.White, radius * 0.1f, Offset(cx + radius * 0.58f, localTy + radius * 0.2f))
        }
    }

    // BLUE: Waving Blue Astronaut
    private fun DrawScope.drawBlueWavingPawn(cx: Float, ty: Float, radius: Float, color: Color, state: PawnAnimationState, time: Double) {
        var waveAngle = 0f
        if (state == PawnAnimationState.IDLE) {
            val speed = 9.0
            waveAngle = sin(time * speed).toFloat() * 18f
        }

        // Oxygen tank backpack
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(Color.Gray, Color.LightGray)),
            topLeft = Offset(cx - radius * 0.7f, ty - radius * 0.7f),
            size = Size(radius * 1.4f, radius * 1.3f),
            cornerRadius = CornerRadius(8.dp.toPx())
        )

        // Suit Body (spherical white spacesuit with 3D shadow)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color(0xFFE2E8F0)),
                center = Offset(cx - radius * 0.2f, ty + radius * 0.1f),
                radius = radius * 0.75f
            ),
            radius = radius * 0.75f,
            center = Offset(cx, ty + radius * 0.2f)
        )
        // Cyan chest stripe
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(color, color.copy(alpha = 0.6f))),
            topLeft = Offset(cx - radius * 0.4f, ty + radius * 0.1f),
            size = Size(radius * 0.8f, radius * 0.2f),
            cornerRadius = CornerRadius(2.dp.toPx())
        )

        // Helmet
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color(0xFFE2E8F0)),
                center = Offset(cx - radius * 0.15f, ty - radius * 0.5f),
                radius = radius * 0.65f
            ),
            radius = radius * 0.65f,
            center = Offset(cx, ty - radius * 0.4f)
        )

        // Visor
        drawRoundRect(
            color = Color(0xFF0F172A),
            topLeft = Offset(cx - radius * 0.45f, ty - radius * 0.65f),
            size = Size(radius * 0.9f, radius * 0.45f),
            cornerRadius = CornerRadius(10.dp.toPx())
        )
        // Visor shine reflection
        drawLine(
            color = Color.White.copy(alpha = 0.5f),
            start = Offset(cx - radius * 0.35f, ty - radius * 0.6f),
            end = Offset(cx - radius * 0.1f, ty - radius * 0.35f),
            strokeWidth = 2.dp.toPx()
        )

        // Left arm resting by side
        drawCircle(
            color = Color.White,
            radius = radius * 0.16f,
            center = Offset(cx - radius * 0.75f, ty + radius * 0.1f)
        )

        // Right arm raised waving
        withTransform({
            translate(cx + radius * 0.65f, ty - radius * 0.1f)
            rotate(waveAngle, Offset(0f, 0f))
            translate(-(cx + radius * 0.65f), -(ty - radius * 0.1f))
        }) {
            drawLine(
                color = Color.White,
                start = Offset(cx + radius * 0.65f, ty - radius * 0.1f),
                end = Offset(cx + radius * 0.85f, ty - radius * 0.9f),
                strokeWidth = 5.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            drawCircle(
                color = color,
                radius = radius * 0.14f,
                center = Offset(cx + radius * 0.85f, ty - radius * 0.9f)
            )
        }

        // Feet
        drawCircle(Color.LightGray, radius * 0.18f, Offset(cx - radius * 0.3f, ty + radius * 0.9f))
        drawCircle(Color.LightGray, radius * 0.18f, Offset(cx + radius * 0.3f, ty + radius * 0.9f))
    }
}
