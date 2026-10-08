package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val isVirtualCamActive: Boolean = false,
    val loopMode: String = "LOOP_ALL", // LOOP_ALL, LOOP_ONE, ONCE
    val isMirrorHorizontal: Boolean = false,
    val isMirrorVertical: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val lipSyncEnabled: Boolean = false,
    val lipSyncSensitivity: Float = 1.0f,
    val noiseCancellationEnabled: Boolean = true,
    val voicePreset: String = "NATURAL", // NATURAL, STUDIO, DEEP_VOCAL, ROBOTIC, FEMALE_PITCH, MALE_PITCH
    val micMonitoringEnabled: Boolean = false,
    val floatingBubbleAlpha: Float = 0.95f,
    val overlaySizePercent: Int = 45
)
