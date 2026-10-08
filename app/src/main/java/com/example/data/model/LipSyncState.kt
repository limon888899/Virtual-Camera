package com.example.data.model

data class LipSyncState(
    val mouthOpenRatio: Float = 0f, // 0.0 (closed) to 1.0 (wide open)
    val mouthWidthRatio: Float = 0.5f, // 0.0 (pucker) to 1.0 (wide smile)
    val currentPhoneme: String = "REST", // A, E, I, O, U, M, REST
    val audioDecibels: Float = -60f, // dB
    val audioEnergy: Float = 0f, // normalized 0..1
    val isSpeaking: Boolean = false,
    val eyeBlinkRatio: Float = 0f // 0 = open, 1 = closed
)
