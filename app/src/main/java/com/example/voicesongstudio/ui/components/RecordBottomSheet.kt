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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.window.Dialog
import com.example.voicesongstudio.dsp.MusicalScale
import com.example.voicesongstudio.model.TrackData
import com.example.voicesongstudio.synthesis.InstrumentType
import com.example.voicesongstudio.ui.theme.StudioAccentCyan
import com.example.voicesongstudio.ui.theme.StudioBorder
import com.example.voicesongstudio.ui.theme.StudioCardBg
import com.example.voicesongstudio.ui.theme.StudioCardElevated
import com.example.voicesongstudio.ui.theme.StudioChineseRed
import com.example.voicesongstudio.ui.theme.StudioPrimary
import com.example.voicesongstudio.ui.theme.TextPrimary
import com.example.voicesongstudio.ui.theme.TextSecondary

@Composable
fun RecordBottomSheet(
    track: TrackData,
    isRecording: Boolean,
    liveAmplitudes: List<Float>,
    recordingDurationSeconds: Float,
    recordedVoiceAudio: ShortArray?,
    previewTransformedAudio: ShortArray?,
    isPlayingPreview: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onPlayOriginalVoice: () -> Unit,
    onTransformAndPreview: (InstrumentType, MusicalScale, Int) -> Unit,
    onApplyToTrack: (ShortArray, ShortArray, InstrumentType, MusicalScale, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedInstrument by remember { mutableStateOf(track.instrument) }
    var selectedScale by remember { mutableStateOf(track.scale) }
    var octaveShift by remember { mutableIntStateOf(track.pitchShift / 12) }
    var showInstrumentDialog by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = {
        if (!isRecording) onDismiss()
    }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp)),
            color = StudioCardBg,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Record Voice for \"${track.name}\"",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Hum, beatbox, or sing to make music",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    if (!isRecording) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Waveform or Live recording area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(StudioCardElevated)
                        .border(1.dp, StudioBorder, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isRecording) {
                        LiveRecordingVisualizer(
                            amplitudes = liveAmplitudes,
                            height = 70.dp,
                            color = StudioAccentCyan
                        )
                    } else if (recordedVoiceAudio != null && recordedVoiceAudio.isNotEmpty()) {
                        WaveformVisualizer(
                            pcmData = recordedVoiceAudio,
                            height = 65.dp,
                            waveColor = StudioAccentCyan
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Ready to record • Tap red button below",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Record / Stop Button & Timer
                if (isRecording) {
                    Text(
                        text = "Recording: %.1f s".format(recordingDurationSeconds),
                        color = StudioChineseRed,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(StudioChineseRed)
                            .clickable { onStopRecording() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop Recording",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(StudioChineseRed)
                            .clickable { onStartRecording() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Start Recording",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Text(
                        text = if (recordedVoiceAudio == null) "Tap to Record" else "Tap to Re-record",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                // If audio has been recorded, show Transformation settings & Preview
                if (recordedVoiceAudio != null && recordedVoiceAudio.isNotEmpty() && !isRecording) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Target Instrument Selector Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(StudioCardElevated)
                            .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
                            .clickable { showInstrumentDialog = true }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = selectedInstrument.iconEmoji, fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Target Instrument: ${selectedInstrument.displayName}",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Tap to switch instrument",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Scale snap selection
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Scale Pitch Snapping:",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val scales = listOf(
                                MusicalScale.CHINESE_GONG to "Chinese 宫",
                                MusicalScale.CHINESE_YU to "Chinese 羽",
                                MusicalScale.MAJOR to "Major",
                                MusicalScale.NATURAL_MINOR to "Minor",
                                MusicalScale.FREE to "Free"
                            )
                            scales.forEach { (scale, label) ->
                                val isSel = selectedScale == scale
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) StudioPrimary else StudioCardElevated)
                                        .clickable {
                                            selectedScale = scale
                                            onTransformAndPreview(selectedInstrument, scale, octaveShift)
                                        }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSel) Color.White else TextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Octave Shift (-2 to +2)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Octave Shift: ${if (octaveShift > 0) "+$octaveShift" else octaveShift}",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(-2, -1, 0, 1, 2).forEach { oct ->
                                val isOctSel = octaveShift == oct
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(if (isOctSel) StudioPrimary else StudioCardElevated)
                                        .clickable {
                                            octaveShift = oct
                                            onTransformAndPreview(selectedInstrument, selectedScale, oct)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (oct > 0) "+$oct" else "$oct",
                                        color = if (isOctSel) Color.White else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action buttons: Preview Voice vs Preview Transformed Sound
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onPlayOriginalVoice,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Play Voice", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                onTransformAndPreview(selectedInstrument, selectedScale, octaveShift)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = StudioAccentCyan),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Transform", fontSize = 12.sp, color = Color.Black)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Save / Apply Button
                    Button(
                        onClick = {
                            val transformed = previewTransformedAudio ?: recordedVoiceAudio
                            onApplyToTrack(
                                recordedVoiceAudio,
                                transformed,
                                selectedInstrument,
                                selectedScale,
                                octaveShift * 12
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = StudioPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Save to Track", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showInstrumentDialog) {
        InstrumentSelectorDialog(
            currentInstrument = selectedInstrument,
            onSelectInstrument = { inst ->
                selectedInstrument = inst
                showInstrumentDialog = false
                if (recordedVoiceAudio != null && recordedVoiceAudio.isNotEmpty()) {
                    onTransformAndPreview(inst, selectedScale, octaveShift)
                }
            },
            onDismiss = { showInstrumentDialog = false }
        )
    }
}
