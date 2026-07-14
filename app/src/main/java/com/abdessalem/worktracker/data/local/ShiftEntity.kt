package com.abdessalem.worktracker.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.abdessalem.worktracker.domain.model.ShiftSource

@Entity(
    tableName = "shifts",
    indices = [Index("clockIn"), Index("clockOut")]
)
data class ShiftEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clockIn: Long,
    val clockOut: Long? = null,
    val breakStart: Long? = null,
    val totalBreakMillis: Long = 0,
    val note: String = "",
    val source: ShiftSource = ShiftSource.LIVE,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
