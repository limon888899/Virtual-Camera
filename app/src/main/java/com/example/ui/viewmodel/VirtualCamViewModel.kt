package com.example.ui.viewmodel

import android.app.Application
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.Process
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.VideoEntity
import com.example.data.model.LipSyncState
import com.example.data.model.VoicePreset
import com.example.data.repository.VirtualCamRepository
import com.example.service.VirtualCameraService
import com.example.profile.WorkProfileAdminReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.provider.OpenableColumns
import android.util.Log
import com.example.data.model.AppInfo
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.io.FileOutputStream
import java.util.UUID


class VirtualCamViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VirtualCamRepository(application)

    val videos: StateFlow<List<VideoEntity>> = repository.allVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<AppSettingsEntity> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettingsEntity())

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val _clonedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val clonedApps: StateFlow<List<AppInfo>> = _clonedApps.asStateFlow()

    private val _cloneStatus = MutableStateFlow("No managed profile selected")
    val cloneStatus: StateFlow<String> = _cloneStatus.asStateFlow()

    private val _isServiceBound = MutableStateFlow(false)
    val isServiceBound: StateFlow<Boolean> = _isServiceBound.asStateFlow()

    private val _currentVideoIndex = MutableStateFlow(0)
    val currentVideoIndex: StateFlow<Int> = _currentVideoIndex.asStateFlow()

    private val _isPlaying = MutableStateFlow(true)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _lipSyncState = MutableStateFlow(LipSyncState())
    val lipSyncState: StateFlow<LipSyncState> = _lipSyncState.asStateFlow()

    private val _audioEnergy = MutableStateFlow(0f)
    val audioEnergy: StateFlow<Float> = _audioEnergy.asStateFlow()

    private val _audioDecibels = MutableStateFlow(-60f)
    val audioDecibels: StateFlow<Float> = _audioDecibels.asStateFlow()

    private val _isAudioActive = MutableStateFlow(false)
    val isAudioActive: StateFlow<Boolean> = _isAudioActive.asStateFlow()

    private var virtualCameraService: VirtualCameraService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as? VirtualCameraService.LocalBinder
            val service = localBinder?.getService()
            virtualCameraService = service
            _isServiceBound.value = true

            // Observe service flows
            service?.let { s ->
                s.selectVideoIndex(_currentVideoIndex.value)
                viewModelScope.launch {
                    s.isPlaying.collect { _isPlaying.value = it }
                }
                viewModelScope.launch {
                    s.currentVideoIndex.collect { _currentVideoIndex.value = it }
                }
                viewModelScope.launch {
                    s.lipSyncEngine.lipSyncState.collect { _lipSyncState.value = it }
                }
                viewModelScope.launch {
                    s.audioEngine.energy.collect { _audioEnergy.value = it }
                }
                viewModelScope.launch {
                    s.audioEngine.decibels.collect { _audioDecibels.value = it }
                }
                viewModelScope.launch {
                    s.audioEngine.isAudioActive.collect { _isAudioActive.value = it }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            virtualCameraService = null
            _isServiceBound.value = false
            _isAudioActive.value = false
        }
    }

    init {
        viewModelScope.launch {
            repository.initDefaultDataIfNeeded()
        }
        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val packageManager = getApplication<Application>().packageManager
            val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val results = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(0L))
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.queryIntentActivities(launcherIntent, 0)
                }
            } catch (_: Exception) {
                emptyList()
            }
            val application = getApplication<Application>()
            val launcherApps = application.getSystemService(LauncherApps::class.java)
            val otherProfiles = launcherApps?.profiles.orEmpty()
                .filter { it != Process.myUserHandle() }
            val profilePackages = results.mapNotNull { it.activityInfo?.packageName }
                .filter { packageName ->
                    otherProfiles.any { profile ->
                        try {
                            launcherApps?.getActivityList(packageName, profile)?.isNotEmpty() == true
                        } catch (exception: SecurityException) {
                            Log.w(TAG, "Cannot inspect managed profile app $packageName", exception)
                            false
                        }
                    }
                }.toSet()
            val apps = results.mapNotNull { info ->
                runCatching {
                    AppInfo(
                        packageName = info.activityInfo.packageName,
                        appName = info.loadLabel(packageManager).toString(),
                        icon = info.loadIcon(packageManager),
                        isCloned = info.activityInfo.packageName in profilePackages
                    )
                }.getOrNull()
            }.distinctBy { it.packageName }.sortedBy { it.appName }
            _installedApps.value = apps
            _clonedApps.value = apps.filter { it.isCloned }
        }
    }

    fun refreshInstalledApps() {
        loadInstalledApps()
    }

    fun cloneApp(app: AppInfo) {
        val application = getApplication<Application>()
        val launcherApps = application.getSystemService(LauncherApps::class.java)
        val profileCloneExists = launcherApps?.profiles.orEmpty().any { profile ->
            profile != Process.myUserHandle() &&
                launcherApps.getActivityList(app.packageName, profile).isNotEmpty()
        }
        if (profileCloneExists) {
            _cloneStatus.value = "Real work-profile clone is installed"
            loadInstalledApps()
            Toast.makeText(application, _cloneStatus.value, Toast.LENGTH_SHORT).show()
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            _cloneStatus.value = "Managed profiles require Android 7.0 or newer"
            Toast.makeText(application, _cloneStatus.value, Toast.LENGTH_LONG).show()
            return
        }

        val admin = ComponentName(application, WorkProfileAdminReceiver::class.java)
        val adminExtras = Bundle().apply {
            putString(WorkProfileAdminReceiver.PENDING_PACKAGE, app.packageName)
        }
        val provisioningIntent = Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE).apply {
            putExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME, admin)
            putExtra(DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE, adminExtras)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { application.startActivity(provisioningIntent) }
            .onSuccess { _cloneStatus.value = "Complete Android setup to install the clone in the work profile" }
            .onFailure {
                _cloneStatus.value = "This phone declined managed-profile setup"
                Toast.makeText(application, _cloneStatus.value, Toast.LENGTH_LONG).show()
            }
    }

    fun removeClonedApp(app: AppInfo) {
        _cloneStatus.value = "Remove the work-profile copy in Android's work-profile settings. App list refreshed."
        loadInstalledApps()
    }

    fun launchClonedApp(app: AppInfo) {
        val application = getApplication<Application>()
        val launcherApps = application.getSystemService(LauncherApps::class.java)
        val profile = launcherApps?.profiles.orEmpty().firstOrNull { user ->
            user != Process.myUserHandle() &&
                launcherApps.getActivityList(app.packageName, user).isNotEmpty()
        }
        val activity = profile?.let { launcherApps.getActivityList(app.packageName, it).firstOrNull() }
        if (profile != null && activity != null) {
            runCatching {
                launcherApps.startMainActivity(activity.componentName, profile, null, null)
            }.onFailure {
                Toast.makeText(application, "Open the clone from the Android work-profile drawer", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(application, "The work-profile clone is not installed yet", Toast.LENGTH_LONG).show()
        }
    }

    fun startService(enableMicrophoneFeatures: Boolean = false) {
        val app = getApplication<Application>()
        val intent = Intent(app, VirtualCameraService::class.java).apply {
            action = VirtualCameraService.ACTION_START
            putExtra(VirtualCameraService.EXTRA_ENABLE_MICROPHONE_FEATURES, enableMicrophoneFeatures)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            app.startForegroundService(intent)
        } else {
            app.startService(intent)
        }

        app.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        viewModelScope.launch {
            repository.updateSettings { it.copy(isVirtualCamActive = true) }
        }
    }

    fun stopService() {
        val app = getApplication<Application>()
        if (_isServiceBound.value) {
            try {
                app.unbindService(serviceConnection)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _isServiceBound.value = false
        }

        val intent = Intent(app, VirtualCameraService::class.java).apply {
            action = VirtualCameraService.ACTION_STOP
        }
        app.startService(intent)

        viewModelScope.launch {
            repository.updateSettings { it.copy(isVirtualCamActive = false) }
        }
    }

    fun togglePlayPause() {
        virtualCameraService?.togglePlayPause() ?: run {
            _isPlaying.value = !_isPlaying.value
        }
    }

    fun toggleMirror() {
        virtualCameraService?.toggleMirror() ?: run {
            viewModelScope.launch {
                val next = !settings.value.isMirrorHorizontal
                repository.updateSettings { it.copy(isMirrorHorizontal = next) }
            }
        }
    }

    fun toggleVirtualCam() {
        virtualCameraService?.toggleVirtualCam() ?: run {
            viewModelScope.launch {
                val next = !settings.value.isVirtualCamActive
                repository.updateSettings { it.copy(isVirtualCamActive = next) }
            }
        }
    }

    fun nextVideo() {
        virtualCameraService?.nextVideo() ?: run {
            val list = videos.value
            if (list.isNotEmpty()) {
                _currentVideoIndex.value = (_currentVideoIndex.value + 1) % list.size
            }
        }
    }

    fun prevVideo() {
        virtualCameraService?.previousVideo() ?: run {
            val list = videos.value
            if (list.isNotEmpty()) {
                val p = _currentVideoIndex.value - 1
                _currentVideoIndex.value = if (p < 0) list.size - 1 else p
            }
        }
    }

    fun selectVideo(index: Int) {
        virtualCameraService?.selectVideoIndex(index)
        _currentVideoIndex.value = index
    }

    fun onVideoCompleted() {
        val service = virtualCameraService
        if (service != null) {
            service.onVideoCompleted()
            return
        }
        when (settings.value.loopMode) {
            "LOOP_ALL" -> nextVideo()
            "ONCE" -> {
                virtualCameraService?.setPlaying(false)
                _isPlaying.value = false
            }
        }
    }

    fun importVideos(uris: List<Uri>) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            uris.forEachIndexed { i, uri ->
                try {
                    val originalName = queryDisplayName(uri) ?: "Imported video ${i + 1}.mp4"
                    val savedUri = try {
                        app.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                        uri
                    } catch (_: SecurityException) {
                        copyToPrivateStorage(uri, originalName)
                    }
                    val metadata = readVideoMetadata(savedUri)
                    repository.addVideo(
                        title = originalName,
                        uriString = savedUri.toString(),
                        durationMs = metadata.durationMs,
                        resolution = metadata.resolution
                    )
                } catch (exception: Exception) {
                    Log.e(TAG, "Could not import selected video: $uri", exception)
                    Toast.makeText(
                        app,
                        "Could not import selected video: ${exception.localizedMessage ?: "unsupported or unreadable file"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun queryDisplayName(uri: Uri): String? =
        getApplication<Application>().contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            } else {
                null
            }
        }

    private fun copyToPrivateStorage(uri: Uri, displayName: String): Uri {
        val app = getApplication<Application>()
        val extension = displayName.substringAfterLast('.', "mp4")
            .takeIf { it.matches(Regex("[A-Za-z0-9]{1,8}")) }
            ?.lowercase() ?: "mp4"
        val directory = File(app.filesDir, "imported_media").apply {
            if (!exists() && !mkdirs()) {
                throw IllegalStateException("Unable to create the private media directory")
            }
        }
        val temporaryFile = File.createTempFile("import_", ".$extension", directory)
        val finalFile = File(directory, "${UUID.randomUUID()}.$extension")
        try {
            val input = app.contentResolver.openInputStream(uri)
                ?: throw IllegalArgumentException("The selected video could not be opened")
            input.use { source ->
                FileOutputStream(temporaryFile).use { destination ->
                    source.copyTo(destination)
                }
            }
            if (temporaryFile.length() == 0L || !temporaryFile.renameTo(finalFile)) {
                throw IllegalStateException("Unable to save the selected video")
            }
            return Uri.fromFile(finalFile)
        } catch (exception: Exception) {
            temporaryFile.delete()
            finalFile.delete()
            throw exception
        }
    }

    private fun readVideoMetadata(uri: Uri): VideoMetadata {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(getApplication<Application>(), uri)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull()
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull()
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                ?.toIntOrNull() ?: 0
            val resolution = if (width != null && height != null && width > 0 && height > 0) {
                if (rotation == 90 || rotation == 270) "${height}x$width" else "${width}x$height"
            } else {
                "Unknown resolution"
            }
            return VideoMetadata(duration, resolution)
        } finally {
            retriever.release()
        }
    }

    private data class VideoMetadata(val durationMs: Long, val resolution: String)

    fun deleteVideo(id: Long) {
        viewModelScope.launch {
            repository.removeVideo(id)
        }
    }

    fun updateLoopMode(mode: String) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(loopMode = mode) }
        }
    }

    fun updateSpeed(speed: Float) {
        if (speed !in listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f)) return
        viewModelScope.launch {
            repository.updateSettings { it.copy(playbackSpeed = speed) }
        }
    }

    fun updateLipSync(enabled: Boolean) {
        virtualCameraService?.let {
            it.lipSyncEngine.isEnabled = enabled
        }
        viewModelScope.launch {
            repository.updateSettings { it.copy(lipSyncEnabled = enabled) }
        }
    }

    fun updateLipSyncSensitivity(sensitivity: Float) {
        virtualCameraService?.let {
            it.lipSyncEngine.sensitivity = sensitivity
        }
        viewModelScope.launch {
            repository.updateSettings { it.copy(lipSyncSensitivity = sensitivity) }
        }
    }

    fun updateNoiseCancellation(enabled: Boolean) {
        virtualCameraService?.let {
            it.audioEngine.isNoiseCancellationEnabled = enabled
        }
        viewModelScope.launch {
            repository.updateSettings { it.copy(noiseCancellationEnabled = enabled) }
        }
    }

    fun updateMicMonitoring(enabled: Boolean) {
        val engine = virtualCameraService?.audioEngine
        if (enabled && engine != null && !engine.setMonitorEnabled(true)) {
            Toast.makeText(
                getApplication(),
                "The device could not start the local audio monitor",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        if (!enabled) engine?.setMonitorEnabled(false)
        viewModelScope.launch {
            repository.updateSettings { it.copy(micMonitoringEnabled = enabled) }
        }
    }

    fun selectVoicePreset(preset: VoicePreset) {
        virtualCameraService?.let {
            it.audioEngine.currentVoicePreset = preset
        }
        viewModelScope.launch {
            repository.updateSettings { it.copy(voicePreset = preset.name) }
        }
    }

    override fun onCleared() {
        if (_isServiceBound.value) {
            try {
                getApplication<Application>().unbindService(serviceConnection)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        super.onCleared()
    }

    companion object {
        private const val TAG = "VirtualCamViewModel"
    }
}
