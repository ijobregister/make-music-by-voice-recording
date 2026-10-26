package com.example.voicesongstudio.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * High quality low-latency microphone audio recorder with live amplitude callback.
 */
class AudioRecorder(private val sampleRate: Int = 44100) {

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    ).coerceAtLeast(4096)

    @Volatile
    var isRecording: Boolean = false
        private set

    /**
     * Starts recording audio from the microphone on an IO coroutine.
     * @param onAmplitudeUpdate Callback invoked with normalized amplitude (0.0 to 1.0) for waveform visualization
     */
    @SuppressLint("MissingPermission")
    fun startRecording(
        scope: CoroutineScope,
        onAmplitudeUpdate: (Float) -> Unit,
        onComplete: (ShortArray) -> Unit
    ) {
        if (isRecording) return

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize * 2
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = null
                return
            }

            audioRecord?.startRecording()
            isRecording = true

            recordingJob = scope.launch(Dispatchers.IO) {
                val pcmBuffer = ShortArray(1024)
                val byteStream = ByteArrayOutputStream()
                val chunkByteBuffer = ByteBuffer.allocate(pcmBuffer.size * 2).order(ByteOrder.LITTLE_ENDIAN)

                while (isActive && isRecording) {
                    val readSamples = audioRecord?.read(pcmBuffer, 0, pcmBuffer.size) ?: 0
                    if (readSamples > 0) {
                        chunkByteBuffer.clear()
                        var sumSquares = 0.0
                        for (i in 0 until readSamples) {
                            val sample = pcmBuffer[i]
                            chunkByteBuffer.putShort(sample)
                            sumSquares += sample * sample
                        }
                        byteStream.write(chunkByteBuffer.array(), 0, readSamples * 2)

                        // Calculate RMS amplitude for live waveform display
                        val rms = sqrt(sumSquares / readSamples).toFloat()
                        val normalizedAmp = (rms / 12000.0f).coerceIn(0.0f, 1.0f)
                        onAmplitudeUpdate(normalizedAmp)
                    }
                }

                // Finalize recorded PCM bytes into ShortArray
                val rawBytes = byteStream.toByteArray()
                val shortCount = rawBytes.size / 2
                val shortBuf = ByteBuffer.wrap(rawBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                val finalPcm = ShortArray(shortCount)
                shortBuf.get(finalPcm)

                onComplete(finalPcm)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isRecording = false
            audioRecord?.release()
            audioRecord = null
        }
    }

    fun stopRecording() {
        if (!isRecording) return
        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            audioRecord = null
            recordingJob?.cancel()
            recordingJob = null
        }
    }
}
