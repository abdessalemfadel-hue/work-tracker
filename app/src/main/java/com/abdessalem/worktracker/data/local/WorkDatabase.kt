package com.abdessalem.worktracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.abdessalem.worktracker.domain.model.ShiftSource

class ShiftConverters {
    @TypeConverter fun sourceToString(source: ShiftSource): String = source.name
    @TypeConverter fun stringToSource(value: String): ShiftSource = runCatching { ShiftSource.valueOf(value) }
        .getOrDefault(ShiftSource.LIVE)
}

@Database(
    entities = [ShiftEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(ShiftConverters::class)
abstract class WorkDatabase : RoomDatabase() {
    abstract fun shiftDao(): ShiftDao

    companion object {
        @Volatile private var instance: WorkDatabase? = null

        fun get(context: Context): WorkDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                WorkDatabase::class.java,
                "work_tracker.db"
            ).build().also { instance = it }
        }
    }
}
