package com.example.voicesongstudio.dsp

import kotlin.math.sqrt

data class PitchResult(
    val pitchHz: Float,
    val confidence: Float,
    val isVoiced: Boolean,
    val rmsEnergy: Float
)

/**
 * Fast YIN pitch detector for extracting fundamental frequency (F0) from vocal audio.
 */
class PitchDetector(
    private val sampleRate: Int = 44100,
    private val windowSize: Int = 1024,
    private val threshold: Float = 0.15f
) {
    private val buffer = FloatArray(windowSize / 2)

    /**
     * Estimates pitch and energy from a window of PCM samples.
     * @param audio Audio buffer of length at least windowSize
     * @param offset Starting offset in audio array
     */
    fun detectPitch(audio: ShortArray, offset: Int): PitchResult {
        if (offset + windowSize > audio.size) {
            return PitchResult(0f, 0f, false, 0f)
        }

        val halfWindow = windowSize / 2
        var sumSquares = 0.0

        // Step 1: Calculate Difference function & RMS energy
        for (i in 0 until windowSize) {
            val sampleVal = audio[offset + i].toFloat() / 32768.0f
            sumSquares += sampleVal * sampleVal
        }
        val rms = sqrt(sumSquares / windowSize).toFloat()

        // Silence gate: if RMS energy is too quiet, treat as unvoiced silence
        if (rms < 0.015f) {
            return PitchResult(0f, 0f, false, rms)
        }

        // Difference function: d(tau) = sum_j (x[j] - x[j+tau])^2
        for (tau in 0 until halfWindow) {
            var diffSum = 0.0f
            for (j in 0 until halfWindow) {
                val diff = (audio[offset + j] - audio[offset + j + tau]) / 32768.0f
                diffSum += diff * diff
            }
            buffer[tau] = diffSum
        }

        // Step 2: Cumulative Mean Normalized Difference Function
        buffer[0] = 1.0f
        var runningSum = 0.0f
        for (tau in 1 until halfWindow) {
            runningSum += buffer[tau]
            buffer[tau] = if (runningSum > 0f) {
                buffer[tau] * tau / runningSum
            } else {
                1.0f
            }
        }

        // Step 3: Absolute Thresholding
        // Human vocal pitch range: 65 Hz to 1200 Hz
        val minTau = (sampleRate / 1200.0f).toInt().coerceAtLeast(2)
        val maxTau = (sampleRate / 65.0f).toInt().coerceAtMost(halfWindow - 2)

        var tauEstimate = -1
        for (tau in minTau..maxTau) {
            if (buffer[tau] < threshold) {
                // Find local minimum
                var localTau = tau
                while (localTau + 1 < maxTau && buffer[localTau + 1] < buffer[localTau]) {
                    localTau++
                }
                tauEstimate = localTau
                break
            }
        }

        // If no value fell below threshold, find global minimum in vocal range
        if (tauEstimate == -1) {
            var minVal = Float.MAX_VALUE
            for (tau in minTau..maxTau) {
                if (buffer[tau] < minVal) {
                    minVal = buffer[tau]
                    tauEstimate = tau
                }
            }
            if (minVal > 0.45f) {
                // Not periodic enough to be a voiced pitch
                return PitchResult(0f, 0f, false, rms)
            }
        }

        // Step 4: Parabolic Interpolation for sub-sample accuracy
        val x0 = if (tauEstimate > 0) tauEstimate - 1 else tauEstimate
        val x2 = if (tauEstimate + 1 < halfWindow) tauEstimate + 1 else tauEstimate
        val s0 = buffer[x0]
        val s1 = buffer[tauEstimate]
        val s2 = buffer[x2]

        val delta = if (2.0f * s1 - s0 - s2 != 0f) {
            (s2 - s0) / (2.0f * (2.0f * s1 - s0 - s2))
        } else {
            0.0f
        }
        val refinedTau = tauEstimate + delta

        val pitchHz = if (refinedTau > 0f) sampleRate / refinedTau else 0f
        val confidence = (1.0f - buffer[tauEstimate]).coerceIn(0f, 1f)

        return PitchResult(
            pitchHz = pitchHz,
            confidence = confidence,
            isVoiced = confidence >= 0.55f && pitchHz in 60f..1400f,
            rmsEnergy = rms
        )
    }
}
