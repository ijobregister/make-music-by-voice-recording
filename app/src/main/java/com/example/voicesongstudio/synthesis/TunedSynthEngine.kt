package com.example.voicesongstudio.synthesis

import com.example.voicesongstudio.dsp.FormantFilter
import java.util.Random
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.tanh

/**
 * High quality melodic synthesizer implementing physical modeling, formant filtering,
 * and wave synthesis for Chinese and Western instruments.
 */
class TunedSynthEngine(private val sampleRate: Int = 44100) {

    private val random = Random(1234)

    /**
     * Synthesizes audio samples for a given pitch contour and amplitude envelope.
     * @param pitches Array of frequency in Hz for each sample (or chunk)
     * @param amplitudes Array of amplitude (0.0 to 1.0)
     * @param instrument Instrument to render
     */
    fun synthesizeMelody(
        pitches: FloatArray,
        amplitudes: FloatArray,
        instrument: InstrumentType
    ): ShortArray {
        val totalSamples = pitches.size
        val output = FloatArray(totalSamples)

        when (instrument) {
            InstrumentType.ERHU -> synthesizeErhu(pitches, amplitudes, output)
            InstrumentType.GUZHENG -> synthesizeGuzheng(pitches, amplitudes, output)
            InstrumentType.DIZI -> synthesizeDizi(pitches, amplitudes, output)
            InstrumentType.PIPA -> synthesizePipa(pitches, amplitudes, output)
            InstrumentType.PIANO -> synthesizePiano(pitches, amplitudes, output)
            InstrumentType.VIOLIN -> synthesizeViolin(pitches, amplitudes, output)
            InstrumentType.GUITAR_ACOUSTIC -> synthesizeGuitar(pitches, amplitudes, output)
            InstrumentType.SAXOPHONE -> synthesizeSaxophone(pitches, amplitudes, output)
            InstrumentType.BASS_808 -> synthesize808Bass(pitches, amplitudes, output)
            InstrumentType.BASS_SYNTH -> synthesizeSynthBass(pitches, amplitudes, output)
            else -> synthesizePiano(pitches, amplitudes, output)
        }

        // Convert to 16-bit PCM with soft limiting
        val result = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val sample = tanh(output[i] * 1.0f)
            result[i] = (sample * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()
        }
        return result
    }

    /**
     * Erhu (Chinese 2-string bowed fiddle):
     * Bowed friction sawtooth wave + nasal python-skin formant filtering + expressive vibrato.
     */
    private fun synthesizeErhu(pitches: FloatArray, amplitudes: FloatArray, output: FloatArray) {
        val filter1 = FormantFilter().apply { setBandPass(1050f, 4.0f, sampleRate) }
        val filter2 = FormantFilter().apply { setBandPass(3200f, 3.0f, sampleRate) }
        val lowPass = FormantFilter().apply { setLowPass(4800f, 0.707f, sampleRate) }

        var phase = 0.0
        var vibratoPhase = 0.0

        for (i in pitches.indices) {
            val baseFreq = pitches[i]
            val amp = amplitudes[i]
            if (baseFreq < 40f || amp < 0.01f) continue

            // 5.2 Hz Erhu vibrato (~25 cents)
            vibratoPhase += 2.0 * PI * 5.2 / sampleRate
            val vibrato = 1.0f + 0.015f * sin(vibratoPhase).toFloat()
            val freq = baseFreq * vibrato

            phase += freq / sampleRate
            if (phase >= 1.0) phase -= 1.0

            // Bowed string stick-slip non-linear waveform
            val saw = (2.0 * phase - 1.0).toFloat()
            val bowed = tanh(saw * 2.2f)

            // Resonant python-skin chamber formants
            val formantSound = filter1.process(bowed) * 0.7f + filter2.process(bowed) * 0.35f
            val mixed = lowPass.process(bowed * 0.4f + formantSound * 0.8f)

            output[i] = mixed * amp * 1.25f
        }
    }

    /**
     * Guzheng (Traditional Chinese 21-string plucked zither):
     * Karplus-Strong plucked physical model with metallic harmonics and shimmering decay.
     */
    private fun synthesizeGuzheng(pitches: FloatArray, amplitudes: FloatArray, output: FloatArray) {
        synthesizePluckedString(
            pitches, amplitudes, output,
            damping = 0.994f,
            brightness = 0.75f,
            bodyResonanceFreq = 420f,
            pluckSharpness = 1.4f
        )
    }

    /**
     * Pipa (Chinese plucked lute):
     * Crisp, percussive plucked string with fast decay.
     */
    private fun synthesizePipa(pitches: FloatArray, amplitudes: FloatArray, output: FloatArray) {
        synthesizePluckedString(
            pitches, amplitudes, output,
            damping = 0.988f,
            brightness = 0.90f,
            bodyResonanceFreq = 580f,
            pluckSharpness = 1.8f
        )
    }

