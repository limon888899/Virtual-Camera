package com.example.ui.components

import com.example.ui.theme.StudioColors

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LipSyncState

@Composable
fun LipSyncAvatarView(
    lipSyncState: LipSyncState,
    modifier: Modifier = Modifier,
    customPhotoBitmap: Bitmap? = null,
    showTrackingMesh: Boolean = true
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(StudioColors.Ink)
            .border(1.dp, StudioColors.Accent.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f
            val centerY = height / 2f

            if (customPhotoBitmap != null) {
                // Draw custom photo
                val imageBitmap = customPhotoBitmap.asImageBitmap()
                drawImage(
                    image = imageBitmap,
                    dstSize = IntSize(width.toInt(), height.toInt())
                )
                // Draw dynamic synchronized mouth overlay on photo
                val mouthCenterY = centerY + (height * 0.18f)
                drawMouth(
                    centerX = centerX,
                    centerY = mouthCenterY,
                    mouthOpen = lipSyncState.mouthOpenRatio,
                    mouthWidth = lipSyncState.mouthWidthRatio,
                    isSpeaking = lipSyncState.isSpeaking
                )
            } else {
                // Draw stylized futuristic avatar
                // 1. Head shape
                val headRadiusX = width * 0.28f
                val headRadiusY = height * 0.36f
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF2C3E50), Color(0xFF1A1E29)),
                        center = Offset(centerX, centerY - 20f),
                        radius = headRadiusY * 1.2f
                    ),
                    topLeft = Offset(centerX - headRadiusX, centerY - headRadiusY),
                    size = Size(headRadiusX * 2, headRadiusY * 2)
                )

                // 2. Neck
                val neckWidth = headRadiusX * 0.8f
                drawRect(
                    color = StudioColors.Surface,
                    topLeft = Offset(centerX - neckWidth / 2f, centerY + headRadiusY * 0.6f),
                    size = Size(neckWidth, height * 0.3f)
                )

                // 3. Eyebrows (slight lift when speaking)
                val eyebrowOffset = if (lipSyncState.isSpeaking) -6f else 0f
                val browY = centerY - (headRadiusY * 0.35f) + eyebrowOffset
                val eyeGap = headRadiusX * 0.55f

                // Left Eyebrow
                drawLine(
                    color = StudioColors.Blue,
                    start = Offset(centerX - eyeGap - 24f, browY + 4f),
                    end = Offset(centerX - eyeGap + 24f, browY - 4f),
                    strokeWidth = 6f
                )
                // Right Eyebrow
                drawLine(
                    color = StudioColors.Blue,
                    start = Offset(centerX + eyeGap - 24f, browY - 4f),
                    end = Offset(centerX + eyeGap + 24f, browY + 4f),
                    strokeWidth = 6f
                )

                // 4. Eyes (with blinking)
                val eyeY = centerY - (headRadiusY * 0.15f)
                val eyeWidth = 32f
                val eyeHeight = 22f * (1f - lipSyncState.eyeBlinkRatio).coerceAtLeast(0.08f)

                // Left Eye
                drawOval(
                    color = StudioColors.Text,
                    topLeft = Offset(centerX - eyeGap - (eyeWidth / 2f), eyeY - (eyeHeight / 2f)),
                    size = Size(eyeWidth, eyeHeight)
                )
                // Left Pupil
                drawCircle(
                    color = StudioColors.Accent,
                    radius = (eyeHeight * 0.42f).coerceAtLeast(2f),
                    center = Offset(centerX - eyeGap, eyeY)
                )

                // Right Eye
                drawOval(
                    color = StudioColors.Text,
                    topLeft = Offset(centerX + eyeGap - (eyeWidth / 2f), eyeY - (eyeHeight / 2f)),
                    size = Size(eyeWidth, eyeHeight)
                )
                // Right Pupil
                drawCircle(
                    color = StudioColors.Accent,
                    radius = (eyeHeight * 0.42f).coerceAtLeast(2f),
                    center = Offset(centerX + eyeGap, eyeY)
                )

                // 5. Nose
                val noseY = centerY + (headRadiusY * 0.10f)
                drawLine(
                    color = StudioColors.Text.copy(alpha = 0.35f),
                    start = Offset(centerX, noseY - 14f),
                    end = Offset(centerX, noseY + 8f),
                    strokeWidth = 4f
                )

                // 6. Dynamic Mouth
                val mouthY = centerY + (headRadiusY * 0.45f)
                drawMouth(
                    centerX = centerX,
                    centerY = mouthY,
                    mouthOpen = lipSyncState.mouthOpenRatio,
                    mouthWidth = lipSyncState.mouthWidthRatio,
                    isSpeaking = lipSyncState.isSpeaking
                )
            }

            // 7. Futuristic AI Tracking Mesh / HUD
            if (showTrackingMesh) {
                // Tracking box
                val hudColor = if (lipSyncState.isSpeaking) StudioColors.AccentSoft.copy(alpha = 0.6f)
                else StudioColors.Accent.copy(alpha = 0.4f)

                // Corner markers
                val markerLen = 22f
                val pad = 16f

                // Top-Left
                drawLine(hudColor, Offset(pad, pad), Offset(pad + markerLen, pad), 3f)
                drawLine(hudColor, Offset(pad, pad), Offset(pad, pad + markerLen), 3f)

                // Top-Right
                drawLine(hudColor, Offset(width - pad, pad), Offset(width - pad - markerLen, pad), 3f)
                drawLine(hudColor, Offset(width - pad, pad), Offset(width - pad, pad + markerLen), 3f)

                // Bottom-Left
                drawLine(hudColor, Offset(pad, height - pad), Offset(pad + markerLen, height - pad), 3f)
                drawLine(hudColor, Offset(pad, height - pad), Offset(pad, height - pad - markerLen), 3f)

                // Bottom-Right
                drawLine(hudColor, Offset(width - pad, height - pad), Offset(width - pad - markerLen, height - pad), 3f)
                drawLine(hudColor, Offset(width - pad, height - pad), Offset(width - pad, height - pad - markerLen), 3f)
            }
        }

        // Live HUD Badges
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            Text(
                text = "LOCAL AUDIO-REACTIVE ANIMATION",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (lipSyncState.isSpeaking) StudioColors.AccentSoft else StudioColors.Blue,
                modifier = Modifier.align(Alignment.TopStart)
            )

            Text(
                text = "Phoneme [${lipSyncState.currentPhoneme}]",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = StudioColors.Accent,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMouth(
    centerX: Float,
    centerY: Float,
    mouthOpen: Float,
    mouthWidth: Float,
    isSpeaking: Boolean
) {
    val baseWidth = 50f + (mouthWidth * 45f)
    val baseOpen = 4f + (mouthOpen * 52f)

    if (mouthOpen > 0.08f) {
        // Inner mouth dark cavity
        drawRoundRect(
            color = StudioColors.SurfaceRaised,
            topLeft = Offset(centerX - (baseWidth / 2f), centerY - (baseOpen / 2f)),
            size = Size(baseWidth, baseOpen),
            cornerRadius = CornerRadius(baseOpen / 2f, baseOpen / 2f)
        )

        // Upper teeth
        val teethHeight = (baseOpen * 0.28f).coerceAtMost(10f)
        drawRoundRect(
            color = StudioColors.Text.copy(alpha = 0.95f),
            topLeft = Offset(centerX - (baseWidth * 0.35f), centerY - (baseOpen / 2f)),
            size = Size(baseWidth * 0.7f, teethHeight),
            cornerRadius = CornerRadius(3f, 3f)
        )

        // Tongue preview on wide open
        if (mouthOpen > 0.35f) {
            val tongueH = baseOpen * 0.3f
            drawOval(
                color = StudioColors.Accent,
                topLeft = Offset(centerX - (baseWidth * 0.3f), centerY + (baseOpen / 2f) - tongueH),
                size = Size(baseWidth * 0.6f, tongueH)
            )
        }

        // Lip outline
        drawRoundRect(
            color = StudioColors.Danger,
            topLeft = Offset(centerX - (baseWidth / 2f), centerY - (baseOpen / 2f)),
            size = Size(baseWidth, baseOpen),
            cornerRadius = CornerRadius(baseOpen / 2f, baseOpen / 2f),
            style = Stroke(width = 4f)
        )
    } else {
        // Closed / resting lips line with gentle curve
        val path = Path().apply {
            moveTo(centerX - (baseWidth / 2f), centerY)
            quadraticTo(centerX, centerY + 3f, centerX + (baseWidth / 2f), centerY)
        }
        drawPath(
            path = path,
            color = StudioColors.Danger,
            style = Stroke(width = 4.5f)
        )
    }
}
