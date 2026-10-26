package com.example.voicesongstudio.synthesis

import com.example.voicesongstudio.dsp.FormantFilter
import com.example.voicesongstudio.dsp.MusicalScale
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Audio effects processor for Vocal tracks: AutoTune pitch snapping and Robot Vocoder synthesis.
 */
class VocalEffectsEngine(private val sampleRate: Int = 44100) {

    /**
     * Pitch-corrects vocal recording by snapping detected pitch to nearest scale degree
     * and resampling or phase vocoding.
     */
    fun processAutoTune(
        audio: ShortArray,
        pitches: FloatArray,
        scale: MusicalScale,
        rootNoteMidi: Int = 60
    ): ShortArray {
        val totalSamples = audio.size
        val output = ShortArray(totalSamples)
        val chunkSize = 512

        var phaseIn = 0.0
        var phaseOut = 0.0

        for (i in 0 until totalSamples) {
            val pitchIdx = i.coerceAtMost(pitches.size - 1)
            val currentPitch = pitches[pitchIdx]

            if (currentPitch > 60f) {
                val targetPitch = MusicalScale.snapPitch(currentPitch, scale, rootNoteMidi)
                val pitchRatio = targetPitch / currentPitch

                // Pitch shift using phase resampling
                val sampleOffset = (i * pitchRatio).toInt() % totalSamples
                val orig = audio[sampleOffset].toFloat() / 32768.0f
                output[i] = (tanh(orig * 1.05f) * 32767.0f).toInt().toShort()
            } else {
                output[i] = audio[i]
            }
        }
        return output
    }

    /**
     * Robot Vocoder:
     * Generates a rich harmonic synthesizer carrier and modulates it with the vocal envelope and formant bands.
     */
    fun processRobotVocoder(
        audio: ShortArray,
        pitches: FloatArray,
        scale: MusicalScale,
        rootNoteMidi: Int = 60
    ): ShortArray {
        val totalSamples = audio.size
        val output = ShortArray(totalSamples)

        val bandFilters = arrayOf(
            FormantFilter().apply { setBandPass(300f, 2.0f, sampleRate) },
            FormantFilter().apply { setBandPass(800f, 2.5f, sampleRate) },
            FormantFilter().apply { setBandPass(1600f, 3.0f, sampleRate) },
            FormantFilter().apply { setBandPass(3200f, 3.0f, sampleRate) }
        )

        var carrierPhase = 0.0

        for (i in 0 until totalSamples) {
            val origSample = audio[i].toFloat() / 32768.0f
            val pitchIdx = i.coerceAtMost(pitches.size - 1)
            val detectedPitch = pitches[pitchIdx]

            val notePitch = if (detectedPitch > 60f) {
                MusicalScale.snapPitch(detectedPitch, scale, rootNoteMidi)
            } else {
                MusicalScale.midiToFrequency(rootNoteMidi.toFloat())
            }

            // Carrier: Sawtooth + Sub Pulse
            carrierPhase += notePitch / sampleRate
            if (carrierPhase >= 1.0) carrierPhase -= 1.0
            val carrierSaw = (2.0 * carrierPhase - 1.0).toFloat()
            val carrierSub = if (carrierPhase < 0.5) 1.0f else -1.0f
            val carrier = carrierSaw * 0.7f + carrierSub * 0.3f

            // Modulate carrier through formant filter bank
            var vocoded = 0.0f
            for (filter in bandFilters) {
                val voiceBand = filter.process(origSample)
                val bandEnergy = kotlin.math.abs(voiceBand)
                vocoded += carrier * bandEnergy
            }

            val mixed = tanh(vocoded * 2.2f)
            output[i] = (mixed * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()
        }

        return output
    }
}
