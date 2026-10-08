package com.example.service

import android.media.audiofx.NoiseSuppressor
import kotlin.math.sqrt

/**
 * Local microphone noise filter combining the optional device NoiseSuppressor
 * with a lightweight adaptive noise gate.
 */
class AINoiseSuppressionModule {

    private var hardwareSuppressor: NoiseSuppressor? = null
    
    // Adaptive Noise Tracking Parameters
    private var noiseFloorRms = 100f
    private val noiseFloorAttack = 0.05f   // Fast adaptation to dips (silence)
    private val noiseFloorRelease = 0.0005f // Slow upward tracking of sustained ambient noise
    
    private var prevSample = 0f

    fun attachHardwareSuppressor(audioSessionId: Int) {
        if (NoiseSuppressor.isAvailable()) {
            try {
                hardwareSuppressor?.release()
                hardwareSuppressor = NoiseSuppressor.create(audioSessionId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        try {
            hardwareSuppressor?.enabled = enabled
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Processes PCM buffer in-place to suppress noise.
     * Returns the clean effective RMS.
     */
    fun processAudioBuffer(buffer: ShortArray, read: Int, isEnabled: Boolean): Float {
        var sumSq = 0.0
        
        // Pass 1: High-pass filter (Rumble removal) & Initial RMS calculation
        for (i in 0 until read) {
            var sample = buffer[i].toFloat()

            if (isEnabled) {
                // 1-Pole High-Pass Filter: Cuts sub-bass rumble (< 80Hz)
                val filtered = sample - 0.95f * prevSample
                prevSample = sample
                sample = filtered
                buffer[i] = sample.toInt().toShort()
            }
            
            sumSq += sample * sample
        }
        
        val rms = sqrt(sumSq / read).toFloat()
        
        if (!isEnabled || rms < 1f) {
            return rms
        }

        // --- AI Adaptive Noise Floor Tracking ---
        if (rms < noiseFloorRms) {
            noiseFloorRms += noiseFloorAttack * (rms - noiseFloorRms) // fast drop
        } else {
            noiseFloorRms += noiseFloorRelease * (rms - noiseFloorRms) // slow rise
        }
        
        // Clamp noise floor to avoid suppressing actual voice
        noiseFloorRms = noiseFloorRms.coerceIn(10f, 1000f)

        // Calculate Signal-to-Noise Ratio (SNR)
        val snr = rms / noiseFloorRms
        
        // --- Dynamic Soft-Knee Expansion ---
        val attenuationFactor = when {
            snr < 1.5f -> 0.05f // Heavy suppression for noise (ambient hum, crowds)
            snr < 3.0f -> 0.05f + 0.95f * ((snr - 1.5f) / 1.5f) // Soft transition
            else -> 1.0f // Clear speech, keep untouched
        }
        
        // Apply attenuation
        if (attenuationFactor < 1.0f) {
            var cleanSumSq = 0.0
            for (i in 0 until read) {
                val attenuated = buffer[i].toFloat() * attenuationFactor
                buffer[i] = attenuated.toInt().toShort()
                cleanSumSq += attenuated * attenuated
            }
            return sqrt(cleanSumSq / read).toFloat()
        }
        
        return rms
    }
    
    fun release() {
        try {
            hardwareSuppressor?.release()
            hardwareSuppressor = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
