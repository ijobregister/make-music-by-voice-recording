package com.example.voicesongstudio.dsp

import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

enum class MusicalScale(val displayName: String, val intervalPattern: IntArray) {
    FREE("Free (Continuous)", intArrayOf()),
    CHROMATIC("Chromatic (All 12 Notes)", intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)),

    // Traditional Chinese Pentatonic Scales (五声音阶)
    CHINESE_GONG("Chinese Pentatonic Gong (宫: 1 2 3 5 6)", intArrayOf(0, 2, 4, 7, 9)),
    CHINESE_YU("Chinese Pentatonic Yu (羽: 6 1 2 3 5)", intArrayOf(0, 3, 5, 7, 10)),
    CHINESE_ZHI("Chinese Pentatonic Zhi (徵: 5 6 1 2 3)", intArrayOf(0, 2, 5, 7, 9)),

    // Western Scales
    MAJOR("Major (Natural)", intArrayOf(0, 2, 4, 5, 7, 9, 11)),
    NATURAL_MINOR("Minor (Natural)", intArrayOf(0, 2, 3, 5, 7, 8, 10)),
    BLUES("Blues Scale", intArrayOf(0, 3, 5, 6, 7, 10)),
    PENTATONIC_MINOR("Minor Pentatonic", intArrayOf(0, 3, 5, 7, 10));

    companion object {
        private val NOTE_NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

        /**
         * Converts frequency in Hz to MIDI note number (float).
         * A4 = 440 Hz = MIDI 69
         */
        fun frequencyToMidi(freqHz: Float): Float {
            if (freqHz <= 10f) return 0f
            return 69f + 12f * (ln(freqHz / 440f) / ln(2f))
        }

        /**
         * Converts MIDI note number to frequency in Hz.
         */
        fun midiToFrequency(midiNote: Float): Float {
            return 440f * 2.0f.pow((midiNote - 69f) / 12f)
        }

        /**
         * Gets note name string for a given MIDI note (e.g. 60 -> "C4", 69 -> "A4").
         */
        fun getNoteName(midiNote: Int): String {
            val noteIndex = (midiNote % 12 + 12) % 12
            val octave = (midiNote / 12) - 1
            return "${NOTE_NAMES[noteIndex]}$octave"
        }

        /**
         * Snaps a detected frequency in Hz to the closest pitch in the given scale and root note.
         * If scale is FREE, returns input frequency unmodified.
         */
        fun snapPitch(freqHz: Float, scale: MusicalScale, rootNoteMidi: Int = 60 /* Middle C */): Float {
            if (scale == FREE || scale.intervalPattern.isEmpty()) return freqHz
            if (freqHz <= 20f || freqHz >= 5000f) return freqHz

            val inputMidi = frequencyToMidi(freqHz)
            var closestMidi = inputMidi.roundToInt()
            var minDistance = Float.MAX_VALUE

            // Search neighboring octaves for the closest scale pitch
            for (octave in -2..2) {
                val octaveOffset = octave * 12
                for (interval in scale.intervalPattern) {
                    val candidateMidi = rootNoteMidi + octaveOffset + interval
                    val distance = kotlin.math.abs(inputMidi - candidateMidi)
                    if (distance < minDistance) {
                        minDistance = distance
                        closestMidi = candidateMidi
                    }
                }
            }

            return midiToFrequency(closestMidi.toFloat())
        }
    }
}
