package com.example.voicesongstudio.dsp

import kotlin.math.abs
import kotlin.math.sqrt

enum class DrumType {
    KICK,
    SNARE,
    HIHAT_CLOSED,
    HIHAT_OPEN,
    TOM,
    PERCUSSION_SHAKER
}

data class DrumHit(
    val sampleIndex: Int,
    val type: DrumType,
    val velocity: Float
)

/**
 * Transient & spectral analyzer to convert vocal beatboxing into drum kit hits.
 */
class TransientDetector(
    private val sampleRate: Int = 44100
) {
    /**
     * Analyzes vocal recording and detects drum hit events.
     */
    fun detectDrumHits(audio: ShortArray): List<DrumHit> {
        val hits = mutableListOf<DrumHit>()
        val hopSize = 512
        val windowSize = 1024
        val totalHops = (audio.size - windowSize) / hopSize
        if (totalHops <= 0) return hits

        var prevEnergy = 0.0f
        var lastHitSample = -4410 // 100ms debounce
        val minIntervalSamples = (sampleRate * 0.07f).toInt() // 70ms between hits

        for (hop in 0 until totalHops) {
            val offset = hop * hopSize
            var lowBandEnergy = 0.0f
            var midBandEnergy = 0.0f
            var highBandEnergy = 0.0f
            var totalEnergy = 0.0f
            var zeroCrossings = 0

            var prevSample = audio[offset].toFloat()
            for (i in 0 until windowSize) {
                val sample = audio[offset + i].toFloat() / 32768.0f
                val absSample = abs(sample)
                totalEnergy += sample * sample

                // Zero crossing rate
                if ((sample >= 0f && prevSample < 0f) || (sample < 0f && prevSample >= 0f)) {
                    zeroCrossings++
                }
                prevSample = sample

                // Approximate spectral energy via 1st & 2nd difference filters
                if (i > 1) {
                    val highVal = sample - audio[offset + i - 1].toFloat() / 32768.0f
                    highBandEnergy += highVal * highVal
                }
            }

            val currentRms = sqrt(totalEnergy / windowSize)
            val zcr = zeroCrossings.toFloat() / windowSize
            val energyRise = currentRms - prevEnergy

            // Transient detection threshold
            if (energyRise > 0.04f && currentRms > 0.05f && (offset - lastHitSample) > minIntervalSamples) {
                // Classify drum type based on spectral characteristics & ZCR
                val velocity = (currentRms * 2.5f).coerceIn(0.2f, 1.0f)
                val drumType = when {
                    // High zero crossing rate & high frequency energy -> Hi-hat ("tss", "chi")
                    zcr > 0.32f -> {
                        if (currentRms > 0.25f) DrumType.HIHAT_OPEN else DrumType.HIHAT_CLOSED
                    }
                    // Low zero-crossing rate and prominent low-end punch -> Kick drum ("boom", "buh")
                    zcr < 0.12f -> DrumType.KICK
                    // Mid-range burst with moderate zero crossings -> Snare drum ("ka", "pff")
                    zcr in 0.12f..0.26f -> DrumType.SNARE
                    // Percussion / Tom
                    else -> DrumType.TOM
                }

                hits.add(DrumHit(offset, drumType, velocity))
                lastHitSample = offset
            }

            prevEnergy = currentRms * 0.85f // leaky peak envelope
        }

        return hits
    }
}
