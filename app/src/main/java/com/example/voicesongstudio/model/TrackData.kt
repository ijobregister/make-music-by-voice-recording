package com.example.voicesongstudio.model

import com.example.voicesongstudio.dsp.MusicalScale
import com.example.voicesongstudio.synthesis.InstrumentType
import java.util.UUID

data class TrackData(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val instrument: InstrumentType,
    val originalVoiceAudio: ShortArray = ShortArray(0),
    val transformedAudio: ShortArray = ShortArray(0),
    val volume: Float = 0.85f,
    val pan: Float = 0.0f, // -1.0 (Left) to +1.0 (Right)
    val isMuted: Boolean = false,
    val isSolo: Boolean = false,
    val scale: MusicalScale = MusicalScale.CHINESE_GONG,
    val pitchShift: Int = 0,
    val colorHex: Long = 0xFF6C5CE7
) {
    val durationSeconds: Float
        get() = transformedAudio.size.toFloat() / 44100.0f

    val hasAudio: Boolean
        get() = transformedAudio.isNotEmpty()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TrackData
        return id == other.id &&
                name == other.name &&
                instrument == other.instrument &&
                volume == other.volume &&
                pan == other.pan &&
                isMuted == other.isMuted &&
                isSolo == other.isSolo &&
                scale == other.scale &&
                pitchShift == other.pitchShift &&
                transformedAudio.size == other.transformedAudio.size
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + instrument.hashCode()
        result = 31 * result + volume.hashCode()
        result = 31 * result + pan.hashCode()
        result = 31 * result + isMuted.hashCode()
        result = 31 * result + isSolo.hashCode()
        return result
    }
}
