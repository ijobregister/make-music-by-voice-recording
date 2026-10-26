package com.example.voicesongstudio.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Generates accurate metronome click tracks for keeping rhythm while recording voice.
 */
object MetronomeGenerator {

    /**
     * Generates a single metronome click burst.
     */
    fun generateClick(sampleRate: Int = 44100, isAccent: Boolean = false): ShortArray {
        val durationSamples = (sampleRate * 0.035f).toInt() // 35ms
        val result = ShortArray(durationSamples)
        val freq = if (isAccent) 2000.0 else 1200.0

        for (i in 0 until durationSamples) {
            val t = i.toFloat() / sampleRate
            val env = exp(-t * 120.0).toFloat()
            val sample = sin(2.0 * PI * freq * t).toFloat() * env * 0.9f
            result[i] = (sample * 32767.0f).toInt().toShort()
        }
        return result
    }

    /**
     * Generates a 4-beat count-in audio buffer at a given BPM.
     */
    fun generateCountIn(sampleRate: Int = 44100, bpm: Int = 120): ShortArray {
        val beatInterval = (sampleRate * (60.0 / bpm)).toInt()
        val totalSamples = beatInterval * 4
        val result = ShortArray(totalSamples)

        val accentClick = generateClick(sampleRate, isAccent = true)
        val regularClick = generateClick(sampleRate, isAccent = false)

        for (beat in 0 until 4) {
            val startIdx = beat * beatInterval
            val click = if (beat == 0) accentClick else regularClick
            for (i in click.indices) {
                if (startIdx + i < totalSamples) {
                    result[startIdx + i] = click[i]
                }
            }
        }
        return result
    }
}
