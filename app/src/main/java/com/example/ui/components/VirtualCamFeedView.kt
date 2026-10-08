package com.example.ui.components

import com.example.ui.theme.StudioColors

import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.local.entity.VideoEntity
import com.example.data.model.LipSyncState

@Composable
fun VirtualCamFeedView(
    video: VideoEntity?,
    videoCount: Int,
    isPlaying: Boolean,
    isMirrored: Boolean,
    isVirtualCamActive: Boolean,
    lipSyncState: LipSyncState,
    loopMode: String,
    playbackSpeed: Float,
    onTogglePlayPause: () -> Unit,
    onToggleMirror: () -> Unit,
    onVideoCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mediaPlayerInstance by remember { mutableStateOf<MediaPlayer?>(null) }
    val latestLoopMode = rememberUpdatedState(loopMode)
    val latestPlaybackSpeed = rememberUpdatedState(playbackSpeed)
    val latestIsPlaying = rememberUpdatedState(isPlaying)
    val latestOnVideoCompleted = rememberUpdatedState(onVideoCompleted)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(StudioColors.Background)
            .border(1.5.dp, StudioColors.Accent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .clickable { onTogglePlayPause() },
        contentAlignment = Alignment.Center
    ) {
        if (video != null && !video.isSample && video.uriString.isNotBlank()) {
            // Real imported video player
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        tag = video.uriString
                        setVideoURI(Uri.parse(video.uriString))
                        setOnPreparedListener { mp ->
                            mp.isLooping = latestLoopMode.value == "LOOP_ONE" ||
                                (latestLoopMode.value == "LOOP_ALL" && videoCount == 1)
                            mediaPlayerInstance = mp
                            mp.playbackParams = PlaybackParams().setSpeed(latestPlaybackSpeed.value)
                            if (isVirtualCamActive && latestIsPlaying.value) start() else pause()
                        }
                        setOnCompletionListener { latestOnVideoCompleted.value() }
                    }
                },
                update = { view ->
                    view.setOnPreparedListener { mp ->
                        mp.isLooping = latestLoopMode.value == "LOOP_ONE" ||
                            (latestLoopMode.value == "LOOP_ALL" && videoCount == 1)
                        mediaPlayerInstance = mp
                        mp.playbackParams = PlaybackParams().setSpeed(latestPlaybackSpeed.value)
                        if (isVirtualCamActive && latestIsPlaying.value) view.start() else view.pause()
                    }
                    view.setOnCompletionListener { latestOnVideoCompleted.value() }
                    if (view.tag != video.uriString) {
                        view.stopPlayback()
                        mediaPlayerInstance = null
                        view.tag = video.uriString
                        view.setVideoURI(Uri.parse(video.uriString))
                    }
                    if (isVirtualCamActive && isPlaying) {
                        if (!view.isPlaying) view.start()
                    } else {
                        if (view.isPlaying) view.pause()
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(scaleX = if (isMirrored) -1f else 1f)
            )

            DisposableEffect(video.uriString) {
                onDispose {
                    mediaPlayerInstance?.release()
                    mediaPlayerInstance = null
                }
            }

            LaunchedEffect(mediaPlayerInstance, loopMode, playbackSpeed, videoCount) {
                mediaPlayerInstance?.let { player ->
                    player.isLooping = loopMode == "LOOP_ONE" ||
                        (loopMode == "LOOP_ALL" && videoCount == 1)
                    player.playbackParams = player.playbackParams.setSpeed(playbackSpeed)
                }
            }
        } else {
            // Local preview fallback with lip-sync avatar
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(scaleX = if (isMirrored) -1f else 1f)
            ) {
                LipSyncAvatarView(
                    lipSyncState = lipSyncState,
                    modifier = Modifier.fillMaxSize(),
                    showTrackingMesh = true
                )
            }
        }

        // Camera Frame Overlay & Stream Status HUD
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Top HUD row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isVirtualCamActive) Color(0xDDFF3838) else Color(0xAA333333)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FiberManualRecord,
                            contentDescription = "Live Dot",
                            tint = StudioColors.Text,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isVirtualCamActive) "LOCAL PLAYBACK: LIVE" else "STANDBY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = StudioColors.Text
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xAA111722)
                ) {
                    Text(
                        text = "LOCAL PREVIEW",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = StudioColors.Accent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Mirror status icon button
                IconButton(
                    onClick = onToggleMirror,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isMirrored) StudioColors.Accent else Color(0x66000000)
                    ),
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Flip,
                        contentDescription = "Toggle Mirror",
                        tint = StudioColors.Text,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Center Play/Pause indication if paused
            if (!isPlaying) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0x99000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = StudioColors.Text,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom HUD row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xCC0D1117))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video?.title ?: "Virtual Studio Stream",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StudioColors.Text,
                        maxLines = 1
                    )
                    Text(
                        text = "Not a camera replacement",
                        fontSize = 10.sp,
                        color = StudioColors.Text.copy(alpha = 0.6f)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = StudioColors.SurfaceHigh
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = "Looping",
                            tint = StudioColors.Blue,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "LOOP ON",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = StudioColors.Blue
                        )
                    }
                }
            }
        }
    }
}
