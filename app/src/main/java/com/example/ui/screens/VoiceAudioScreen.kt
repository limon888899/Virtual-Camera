package com.example.ui.screens

import com.example.ui.theme.StudioColors

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VoicePreset
import com.example.ui.components.AudioWaveformVisualizer

@Composable
fun VoiceAudioScreen(
    energy: Float,
    decibels: Float,
    isAudioActive: Boolean,
    isMonitorEnabled: Boolean,
    isNoiseCancellationEnabled: Boolean,
    currentVoicePreset: VoicePreset,
    onToggleNoiseCancellation: (Boolean) -> Unit,
    onToggleMonitor: (Boolean) -> Unit,
    onSelectVoicePreset: (VoicePreset) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // VU Level Meter & Decibel Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = StudioColors.Surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(StudioColors.Accent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Equalizer,
                                contentDescription = "Audio VU",
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Microphone Live Feed",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = StudioColors.Text
                            )
                            Text(
                                text = if (isAudioActive) "Microphone capture active" else "Microphone idle",
                                fontSize = 11.sp,
                                color = if (isAudioActive) StudioColors.AccentSoft else StudioColors.Text.copy(alpha = 0.5f)
                            )
                        }
                    }

                    Text(
                        text = "${decibels.toInt()} dB",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = StudioColors.Accent
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                AudioWaveformVisualizer(
                    energy = energy,
                    decibels = decibels,
                    activeColor = StudioColors.AccentSoft,
                    accentColor = StudioColors.Accent,
                    modifier = Modifier.height(56.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // VU Meter Progress Bar
                val normalizedLevel = ((decibels + 60f) / 60f).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { normalizedLevel },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = when {
                        normalizedLevel > 0.85f -> StudioColors.Danger
                        normalizedLevel > 0.65f -> StudioColors.Warning
                        else -> StudioColors.AccentSoft
                    },
                    trackColor = StudioColors.SurfaceHigh
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = StudioColors.Surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Listen to processed audio here",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = StudioColors.Text
                        )
                        Text(
                            text = if (isAudioActive) {
                                "App-local monitor only. Use headphones to avoid acoustic feedback."
                            } else {
                                "Start the local media service to monitor microphone audio."
                            },
                            fontSize = 11.sp,
                            color = StudioColors.Text.copy(alpha = 0.72f)
                        )
                    }
                    Switch(
                        checked = isMonitorEnabled,
                        onCheckedChange = onToggleMonitor,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = StudioColors.AccentSoft,
                            checkedTrackColor = StudioColors.AccentSoft.copy(alpha = 0.5f)
                        )
                    )
                }
                Text(
                    text = "Audio is played only by this app. Android does not let this app replace another app's microphone input.",
                    fontSize = 11.sp,
                    color = StudioColors.Text.copy(alpha = 0.72f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Local microphone noise filtering.
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = StudioColors.Surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(StudioColors.AccentSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Noise Cancellation",
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Microphone Noise Filter",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = StudioColors.Text
                            )
                            Text(
                                text = "Built-in device suppression (when available) and a local high-pass/noise gate",
                                fontSize = 11.sp,
                                color = StudioColors.Text.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Switch(
                        checked = isNoiseCancellationEnabled,
                        onCheckedChange = onToggleNoiseCancellation,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = StudioColors.AccentSoft,
                            checkedTrackColor = StudioColors.AccentSoft.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Voice Presets & Cloning Cards
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = StudioColors.Surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(StudioColors.Accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = "Audio monitor effects",
                            tint = StudioColors.Text,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Local voice monitor effects",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = StudioColors.Text
                        )
                        Text(
                            text = "Selected filters affect only the audio monitored inside this app; they do not change calls or other apps.",
                            fontSize = 11.sp,
                            color = StudioColors.Text.copy(alpha = 0.6f)
                        )
                    }
                }

                VoicePreset.entries.forEach { preset ->
                    val isSelected = preset == currentVoicePreset

                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) StudioColors.SurfaceRaised else StudioColors.Surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onSelectVoicePreset(preset) }
                            .then(
                                if (isSelected) Modifier.border(1.5.dp, StudioColors.Accent, RoundedCornerShape(10.dp))
                                else Modifier
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = preset.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) StudioColors.Accent else StudioColors.Text
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = preset.description,
                                    fontSize = 11.sp,
                                    color = StudioColors.Text.copy(alpha = 0.6f)
                                )
                            }

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(StudioColors.Accent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = StudioColors.Text,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
