package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: CapturedPhoto): Long

    @Query("SELECT * FROM captured_photos ORDER BY timestamp DESC")
    fun getAllPhotos(): Flow<List<CapturedPhoto>>

    @Query("SELECT * FROM captured_photos WHERE id = :id LIMIT 1")
    suspend fun getPhotoById(id: Long): CapturedPhoto?

    @Delete
    suspend fun deletePhoto(photo: CapturedPhoto)

    @Query("DELETE FROM captured_photos WHERE id = :id")
    suspend fun deletePhotoById(id: Long)
}
