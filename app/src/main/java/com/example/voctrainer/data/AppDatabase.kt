package com.example.voctrainer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Lesson::class, WordEntry::class, LessonSessionStats::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun lessonDao(): LessonDao
    abstract fun wordEntryDao(): WordEntryDao
    abstract fun lessonSessionStatsDao(): LessonSessionStatsDao // Added for session stats

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "voc_trainer_database"
                )
                // For schema changes, increment version and add a migration strategy.
                // For development, fallbackToDestructiveMigration is simplest.
                .fallbackToDestructiveMigration() // This will wipe existing data on version upgrade
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
