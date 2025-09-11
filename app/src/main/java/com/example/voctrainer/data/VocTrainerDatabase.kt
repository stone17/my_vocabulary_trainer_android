package com.example.voctrainer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Lesson::class, WordEntry::class], version = 1, exportSchema = false)
abstract class VocTrainerDatabase : RoomDatabase() {

    abstract fun lessonDao(): LessonDao
    abstract fun wordEntryDao(): WordEntryDao

    companion object {
        @Volatile
        private var INSTANCE: VocTrainerDatabase? = null

        fun getDatabase(context: Context): VocTrainerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VocTrainerDatabase::class.java,
                    "voctrainer_database"
                )
                // TODO: Add database migrations if you change the schema in the future
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
