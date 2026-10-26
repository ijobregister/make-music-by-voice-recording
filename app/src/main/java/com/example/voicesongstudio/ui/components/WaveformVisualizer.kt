package com.example.voicesongstudio.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.voicesongstudio.ui.theme.PlayheadColor
import com.example.voicesongstudio.ui.theme.WaveformColor
import kotlin.math.abs

/**
 * High performance canvas waveform visualizer.
 */
@Composable
fun WaveformVisualizer(
    pcmData: ShortArray,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
    waveColor: Color = WaveformColor,
    playbackProgress: Float = -1.0f // 0.0 to 1.0 (or -1 if not playing)
) {
    // Downsample PCM buffer to 120 bars for smooth rendering
    val peaks = remember(pcmData) {
        if (pcmData.isEmpty()) {
            FloatArray(0)
        } else {
            val barCount = 120
            val chunkSize = (pcmData.size / barCount).coerceAtLeast(1)
            val result = FloatArray(barCount)
            for (i in 0 until barCount) {
                val start = i * chunkSize
                if (start >= pcmData.size) break
                val end = (start + chunkSize).coerceAtMost(pcmData.size)
                var maxVal = 0
                for (j in start until end) {
                    val absVal = abs(pcmData[j].toInt())
                    if (absVal > maxVal) maxVal = absVal
                }
                result[i] = (maxVal / 32768.0f).coerceIn(0.05f, 1.0f)
            }
            result
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val width = size.width
        val canvasHeight = size.height
        val centerY = canvasHeight / 2.0f

        if (peaks.isEmpty()) {
            // Draw center idle line
            drawLine(
                color = waveColor.copy(alpha = 0.3f),
                start = Offset(0f, centerY),
                end = Offset(width, centerY),
                strokeWidth = 2f
            )
            return@Canvas
        }

        val step = width / peaks.size
        val barWidth = (step * 0.7f).coerceIn(2f, 8f)

        for (i in peaks.indices) {
            val x = i * step + step / 2.0f
            val barHeight = (peaks[i] * (canvasHeight * 0.88f)).coerceAtLeast(4f)
            val topY = centerY - barHeight / 2.0f
            val bottomY = centerY + barHeight / 2.0f

            val isPlayed = playbackProgress >= 0f && (x / width) <= playbackProgress
            val color = if (isPlayed) PlayheadColor else waveColor

            drawLine(
                color = color,
                start = Offset(x, topY),
                end = Offset(x, bottomY),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }

        // Draw vertical playhead line if active
        if (playbackProgress in 0.0f..1.0f) {
            val playheadX = width * playbackProgress
            drawLine(
                color = PlayheadColor,
                start = Offset(playheadX, 0f),
                end = Offset(playheadX, canvasHeight),
                strokeWidth = 3f
            )
        }
    }
}

/**
 * Animated real-time waveform for live microphone recording.
 */
@Composable
fun LiveRecordingVisualizer(
    amplitudes: List<Float>,
    modifier: Modifier = Modifier,
    height: Dp = 64.dp,
    color: Color = Color(0xFF00CEC9)
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val width = size.width
        val canvasHeight = size.height
        val centerY = canvasHeight / 2.0f

        if (amplitudes.isEmpty()) return@Canvas

        val barCount = amplitudes.size
        val step = width / barCount.coerceAtLeast(1)
        val barWidth = (step * 0.65f).coerceIn(3f, 10f)

        for (i in amplitudes.indices) {
            val x = i * step + step / 2.0f
            val amp = amplitudes[i].coerceIn(0.05f, 1.0f)
            val barH = amp * (canvasHeight * 0.9f)

            drawLine(
                color = color.copy(alpha = (0.4f + 0.6f * (i.toFloat() / barCount))),
                start = Offset(x, centerY - barH / 2.0f),
                end = Offset(x, centerY + barH / 2.0f),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }
    }
}
