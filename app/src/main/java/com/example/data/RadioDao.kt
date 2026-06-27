package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RadioDao {
    @Query("SELECT * FROM radio_stations ORDER BY isFavorite DESC, name ASC")
    fun getAllStations(): Flow<List<RadioStation>>

    @Query("SELECT * FROM radio_stations WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteStations(): Flow<List<RadioStation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStation(station: RadioStation)

    @Update
    suspend fun updateStation(station: RadioStation)

    @Delete
    suspend fun deleteStation(station: RadioStation)

    @Query("SELECT * FROM recordings ORDER BY timestamp DESC")
    fun getAllRecordings(): Flow<List<Recording>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: Recording)

    @Delete
    suspend fun deleteRecording(recording: Recording)
    
    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun deleteRecordingById(id: Int)
}
