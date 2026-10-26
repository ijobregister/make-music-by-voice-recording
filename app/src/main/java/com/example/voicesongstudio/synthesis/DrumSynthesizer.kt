package com.example.voicesongstudio.synthesis

import com.example.voicesongstudio.dsp.DrumHit
import com.example.voicesongstudio.dsp.DrumType
import com.example.voicesongstudio.dsp.FormantFilter
import java.util.Random
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Procedural drum synthesis engine generating authentic punchy drum hits from vocal beatbox events.
 */
class DrumSynthesizer(private val sampleRate: Int = 44100) {

    private val random = Random(42)

    /**
     * Renders detected drum hits into a continuous ShortArray PCM buffer.
     */
    fun renderDrumHits(hits: List<DrumHit>, totalSamples: Int): ShortArray {
        val output = FloatArray(totalSamples)

        for (hit in hits) {
            val hitBuffer = when (hit.type) {
                DrumType.KICK -> generateKick(hit.velocity)
                DrumType.SNARE -> generateSnare(hit.velocity)
                DrumType.HIHAT_CLOSED -> generateHiHat(hit.velocity, isOpen = false)
                DrumType.HIHAT_OPEN -> generateHiHat(hit.velocity, isOpen = true)
                DrumType.TOM -> generateTom(hit.velocity)
                DrumType.PERCUSSION_SHAKER -> generateShaker(hit.velocity)
            }

            for (i in hitBuffer.indices) {
                val targetIdx = hit.sampleIndex + i
                if (targetIdx < totalSamples) {
                    output[targetIdx] += hitBuffer[i]
                }
            }
        }

        // Soft limit & convert to 16-bit PCM
        val result = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val sample = tanh(output[i] * 1.1f) // soft saturation limiter
            result[i] = (sample * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()
        }
        return result
    }

    /**
     * Punchy kick drum: fast frequency sweep from 160 Hz -> 48 Hz with punch transient.
     */
    fun generateKick(velocity: Float): FloatArray {
        val durationSamples = (sampleRate * 0.35f).toInt() // 350 ms
        val buffer = FloatArray(durationSamples)
        var phase = 0.0

        for (i in 0 until durationSamples) {
            val t = i.toFloat() / sampleRate
            // Exponential frequency drop from 160 Hz to 48 Hz
            val freq = 48.0 + (160.0 - 48.0) * exp(-t * 28.0)
            phase += 2.0 * PI * freq / sampleRate
            val sineVal = sin(phase).toFloat()

            // Amplitude envelope
            val env = exp(-t * 9.0).toFloat()

            // Click transient at start (first 5ms)
            val click = if (i < (sampleRate * 0.005f)) {
                (1.0f - i / (sampleRate * 0.005f)) * 0.4f
            } else 0.0f

            buffer[i] = tanh((sineVal * env + click) * 1.5f) * velocity
        }
        return buffer
    }

    /**
     * Crisp snare drum: body tone (180 Hz) + white noise snare rattle.
     */
    fun generateSnare(velocity: Float): FloatArray {
        val durationSamples = (sampleRate * 0.28f).toInt() // 280 ms
        val buffer = FloatArray(durationSamples)

        val noiseFilter = FormantFilter().apply {
            setBandPass(centerFreq = 2200f, q = 1.2f, sampleRate = sampleRate)
        }
        var tonePhase = 0.0

        for (i in 0 until durationSamples) {
            val t = i.toFloat() / sampleRate

            // Body tone (185 Hz -> 130 Hz)
            val toneFreq = 130.0 + 55.0 * exp(-t * 30.0)
            tonePhase += 2.0 * PI * toneFreq / sampleRate
            val tone = sin(tonePhase).toFloat() * exp(-t * 18.0).toFloat()

            // Noise snare crack
            val rawNoise = (random.nextFloat() * 2.0f - 1.0f)
            val filteredNoise = noiseFilter.process(rawNoise)
            val noiseEnv = exp(-t * 14.0).toFloat()

            val snap = if (i < (sampleRate * 0.004f)) 0.35f else 0.0f

            buffer[i] = (tone * 0.5f + filteredNoise * 0.8f * noiseEnv + snap) * velocity
        }
        return buffer
    }

    /**
     * Hi-Hat: high-pass filtered metallic noise cluster.
     */
    fun generateHiHat(velocity: Float, isOpen: Boolean): FloatArray {
        val duration = if (isOpen) 0.22f else 0.05f
        val durationSamples = (sampleRate * duration).toInt()
        val buffer = FloatArray(durationSamples)

        val hpFilter = FormantFilter().apply {
            setHighPass(cutoffFreq = 7500f, q = 2.0f, sampleRate = sampleRate)
        }
        val decayRate = if (isOpen) 16.0 else 65.0

        for (i in 0 until durationSamples) {
            val t = i.toFloat() / sampleRate
            val noise = (random.nextFloat() * 2.0f - 1.0f)
            val filtered = hpFilter.process(noise)
            val env = exp(-t * decayRate).toFloat()
            buffer[i] = filtered * env * velocity * 0.85f
        }
        return buffer
    }

    /**
     * Tom drum: downward resonant pitch sweep.
     */
    fun generateTom(velocity: Float): FloatArray {
        val durationSamples = (sampleRate * 0.3f).toInt()
        val buffer = FloatArray(durationSamples)
        var phase = 0.0

        for (i in 0 until durationSamples) {
            val t = i.toFloat() / sampleRate
            val freq = 85.0 + 95.0 * exp(-t * 18.0)
            phase += 2.0 * PI * freq / sampleRate
            val env = exp(-t * 11.0).toFloat()
            buffer[i] = sin(phase).toFloat() * env * velocity * 0.9f
        }
        return buffer
    }

    /**
     * Shaker percussion: burst of filtered high noise.
     */
    fun generateShaker(velocity: Float): FloatArray {
        val durationSamples = (sampleRate * 0.09f).toInt()
        val buffer = FloatArray(durationSamples)

        val bpFilter = FormantFilter().apply {
            setBandPass(centerFreq = 6000f, q = 2.5f, sampleRate = sampleRate)
        }

        for (i in 0 until durationSamples) {
            val t = i.toFloat() / sampleRate
            val noise = (random.nextFloat() * 2.0f - 1.0f)
            val filtered = bpFilter.process(noise)
            // Symmetrical attack and decay envelope
            val env = sin(PI.toFloat() * (i.toFloat() / durationSamples))
            buffer[i] = filtered * env * velocity * 0.75f
        }
        return buffer
    }
}
