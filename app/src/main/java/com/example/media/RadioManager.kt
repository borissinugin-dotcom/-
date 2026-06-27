package com.example.media

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.example.data.RadioStation
import com.example.data.Recording
import com.example.data.RadioRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

enum class PlaybackState {
    IDLE, PREPARING, PLAYING, ERROR
}

class RadioManager private constructor() {

    private var mediaPlayer: MediaPlayer? = null
    
    // Playback States
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentPlayingStation = MutableStateFlow<RadioStation?>(null)
    val currentPlayingStation: StateFlow<RadioStation?> = _currentPlayingStation.asStateFlow()

    private val _currentPlayingRecording = MutableStateFlow<Recording?>(null)
    val currentPlayingRecording: StateFlow<Recording?> = _currentPlayingRecording.asStateFlow()

    // Playback progress for local recordings
    private val _playbackProgressMs = MutableStateFlow(0L)
    val playbackProgressMs: StateFlow<Long> = _playbackProgressMs.asStateFlow()

    // Recording States
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _currentRecordingStation = MutableStateFlow<RadioStation?>(null)
    val currentRecordingStation: StateFlow<RadioStation?> = _currentRecordingStation.asStateFlow()

    private val _recordingDurationMs = MutableStateFlow(0L)
    val recordingDurationMs: StateFlow<Long> = _recordingDurationMs.asStateFlow()

    private val _recordingFileSize = MutableStateFlow(0L)
    val recordingFileSize: StateFlow<Long> = _recordingFileSize.asStateFlow()

    private var recordingJob: Job? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun playStation(context: Context, station: RadioStation) {
        stop() // Stop anything else playing
        
        _currentPlayingStation.value = station
        _currentPlayingRecording.value = null
        _playbackState.value = PlaybackState.PREPARING
        
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            try {
                val headers = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36",
                    "Icy-MetaData" to "1"
                )
                setDataSource(context, android.net.Uri.parse(station.streamUrl), headers)
                setOnPreparedListener {
                    start()
                    _playbackState.value = PlaybackState.PLAYING
                    _isPlaying.value = true
                }
                setOnErrorListener { _, what, extra ->
                    android.util.Log.e("RadioManager", "MediaPlayer Error: what=$what, extra=$extra")
                    _playbackState.value = PlaybackState.ERROR
                    _isPlaying.value = false
                    false
                }
                prepareAsync()
            } catch (e: Exception) {
                e.printStackTrace()
                _playbackState.value = PlaybackState.ERROR
                _isPlaying.value = false
            }
        }
    }

    fun playRecording(recording: Recording) {
        stop() // Stop anything else playing
        
        _currentPlayingRecording.value = recording
        _currentPlayingStation.value = null
        _playbackState.value = PlaybackState.PREPARING
        _playbackProgressMs.value = 0L
        
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            try {
                setDataSource(recording.filePath)
                setOnPreparedListener {
                    start()
                    _playbackState.value = PlaybackState.PLAYING
                    _isPlaying.value = true
                    startProgressTracker()
                }
                setOnCompletionListener {
                    stop()
                }
                setOnErrorListener { _, _, _ ->
                    _playbackState.value = PlaybackState.ERROR
                    _isPlaying.value = false
                    false
                }
                prepareAsync()
            } catch (e: Exception) {
                e.printStackTrace()
                _playbackState.value = PlaybackState.ERROR
                _isPlaying.value = false
            }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && _isPlaying.value) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        _playbackProgressMs.value = player.currentPosition.toLong()
                    }
                }
                delay(200)
            }
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            _isPlaying.value = false
        } else {
            player.start()
            _isPlaying.value = true
            if (_currentPlayingRecording.value != null) {
                startProgressTracker()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let { player ->
            player.seekTo(positionMs.toInt())
            _playbackProgressMs.value = positionMs
        }
    }

    fun stop() {
        progressJob?.cancel()
        mediaPlayer?.let { player ->
            try {
                if (player.isPlaying) {
                    player.stop()
                }
            } catch (e: Exception) {}
            player.release()
        }
        mediaPlayer = null
        _isPlaying.value = false
        _playbackState.value = PlaybackState.IDLE
        _playbackProgressMs.value = 0L
        _currentPlayingStation.value = null
        _currentPlayingRecording.value = null
    }

    // Recording API
    fun startRecording(context: Context, station: RadioStation, repository: RadioRepository) {
        if (_isRecording.value) return

        val recordingsDir = File(context.filesDir, "recordings").apply {
            if (!exists()) mkdirs()
        }

        val timestamp = System.currentTimeMillis()
        val sanitizedStationName = station.name.replace("[^a-zA-Z0-9а-яА-Я]".toRegex(), "_")
        val file = File(recordingsDir, "rec_${sanitizedStationName}_$timestamp.mp3")
        
        _currentRecordingStation.value = station
        _isRecording.value = true
        _recordingDurationMs.value = 0L
        _recordingFileSize.value = 0L

        recordingJob = scope.launch(Dispatchers.IO) {
            var inputStream: InputStream? = null
            var outputStream: FileOutputStream? = null
            var connection: HttpURLConnection? = null
            try {
                connection = URL(station.streamUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 15000
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36")
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw IOException("Server returned HTTP ${connection.responseCode}")
                }

                inputStream = connection.inputStream
                outputStream = FileOutputStream(file)

                val buffer = ByteArray(16 * 1024) // 16KB buffer for efficient recording
                var bytesRead: Int
                var totalBytes = 0L
                val startTime = System.currentTimeMillis()
                var lastUpdate = startTime

                while (isActive) {
                    bytesRead = inputStream.read(buffer)
                    if (bytesRead == -1) break

                    outputStream.write(buffer, 0, bytesRead)
                    totalBytes += bytesRead

                    val now = System.currentTimeMillis()
                    if (now - lastUpdate > 500) { // update UI twice a second
                        _recordingDurationMs.value = now - startTime
                        _recordingFileSize.value = totalBytes
                        lastUpdate = now
                    }
                }
                outputStream.flush()

                val duration = System.currentTimeMillis() - startTime
                if (totalBytes > 1024) { // Only save if more than 1KB
                    val recording = Recording(
                        stationName = station.name,
                        filePath = file.absolutePath,
                        timestamp = timestamp,
                        durationMs = duration,
                        fileSize = totalBytes
                    )
                    repository.insertRecording(recording)
                } else {
                    if (file.exists()) file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (file.exists() && file.length() < 1024) {
                    file.delete()
                }
            } finally {
                try { inputStream?.close() } catch (e: Exception) {}
                try { outputStream?.close() } catch (e: Exception) {}
                try { connection?.disconnect() } catch (e: Exception) {}

                withContext(Dispatchers.Main) {
                    _isRecording.value = false
                    _currentRecordingStation.value = null
                }
            }
        }
    }

    fun stopRecording() {
        recordingJob?.cancel()
        recordingJob = null
    }

    companion object {
        @Volatile
        private var INSTANCE: RadioManager? = null

        fun getInstance(): RadioManager {
            return INSTANCE ?: synchronized(this) {
                val instance = RadioManager()
                INSTANCE = instance
                instance
            }
        }
    }
}
