package com.example.voicesongstudio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voicesongstudio.model.TrackData
import com.example.voicesongstudio.synthesis.InstrumentType
import com.example.voicesongstudio.ui.theme.StudioBorder
import com.example.voicesongstudio.ui.theme.StudioCardBg
import com.example.voicesongstudio.ui.theme.StudioCardElevated
import com.example.voicesongstudio.ui.theme.StudioPrimary
import com.example.voicesongstudio.ui.theme.TextPrimary
import com.example.voicesongstudio.ui.theme.TextSecondary
import com.example.voicesongstudio.ui.theme.TextTertiary
import kotlin.math.roundToInt

@Composable
fun TrackItemView(
    track: TrackData,
    playbackProgress: Float,
    onVolumeChange: (Float) -> Unit,
    onPanChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onToggleSolo: () -> Unit,
    onRecordVoice: () -> Unit,
    onChangeInstrument: (InstrumentType) -> Unit,
    onDeleteTrack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showInstrumentPicker by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, StudioBorder, RoundedCornerShape(16.dp)),
        color = StudioCardBg,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Track Header: Color Dot + Title + Instrument Badge + Record Voice + Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Color Dot
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(track.colorHex))
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Track Name
                Text(
                    text = track.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // Instrument Badge (Click to Change)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(StudioCardElevated)
                        .border(1.dp, StudioBorder, RoundedCornerShape(20.dp))
                        .clickable { showInstrumentPicker = true }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = track.instrument.iconEmoji, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = track.instrument.displayName,
                        fontSize = 12.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Record Voice Button
                IconButton(
                    onClick = onRecordVoice,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(StudioPrimary.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Record Voice for Track",
                        tint = StudioPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Delete Track Button
                IconButton(
                    onClick = onDeleteTrack,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Track",
                        tint = TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Waveform Visualizer for Track Audio
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(StudioCardElevated)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                if (track.hasAudio) {
                    WaveformVisualizer(
                        pcmData = track.transformedAudio,
                        waveColor = Color(track.colorHex),
                        playbackProgress = playbackProgress,
                        height = 36.dp
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No audio recorded yet • Tap Mic to record voice",
                            color = TextTertiary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Controls Strip: Mute (M), Solo (S), Volume Fader, Pan
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Mute Button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (track.isMuted) Color(0xFFD63031) else StudioCardElevated)
                        .clickable { onToggleMute() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "M",
                        color = if (track.isMuted) Color.White else TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Solo Button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (track.isSolo) Color(0xFFFDCB6E) else StudioCardElevated)
                        .clickable { onToggleSolo() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "S",
                        color = if (track.isSolo) Color.Black else TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Volume Slider
                Icon(
                    imageVector = if (track.volume > 0f) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                    contentDescription = "Volume",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )

                Slider(
                    value = track.volume,
                    onValueChange = onVolumeChange,
                    valueRange = 0.0f..1.2f,
                    modifier = Modifier.weight(1.0f),
                    colors = SliderDefaults.colors(
                        thumbColor = StudioPrimary,
                        activeTrackColor = StudioPrimary,
                        inactiveTrackColor = StudioCardElevated
                    )
                )

                Text(
                    text = "${(track.volume * 100).roundToInt()}%",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.width(36.dp)
                )

                // Pan Indicator
                val panLabel = when {
                    track.pan < -0.1f -> "L ${(track.pan * -100).roundToInt()}"
                    track.pan > 0.1f -> "R ${(track.pan * 100).roundToInt()}"
                    else -> "C"
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(StudioCardElevated)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = panLabel,
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showInstrumentPicker) {
        InstrumentSelectorDialog(
            currentInstrument = track.instrument,
            onSelectInstrument = { newInstrument ->
                onChangeInstrument(newInstrument)
                showInstrumentPicker = false
            },
            onDismiss = { showInstrumentPicker = false }
        )
    }
}
