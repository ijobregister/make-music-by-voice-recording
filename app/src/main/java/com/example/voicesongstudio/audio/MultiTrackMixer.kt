package com.example.voicesongstudio.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.voicesongstudio.model.SongProject
import com.example.voicesongstudio.model.TrackData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Multi-track DAW playback and export mixing engine.
 */
class MultiTrackMixer(private val sampleRate: Int = 44100) {

    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null

    @Volatile
    var isPlaying: Boolean = false
        private set

    /**
     * Plays multi-track project in real-time on an IO coroutine.
     */
    fun startPlayback(
        scope: CoroutineScope,
        project: SongProject,
        onProgress: (Float) -> Unit,
        onFinished: () -> Unit
    ) {
        stopPlayback()

        val activeTracks = project.tracks.filter { it.hasAudio }
        if (activeTracks.isEmpty()) {
            onFinished()
            return
        }

        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(8192)

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
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()
        isPlaying = true

        playbackJob = scope.launch(Dispatchers.IO) {
            val hasSolo = project.tracks.any { it.isSolo }
            val audibleTracks = project.tracks.filter {
                it.hasAudio && (!hasSolo || it.isSolo) && !it.isMuted
            }

            val loopDuration = project.loopDurationSamples
            val maxTrackLen = audibleTracks.maxOfOrNull { it.transformedAudio.size } ?: loopDuration
            val totalPlaybackLength = if (project.isLooping) loopDuration else maxTrackLen

            val chunkSize = 1024 // in frames (stereo: 2048 shorts)
            val stereoPcmChunk = ShortArray(chunkSize * 2)

            var currentSampleIndex = 0

            while (isActive && isPlaying) {
                // Mix chunkSize frames
                for (frame in 0 until chunkSize) {
                    val sampleIdxInLoop = if (project.isLooping) {
                        (currentSampleIndex + frame) % loopDuration
                    } else {
                        currentSampleIndex + frame
                    }

                    if (!project.isLooping && sampleIdxInLoop >= maxTrackLen) {
                        // End of non-looping song
                        isPlaying = false
                        break
                    }

                    var leftAccum = 0.0f
                    var rightAccum = 0.0f

                    for (track in audibleTracks) {
                        val trackAudio = track.transformedAudio
                        val trackSampleIdx = sampleIdxInLoop % trackAudio.size
                        val rawSample = trackAudio[trackSampleIdx].toFloat() / 32768.0f

                        // Constant-power panning
                        val panAngle = (track.pan + 1.0f) * (PI.toFloat() / 4.0f) // 0 to PI/2
                        val leftGain = cos(panAngle) * track.volume
                        val rightGain = sin(panAngle) * track.volume

                        leftAccum += rawSample * leftGain
                        rightAccum += rawSample * rightGain
                    }

                    // Soft saturation limiter to eliminate clipping
                    val limitedLeft = tanh(leftAccum)
                    val limitedRight = tanh(rightAccum)

                    stereoPcmChunk[frame * 2] = (limitedLeft * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()
                    stereoPcmChunk[frame * 2 + 1] = (limitedRight * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()
                }

                if (!isPlaying) break

                val written = audioTrack?.write(stereoPcmChunk, 0, chunkSize * 2) ?: 0
                if (written > 0) {
                    currentSampleIndex += chunkSize
                    val progress = (currentSampleIndex % totalPlaybackLength).toFloat() / totalPlaybackLength
                    onProgress(progress)
                }
            }

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

    fun stopPlayback() {
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

    /**
     * Renders all tracks into an interleaved stereo 16-bit PCM buffer and writes to a WAV file.
     */
    fun renderToWav(project: SongProject, destinationFile: File) {
        val hasSolo = project.tracks.any { it.isSolo }
        val audibleTracks = project.tracks.filter {
            it.hasAudio && (!hasSolo || it.isSolo) && !it.isMuted
        }

        val totalFrames = if (project.isLooping) {
            project.loopDurationSamples
        } else {
            audibleTracks.maxOfOrNull { it.transformedAudio.size } ?: (sampleRate * 4)
        }

        val stereoPcm = ShortArray(totalFrames * 2)

        for (frame in 0 until totalFrames) {
            var leftAccum = 0.0f
            var rightAccum = 0.0f

            for (track in audibleTracks) {
                val trackAudio = track.transformedAudio
                val sampleIdx = frame % trackAudio.size
                val rawSample = trackAudio[sampleIdx].toFloat() / 32768.0f

                val panAngle = (track.pan + 1.0f) * (PI.toFloat() / 4.0f)
                val leftGain = cos(panAngle) * track.volume
                val rightGain = sin(panAngle) * track.volume

                leftAccum += rawSample * leftGain
                rightAccum += rawSample * rightGain
            }

            val limitedLeft = tanh(leftAccum)
            val limitedRight = tanh(rightAccum)

            stereoPcm[frame * 2] = (limitedLeft * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()
            stereoPcm[frame * 2 + 1] = (limitedRight * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()
        }

        WavFileHelper.writeStereoWavFile(destinationFile, stereoPcm, sampleRate)
    }
}
