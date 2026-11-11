package com.jigneshgaming.micai.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jigneshgaming.micai.AssistantState
import com.jigneshgaming.micai.ui.theme.JarvisPrimary
import com.jigneshgaming.micai.ui.theme.JarvisSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The JARVIS orb: layered glowing rings + a pulsing core. Animation speed
 * and ring count change with [state] — slow breathing when idle, fast
 * ripple while listening, shimmer while thinking, steady glow while speaking.
 */
@Composable
fun AssistantOrb(
    state: AssistantState,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp
) {
    val transition = rememberInfiniteTransition(label = "orb")
    val speed = when (state) {
        AssistantState.IDLE -> 2400
        AssistantState.LISTENING -> 700
        AssistantState.THINKING -> 1100
        AssistantState.SPEAKING -> 900
    }
    val pulse by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(speed),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(when (state) {
                AssistantState.IDLE -> 12000
                AssistantState.LISTENING -> 3000
                AssistantState.THINKING -> 5000
                AssistantState.SPEAKING -> 4000
            }),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(modifier = modifier.size(size)) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.center.x, this.center.y)
            val baseRadius = this.size.minDimension / 2f

            // Outer halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        JarvisPrimary.copy(alpha = 0.28f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * pulse
                ),
                radius = baseRadius * pulse,
                center = center
            )

            // Rotating dashed rings (drawn as arc segments)
            val rings = when (state) {
                AssistantState.IDLE -> 2
                AssistantState.LISTENING -> 3
                AssistantState.THINKING -> 3
                AssistantState.SPEAKING -> 2
            }
            for (i in 0 until rings) {
                val radius = baseRadius * (0.62f + i * 0.13f) * pulse
                val startAngle = rotation + i * 55f
                val sweep = 300f - i * 40f
                drawArc(
                    color = if (i % 2 == 0) JarvisPrimary else JarvisSecondary,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                )
            }

            // Core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFE0F7FF),
                        JarvisPrimary,
                        JarvisSecondary
                    ),
                    center = center,
                    radius = baseRadius * 0.42f * pulse
                ),
                radius = baseRadius * 0.42f * pulse,
                center = center
            )
        }
    }
}

/**
 * Voice waveform: 24 bars whose heights follow a sine wave; amplitude
 * grows while listening/speaking and flattens when idle.
 */
@Composable
fun VoiceWaveform(
    state: AssistantState,
    modifier: Modifier = Modifier,
    barCount: Int = 24,
    height: Dp = 48.dp
) {
    val transition = rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    val amplitude = when (state) {
        AssistantState.IDLE -> 0.12f
        AssistantState.LISTENING -> 1f
        AssistantState.THINKING -> 0.45f
        AssistantState.SPEAKING -> 0.85f
    }
    val density = LocalDensity.current
    val maxHeightPx = with(density) { height.toPx() }

    Canvas(modifier = modifier) {
        val barWidth = size.width / (barCount * 2)
        for (i in 0 until barCount) {
            val wave = (sin(phase + i * 0.55f) + 1f) / 2f
            val barHeight = maxHeightPx * (0.15f + amplitude * wave * 0.85f)
            val x = barWidth * (i * 2 + 0.5f)
            val top = (size.height - barHeight) / 2f
            drawRoundRect(
                color = JarvisPrimary.copy(alpha = 0.35f + wave * 0.65f),
                topLeft = Offset(x, top),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f)
            )
        }
    }
}
