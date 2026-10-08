package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.local.entity.VideoEntity
import com.example.data.model.VoicePreset
import com.example.data.repository.VirtualCamRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VirtualCameraService : Service() {

    private val binder = LocalBinder()
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var repository: VirtualCamRepository
    lateinit var audioEngine: AudioEngine
        private set
    lateinit var lipSyncEngine: LipSyncEngine
        private set
    lateinit var floatingOverlayManager: FloatingOverlayManager
        private set

    private var wakeLock: PowerManager.WakeLock? = null
    private var pendingVideoIndex: Int? = null
    @Volatile private var microphoneFeaturesRequested = false
    private var lastVideoCompletionAtMs = 0L

    // Video playlist & playback state
    private val _playlist = MutableStateFlow<List<VideoEntity>>(emptyList())
    val playlist: StateFlow<List<VideoEntity>> = _playlist.asStateFlow()

    private val _currentVideoIndex = MutableStateFlow(0)
    val currentVideoIndex: StateFlow<Int> = _currentVideoIndex.asStateFlow()

    private val _isPlaying = MutableStateFlow(true)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private var loopMode: String = "LOOP_ALL"

    private val _isMirrored = MutableStateFlow(false)
    val isMirrored: StateFlow<Boolean> = _isMirrored.asStateFlow()

    private val _isVirtualCamActive = MutableStateFlow(true)
    val isVirtualCamActive: StateFlow<Boolean> = _isVirtualCamActive.asStateFlow()

    private val _currentVideoUri = MutableStateFlow<String?>(null)
    val currentVideoUri: StateFlow<String?> = _currentVideoUri.asStateFlow()

    inner class LocalBinder : Binder() {
        fun getService(): VirtualCameraService = this@VirtualCameraService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        repository = VirtualCamRepository(applicationContext)
        audioEngine = AudioEngine(serviceScope)
        lipSyncEngine = LipSyncEngine(serviceScope)
        floatingOverlayManager = FloatingOverlayManager(applicationContext)

        // Wire audio engine callbacks to lip sync engine
        audioEngine.onAudioFrameProcessed = { energy, db, phoneme ->
            lipSyncEngine.onAudioFrame(energy, db, phoneme)
            floatingOverlayManager.phoneme.value = phoneme
            floatingOverlayManager.decibels.value = db
        }

        // Wire floating overlay controls
        setupFloatingOverlayCallbacks()

        // Acquire WakeLock safely for Android 15 background resilience
        acquireWakeLock()

        // Load repository settings and playlist
        serviceScope.launch {
            repository.allVideos.collect { videos ->
                _playlist.value = videos
                floatingOverlayManager.videoCount.value = videos.size
                val requestedIndex = pendingVideoIndex
                if (requestedIndex != null && videos.isNotEmpty()) {
                    _currentVideoIndex.value = requestedIndex.coerceIn(videos.indices)
                    pendingVideoIndex = null
                } else if (videos.isEmpty()) {
                    _currentVideoIndex.value = 0
                } else if (_currentVideoIndex.value !in videos.indices) {
                    _currentVideoIndex.value = videos.lastIndex
                }
                updateFloatingOverlayVideoTitle()
            }
        }

        serviceScope.launch {
            repository.settingsFlow.collect { settings ->
                _isMirrored.value = settings.isMirrorHorizontal
                _isVirtualCamActive.value = settings.isVirtualCamActive
                loopMode = settings.loopMode
                floatingOverlayManager.loopMode.value = settings.loopMode
                floatingOverlayManager.playbackSpeed.value = settings.playbackSpeed
                lipSyncEngine.isEnabled = settings.lipSyncEnabled
                lipSyncEngine.sensitivity = settings.lipSyncSensitivity
                audioEngine.isNoiseCancellationEnabled = settings.noiseCancellationEnabled
                audioEngine.currentVoicePreset = VoicePreset.fromString(settings.voicePreset)
                audioEngine.setMonitorEnabled(settings.micMonitoringEnabled)

                floatingOverlayManager.isMirrored.value = settings.isMirrorHorizontal
                floatingOverlayManager.isVirtualCamActive.value = settings.isVirtualCamActive
                floatingOverlayManager.isLipSyncActive.value = settings.lipSyncEnabled
                floatingOverlayManager.isNoiseCancellationActive.value = settings.noiseCancellationEnabled
                val audioFeatureEnabled = settings.micMonitoringEnabled || settings.lipSyncEnabled
                val microphonePermissionGranted =
                    checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                if (microphoneFeaturesRequested && audioFeatureEnabled && microphonePermissionGranted &&
                    !audioEngine.isAudioActive.value
                ) {
                    audioEngine.startCapture()
                } else if ((!microphoneFeaturesRequested || !audioFeatureEnabled || !microphonePermissionGranted) &&
                    audioEngine.isAudioActive.value
                ) {
                    audioEngine.stopCapture()
                }
            }
        }
    }

    private fun setupFloatingOverlayCallbacks() {
        floatingOverlayManager.onVideoCompleted = {
            onVideoCompleted()
        }
        floatingOverlayManager.onToggleVirtualCam = {
            toggleVirtualCam()
        }
        floatingOverlayManager.onTogglePlayPause = {
            togglePlayPause()
        }
        floatingOverlayManager.onNextVideo = {
            nextVideo()
        }
        floatingOverlayManager.onToggleMirror = {
            toggleMirror()
        }
        floatingOverlayManager.onToggleLipSync = {
            val nextState = !lipSyncEngine.isEnabled
            lipSyncEngine.isEnabled = nextState
            floatingOverlayManager.isLipSyncActive.value = nextState
            serviceScope.launch {
                repository.updateSettings { it.copy(lipSyncEnabled = nextState) }
            }
        }
        floatingOverlayManager.onToggleNoiseCancellation = {
            val nextState = !audioEngine.isNoiseCancellationEnabled
            audioEngine.isNoiseCancellationEnabled = nextState
            floatingOverlayManager.isNoiseCancellationActive.value = nextState
            serviceScope.launch {
                repository.updateSettings { it.copy(noiseCancellationEnabled = nextState) }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_START -> {
                microphoneFeaturesRequested =
                    intent?.getBooleanExtra(EXTRA_ENABLE_MICROPHONE_FEATURES, false) == true
                startForegroundServiceSafely(microphoneFeaturesRequested)
                if (floatingOverlayManager.canDrawOverlays()) {
                    floatingOverlayManager.show()
                }
            }
            ACTION_STOP -> {
                stopForegroundSafely()
                stopSelf()
            }
            ACTION_TOGGLE_CAM -> toggleVirtualCam()
            ACTION_TOGGLE_PLAY_PAUSE -> togglePlayPause()
            ACTION_NEXT_VIDEO -> nextVideo()
            ACTION_TOGGLE_MIRROR -> toggleMirror()
            ACTION_SHOW_OVERLAY -> {
                startForegroundServiceSafely(enableMicrophoneFeatures = false)
                if (floatingOverlayManager.canDrawOverlays()) {
                    floatingOverlayManager.show()
                }
            }
            ACTION_HIDE_OVERLAY -> floatingOverlayManager.hide()
        }

        return START_STICKY
    }

    @SuppressLint("WakelockTimeout")
    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "VirtualCamera::ProcessingWakeLock"
            ).apply {
                acquire(4 * 60 * 60 * 1000L) // 4 hours safety window
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            wakeLock = null
        }
    }

    private fun startForegroundServiceSafely(enableMicrophoneFeatures: Boolean) {
        createNotificationChannel()
        val notification = buildForegroundNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val playbackServiceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            val serviceTypes = if (
                enableMicrophoneFeatures &&
                checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            ) {
                playbackServiceType or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            } else {
                playbackServiceType
            }
            startForeground(NOTIFICATION_ID, notification, serviceTypes)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun stopForegroundSafely() {
        audioEngine.stopCapture()
        lipSyncEngine.stop()
        floatingOverlayManager.hide()
        releaseWakeLock()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Live Media Assistant",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground service for local media playback and audio analysis"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val appIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIntent = Intent(this, VirtualCameraService::class.java).apply {
            action = ACTION_TOGGLE_PLAY_PAUSE
        }
        val playPausePending = PendingIntent.getService(
            this, 1, playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = Intent(this, VirtualCameraService::class.java).apply {
            action = ACTION_NEXT_VIDEO
        }
        val nextPending = PendingIntent.getService(
            this, 2, nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, VirtualCameraService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this, 3, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playActionTitle = if (_isPlaying.value) "Pause" else "Play"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Live Media Assistant active")
            .setContentText("Local media playback and audio analysis are running")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_play, playActionTitle, playPausePending)
            .addAction(android.R.drawable.ic_media_next, "Next", nextPending)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPending)
            .setOngoing(true)
            .build()
    }

    fun toggleVirtualCam() {
        val nextState = !_isVirtualCamActive.value
        _isVirtualCamActive.value = nextState
        floatingOverlayManager.isVirtualCamActive.value = nextState
        serviceScope.launch {
            repository.updateSettings { it.copy(isVirtualCamActive = nextState) }
        }
    }

    fun togglePlayPause() {
        setPlaying(!_isPlaying.value)
    }

    fun setPlaying(playing: Boolean) {
        _isPlaying.value = playing
        floatingOverlayManager.isPlaying.value = playing
    }

    fun onVideoCompleted() {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastVideoCompletionAtMs < VIDEO_COMPLETION_DEBOUNCE_MS) return
        lastVideoCompletionAtMs = now
        when (loopMode) {
            "LOOP_ALL" -> nextVideo()
            "ONCE" -> setPlaying(false)
        }
    }

    fun nextVideo() {
        val currentList = _playlist.value
        if (currentList.isNotEmpty()) {
            val nextIndex = (_currentVideoIndex.value + 1) % currentList.size
            _currentVideoIndex.value = nextIndex
            updateFloatingOverlayVideoTitle()
        }
    }

    fun previousVideo() {
        val currentList = _playlist.value
        if (currentList.isNotEmpty()) {
            val prevIndex = if (_currentVideoIndex.value - 1 < 0) currentList.size - 1 else _currentVideoIndex.value - 1
            _currentVideoIndex.value = prevIndex
            updateFloatingOverlayVideoTitle()
        }
    }

    fun selectVideoIndex(index: Int) {
        if (_playlist.value.isEmpty()) {
            pendingVideoIndex = index
            return
        }
        if (index in _playlist.value.indices) {
            _currentVideoIndex.value = index
            updateFloatingOverlayVideoTitle()
        }
    }

    fun toggleMirror() {
        val nextState = !_isMirrored.value
        _isMirrored.value = nextState
        floatingOverlayManager.isMirrored.value = nextState
        serviceScope.launch {
            repository.updateSettings { it.copy(isMirrorHorizontal = nextState) }
        }
    }

    private fun updateFloatingOverlayVideoTitle() {
        val list = _playlist.value
        val index = _currentVideoIndex.value
        if (index in list.indices) {
            floatingOverlayManager.currentVideoTitle.value = list[index].title
            _currentVideoUri.value = list[index].uriString.takeIf { !list[index].isSample && it.isNotBlank() }
            floatingOverlayManager.currentVideoUri.value = _currentVideoUri.value
        } else {
            floatingOverlayManager.currentVideoTitle.value = "No video selected"
            _currentVideoUri.value = null
            floatingOverlayManager.currentVideoUri.value = null
        }
    }

    override fun onDestroy() {
        stopForegroundSafely()
        serviceJob.cancel()
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 8841
        const val CHANNEL_ID = "virtual_camera_live_channel"
        private const val VIDEO_COMPLETION_DEBOUNCE_MS = 750L

        const val ACTION_START = "com.example.action.START_VIRTUAL_CAM"
        const val ACTION_STOP = "com.example.action.STOP_VIRTUAL_CAM"
        const val ACTION_TOGGLE_CAM = "com.example.action.TOGGLE_CAM"
        const val ACTION_TOGGLE_PLAY_PAUSE = "com.example.action.TOGGLE_PLAY_PAUSE"
        const val ACTION_NEXT_VIDEO = "com.example.action.NEXT_VIDEO"
        const val ACTION_TOGGLE_MIRROR = "com.example.action.TOGGLE_MIRROR"
        const val ACTION_SHOW_OVERLAY = "com.example.action.SHOW_OVERLAY"
        const val ACTION_HIDE_OVERLAY = "com.example.action.HIDE_OVERLAY"
        const val EXTRA_ENABLE_MICROPHONE_FEATURES = "com.example.extra.ENABLE_MICROPHONE_FEATURES"

        @Volatile
        var isServiceRunning = false
    }
}
