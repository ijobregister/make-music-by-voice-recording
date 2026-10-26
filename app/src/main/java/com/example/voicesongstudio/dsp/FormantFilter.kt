package com.example.voicesongstudio.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * High performance Direct Form II Biquad Filter.
 */
class FormantFilter {

    private var b0 = 1.0f
    private var b1 = 0.0f
    private var b2 = 0.0f
    private var a1 = 0.0f
    private var a2 = 0.0f

    private var z1 = 0.0f
    private var z2 = 0.0f

    fun reset() {
        z1 = 0.0f
        z2 = 0.0f
    }

    fun process(input: Float): Float {
        val out = b0 * input + z1
        z1 = b1 * input - a1 * out + z2
        z2 = b2 * input - a2 * out
        return out
    }

    /**
     * Configures as a resonant bandpass filter (useful for formants).
     */
    fun setBandPass(centerFreq: Float, q: Float, sampleRate: Int = 44100) {
        val omega = 2.0f * PI.toFloat() * (centerFreq / sampleRate).coerceIn(0.001f, 0.49f)
        val alpha = sin(omega) / (2.0f * q.coerceAtLeast(0.1f))

        val a0 = 1.0f + alpha
        b0 = alpha / a0
        b1 = 0.0f
        b2 = -alpha / a0
        a1 = (-2.0f * cos(omega)) / a0
        a2 = (1.0f - alpha) / a0
    }

    /**
     * Configures as a resonant low-pass filter.
     */
    fun setLowPass(cutoffFreq: Float, q: Float = 0.707f, sampleRate: Int = 44100) {
        val omega = 2.0f * PI.toFloat() * (cutoffFreq / sampleRate).coerceIn(0.001f, 0.49f)
        val alpha = sin(omega) / (2.0f * q.coerceAtLeast(0.1f))
        val cosOmega = cos(omega)

        val a0 = 1.0f + alpha
        b0 = ((1.0f - cosOmega) / 2.0f) / a0
        b1 = (1.0f - cosOmega) / a0
        b2 = ((1.0f - cosOmega) / 2.0f) / a0
        a1 = (-2.0f * cosOmega) / a0
        a2 = (1.0f - alpha) / a0
    }

    /**
     * Configures as a resonant high-pass filter.
     */
    fun setHighPass(cutoffFreq: Float, q: Float = 0.707f, sampleRate: Int = 44100) {
        val omega = 2.0f * PI.toFloat() * (cutoffFreq / sampleRate).coerceIn(0.001f, 0.49f)
        val alpha = sin(omega) / (2.0f * q.coerceAtLeast(0.1f))
        val cosOmega = cos(omega)

        val a0 = 1.0f + alpha
        b0 = ((1.0f + cosOmega) / 2.0f) / a0
        b1 = (-(1.0f + cosOmega)) / a0
        b2 = ((1.0f + cosOmega) / 2.0f) / a0
        a1 = (-2.0f * cosOmega) / a0
        a2 = (1.0f - alpha) / a0
    }
}
