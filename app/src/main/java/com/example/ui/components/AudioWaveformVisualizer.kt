package com.example.ui.components

import com.example.ui.theme.StudioColors

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun AudioWaveformVisualizer(
    energy: Float,
    decibels: Float,
    modifier: Modifier = Modifier,
    barCount: Int = 32,
    activeColor: Color = StudioColors.Accent,
    accentColor: Color = StudioColors.Accent
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2831f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        val width = size.width
        val height = size.height
        val barWidth = (width / barCount) * 0.65f
        val gap = (width - (barWidth * barCount)) / (barCount - 1)
        val centerY = height / 2f

        for (i in 0 until barCount) {
            val progress = i.toFloat() / barCount
            val wave = sin(progress * 4f + phase) * 0.35f + 0.65f
            val dynamicHeight = (height * 0.85f * (energy * 0.75f + 0.12f) * wave.toFloat())
                .coerceIn(6f, height)

            val x = i * (barWidth + gap)
            val y = centerY - (dynamicHeight / 2f)

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(accentColor, activeColor)
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, dynamicHeight),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
        }
    }
}