    /**
     * Acoustic Guitar:
     * Karplus-Strong string synthesis with warm hollow wooden body cavity.
     */
    private fun synthesizeGuitar(pitches: FloatArray, amplitudes: FloatArray, output: FloatArray) {
        synthesizePluckedString(
            pitches, amplitudes, output,
            damping = 0.996f,
            brightness = 0.50f,
            bodyResonanceFreq = 180f,
            pluckSharpness = 1.0f
        )
    }

    /**
     * Plucked string Karplus-Strong physical modeling algorithm with body resonance.
     */
    private fun synthesizePluckedString(
        pitches: FloatArray,
        amplitudes: FloatArray,
        output: FloatArray,
        damping: Float,
        brightness: Float,
        bodyResonanceFreq: Float,
        pluckSharpness: Float
    ) {
        val maxDelay = (sampleRate / 40.0f).toInt()
        val delayLine = FloatArray(maxDelay)
        var writeIdx = 0

        val bodyFilter = FormantFilter().apply {
            setBandPass(bodyResonanceFreq, 2.5f, sampleRate)
        }

        var prevAmp = 0.0f

        for (i in pitches.indices) {
            val freq = pitches[i].coerceIn(50f, 2500f)
            val amp = amplitudes[i]

            val delayLength = (sampleRate / freq).toInt().coerceIn(2, maxDelay - 2)

            // Pluck excitation when amplitude rises sharply (note onset)
            if (amp > prevAmp + 0.08f && amp > 0.06f) {
                for (d in 0 until delayLength) {
                    val noise = (random.nextFloat() * 2.0f - 1.0f) * amp * pluckSharpness
                    delayLine[d] = noise
                }
            }

            // Karplus-Strong delay line read & low-pass feedback
            var readIdx = writeIdx - delayLength
            if (readIdx < 0) readIdx += delayLine.size
            var nextReadIdx = readIdx + 1
            if (nextReadIdx >= delayLine.size) nextReadIdx = 0

            val currentSample = delayLine[readIdx]
            val nextSample = delayLine[nextReadIdx]

            // Two-point lowpass filter with damping
            val filtered = (currentSample * (1.0f - brightness) + nextSample * brightness) * damping
            delayLine[writeIdx] = filtered

            writeIdx++
            if (writeIdx >= delayLine.size) writeIdx = 0

            val body = bodyFilter.process(currentSample)
            output[i] += (currentSample * 0.7f + body * 0.45f) * amp.coerceAtLeast(0.3f)
            prevAmp = amp
        }
    }

    /**
     * Dizi (Chinese Bamboo Flute):
     * Pure air tone + Dimo buzzing membrane non-linear harmonic saturation + breath noise.
     */
    private fun synthesizeDizi(pitches: FloatArray, amplitudes: FloatArray, output: FloatArray) {
        var phase = 0.0
        var vibratoPhase = 0.0
        val breathFilter = FormantFilter().apply { setBandPass(3500f, 2.0f, sampleRate) }

        for (i in pitches.indices) {
            val baseFreq = pitches[i]
            val amp = amplitudes[i]
            if (baseFreq < 50f || amp < 0.01f) continue

            // 4.8 Hz gentle flute vibrato
            vibratoPhase += 2.0 * PI * 4.8 / sampleRate
            val vibrato = 1.0f + 0.01f * sin(vibratoPhase).toFloat()
            val freq = baseFreq * vibrato

            phase += 2.0 * PI * freq / sampleRate
            val sineVal = sin(phase).toFloat()

            // Dimo membrane buzz: asymmetric polynomial non-linearity creating 2nd & 3rd harmonics
            val dimoBuzz = sineVal + 0.28f * sin(2.0 * phase).toFloat() + 0.18f * sin(3.0 * phase).toFloat()

            // Breath air noise
            val breath = breathFilter.process(random.nextFloat() * 2.0f - 1.0f) * 0.12f

            output[i] = (dimoBuzz + breath) * amp * 1.1f
        }
    }

    /**
     * Grand Piano:
     * Concert multi-harmonic synthesis with dynamic hammer strike velocity and unison beating.
     */
    private fun synthesizePiano(pitches: FloatArray, amplitudes: FloatArray, output: FloatArray) {
        var phase1 = 0.0
        var phase2 = 0.0
        var prevAmp = 0.0f
        var strikeEnvelope = 0.0f

        for (i in pitches.indices) {
            val freq = pitches[i]
            val amp = amplitudes[i]
            if (freq < 30f || amp < 0.01f) {
                prevAmp = amp
                strikeEnvelope *= 0.999f
                continue
            }

            // Note onset strike
            if (amp > prevAmp + 0.07f) {
                strikeEnvelope = 1.0f
            } else {
                strikeEnvelope *= 0.9997f
            }

            // Unison pair with slight 0.4 Hz beating for rich acoustic resonance
            phase1 += 2.0 * PI * freq / sampleRate
            phase2 += 2.0 * PI * (freq + 0.4) / sampleRate

            // Multi-harmonic spectrum
            val h1 = sin(phase1).toFloat()
            val h2 = 0.55f * sin(2.0 * phase1).toFloat()
            val h3 = 0.25f * sin(3.0 * phase1).toFloat()
            val h4 = 0.12f * sin(4.0 * phase1).toFloat()
            val h5 = 0.06f * sin(5.0 * phase1).toFloat()

            val string2 = sin(phase2).toFloat() * 0.85f

            val harmonicSum = (h1 + h2 + h3 + h4 + h5 + string2) * 0.45f
            output[i] = harmonicSum * amp * (0.6f + 0.4f * strikeEnvelope)
            prevAmp = amp
        }
    }

