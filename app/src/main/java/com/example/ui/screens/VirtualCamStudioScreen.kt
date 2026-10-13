package com.example.ui.screens

import com.example.ui.theme.StudioColors

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.VideoEntity
import com.example.data.model.LipSyncState
import com.example.service.VirtualCameraService
import com.example.ui.components.AudioWaveformVisualizer
import com.example.ui.components.VirtualCamFeedView

@Composable
fun VirtualCamStudioScreen(
    currentVideo: VideoEntity?,
    videoCount: Int,
    settings: AppSettingsEntity,
    isPlaying: Boolean,
    isServiceActive: Boolean,
    lipSyncState: LipSyncState,
    audioEnergy: Float,
    audioDecibels: Float,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onToggleCam: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onToggleMirror: () -> Unit,
    onNextVideo: () -> Unit,
    onPrevVideo: () -> Unit,
    onVideoCompleted: () -> Unit,
    onToggleLipSync: (Boolean) -> Unit,
    onToggleNoiseCancellation: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hasOverlayPermission = Settings.canDrawOverlays(context)
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Live Virtual Camera Feed Window
        VirtualCamFeedView(
            video = currentVideo,
            videoCount = videoCount,
            isPlaying = isPlaying,
            isMirrored = settings.isMirrorHorizontal,
            isVirtualCamActive = settings.isVirtualCamActive && isServiceActive,
            lipSyncState = lipSyncState,
            loopMode = settings.loopMode,
            playbackSpeed = settings.playbackSpeed,
            onTogglePlayPause = onTogglePlayPause,
            onToggleMirror = onToggleMirror,
            onVideoCompleted = onVideoCompleted,
            modifier = Modifier
                .fillMaxWidth()
                .height(290.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "This preview and floating player work inside this app. Standard Android apps cannot replace the camera feed used by other apps.",
            fontSize = 12.sp,
            color = StudioColors.Text.copy(alpha = 0.72f),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Playback Controller Card
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = StudioColors.Surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Live Media Engine",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = StudioColors.Text
                        )
                        Text(
                            text = if (isServiceActive) "Active in Background (Android 15 FGS)" else "Service Stopped",
                            fontSize = 11.sp,
                            color = if (isServiceActive) StudioColors.AccentSoft else StudioColors.Text.copy(alpha = 0.6f)
                        )
                    }

                    // Master Start/Stop Service Button
                    FilledTonalButton(
                        onClick = {
                            if (isServiceActive) onStopService() else onStartService()
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isServiceActive) StudioColors.Danger else StudioColors.Accent
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Power",
                            tint = StudioColors.Text,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isServiceActive) "STOP" else "START",
                            fontWeight = FontWeight.Bold,
                            color = StudioColors.Text
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Control action buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Prev
                    IconButton(
                        onClick = onPrevVideo,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = StudioColors.SurfaceHigh),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous Video",
                            tint = StudioColors.Text
                        )
                    }

                    // Play/Pause Main
                    IconButton(
                        onClick = onTogglePlayPause,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = StudioColors.Accent
                        ),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = StudioColors.Text,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Next
                    IconButton(
                        onClick = onNextVideo,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = StudioColors.SurfaceHigh),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Video",
                            tint = StudioColors.Text
                        )
                    }

                    // Mirror Toggle
                    IconButton(
                        onClick = onToggleMirror,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (settings.isMirrorHorizontal) StudioColors.Accent else StudioColors.SurfaceHigh
                        ),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flip,
                            contentDescription = "Mirror Flip",
                            tint = StudioColors.Text
                        )
                    }

                    // Feed Active Switch
                    IconButton(
                        onClick = onToggleCam,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (settings.isVirtualCamActive) StudioColors.Accent else StudioColors.SurfaceHigh
                        ),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = if (settings.isVirtualCamActive) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            contentDescription = "Local playback active",
                            tint = StudioColors.Text
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Floating Control Panel / Bubble Card (WhatsApp / Zoom Assistant)
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = StudioColors.Surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(StudioColors.AccentSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureInPicture,
                                contentDescription = "Floating Overlay",
                                tint = StudioColors.Text,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Floating Control Bubble",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = StudioColors.Text
                            )
                            Text(
                                text = "Floating controls for supported local playback",
                                fontSize = 11.sp,
                                color = StudioColors.Text.copy(alpha = 0.65f)
                            )
                        }
                    }

                    if (!hasOverlayPermission) {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Grant", fontSize = 12.sp)
                        }
                    } else {
                        Button(
                            onClick = {
                                if (Settings.canDrawOverlays(context)) {
                                    val intent = Intent(context, VirtualCameraService::class.java).apply {
                                        action = VirtualCameraService.ACTION_SHOW_OVERLAY
                                    }
                                    androidx.core.content.ContextCompat.startForegroundService(context, intent)
                                } else {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StudioColors.AccentSoft),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Show over other apps", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Live Audio & AI Lip-Sync Waveform Card
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = StudioColors.Surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Audio Waveform",
                            tint = StudioColors.Accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Live Audio & Phoneme Analysis",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = StudioColors.Text
                        )
                    }

                    Text(
                        text = "${audioDecibels.toInt()} dB • Phoneme [${lipSyncState.currentPhoneme}]",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StudioColors.Accent
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                AudioWaveformVisualizer(
                    energy = audioEnergy,
                    decibels = audioDecibels,
                    activeColor = StudioColors.Accent,
                    accentColor = StudioColors.Accent
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quick AI Lip-Sync & Noise Shield Toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = "Lip-Sync",
                            tint = StudioColors.Blue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Lip-Sync Animation",
                            fontSize = 12.sp,
                            color = StudioColors.Text
                        )
                    }
                    Switch(
                        checked = settings.lipSyncEnabled,
                        onCheckedChange = onToggleLipSync,
                        colors = SwitchDefaults.colors(checkedThumbColor = StudioColors.Accent)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Noise Shield",
                            tint = StudioColors.AccentSoft,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Microphone Noise Filter",
                            fontSize = 12.sp,
                            color = StudioColors.Text
                        )
                    }
                    Switch(
                        checked = settings.noiseCancellationEnabled,
                        onCheckedChange = onToggleNoiseCancellation,
                        colors = SwitchDefaults.colors(checkedThumbColor = StudioColors.AccentSoft)
                    )
                }

                // Deepfake Camera Feed Injection Toggle (Added here)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Deepfake Hook",
                            tint = StudioColors.Accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Deepfake Camera Injection",
                            fontSize = 12.sp,
                            color = StudioColors.Text
                        )
                    }
                    Switch(
                        checked = settings.isVirtualCamActive,
                        onCheckedChange = { 
                            onToggleCam() 
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = StudioColors.Accent)
                    )
                }
            }
        }
    }
}
