package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VirtualCamViewModel : ViewModel() {

    // UI State variables
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

    // Mock Engines for LipSync and Audio
    val lipSyncEngine = Object()
    val audioEngine = Object()

    fun toggleVirtualCam(enabled: Boolean) {
        _isVirtualCamActive.value = enabled
    }

    fun toggleMirror(enabled: Boolean) {
        _isMirrorActive.value = enabled
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

    fun setLipSyncEnabled(enabled: Boolean) {
        _isLipSyncEnabled.value = enabled
    }

    fun setMonitorEnabled(enabled: Boolean) {
        _isAudioMonitorEnabled.value = enabled
    }
}
