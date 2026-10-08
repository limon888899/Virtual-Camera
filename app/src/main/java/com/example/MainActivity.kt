package com.example

import com.example.ui.theme.StudioColors

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.screens.Android15SecurityScreen
import com.example.ui.screens.LipSyncStudioScreen
import com.example.ui.screens.PlaylistScreen
import com.example.ui.screens.VirtualCamStudioScreen
import com.example.ui.screens.VoiceAudioScreen
import com.example.ui.screens.VirtualSpaceScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.VirtualCamViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: VirtualCamViewModel by viewModels()

    override fun onResume() {
        super.onResume()
        viewModel.refreshInstalledApps()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme(darkTheme = true) {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: VirtualCamViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentTab by remember { mutableIntStateOf(0) }

    val videos by viewModel.videos.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isServiceActive by viewModel.isServiceBound.collectAsState()
    val currentIndex by viewModel.currentVideoIndex.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val lipSyncState by viewModel.lipSyncState.collectAsState()
    val audioEnergy by viewModel.audioEnergy.collectAsState()
    val audioDecibels by viewModel.audioDecibels.collectAsState()
    val isAudioActive by viewModel.isAudioActive.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val clonedApps by viewModel.clonedApps.collectAsState()
    val cloneStatus by viewModel.cloneStatus.collectAsState()
    
    val currentVideo = videos.getOrNull(currentIndex)
    var pendingMicAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val serviceMicPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingMicAction?.invoke()
        } else {
            pendingMicAction = null
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Microphone access was denied; local audio monitoring remains off.")
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("virtual_cam_main_scaffold"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = StudioColors.Ink,
                contentColor = StudioColors.Text
            ) {
                // Studio Tab
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.Videocam, contentDescription = "Studio") },
                    label = { Text("Studio", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = StudioColors.Accent,
                        indicatorColor = StudioColors.Accent,
                        unselectedIconColor = StudioColors.Text.copy(alpha = 0.6f),
                        unselectedTextColor = StudioColors.Text.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("tab_studio")
                )

                // Playlist Tab
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.VideoLibrary, contentDescription = "Playlist") },
                    label = { Text("Playlist", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = StudioColors.Accent,
                        indicatorColor = StudioColors.Accent,
                        unselectedIconColor = StudioColors.Text.copy(alpha = 0.6f),
                        unselectedTextColor = StudioColors.Text.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("tab_playlist")
                )

                // Lip-Sync Tab
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.Face, contentDescription = "Lip-Sync") },
                    label = { Text("Lip-Sync", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = StudioColors.Accent,
                        indicatorColor = StudioColors.Accent,
                        unselectedIconColor = StudioColors.Text.copy(alpha = 0.6f),
                        unselectedTextColor = StudioColors.Text.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("tab_lip_sync")
                )

                // Voice FX Tab
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    icon = { Icon(Icons.Default.Equalizer, contentDescription = "Voice FX") },
                    label = { Text("Voice FX", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = StudioColors.AccentSoft,
                        indicatorColor = StudioColors.AccentSoft,
                        unselectedIconColor = StudioColors.Text.copy(alpha = 0.6f),
                        unselectedTextColor = StudioColors.Text.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("tab_voice_fx")
                )

                // Android 15 & Security Tab
                NavigationBarItem(
                    selected = currentTab == 4,
                    onClick = { currentTab = 4 },
                    icon = { Icon(Icons.Default.Security, contentDescription = "OS 15") },
                    label = { Text("OS 15", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = StudioColors.Blue,
                        indicatorColor = StudioColors.Blue,
                        unselectedIconColor = StudioColors.Text.copy(alpha = 0.6f),
                        unselectedTextColor = StudioColors.Text.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("tab_android15")
                )

                NavigationBarItem(
                    selected = currentTab == 5,
                    onClick = { currentTab = 5 },
                    icon = { Icon(Icons.Default.SpaceDashboard, contentDescription = "Virtual Space") },
                    label = { Text("App Space", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = StudioColors.Warning,
                        indicatorColor = StudioColors.Warning,
                        unselectedIconColor = StudioColors.Text.copy(alpha = 0.6f),
                        unselectedTextColor = StudioColors.Text.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("tab_virtual_space")
                )

            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(StudioColors.Surface.copy(alpha = 0.72f), StudioColors.Background)
                        )
                    )
                    .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> VirtualCamStudioScreen(
                    currentVideo = currentVideo,
                    videoCount = videos.size,
                    settings = settings,
                    isPlaying = isPlaying,
                    isServiceActive = isServiceActive,
                    lipSyncState = lipSyncState,
                    audioEnergy = audioEnergy,
                    audioDecibels = audioDecibels,
                    onStartService = { viewModel.startService() },
                    onStopService = {
                        viewModel.stopService()
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Virtual Camera Service stopped.")
                        }
                    },
                    onToggleCam = { viewModel.toggleVirtualCam() },
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onToggleMirror = { viewModel.toggleMirror() },
                    onNextVideo = { viewModel.nextVideo() },
                    onPrevVideo = { viewModel.prevVideo() },
                    onVideoCompleted = { viewModel.onVideoCompleted() },
                    onToggleLipSync = { enabled ->
                        if (!enabled) {
                            viewModel.updateLipSync(false)
                        } else if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            viewModel.updateLipSync(true)
                            viewModel.startService(enableMicrophoneFeatures = true)
                        } else {
                            pendingMicAction = {
                                viewModel.updateLipSync(true)
                                viewModel.startService(enableMicrophoneFeatures = true)
                            }
                            serviceMicPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onToggleNoiseCancellation = { viewModel.updateNoiseCancellation(it) }
                )

                1 -> PlaylistScreen(
                    videos = videos,
                    currentIndex = currentIndex,
                    settings = settings,
                    onSelectVideo = { viewModel.selectVideo(it) },
                    onDeleteVideo = { viewModel.deleteVideo(it) },
                    onImportVideos = { viewModel.importVideos(it) },
                    onUpdateLoopMode = { viewModel.updateLoopMode(it) },
                    onUpdateSpeed = { viewModel.updateSpeed(it) }
                )

                2 -> LipSyncStudioScreen(
                    lipSyncState = lipSyncState,
                    sensitivity = settings.lipSyncSensitivity,
                    isEnabled = settings.lipSyncEnabled,
                    onToggleEnabled = { enabled ->
                        if (!enabled) {
                            viewModel.updateLipSync(false)
                        } else if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            viewModel.updateLipSync(true)
                            viewModel.startService(enableMicrophoneFeatures = true)
                        } else {
                            pendingMicAction = {
                                viewModel.updateLipSync(true)
                                viewModel.startService(enableMicrophoneFeatures = true)
                            }
                            serviceMicPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onUpdateSensitivity = { viewModel.updateLipSyncSensitivity(it) }
                )

                3 -> VoiceAudioScreen(
                    energy = audioEnergy,
                    decibels = audioDecibels,
                    isAudioActive = isAudioActive,
                    isMonitorEnabled = settings.micMonitoringEnabled,
                    isNoiseCancellationEnabled = settings.noiseCancellationEnabled,
                    currentVoicePreset = com.example.data.model.VoicePreset.fromString(settings.voicePreset),
                    onToggleNoiseCancellation = { viewModel.updateNoiseCancellation(it) },
                    onToggleMonitor = { enabled ->
                        if (!enabled) {
                            viewModel.updateMicMonitoring(false)
                        } else if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                            PackageManager.PERMISSION_GRANTED
                        ) {
                            viewModel.updateMicMonitoring(true)
                            viewModel.startService(enableMicrophoneFeatures = true)
                        } else {
                            pendingMicAction = {
                                viewModel.updateMicMonitoring(true)
                                viewModel.startService(enableMicrophoneFeatures = true)
                            }
                            serviceMicPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onSelectVoicePreset = { viewModel.selectVoicePreset(it) }
                )

                4 -> Android15SecurityScreen()

                5 -> VirtualSpaceScreen(
                    installedApps = installedApps,
                    clonedApps = clonedApps,
                    cloneStatus = cloneStatus,
                    onCloneApp = { app -> viewModel.cloneApp(app) },
                    onRemoveClonedApp = { viewModel.removeClonedApp(it) },
                    onLaunchClonedApp = { viewModel.launchClonedApp(it) }
                )

            }
        }
    }
}
