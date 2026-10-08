package com.example.ui.screens

import com.example.ui.theme.StudioColors

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.VideoEntity

@Composable
fun PlaylistScreen(
    videos: List<VideoEntity>,
    currentIndex: Int,
    settings: AppSettingsEntity,
    onSelectVideo: (Int) -> Unit,
    onDeleteVideo: (Long) -> Unit,
    onImportVideos: (List<Uri>) -> Unit,
    onUpdateLoopMode: (String) -> Unit,
    onUpdateSpeed: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Media Picker for Videos
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onImportVideos(uris)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header & Import Button Card
        item {
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
                                    imageVector = Icons.Default.VideoLibrary,
                                    contentDescription = "Playlist",
                                    tint = StudioColors.Text,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Local Media Playlist",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StudioColors.Text
                                )
                                Text(
                                    text = "${videos.size} Videos Loaded",
                                    fontSize = 11.sp,
                                    color = StudioColors.Text.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                videoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StudioColors.Accent),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Import",
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import MP4", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Loop Mode Selector
                    Text(
                        text = "Playback Mode",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StudioColors.Text.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = settings.loopMode == "LOOP_ALL",
                            onClick = { onUpdateLoopMode("LOOP_ALL") },
                            label = { Text("Loop All", fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Repeat, contentDescription = null, modifier = Modifier.size(14.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StudioColors.Accent,
                                selectedLabelColor = StudioColors.Text
                            )
                        )

                        FilterChip(
                            selected = settings.loopMode == "LOOP_ONE",
                            onClick = { onUpdateLoopMode("LOOP_ONE") },
                            label = { Text("Loop Single", fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.RepeatOne, contentDescription = null, modifier = Modifier.size(14.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StudioColors.Accent,
                                selectedLabelColor = StudioColors.Text
                            )
                        )

                        FilterChip(
                            selected = settings.loopMode == "ONCE",
                            onClick = { onUpdateLoopMode("ONCE") },
                            label = { Text("Play Once", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StudioColors.Accent,
                                selectedLabelColor = StudioColors.Text
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Playback Speed Selector
                    Text(
                        text = "Playback Speed (${settings.playbackSpeed}x)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StudioColors.Text.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val speeds = listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f)
                        speeds.forEach { speed ->
                            FilterChip(
                                selected = settings.playbackSpeed == speed,
                                onClick = { onUpdateSpeed(speed) },
                                label = { Text("${speed}x", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StudioColors.Accent,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                }
            }
        }

        if (videos.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = StudioColors.Surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = null,
                            tint = StudioColors.Accent,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your playlist is empty",
                            fontWeight = FontWeight.Bold,
                            color = StudioColors.Text
                        )
                        Text(
                            text = "Import a video to preview and control it locally.",
                            color = StudioColors.Text.copy(alpha = 0.72f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Video Items List
        itemsIndexed(videos, key = { _, v -> v.id }) { index, video ->
            val isCurrent = index == currentIndex

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrent) StudioColors.SurfaceRaised else StudioColors.Surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectVideo(index) }
                    .then(
                        if (isCurrent) Modifier.border(1.5.dp, StudioColors.Accent, RoundedCornerShape(12.dp))
                        else Modifier
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Video Index or Play Icon
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isCurrent) StudioColors.Accent else StudioColors.SurfaceHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCurrent) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Active",
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                text = "${index + 1}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = StudioColors.Text
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = video.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isCurrent) StudioColors.Accent else StudioColors.Text,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = StudioColors.SurfaceHigh
                            ) {
                                Text(
                                    text = video.resolution,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = StudioColors.Blue,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            if (video.isSample) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = StudioColors.SurfaceRaised
                                ) {
                                    Text(
                                        text = "BUILT-IN DEMO",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StudioColors.Accent,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (!video.isSample) {
                        IconButton(
                            onClick = { onDeleteVideo(video.id) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete Video",
                                tint = StudioColors.Danger,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
