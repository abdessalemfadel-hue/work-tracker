package com.abdessalem.worktracker.data.repository

import androidx.room.withTransaction
import com.abdessalem.worktracker.data.local.ShiftDao
import com.abdessalem.worktracker.data.local.ShiftEntity
import com.abdessalem.worktracker.data.local.WorkDatabase
import com.abdessalem.worktracker.domain.model.ShiftSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShiftRepository @Inject constructor(
    private val database: WorkDatabase,
    private val dao: ShiftDao
) {
    fun observeAll(): Flow<List<ShiftEntity>> = dao.observeAll()
    fun observeActive(): Flow<ShiftEntity?> = dao.observeActive()
    fun observeRange(from: Long, to: Long): Flow<List<ShiftEntity>> = dao.observeRange(from, to)

    suspend fun getActive(): ShiftEntity? = dao.getActive()
    suspend fun getAllOnce(): List<ShiftEntity> = dao.getAllOnce()
    suspend fun getRange(from: Long, to: Long): List<ShiftEntity> = dao.getRange(from, to)

    suspend fun clockIn(now: Long = System.currentTimeMillis()): Result<Long> = runCatching {
        database.withTransaction {
            check(dao.getActive() == null) { "A shift is already active" }
            dao.insert(
                ShiftEntity(
                    clockIn = now,
                    source = ShiftSource.LIVE,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }

    suspend fun clockOut(now: Long = System.currentTimeMillis()): Result<Unit> = runCatching {
        database.withTransaction {
            val active = requireNotNull(dao.getActive()) { "No active shift" }
            val additionalBreak = active.breakStart?.let { (now - it).coerceAtLeast(0) } ?: 0
            dao.update(
                active.copy(
                    clockOut = now.coerceAtLeast(active.clockIn),
                    breakStart = null,
                    totalBreakMillis = active.totalBreakMillis + additionalBreak,
                    updatedAt = now
                )
            )
        }
    }

    suspend fun toggleBreak(now: Long = System.currentTimeMillis()): Result<Boolean> = runCatching {
        database.withTransaction {
            val active = requireNotNull(dao.getActive()) { "No active shift" }
            if (active.breakStart == null) {
                dao.update(active.copy(breakStart = now, updatedAt = now))
                true
            } else {
                val added = (now - active.breakStart).coerceAtLeast(0)
                dao.update(
                    active.copy(
                        breakStart = null,
                        totalBreakMillis = active.totalBreakMillis + added,
                        updatedAt = now
                    )
                )
                false
            }
        }
    }

    suspend fun addManual(
        clockIn: Long,
        clockOut: Long,
        totalBreakMillis: Long,
        note: String
    ): Result<Long> = runCatching {
        require(clockOut > clockIn) { "Clock-out must be after clock-in" }
        require(totalBreakMillis >= 0) { "Break time cannot be negative" }
        require(totalBreakMillis < clockOut - clockIn) { "Break time must be shorter than the shift" }
        val now = System.currentTimeMillis()
        dao.insert(
            ShiftEntity(
                clockIn = clockIn,
                clockOut = clockOut,
                totalBreakMillis = totalBreakMillis,
                note = note.trim(),
                source = ShiftSource.MANUAL,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun updateShift(shift: ShiftEntity): Result<Unit> = runCatching {
        val out = shift.clockOut
        require(out == null || out > shift.clockIn) { "Clock-out must be after clock-in" }
        require(shift.totalBreakMillis >= 0) { "Break time cannot be negative" }
        if (out != null) require(shift.totalBreakMillis < out - shift.clockIn) {
            "Break time must be shorter than the shift"
        }
        dao.update(shift.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun updateNote(id: Long, note: String): Result<Unit> = runCatching {
        val shift = requireNotNull(dao.getById(id)) { "Shift not found" }
        dao.update(shift.copy(note = note.trim(), updatedAt = System.currentTimeMillis()))
    }

    suspend fun duplicate(id: Long): Result<Long> = runCatching {
        val shift = requireNotNull(dao.getById(id)) { "Shift not found" }
        require(shift.clockOut != null) { "Only completed shifts can be duplicated" }
        val now = System.currentTimeMillis()
        dao.insert(
            shift.copy(
                id = 0,
                source = ShiftSource.MANUAL,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun importShift(
        clockIn: Long,
        clockOut: Long?,
        totalBreakMillis: Long,
        note: String
    ): Result<Long> = runCatching {
        require(clockOut == null || clockOut > clockIn) { "Clock-out must be after clock-in" }
        require(totalBreakMillis >= 0) { "Break time cannot be negative" }
        val now = System.currentTimeMillis()
        dao.insert(
            ShiftEntity(
                clockIn = clockIn,
                clockOut = clockOut,
                totalBreakMillis = totalBreakMillis,
                note = note.trim(),
                source = ShiftSource.IMPORTED,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun delete(id: Long) = dao.deleteById(id)
    suspend fun deleteAll() = dao.deleteAll()
}