    /**
     * Violin / Strings:
     * Sawtooth wave + violin wood cavity formant filtering (280 Hz, 460 Hz, 2800 Hz) + vibrato.
     */
    private fun synthesizeViolin(pitches: FloatArray, amplitudes: FloatArray, output: FloatArray) {
        val airFilter = FormantFilter().apply { setBandPass(280f, 3.5f, sampleRate) }
        val woodFilter = FormantFilter().apply { setBandPass(460f, 3.0f, sampleRate) }
        val bridgeFilter = FormantFilter().apply { setBandPass(2800f, 2.5f, sampleRate) }
        val lowPass = FormantFilter().apply { setLowPass(5200f, 0.707f, sampleRate) }

        var phase = 0.0
        var vibratoPhase = 0.0

        for (i in pitches.indices) {
            val baseFreq = pitches[i]
            val amp = amplitudes[i]
            if (baseFreq < 45f || amp < 0.01f) continue

            vibratoPhase += 2.0 * PI * 5.8 / sampleRate
            val vibrato = 1.0f + 0.018f * sin(vibratoPhase).toFloat()
            val freq = baseFreq * vibrato

            phase += freq / sampleRate
            if (phase >= 1.0) phase -= 1.0

            val saw = (2.0 * phase - 1.0).toFloat()
            val body = airFilter.process(saw) * 0.4f + woodFilter.process(saw) * 0.5f + bridgeFilter.process(saw) * 0.35f
            val combined = lowPass.process(saw * 0.35f + body)

            output[i] = combined * amp * 1.2f
        }
    }

    /**
     * Saxophone:
     * Reed pressure non-linearity + horn bell resonance.
     */
    private fun synthesizeSaxophone(pitches: FloatArray, amplitudes: FloatArray, output: FloatArray) {
        val bellFilter = FormantFilter().apply { setBandPass(1400f, 2.5f, sampleRate) }
        var phase = 0.0

        for (i in pitches.indices) {
            val freq = pitches[i]
            val amp = amplitudes[i]
            if (freq < 45f || amp < 0.01f) continue

            phase += 2.0 * PI * freq / sampleRate
            // Reed square/sine hybrid
            val reed = sin(phase).toFloat()
            val reedSaturated = tanh(reed * 3.0f)

            val bell = bellFilter.process(reedSaturated)
            output[i] = (reedSaturated * 0.6f + bell * 0.5f) * amp
        }
    }

    /**
     * 808 Sub Bass:
     * Deep sub-sine wave + warm harmonic saturation.
     */
    private fun synthesize808Bass(pitches: FloatArray, amplitudes: FloatArray, output: FloatArray) {
        var phase = 0.0
        val lpFilter = FormantFilter().apply { setLowPass(250f, 1.2f, sampleRate) }

        for (i in pitches.indices) {
            val rawFreq = pitches[i]
            val amp = amplitudes[i]
            if (rawFreq < 30f || amp < 0.01f) continue

            // Drop down into sub bass frequencies (35 Hz to 130 Hz)
            var subFreq = rawFreq
            while (subFreq > 130f) subFreq *= 0.5f
            while (subFreq < 35f) subFreq *= 2.0f

            phase += 2.0 * PI * subFreq / sampleRate
            val subSine = sin(phase).toFloat()

            // 808 saturation
            val saturated = tanh(subSine * 2.2f)
            val filtered = lpFilter.process(saturated)

            output[i] = filtered * amp * 1.4f
        }
    }

    /**
     * Synth Funk Bass:
     * Punchy resonant sawtooth bassline.
     */
    private fun synthesizeSynthBass(pitches: FloatArray, amplitudes: FloatArray, output: FloatArray) {
        var phase = 0.0
        val lpFilter = FormantFilter().apply { setLowPass(650f, 2.8f, sampleRate) }

        for (i in pitches.indices) {
            val rawFreq = pitches[i]
            val amp = amplitudes[i]
            if (rawFreq < 30f || amp < 0.01f) continue

            var bassFreq = rawFreq
            while (bassFreq > 200f) bassFreq *= 0.5f
            while (bassFreq < 40f) bassFreq *= 2.0f

            phase += bassFreq / sampleRate
            if (phase >= 1.0) phase -= 1.0

            val saw = (2.0 * phase - 1.0).toFloat()
            val filtered = lpFilter.process(saw)

            output[i] = filtered * amp * 1.3f
        }
    }
}
