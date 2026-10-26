package com.example.voicesongstudio.model

import com.example.voicesongstudio.dsp.DrumHit
import com.example.voicesongstudio.dsp.DrumType
import com.example.voicesongstudio.dsp.MusicalScale
import com.example.voicesongstudio.synthesis.DrumSynthesizer
import com.example.voicesongstudio.synthesis.InstrumentType
import com.example.voicesongstudio.synthesis.TunedSynthEngine

/**
 * Creates pre-composed demo tracks demonstrating the combination of Chinese & Western instruments,
 * drums, bass, and piano into a cohesive musical song.
 */
object DemoSongFactory {

    fun createDemoSong(): SongProject {
        val sampleRate = 44100
        val bpm = 92
        val secondsPerBeat = 60.0 / bpm
        val samplesPerBeat = (secondsPerBeat * sampleRate).toInt()
        val totalBars = 4
        val totalBeats = totalBars * 4
        val totalSamples = samplesPerBeat * totalBeats

        val drumSynth = DrumSynthesizer(sampleRate)
        val tunedSynth = TunedSynthEngine(sampleRate)

        // 1. Drum Track (Beatbox Drum Kit)
        val drumHits = mutableListOf<DrumHit>()
        for (bar in 0 until totalBars) {
            val barStart = bar * 4 * samplesPerBeat
            // Beat 1: Kick
            drumHits.add(DrumHit(barStart, DrumType.KICK, 0.95f))
            // Beat 1.5: Closed Hat
            drumHits.add(DrumHit(barStart + (samplesPerBeat * 0.5f).toInt(), DrumType.HIHAT_CLOSED, 0.5f))
            // Beat 2: Snare
            drumHits.add(DrumHit(barStart + samplesPerBeat, DrumType.SNARE, 0.90f))
            // Beat 2.5: Closed Hat
            drumHits.add(DrumHit(barStart + (samplesPerBeat * 1.5f).toInt(), DrumType.HIHAT_CLOSED, 0.55f))
            // Beat 3: Kick + Kick syncopation
            drumHits.add(DrumHit(barStart + samplesPerBeat * 2, DrumType.KICK, 0.90f))
            drumHits.add(DrumHit(barStart + (samplesPerBeat * 2.75f).toInt(), DrumType.KICK, 0.80f))
            // Beat 4: Snare + Open Hat
            drumHits.add(DrumHit(barStart + samplesPerBeat * 3, DrumType.SNARE, 0.95f))
            drumHits.add(DrumHit(barStart + (samplesPerBeat * 3.5f).toInt(), DrumType.HIHAT_OPEN, 0.70f))
        }
        val drumAudio = drumSynth.renderDrumHits(drumHits, totalSamples)

        // 2. 808 Sub Bass Track (C -> G -> A -> F progression)
        val bassPitches = FloatArray(totalSamples)
        val bassAmps = FloatArray(totalSamples)
        val bassNotes = intArrayOf(36, 43, 45, 41) // C2, G2, A2, F2
        for (bar in 0 until totalBars) {
            val barStart = bar * 4 * samplesPerBeat
            val noteFreq = MusicalScale.midiToFrequency(bassNotes[bar % bassNotes.size].toFloat())
            for (i in 0 until (samplesPerBeat * 3.8f).toInt()) {
                val idx = barStart + i
                if (idx < totalSamples) {
                    bassPitches[idx] = noteFreq
                    bassAmps[idx] = 0.90f
                }
            }
        }
        val bassAudio = tunedSynth.synthesizeMelody(bassPitches, bassAmps, InstrumentType.BASS_808)

        // 3. Erhu (二胡) Lead Melody Track (Authentic Chinese Pentatonic Gong / Yu melody)
        val erhuPitches = FloatArray(totalSamples)
        val erhuAmps = FloatArray(totalSamples)
        // Chinese Pentatonic melody: C5(72), D5(74), E5(76), G5(79), A5(81), E5(76), D5(74), C5(72)
        val erhuMelodyNotes = floatArrayOf(72f, 74f, 76f, 79f, 81f, 79f, 76f, 74f, 72f, 76f, 74f, 72f, 69f, 72f, 74f, 72f)
        val noteDurationSamples = (samplesPerBeat * 1.0f).toInt()
        for (n in erhuMelodyNotes.indices) {
            val noteStart = n * noteDurationSamples
            val freq = MusicalScale.midiToFrequency(erhuMelodyNotes[n])
            for (i in 0 until (noteDurationSamples * 0.92f).toInt()) {
                val idx = noteStart + i
                if (idx < totalSamples) {
                    erhuPitches[idx] = freq
                    erhuAmps[idx] = 0.85f
                }
            }
        }
        val erhuAudio = tunedSynth.synthesizeMelody(erhuPitches, erhuAmps, InstrumentType.ERHU)

        // 4. Guzheng (古筝) Plucks Track (Shimmering Chinese Zither arpeggios)
        val guzhengPitches = FloatArray(totalSamples)
        val guzhengAmps = FloatArray(totalSamples)
        val guzhengArp = floatArrayOf(60f, 64f, 67f, 69f, 72f, 69f, 67f, 64f)
        val guzhengStep = (samplesPerBeat * 0.5f).toInt() // 8th notes
        for (step in 0 until (totalBeats * 2)) {
            val stepStart = step * guzhengStep
            val noteMidi = guzhengArp[step % guzhengArp.size] + if ((step / 8) % 2 == 1) 2f else 0f
            val freq = MusicalScale.midiToFrequency(noteMidi)
            for (i in 0 until (guzhengStep * 0.85f).toInt()) {
                val idx = stepStart + i
                if (idx < totalSamples) {
                    guzhengPitches[idx] = freq
                    guzhengAmps[idx] = 0.70f
                }
            }
        }
        val guzhengAudio = tunedSynth.synthesizeMelody(guzhengPitches, guzhengAmps, InstrumentType.GUZHENG)

        // 5. Grand Piano Chords Track
        val pianoPitches = FloatArray(totalSamples)
        val pianoAmps = FloatArray(totalSamples)
        val pianoChords = floatArrayOf(60f, 67f, 69f, 65f) // C4, G4, A4, F4 root voices
        for (bar in 0 until totalBars) {
            val barStart = bar * 4 * samplesPerBeat
            val freq = MusicalScale.midiToFrequency(pianoChords[bar % pianoChords.size])
            // Chord hit on beat 1 and beat 2.5
            val hit1Len = (samplesPerBeat * 1.5f).toInt()
            for (i in 0 until hit1Len) {
                val idx = barStart + i
                if (idx < totalSamples) {
                    pianoPitches[idx] = freq
                    pianoAmps[idx] = 0.75f
                }
            }
            val hit2Start = barStart + (samplesPerBeat * 2.5f).toInt()
            val hit2Len = (samplesPerBeat * 1.2f).toInt()
            for (i in 0 until hit2Len) {
                val idx = hit2Start + i
                if (idx < totalSamples) {
                    pianoPitches[idx] = freq * 1.25f // Major 3rd
                    pianoAmps[idx] = 0.65f
                }
            }
        }
        val pianoAudio = tunedSynth.synthesizeMelody(pianoPitches, pianoAmps, InstrumentType.PIANO)

        // 6. Percussion & Shaker Track
        val shakerHits = mutableListOf<DrumHit>()
        val sixteenth = (samplesPerBeat * 0.25f).toInt()
        for (s in 0 until (totalBeats * 4)) {
            val pos = s * sixteenth
            val vel = if (s % 2 == 0) 0.55f else 0.35f
            shakerHits.add(DrumHit(pos, DrumType.PERCUSSION_SHAKER, vel))
        }
        val shakerAudio = drumSynth.renderDrumHits(shakerHits, totalSamples)

        return SongProject(
            title = "Silk Road Beats (Demo)",
            bpm = bpm,
            keyRootMidi = 60,
            scale = MusicalScale.CHINESE_GONG,
            isLooping = true,
            loopBars = totalBars,
            tracks = listOf(
                TrackData(
                    name = "Beatbox Drums",
                    instrument = InstrumentType.DRUM_KIT,
                    transformedAudio = drumAudio,
                    volume = 0.90f,
                    pan = 0.0f,
                    colorHex = 0xFFFF6B6B
                ),
                TrackData(
                    name = "808 Bassline",
                    instrument = InstrumentType.BASS_808,
                    transformedAudio = bassAudio,
                    volume = 0.85f,
                    pan = 0.0f,
                    pitchShift = -12,
                    colorHex = 0xFF4ECDC4
                ),
                TrackData(
                    name = "Erhu Lead (二胡)",
                    instrument = InstrumentType.ERHU,
                    transformedAudio = erhuAudio,
                    volume = 0.95f,
                    pan = -0.15f,
                    scale = MusicalScale.CHINESE_GONG,
                    colorHex = 0xFFFFBE0B
                ),
                TrackData(
                    name = "Guzheng Pluck (古筝)",
                    instrument = InstrumentType.GUZHENG,
                    transformedAudio = guzhengAudio,
                    volume = 0.70f,
                    pan = 0.35f,
                    scale = MusicalScale.CHINESE_GONG,
                    colorHex = 0xFFFB5607
                ),
                TrackData(
                    name = "Grand Piano",
                    instrument = InstrumentType.PIANO,
                    transformedAudio = pianoAudio,
                    volume = 0.65f,
                    pan = -0.30f,
                    colorHex = 0xFF8338EC
                ),
                TrackData(
                    name = "Groove Shakers",
                    instrument = InstrumentType.PERCUSSION,
                    transformedAudio = shakerAudio,
                    volume = 0.50f,
                    pan = 0.20f,
                    colorHex = 0xFF3A86FF
                )
            )
        )
    }
}
