package com.abdessalem.worktracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftDao {
    @Query("SELECT * FROM shifts ORDER BY clockIn DESC")
    fun observeAll(): Flow<List<ShiftEntity>>

    @Query("SELECT * FROM shifts WHERE clockOut IS NULL ORDER BY clockIn DESC LIMIT 1")
    fun observeActive(): Flow<ShiftEntity?>

    @Query("SELECT * FROM shifts WHERE clockOut IS NULL ORDER BY clockIn DESC LIMIT 1")
    suspend fun getActive(): ShiftEntity?

    @Query("SELECT * FROM shifts WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ShiftEntity?

    @Query("SELECT * FROM shifts WHERE clockIn >= :from AND clockIn < :to ORDER BY clockIn ASC")
    fun observeRange(from: Long, to: Long): Flow<List<ShiftEntity>>

    @Query("SELECT * FROM shifts WHERE clockIn >= :from AND clockIn < :to ORDER BY clockIn ASC")
    suspend fun getRange(from: Long, to: Long): List<ShiftEntity>

    @Query("SELECT * FROM shifts ORDER BY clockIn ASC")
    suspend fun getAllOnce(): List<ShiftEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(shift: ShiftEntity): Long

    @Update
    suspend fun update(shift: ShiftEntity)

    @Delete
    suspend fun delete(shift: ShiftEntity)

    @Query("DELETE FROM shifts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM shifts")
    suspend fun deleteAll()
}
