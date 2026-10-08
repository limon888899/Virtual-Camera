package com.example.service

import com.example.data.model.LipSyncState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

class LipSyncEngine(private val scope: CoroutineScope) {

    private val _lipSyncState = MutableStateFlow(LipSyncState())
    val lipSyncState: StateFlow<LipSyncState> = _lipSyncState.asStateFlow()

    private var animationJob: Job? = null
    var sensitivity: Float = 1.0f
    var isEnabled: Boolean = true

    // Target values for smooth animation
    private var targetMouthOpen = 0f
    private var targetMouthWidth = 0.5f
    private var currentPhoneme = "REST"
    private var lastAudioEnergy = 0f
    private var lastDecibels = -60f

    init {
        startAnimationLoop()
    }

    private fun startAnimationLoop() {
        animationJob = scope.launch(Dispatchers.Default) {
            var blinkCounter = 0
            var isBlinking = false
            var blinkProgress = 0f

            while (isActive) {
                // Smooth interpolation (60 FPS = ~16ms)
                delay(16)

                if (!isEnabled) {
                    _lipSyncState.value = LipSyncState(currentPhoneme = "REST")
                    continue
                }

                // Handle natural eye blinking
                blinkCounter++
                if (!isBlinking && blinkCounter > Random.nextInt(120, 250)) {
                    isBlinking = true
                    blinkCounter = 0
                    blinkProgress = 0f
                }

                if (isBlinking) {
                    blinkProgress += 0.2f
                    if (blinkProgress >= 1f) {
                        isBlinking = false
                        blinkProgress = 0f
                    }
                }

                val eyeBlink = if (isBlinking) {
                    // Triangle wave for blink: 0 -> 1 -> 0
                    if (blinkProgress < 0.5f) blinkProgress * 2f else (1f - blinkProgress) * 2f
                } else 0f

                // Interpolate mouth open/width towards target
                val current = _lipSyncState.value
                val lerpFactor = 0.35f
                val newOpen = current.mouthOpenRatio + (targetMouthOpen - current.mouthOpenRatio) * lerpFactor
                val newWidth = current.mouthWidthRatio + (targetMouthWidth - current.mouthWidthRatio) * lerpFactor

                val isSpeaking = lastAudioEnergy > 0.15f

                _lipSyncState.value = LipSyncState(
                    mouthOpenRatio = newOpen.coerceIn(0f, 1f),
                    mouthWidthRatio = newWidth.coerceIn(0f, 1f),
                    currentPhoneme = currentPhoneme,
                    audioDecibels = lastDecibels,
                    audioEnergy = lastAudioEnergy,
                    isSpeaking = isSpeaking,
                    eyeBlinkRatio = eyeBlink
                )
            }
        }
    }

    fun onAudioFrame(energy: Float, decibels: Float, phoneme: String) {
        lastAudioEnergy = energy
        lastDecibels = decibels
        currentPhoneme = phoneme

        val scaledEnergy = (energy * sensitivity).coerceIn(0f, 1f)

        if (scaledEnergy < 0.10f) {
            targetMouthOpen = 0f
            targetMouthWidth = 0.5f
            currentPhoneme = "REST"
        } else {
            // Map phoneme to mouth shape
            when (phoneme) {
                "A" -> {
                    targetMouthOpen = scaledEnergy * 0.95f
                    targetMouthWidth = 0.55f
                }
                "O" -> {
                    targetMouthOpen = scaledEnergy * 0.85f
                    targetMouthWidth = 0.25f // puckered/rounded
                }
                "E" -> {
                    targetMouthOpen = scaledEnergy * 0.45f
                    targetMouthWidth = 0.85f // wide grin/smile
                }
                "I" -> {
                    targetMouthOpen = scaledEnergy * 0.35f
                    targetMouthWidth = 0.75f
                }
                else -> {
                    targetMouthOpen = scaledEnergy * 0.6f
                    targetMouthWidth = 0.5f
                }
            }
        }
    }

    fun stop() {
        animationJob?.cancel()
        animationJob = null
    }
}
