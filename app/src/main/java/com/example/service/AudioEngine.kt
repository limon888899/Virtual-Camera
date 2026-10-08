package com.example.service

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioAttributes
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import com.example.data.model.VoicePreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class AudioEngine(private val scope: CoroutineScope) {

    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = max(
        AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat),
        2048
    )

    private var audioRecord: AudioRecord? = null
    private var recordJob: Job? = null
    private val monitorLock = Any()
    private var audioTrack: AudioTrack? = null
    @Volatile private var monitorEnabled = false

    private val _decibels = MutableStateFlow(-60f)
    val decibels: StateFlow<Float> = _decibels.asStateFlow()

    private val _energy = MutableStateFlow(0f)
    val energy: StateFlow<Float> = _energy.asStateFlow()

    private val _isAudioActive = MutableStateFlow(false)
    val isAudioActive: StateFlow<Boolean> = _isAudioActive.asStateFlow()

    private val noiseModule = AINoiseSuppressionModule()

    var isNoiseCancellationEnabled: Boolean = true
        set(value) {
            field = value
            noiseModule.setEnabled(value)
        }

    var currentVoicePreset: VoicePreset = VoicePreset.NATURAL
    var noiseThresholdRms: Float = 350f

    // Callback for lip sync engine
    var onAudioFrameProcessed: ((Float, Float, String) -> Unit)? = null

    fun setMonitorEnabled(enabled: Boolean): Boolean {
        monitorEnabled = enabled
        synchronized(monitorLock) {
            releaseMonitorTrack()
            if (!enabled || audioRecord?.recordingState != AudioRecord.RECORDSTATE_RECORDING) return true

            return try {
                val minimumSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    audioFormat
                )
                if (minimumSize <= 0) {
                    monitorEnabled = false
                    return false
                }
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .setEncoding(audioFormat)
                            .build()
                    )
                    .setBufferSizeInBytes(max(minimumSize, bufferSize * 2))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
                if (track.state != AudioTrack.STATE_INITIALIZED) {
                    track.release()
                    monitorEnabled = false
                    return false
                }
                track.play()
                audioTrack = track
                true
            } catch (exception: RuntimeException) {
                monitorEnabled = false
                Log.e(TAG, "Unable to start local processed-audio monitor", exception)
                false
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startCapture() {
        if (recordJob?.isActive == true) return

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = null
                return
            }

            audioRecord?.startRecording()
            if (monitorEnabled) setMonitorEnabled(true)
            audioRecord?.audioSessionId?.let { sessionId ->
                noiseModule.attachHardwareSuppressor(sessionId)
                noiseModule.setEnabled(isNoiseCancellationEnabled)
            }
            
            _isAudioActive.value = true

            recordJob = scope.launch(Dispatchers.Default) {
                val buffer = ShortArray(bufferSize)
                var prevFxSample = 0f

                while (isActive && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        
                        // 1. AI-Powered Noise Cancellation Module (Processes buffer in-place)
                        val effectiveRms = noiseModule.processAudioBuffer(buffer, read, isNoiseCancellationEnabled)
                        
                        // 2. Voice FX & Phoneme Classification
                        var zeroCrossings = 0
                        
                        for (i in 0 until read) {
                            var sample = buffer[i].toFloat()

                            // Voice Preset FX modulation
                            val unprocessedSample = sample
                            when (currentVoicePreset) {
                                VoicePreset.STUDIO -> {
                                    sample *= 1.15f
                                }
                                VoicePreset.DEEP_VOCAL -> {
                                    sample = sample * 0.35f + prevFxSample * 0.65f
                                    prevFxSample = unprocessedSample
                                }
                                VoicePreset.ROBOTIC -> {
                                    sample = (sample / 1024f).toInt() * 1024f
                                    if ((i / 12) % 2 == 0) sample *= 0.7f else sample *= -0.7f
                                }
                                VoicePreset.FEMALE_PITCH -> {
                                    sample = unprocessedSample - prevFxSample * 0.82f
                                    prevFxSample = unprocessedSample
                                }
                                VoicePreset.MALE_PITCH -> {
                                    sample = sample * 0.65f + prevFxSample * 0.35f
                                    prevFxSample = unprocessedSample
                                }
                                VoicePreset.NATURAL -> {}
                            }
                            
                            buffer[i] = sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()

                            if (i > 0 && ((buffer[i] >= 0 && buffer[i - 1] < 0) || (buffer[i] < 0 && buffer[i - 1] >= 0))) {
                                zeroCrossings++
                            }
                        }

                        // Calculate dB (-60 dB to 0 dB)
                        val db = if (effectiveRms > 10f) {
                            (20 * log10(effectiveRms / 32767f)).coerceIn(-60f, 0f)
                        } else {
                            -60f
                        }

                        val normalizedEnergy = ((db + 60f) / 60f).coerceIn(0f, 1f)

                        // Phoneme classification based on zero crossings & energy
                        val phoneme = if (normalizedEnergy < 0.12f) {
                            "REST"
                        } else {
                            val freqRatio = zeroCrossings.toFloat() / read
                            when {
                                freqRatio > 0.25f -> "E" // High frequency (sibilants/front vowels)
                                freqRatio > 0.15f -> "I"
                                freqRatio > 0.08f -> "A" // Open mid vowel
                                else -> "O"              // Low frequency / back vowel
                            }
                        }

                        if (monitorEnabled) {
                            synchronized(monitorLock) {
                                audioTrack?.write(buffer, 0, read, AudioTrack.WRITE_BLOCKING)
                            }
                        }

                        _decibels.value = db
                        _energy.value = normalizedEnergy
                        onAudioFrameProcessed?.invoke(normalizedEnergy, db, phoneme)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _isAudioActive.value = false
        }
    }

    fun stopCapture() {
        recordJob?.cancel()
        recordJob = null
        synchronized(monitorLock) {
            releaseMonitorTrack()
        }
        try {
            if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord?.stop()
            }
            audioRecord?.release()
            noiseModule.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            audioRecord = null
            _isAudioActive.value = false
            _decibels.value = -60f
            _energy.value = 0f
        }
    }

    private fun releaseMonitorTrack() {
        audioTrack?.let { track ->
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.pause()
            track.flush()
            track.release()
        }
        audioTrack = null
    }

    companion object {
        private const val TAG = "AudioEngine"
    }
}
