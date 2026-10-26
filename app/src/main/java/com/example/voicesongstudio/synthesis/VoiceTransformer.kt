package com.example.voicesongstudio.synthesis

import com.example.voicesongstudio.dsp.DrumHit
import com.example.voicesongstudio.dsp.DrumType
import com.example.voicesongstudio.dsp.MusicalScale
import com.example.voicesongstudio.dsp.PitchDetector
import com.example.voicesongstudio.dsp.TransientDetector
import kotlin.math.pow

/**
 * Transforms voice recordings into instrument audio tracks.
 */
class VoiceTransformer(private val sampleRate: Int = 44100) {

    private val pitchDetector = PitchDetector(sampleRate = sampleRate)
    private val transientDetector = TransientDetector(sampleRate = sampleRate)
    private val drumSynthesizer = DrumSynthesizer(sampleRate = sampleRate)
    private val tunedSynthEngine = TunedSynthEngine(sampleRate = sampleRate)
    private val vocalEffectsEngine = VocalEffectsEngine(sampleRate = sampleRate)

    /**
     * Transforms raw recorded voice PCM into the chosen instrument sound.
     */
    fun transformVoice(
        voiceAudio: ShortArray,
        instrument: InstrumentType,
        scale: MusicalScale = MusicalScale.CHINESE_GONG,
        rootNoteMidi: Int = 60, // C4
        octaveShift: Int = 0
    ): ShortArray {
        if (voiceAudio.isEmpty()) return ShortArray(0)

        return when (instrument) {
            InstrumentType.DRUM_KIT -> {
                // Percussive analysis -> Drum synthesis
                val hits = transientDetector.detectDrumHits(voiceAudio)
                drumSynthesizer.renderDrumHits(hits, voiceAudio.size)
            }

            InstrumentType.PERCUSSION -> {
                // Percussive analysis -> Tuned Percussion / Shakers
                val hits = transientDetector.detectDrumHits(voiceAudio).map { hit ->
                    if (hit.type == DrumType.KICK) hit.copy(type = DrumType.TOM)
                    else hit.copy(type = DrumType.PERCUSSION_SHAKER)
                }
                drumSynthesizer.renderDrumHits(hits, voiceAudio.size)
            }

            InstrumentType.VOCAL_AUTOTUNE -> {
                val (pitches, _) = analyzePitchAndEnvelope(voiceAudio, scale, rootNoteMidi, octaveShift)
                vocalEffectsEngine.processAutoTune(voiceAudio, pitches, scale, rootNoteMidi)
            }

            InstrumentType.VOCAL_VOCODER -> {
                val (pitches, _) = analyzePitchAndEnvelope(voiceAudio, scale, rootNoteMidi, octaveShift)
                vocalEffectsEngine.processRobotVocoder(voiceAudio, pitches, scale, rootNoteMidi)
            }

            else -> {
                // Melodic instruments: Erhu, Guzheng, Dizi, Pipa, Piano, Violin, Guitar, Saxophone, 808 Bass, Synth Bass
                val (pitches, amplitudes) = analyzePitchAndEnvelope(
                    voiceAudio,
                    scale,
                    rootNoteMidi,
                    octaveShift + instrument.defaultOctaveShift
                )
                tunedSynthEngine.synthesizeMelody(pitches, amplitudes, instrument)
            }
        }
    }

    /**
     * Extracts pitch contour and amplitude envelope per sample, applying scale snapping and octave shifts.
     */
    private fun analyzePitchAndEnvelope(
        audio: ShortArray,
        scale: MusicalScale,
        rootNoteMidi: Int,
        octaveShift: Int
    ): Pair<FloatArray, FloatArray> {
        val totalSamples = audio.size
        val pitches = FloatArray(totalSamples)
        val amplitudes = FloatArray(totalSamples)

        val hopSize = 256
        val windowSize = 1024
        val totalHops = (totalSamples - windowSize) / hopSize

        val octaveMultiplier = 2.0f.pow(octaveShift.toFloat())
        var lastValidPitch = MusicalScale.midiToFrequency(rootNoteMidi.toFloat()) * octaveMultiplier

        for (hop in 0 until totalHops) {
            val offset = hop * hopSize
            val result = pitchDetector.detectPitch(audio, offset)

            val currentPitch = if (result.isVoiced && result.pitchHz > 50f) {
                val shiftedPitch = result.pitchHz * octaveMultiplier
                val snapped = MusicalScale.snapPitch(shiftedPitch, scale, rootNoteMidi)
                lastValidPitch = snapped
                snapped
            } else {
                // If unvoiced or silent, smoothly decay or hold last pitch
                lastValidPitch
            }

            val amp = if (result.isVoiced) result.rmsEnergy * 2.2f else 0.0f

            val endSample = (offset + hopSize).coerceAtMost(totalSamples)
            for (i in offset until endSample) {
                pitches[i] = currentPitch
                amplitudes[i] = amp.coerceIn(0f, 1f)
            }
        }

        // Fill remaining tail
        val filledUntil = (totalHops * hopSize).coerceAtMost(totalSamples)
        for (i in filledUntil until totalSamples) {
            pitches[i] = lastValidPitch
            amplitudes[i] = 0.0f
        }

        return Pair(pitches, amplitudes)
    }
}
