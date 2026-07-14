package com.abdessalem.worktracker.di

import android.content.Context
import com.abdessalem.worktracker.data.local.ShiftDao
import com.abdessalem.worktracker.data.local.WorkDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WorkDatabase = WorkDatabase.get(context)

    @Provides
    fun provideShiftDao(database: WorkDatabase): ShiftDao = database.shiftDao()
}
