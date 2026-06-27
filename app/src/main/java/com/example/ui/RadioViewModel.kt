package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.RadioRepository
import com.example.data.RadioStation
import com.example.data.Recording
import com.example.media.PlaybackState
import com.example.media.RadioManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.random.Random

data class AdventureEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: String,
    val stationName: String,
    val genre: String,
    val sectorDescription: String,
    val jumpDistance: Int
)

class RadioViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RadioRepository
    val radioManager = RadioManager.getInstance()

    val allStations: StateFlow<List<RadioStation>>
    val favoriteStations: StateFlow<List<RadioStation>>
    val allRecordings: StateFlow<List<Recording>>

    // RadioManager state exposures
    val isPlaying: StateFlow<Boolean> = radioManager.isPlaying
    val playbackState: StateFlow<PlaybackState> = radioManager.playbackState
    val currentPlayingStation: StateFlow<RadioStation?> = radioManager.currentPlayingStation
    val currentPlayingRecording: StateFlow<Recording?> = radioManager.currentPlayingRecording
    val playbackProgressMs: StateFlow<Long> = radioManager.playbackProgressMs
    
    val isRecording: StateFlow<Boolean> = radioManager.isRecording
    val currentRecordingStation: StateFlow<RadioStation?> = radioManager.currentRecordingStation
    val recordingDurationMs: StateFlow<Long> = radioManager.recordingDurationMs
    val recordingFileSize: StateFlow<Long> = radioManager.recordingFileSize

    init {
        val database = AppDatabase.getDatabase(application)
        repository = RadioRepository(database.radioDao())
        
        allStations = repository.allStations
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
            
        favoriteStations = repository.favoriteStations
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
            
        allRecordings = repository.allRecordings
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
            
        viewModelScope.launch {
            repository.prepopulateIfNeeded()
        }
    }

    fun shuffleStation(): RadioStation? {
        val stationsList = allStations.value
        if (stationsList.isNotEmpty()) {
            var randomStation = stationsList.random()
            if (stationsList.size > 1 && randomStation.id == currentPlayingStation.value?.id) {
                randomStation = stationsList.filter { it.id != currentPlayingStation.value?.id }.random()
            }
            selectStation(randomStation)
            return randomStation
        }
        return null
    }

    fun selectStation(station: RadioStation) {
        radioManager.playStation(getApplication(), station)
    }

    fun playRecording(recording: Recording) {
        radioManager.playRecording(recording)
    }

    fun togglePlayPause() {
        radioManager.togglePlayPause()
    }

    fun stopPlayback() {
        radioManager.stop()
    }

    fun seekTo(positionMs: Long) {
        radioManager.seekTo(positionMs)
    }

    fun toggleFavorite(station: RadioStation) {
        viewModelScope.launch {
            repository.updateStation(station.copy(isFavorite = !station.isFavorite))
        }
    }

    fun addCustomStation(name: String, streamUrl: String, genre: String) {
        viewModelScope.launch {
            val station = RadioStation(
                name = name,
                streamUrl = streamUrl,
                genre = genre.ifBlank { "Custom" },
                isCustom = true
            )
            repository.insertStation(station)
        }
    }

    fun deleteStation(station: RadioStation) {
        viewModelScope.launch {
            if (currentPlayingStation.value?.id == station.id) {
                stopPlayback()
            }
            if (currentRecordingStation.value?.id == station.id) {
                toggleRecording(station)
            }
            repository.deleteStation(station)
        }
    }

    fun toggleRecording(station: RadioStation) {
        if (isRecording.value) {
            radioManager.stopRecording()
        } else {
            radioManager.startRecording(getApplication(), station, repository)
        }
    }

    fun deleteRecording(recording: Recording) {
        viewModelScope.launch {
            if (currentPlayingRecording.value?.id == recording.id) {
                stopPlayback()
            }
            val file = File(recording.filePath)
            if (file.exists()) {
                file.delete()
            }
            repository.deleteRecording(recording)
        }
    }
}
