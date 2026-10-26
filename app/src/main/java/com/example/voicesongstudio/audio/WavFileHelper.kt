package com.example.voicesongstudio.audio

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Utility to encode and decode standard 16-bit PCM RIFF WAV audio files.
 */
object WavFileHelper {

    const val SAMPLE_RATE_44100 = 44100

    /**
     * Converts a ShortArray of mono audio to a WAV byte array.
     */
    fun encodeMonoWav(pcmData: ShortArray, sampleRate: Int = SAMPLE_RATE_44100): ByteArray {
        val byteBuffer = ByteBuffer.allocate(pcmData.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (sample in pcmData) {
            byteBuffer.putShort(sample)
        }
        val pcmBytes = byteBuffer.array()
        return createWavHeaderAndData(pcmBytes, channels = 1, sampleRate = sampleRate, bitsPerSample = 16)
    }

    /**
     * Converts interleaved stereo ShortArray (L, R, L, R...) to a WAV byte array.
     */
    fun encodeStereoWav(pcmData: ShortArray, sampleRate: Int = SAMPLE_RATE_44100): ByteArray {
        val byteBuffer = ByteBuffer.allocate(pcmData.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (sample in pcmData) {
            byteBuffer.putShort(sample)
        }
        val pcmBytes = byteBuffer.array()
        return createWavHeaderAndData(pcmBytes, channels = 2, sampleRate = sampleRate, bitsPerSample = 16)
    }

    /**
     * Writes mono PCM audio to a WAV file.
     */
    fun writeMonoWavFile(file: File, pcmData: ShortArray, sampleRate: Int = SAMPLE_RATE_44100) {
        val wavBytes = encodeMonoWav(pcmData, sampleRate)
        FileOutputStream(file).use { it.write(wavBytes) }
    }

    /**
     * Writes stereo PCM audio to a WAV file.
     */
    fun writeStereoWavFile(file: File, pcmData: ShortArray, sampleRate: Int = SAMPLE_RATE_44100) {
        val wavBytes = encodeStereoWav(pcmData, sampleRate)
        FileOutputStream(file).use { it.write(wavBytes) }
    }

    /**
     * Decodes a mono 16-bit WAV input stream into a ShortArray.
     */
    fun decodeWav(inputStream: InputStream): ShortArray {
        val bytes = inputStream.readBytes()
        if (bytes.size < 44) return ShortArray(0)

        // Find "data" chunk
        var dataOffset = 44
        for (i in 36 until bytes.size - 4) {
            if (bytes[i] == 'd'.code.toByte() &&
                bytes[i + 1] == 'a'.code.toByte() &&
                bytes[i + 2] == 't'.code.toByte() &&
                bytes[i + 3] == 'a'.code.toByte()
            ) {
                dataOffset = i + 8
                break
            }
        }

        val dataSize = bytes.size - dataOffset
        val sampleCount = dataSize / 2
        val shortBuffer = ByteBuffer.wrap(bytes, dataOffset, dataSize).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val result = ShortArray(sampleCount)
        shortBuffer.get(result)
        return result
    }

    private fun createWavHeaderAndData(
        pcmBytes: ByteArray,
        channels: Short,
        sampleRate: Int,
        bitsPerSample: Short
    ): ByteArray {
        val totalAudioLen = pcmBytes.size
        val totalDataLen = totalAudioLen + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = (channels * bitsPerSample / 8).toShort()

        val header = ByteArray(44)
        val buf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

        // RIFF chunk descriptor
        buf.put("RIFF".toByteArray())
        buf.putInt(totalDataLen)
        buf.put("WAVE".toByteArray())

        // "fmt " sub-chunk
        buf.put("fmt ".toByteArray())
        buf.putInt(16) // Subchunk1Size (16 for PCM)
        buf.putShort(1) // AudioFormat (1 for PCM)
        buf.putShort(channels)
        buf.putInt(sampleRate)
        buf.putInt(byteRate)
        buf.putShort(blockAlign)
        buf.putShort(bitsPerSample)

        // "data" sub-chunk
        buf.put("data".toByteArray())
        buf.putInt(totalAudioLen)

        val out = ByteArrayOutputStream(44 + totalAudioLen)
        out.write(header)
        out.write(pcmBytes)
        return out.toByteArray()
    }
}
