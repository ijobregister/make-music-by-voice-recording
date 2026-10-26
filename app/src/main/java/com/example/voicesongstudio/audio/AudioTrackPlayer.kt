package com.example.voicesongstudio.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Low latency PCM audio player using Android's native AudioTrack API.
 */
class AudioTrackPlayer(private val sampleRate: Int = 44100) {

    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null

    @Volatile
    var isPlaying: Boolean = false
        private set

    /**
     * Plays a mono 16-bit PCM buffer.
     */
    fun playMono(
        scope: CoroutineScope,
        pcmData: ShortArray,
        onProgress: (Float) -> Unit = {},
        onFinished: () -> Unit = {}
    ) {
        stop()
        if (pcmData.isEmpty()) {
            onFinished()
            return
        }

        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()
        isPlaying = true

        playbackJob = scope.launch(Dispatchers.IO) {
            val chunkSize = 1024
            var offset = 0
            val total = pcmData.size

            while (isActive && isPlaying && offset < total) {
                val toWrite = (total - offset).coerceAtMost(chunkSize)
                val written = audioTrack?.write(pcmData, offset, toWrite) ?: 0
                if (written > 0) {
                    offset += written
                    onProgress(offset.toFloat() / total)
                } else {
                    break
                }
            }

            isPlaying = false
            try {
                audioTrack?.stop()
                audioTrack?.release()
            } catch (e: Exception) {
                // ignore
            } finally {
                audioTrack = null
                onFinished()
            }
        }
    }

    fun stop() {
        isPlaying = false
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            // ignore
        } finally {
            audioTrack = null
        }
    }
}
