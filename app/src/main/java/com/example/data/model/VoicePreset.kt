package com.example.data.model

enum class VoicePreset(
    val title: String,
    val description: String
) {
    NATURAL("Natural", "Unmodified microphone audio"),
    STUDIO("Studio Level", "Raises the local monitor level with clipping protection"),
    DEEP_VOCAL("Warm Low-Pass", "Smooths high frequencies for a warmer tone"),
    ROBOTIC("Robotic Bit-Crush", "Quantizes and modulates the local monitor signal"),
    FEMALE_PITCH("Bright High-Pass", "Emphasizes high frequencies; does not shift pitch"),
    MALE_PITCH("Warm Low-Pass", "Smooths high frequencies; does not shift pitch");

    companion object {
        fun fromString(name: String): VoicePreset {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: NATURAL
        }
    }
}
