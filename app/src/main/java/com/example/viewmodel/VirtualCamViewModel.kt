package com.example.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VirtualCamViewModel : ViewModel() {

    // UI States
    private val _isVirtualCamActive = MutableStateFlow(false)
    val isVirtualCamActive: StateFlow<Boolean> = _isVirtualCamActive.asStateFlow()

    private val _isMirrorActive = MutableStateFlow(false)
    val isMirrorActive: StateFlow<Boolean> = _isMirrorActive.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentVideoIndex = MutableStateFlow(0)
    val currentVideoIndex: StateFlow<Int> = _currentVideoIndex.asStateFlow()

    private val _isLipSyncEnabled = MutableStateFlow(false)
    val isLipSyncEnabled: StateFlow<Boolean> = _isLipSyncEnabled.asStateFlow()

    private val _isAudioMonitorEnabled = MutableStateFlow(false)
    val isAudioMonitorEnabled: StateFlow<Boolean> = _isAudioMonitorEnabled.asStateFlow()

    // Engine references used across UI/Service
    val lipSyncEngine = Any()
    val audioEngine = Any()

    companion object {
        const val ACTION_START = "com.example.action.START"
        const val ACTION_STOP = "com.example.action.STOP"
        const val EXTRA_ENABLE_MICROPHONE_FEATURES = "extra_mic_features"
    }

    // Toggle and action methods matching all references
    fun toggleVirtualCam() {
        _isVirtualCamActive.value = !_isVirtualCamActive.value
    }

    fun toggleVirtualCam(enabled: Boolean) {
        _isVirtualCamActive.value = enabled
    }

    fun toggleMirror() {
        _isMirrorActive.value = !_isMirrorActive.value
    }

    fun togglePlayPause() {
        _isPlaying.value = !_isPlaying.value
    }

    fun setPlaying(playing: Boolean) {
        _isPlaying.value = playing
    }

    fun nextVideo() {
        _currentVideoIndex.value += 1
    }

    fun previousVideo() {
        if (_currentVideoIndex.value > 0) {
            _currentVideoIndex.value -= 1
        }
    }

    fun selectVideoIndex(index: Int) {
        _currentVideoIndex.value = index
    }

    fun onVideoCompleted() {
        _isPlaying.value = false
    }

    // Overloaded methods to support both naming styles
    fun setLipSyncEnabled(enabled: Boolean) {
        _isLipSyncEnabled.value = enabled
    }

    fun setAudioEngine(enabled: Boolean) {
        _isAudioMonitorEnabled.value = enabled
    }

    fun setMonitorEnabled(enabled: Boolean) {
        _isAudioMonitorEnabled.value = enabled
    }
}
