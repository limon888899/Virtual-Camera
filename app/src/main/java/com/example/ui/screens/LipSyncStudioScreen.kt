package com.example.ui.screens

import com.example.ui.theme.StudioColors

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LipSyncState
import com.example.ui.components.AudioWaveformVisualizer
import com.example.ui.components.LipSyncAvatarView

@Composable
fun LipSyncStudioScreen(
    lipSyncState: LipSyncState,
    sensitivity: Float,
    isEnabled: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onUpdateSensitivity: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var customPhotoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var aiStatusMessage by remember { mutableStateOf("Mouth animation follows the microphone level while audio analysis is active.") }

    // Image Picker for custom avatar photo
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                customPhotoBitmap = bitmap
                aiStatusMessage = "Photo loaded. Mouth animation follows microphone level; face tracking and video export are not available."
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // AI Realtime Avatar View
        LipSyncAvatarView(
            lipSyncState = lipSyncState,
            customPhotoBitmap = customPhotoBitmap,
            showTrackingMesh = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Photo Upload & Reset Card
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
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(StudioColors.Accent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Photo Lip Sync",
                                tint = StudioColors.Text,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Photo Avatar Animation",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = StudioColors.Text
                            )
                            Text(
                                text = if (customPhotoBitmap != null) "Custom Image Active" else "Default Avatar Mode",
                                fontSize = 11.sp,
                                color = StudioColors.Text.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Row {
                        if (customPhotoBitmap != null) {
                            OutlinedButton(
                                onClick = {
                                    customPhotoBitmap = null
                                    aiStatusMessage = "Reverted to default stylized 3D avatar."
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                Icon(Icons.Default.RestartAlt, contentDescription = "Reset", modifier = Modifier.size(16.dp))
                            }
                        }

                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StudioColors.Accent),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Upload Photo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StudioColors.Ink,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = StudioColors.Accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = aiStatusMessage,
                            fontSize = 11.sp,
                            color = StudioColors.Text.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Phoneme Real-time Grid Monitor
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
                    Text(
                        text = "Vocal Phoneme Tracking Grid",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = StudioColors.Text
                    )
                    Text(
                        text = if (lipSyncState.isSpeaking) "SPEAKING" else "REST",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (lipSyncState.isSpeaking) StudioColors.AccentSoft else StudioColors.Text.copy(alpha = 0.5f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val phonemes = listOf("A", "E", "I", "O", "U", "REST")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    phonemes.forEach { p ->
                        val isActive = lipSyncState.currentPhoneme.equals(p, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 3.dp)
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isActive) StudioColors.Accent else StudioColors.SurfaceHigh
                                )
                                .border(
                                    1.dp,
                                    if (isActive) StudioColors.Text else Color.Transparent,
                                    RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = p,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isActive) Color.Black else StudioColors.Text
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Engine Sensitivity & Settings Card
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
                    Text(
                        text = "Avatar animation settings",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = StudioColors.Text
                    )
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = onToggleEnabled,
                        colors = SwitchDefaults.colors(checkedThumbColor = StudioColors.Accent)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Mouth Movement Sensitivity (${String.format("%.1f", sensitivity)}x)",
                    fontSize = 12.sp,
                    color = StudioColors.Text.copy(alpha = 0.8f)
                )
                Slider(
                    value = sensitivity,
                    onValueChange = onUpdateSensitivity,
                    valueRange = 0.5f..2.5f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = StudioColors.Accent,
                        activeTrackColor = StudioColors.Accent
                    )
                )
            }
        }
    }
}
