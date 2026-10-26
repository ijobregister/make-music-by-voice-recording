package com.example.voicesongstudio.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicesongstudio.audio.AudioRecorder
import com.example.voicesongstudio.audio.AudioTrackPlayer
import com.example.voicesongstudio.audio.MultiTrackMixer
import com.example.voicesongstudio.dsp.MusicalScale
import com.example.voicesongstudio.model.DemoSongFactory
import com.example.voicesongstudio.model.SongProject
import com.example.voicesongstudio.model.TrackData
import com.example.voicesongstudio.synthesis.InstrumentType
import com.example.voicesongstudio.synthesis.VoiceTransformer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StudioViewModel : ViewModel() {

    private val multiTrackMixer = MultiTrackMixer()
    private val audioRecorder = AudioRecorder()
    private val previewPlayer = AudioTrackPlayer()
    private val voiceTransformer = VoiceTransformer()

    private val _project = MutableStateFlow(DemoSongFactory.createDemoSong())
    val project: StateFlow<SongProject> = _project.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0.0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    // Recording Sheet State
    private val _recordingTrack = MutableStateFlow<TrackData?>(null)
    val recordingTrack: StateFlow<TrackData?> = _recordingTrack.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _liveAmplitudes = MutableStateFlow<List<Float>>(emptyList())
    val liveAmplitudes: StateFlow<List<Float>> = _liveAmplitudes.asStateFlow()

    private val _recordingDurationSeconds = MutableStateFlow(0.0f)
    val recordingDurationSeconds: StateFlow<Float> = _recordingDurationSeconds.asStateFlow()

    private val _recordedVoiceAudio = MutableStateFlow<ShortArray?>(null)
    val recordedVoiceAudio: StateFlow<ShortArray?> = _recordedVoiceAudio.asStateFlow()

    private val _previewTransformedAudio = MutableStateFlow<ShortArray?>(null)
    val previewTransformedAudio: StateFlow<ShortArray?> = _previewTransformedAudio.asStateFlow()

    private val _isPlayingPreview = MutableStateFlow(false)
    val isPlayingPreview: StateFlow<Boolean> = _isPlayingPreview.asStateFlow()

    // Export State
    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportedFile = MutableStateFlow<File?>(null)
    val exportedFile: StateFlow<File?> = _exportedFile.asStateFlow()

    private var recordTimerJob: Job? = null

    // Playback Controls
    fun togglePlayPause() {
        if (_isPlaying.value) {
            stopPlayback()
        } else {
            startPlayback()
        }
    }

    fun startPlayback() {
        _isPlaying.value = true
        multiTrackMixer.startPlayback(
            scope = viewModelScope,
            project = _project.value,
            onProgress = { progress ->
                _playbackProgress.value = progress
            },
            onFinished = {
                _isPlaying.value = false
                _playbackProgress.value = 0.0f
            }
        )
    }

    fun stopPlayback() {
        multiTrackMixer.stopPlayback()
        _isPlaying.value = false
        _playbackProgress.value = 0.0f
    }

    fun toggleLoop() {
        _project.update { it.copy(isLooping = !it.isLooping) }
    }

    fun setBpm(newBpm: Int) {
        _project.update { it.copy(bpm = newBpm) }
    }

    fun setScale(newScale: MusicalScale) {
        _project.update { it.copy(scale = newScale) }
    }

    // Track Controls
    fun setTrackVolume(trackId: String, volume: Float) {
        _project.update { p ->
            p.copy(tracks = p.tracks.map { if (it.id == trackId) it.copy(volume = volume) else it })
        }
    }

    fun setTrackPan(trackId: String, pan: Float) {
        _project.update { p ->
            p.copy(tracks = p.tracks.map { if (it.id == trackId) it.copy(pan = pan) else it })
        }
    }

    fun toggleTrackMute(trackId: String) {
        _project.update { p ->
            p.copy(tracks = p.tracks.map { if (it.id == trackId) it.copy(isMuted = !it.isMuted) else it })
        }
    }

    fun toggleTrackSolo(trackId: String) {
        _project.update { p ->
            p.copy(tracks = p.tracks.map { if (it.id == trackId) it.copy(isSolo = !it.isSolo) else it })
        }
    }

    fun changeTrackInstrument(trackId: String, instrument: InstrumentType) {
        val currentTrack = _project.value.tracks.find { it.id == trackId } ?: return
        _project.update { p ->
            p.copy(tracks = p.tracks.map { if (it.id == trackId) it.copy(instrument = instrument) else it })
        }
        // If track already has recorded voice, re-transform with new instrument
        if (currentTrack.originalVoiceAudio.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.Default) {
                val transformed = voiceTransformer.transformVoice(
                    voiceAudio = currentTrack.originalVoiceAudio,
                    instrument = instrument,
                    scale = currentTrack.scale,
                    rootNoteMidi = _project.value.keyRootMidi,
                    octaveShift = currentTrack.pitchShift / 12
                )
                _project.update { p ->
                    p.copy(tracks = p.tracks.map {
                        if (it.id == trackId) it.copy(transformedAudio = transformed) else it
                    })
                }
            }
        }
    }

    fun addNewTrack(instrument: InstrumentType) {
        val count = _project.value.tracks.size + 1
        val colorHexes = listOf(
            0xFF6C5CE7, 0xFF00CEC9, 0xFFFDCB6E, 0xFFFF7675, 0xFFD63031, 0xFF0984E3, 0xFFE84393
        )
        val color = colorHexes[count % colorHexes.size]

        val newTrack = TrackData(
            name = "${instrument.displayName} $count",
            instrument = instrument,
            scale = _project.value.scale,
            colorHex = color
        )
        _project.update { it.copy(tracks = it.tracks + newTrack) }
    }

    fun deleteTrack(trackId: String) {
        _project.update { p ->
            p.copy(tracks = p.tracks.filter { it.id != trackId })
        }
    }

    // Voice Recording & Transformation
    fun openRecordSheet(track: TrackData) {
        stopPlayback()
        _recordingTrack.value = track
        _recordedVoiceAudio.value = if (track.originalVoiceAudio.isNotEmpty()) track.originalVoiceAudio else null
        _previewTransformedAudio.value = if (track.transformedAudio.isNotEmpty()) track.transformedAudio else null
        _liveAmplitudes.value = emptyList()
        _recordingDurationSeconds.value = 0.0f
    }

    fun closeRecordSheet() {
        stopRecordingVoice()
        previewPlayer.stop()
        _recordingTrack.value = null
        _recordedVoiceAudio.value = null
        _previewTransformedAudio.value = null
    }

    fun startRecordingVoice() {
        stopPlayback()
        previewPlayer.stop()
        _isRecording.value = true
        _liveAmplitudes.value = emptyList()
        _recordingDurationSeconds.value = 0.0f

        recordTimerJob = viewModelScope.launch {
            var elapsed = 0.0f
            while (isActive && _isRecording.value) {
                delay(100)
                elapsed += 0.1f
                _recordingDurationSeconds.value = elapsed
            }
        }

        audioRecorder.startRecording(
            scope = viewModelScope,
            onAmplitudeUpdate = { amp ->
                val current = _liveAmplitudes.value.toMutableList()
                if (current.size > 80) current.removeAt(0)
                current.add(amp)
                _liveAmplitudes.value = current
            },
            onComplete = { pcm ->
                viewModelScope.launch(Dispatchers.Main) {
                    _recordedVoiceAudio.value = pcm
                    // Automatically trigger initial transform preview
                    val track = _recordingTrack.value
                    if (track != null && pcm.isNotEmpty()) {
                        transformAndPreview(track.instrument, track.scale, track.pitchShift / 12)
                    }
                }
            }
        )
    }

    fun stopRecordingVoice() {
        _isRecording.value = false
        recordTimerJob?.cancel()
        recordTimerJob = null
        audioRecorder.stopRecording()
    }

    fun playOriginalVoice() {
        val audio = _recordedVoiceAudio.value ?: return
        previewPlayer.stop()
        _isPlayingPreview.value = true
        previewPlayer.playMono(
            scope = viewModelScope,
            pcmData = audio,
            onFinished = { _isPlayingPreview.value = false }
        )
    }

    fun transformAndPreview(
        instrument: InstrumentType,
        scale: MusicalScale,
        octaveShift: Int
    ) {
        val voiceAudio = _recordedVoiceAudio.value ?: return
        viewModelScope.launch(Dispatchers.Default) {
            val transformed = voiceTransformer.transformVoice(
                voiceAudio = voiceAudio,
                instrument = instrument,
                scale = scale,
                rootNoteMidi = _project.value.keyRootMidi,
                octaveShift = octaveShift
            )
            withContext(Dispatchers.Main) {
                _previewTransformedAudio.value = transformed
                // Play preview of transformed sound
                previewPlayer.stop()
                _isPlayingPreview.value = true
                previewPlayer.playMono(
                    scope = viewModelScope,
                    pcmData = transformed,
                    onFinished = { _isPlayingPreview.value = false }
                )
            }
        }
    }

    fun applyVoiceToTrack(
        originalVoice: ShortArray,
        transformedAudio: ShortArray,
        instrument: InstrumentType,
        scale: MusicalScale,
        pitchShift: Int
    ) {
        val track = _recordingTrack.value ?: return
        _project.update { p ->
            p.copy(tracks = p.tracks.map {
                if (it.id == track.id) {
                    it.copy(
                        originalVoiceAudio = originalVoice,
                        transformedAudio = transformedAudio,
                        instrument = instrument,
                        scale = scale,
                        pitchShift = pitchShift
                    )
                } else it
            })
        }
        closeRecordSheet()
    }

    // Demo & Project resets
    fun loadDemoSong() {
        stopPlayback()
        _project.value = DemoSongFactory.createDemoSong()
    }

    fun resetNewSong() {
        stopPlayback()
        _project.value = SongProject(
            title = "New Song",
            bpm = 100,
            scale = MusicalScale.CHINESE_GONG,
            tracks = listOf(
                TrackData(name = "Vocal Lead", instrument = InstrumentType.ERHU, colorHex = 0xFFFFBE0B),
                TrackData(name = "Beatbox Drums", instrument = InstrumentType.DRUM_KIT, colorHex = 0xFFFF6B6B),
                TrackData(name = "Sub Bass", instrument = InstrumentType.BASS_808, colorHex = 0xFF4ECDC4),
                TrackData(name = "Acoustic Pluck", instrument = InstrumentType.GUZHENG, colorHex = 0xFFFB5607)
            )
        )
    }

    // Master Export
    fun exportSong(context: Context, songTitle: String) {
        _isExporting.value = true
        _exportedFile.value = null

        viewModelScope.launch(Dispatchers.IO) {
            val safeTitle = songTitle.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val outFile = File(exportDir, "${safeTitle}_$timeStamp.wav")

            val currentProj = _project.value.copy(title = songTitle)
            multiTrackMixer.renderToWav(currentProj, outFile)

            withContext(Dispatchers.Main) {
                _exportedFile.value = outFile
                _isExporting.value = false
            }
        }
    }

    fun clearExportState() {
        _exportedFile.value = null
        _isExporting.value = false
    }

    override fun onCleared() {
        super.onCleared()
        multiTrackMixer.stopPlayback()
        previewPlayer.stop()
        audioRecorder.stopRecording()
    }
}
