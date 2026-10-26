package com.example.voicesongstudio.model

import com.example.voicesongstudio.dsp.MusicalScale

data class SongProject(
    val title: String = "My Voice Masterpiece",
    val bpm: Int = 96,
    val keyRootMidi: Int = 60, // C4
    val scale: MusicalScale = MusicalScale.CHINESE_GONG,
    val tracks: List<TrackData> = emptyList(),
    val isLooping: Boolean = true,
    val loopBars: Int = 4
) {
    /**
     * Duration in samples of one full loop given BPM and loopBars (4/4 time signature).
     */
    val loopDurationSamples: Int
        get() {
            val beats = loopBars * 4
            val secondsPerBeat = 60.0 / bpm
            return (secondsPerBeat * beats * 44100.0).toInt()
        }

    val maxTrackSamples: Int
        get() = tracks.maxOfOrNull { it.transformedAudio.size } ?: 0
}
