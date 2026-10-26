package com.example.voicesongstudio.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.voicesongstudio.dsp.MusicalScale
import com.example.voicesongstudio.synthesis.InstrumentType
import com.example.voicesongstudio.ui.components.InstrumentSelectorDialog
import com.example.voicesongstudio.ui.components.RecordBottomSheet
import com.example.voicesongstudio.ui.components.TrackItemView
import com.example.voicesongstudio.ui.components.TransportBar
import com.example.voicesongstudio.ui.theme.StudioAccentCyan
import com.example.voicesongstudio.ui.theme.StudioAccentGold
import com.example.voicesongstudio.ui.theme.StudioBorder
import com.example.voicesongstudio.ui.theme.StudioCardBg
import com.example.voicesongstudio.ui.theme.StudioCardElevated
import com.example.voicesongstudio.ui.theme.StudioDarkBg
import com.example.voicesongstudio.ui.theme.StudioPrimary
import com.example.voicesongstudio.ui.theme.TextPrimary
import com.example.voicesongstudio.ui.theme.TextSecondary
import com.example.voicesongstudio.ui.viewmodel.StudioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val project by viewModel.project.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playbackProgress by viewModel.playbackProgress.collectAsState()

    val recordingTrack by viewModel.recordingTrack.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val liveAmplitudes by viewModel.liveAmplitudes.collectAsState()
    val recordingDuration by viewModel.recordingDurationSeconds.collectAsState()
    val recordedVoiceAudio by viewModel.recordedVoiceAudio.collectAsState()
    val previewTransformedAudio by viewModel.previewTransformedAudio.collectAsState()
    val isPlayingPreview by viewModel.isPlayingPreview.collectAsState()

    val isExporting by viewModel.isExporting.collectAsState()
    val exportedFile by viewModel.exportedFile.collectAsState()

    var showAddTrackDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showScaleDialog by remember { mutableStateOf(false) }
    var showHelpBanner by remember { mutableStateOf(true) }

    // Audio permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startRecordingVoice()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(StudioPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "VoiceSong Studio",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = project.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = StudioAccentCyan
                            )
                        }
                    }
                },
                actions = {
                    // Demo Button
                    IconButton(onClick = { viewModel.loadDemoSong() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reload Demo",
                            tint = StudioAccentGold
                        )
                    }

                    // Export Button
                    IconButton(onClick = {
                        viewModel.clearExportState()
                        showExportDialog = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export Master WAV",
                            tint = StudioAccentCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StudioDarkBg
                )
            )
        },
        bottomBar = {
            TransportBar(
                isPlaying = isPlaying,
                isLooping = project.isLooping,
                bpm = project.bpm,
                scaleName = project.scale.displayName,
                onPlayPause = { viewModel.togglePlayPause() },
                onStop = { viewModel.stopPlayback() },
                onToggleLoop = { viewModel.toggleLoop() },
                onBpmChange = { viewModel.setBpm(it) },
                onClickScale = { showScaleDialog = true }
            )
        },
        containerColor = StudioDarkBg
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Helper Tutorial Banner (collapsible)
            if (showHelpBanner) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, StudioBorder, RoundedCornerShape(12.dp)),
                    color = StudioCardElevated
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = StudioAccentCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Record or beatbox with your voice -> auto-transforms into Erhu, Guzheng, Piano, Drums, Violin & Bass! Tap Play to hear the demo.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Hide",
                            color = StudioPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { showHelpBanner = false }
                        )
                    }
                }
            }

            // Track List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(project.tracks, key = { it.id }) { track ->
                    TrackItemView(
                        track = track,
                        playbackProgress = playbackProgress,
                        onVolumeChange = { viewModel.setTrackVolume(track.id, it) },
                        onPanChange = { viewModel.setTrackPan(track.id, it) },
                        onToggleMute = { viewModel.toggleTrackMute(track.id) },
                        onToggleSolo = { viewModel.toggleTrackSolo(track.id) },
                        onRecordVoice = {
                            viewModel.openRecordSheet(track)
                        },
                        onChangeInstrument = { viewModel.changeTrackInstrument(track.id, it) },
                        onDeleteTrack = { viewModel.deleteTrack(track.id) }
                    )
                }

                // Add Track Button
                item {
                    Button(
                        onClick = { showAddTrackDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StudioCardElevated),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = StudioPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add Instrument Track",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    // Add Track Instrument Dialog
    if (showAddTrackDialog) {
        InstrumentSelectorDialog(
            currentInstrument = InstrumentType.ERHU,
            onSelectInstrument = { inst ->
                viewModel.addNewTrack(inst)
                showAddTrackDialog = false
            },
            onDismiss = { showAddTrackDialog = false }
        )
    }

    // Scale Chooser Dialog
    if (showScaleDialog) {
        Dialog(onDismissRequest = { showScaleDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp)),
                color = StudioCardBg,
                tonalElevation = 6.dp
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Select Master Tuning Scale",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Snaps hummed pitches into musical keys",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    MusicalScale.values().forEach { scale ->
                        val isSel = scale == project.scale
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) StudioPrimary.copy(alpha = 0.2f) else StudioCardElevated)
                                .clickable {
                                    viewModel.setScale(scale)
                                    showScaleDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = scale.displayName,
                                color = if (isSel) StudioAccentCyan else TextPrimary,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }

    // Record Voice Bottom Sheet / Dialog
    val currentRecordingTrack = recordingTrack
    if (currentRecordingTrack != null) {
        RecordBottomSheet(
            track = currentRecordingTrack,
            isRecording = isRecording,
            liveAmplitudes = liveAmplitudes,
            recordingDurationSeconds = recordingDuration,
            recordedVoiceAudio = recordedVoiceAudio,
            previewTransformedAudio = previewTransformedAudio,
            isPlayingPreview = isPlayingPreview,
            onStartRecording = {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED

                if (hasPermission) {
                    viewModel.startRecordingVoice()
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            onStopRecording = { viewModel.stopRecordingVoice() },
            onPlayOriginalVoice = { viewModel.playOriginalVoice() },
            onTransformAndPreview = { inst, scale, oct ->
                viewModel.transformAndPreview(inst, scale, oct)
            },
            onApplyToTrack = { origVoice, transformed, inst, scale, pitchShift ->
                viewModel.applyVoiceToTrack(origVoice, transformed, inst, scale, pitchShift)
            },
            onDismiss = { viewModel.closeRecordSheet() }
        )
    }

    // Export Dialog
    if (showExportDialog) {
        ExportDialog(
            project = project,
            isExporting = isExporting,
            exportedFile = exportedFile,
            onExportMix = { title -> viewModel.exportSong(context, title) },
            onDismiss = {
                viewModel.clearExportState()
                showExportDialog = false
            }
        )
    }
}
