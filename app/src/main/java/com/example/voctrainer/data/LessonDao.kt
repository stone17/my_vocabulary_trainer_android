package com.example.voctrainer.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LessonDao {
    @Insert
    suspend fun insertLesson(lesson: Lesson): Long // Returns the new lessonId

    @Update
    suspend fun updateLesson(lesson: Lesson)

    @Delete
    suspend fun deleteLesson(lesson: Lesson) // Added for deleting a lesson

    @Query("SELECT * FROM lessons ORDER BY lessonName ASC")
    fun getAllLessons(): Flow<List<Lesson>> // Use Flow for reactive updates

    @Query("SELECT * FROM lessons WHERE lessonId = :id")
    suspend fun getLessonById(id: Long): Lesson?

    @Query("SELECT * FROM lessons WHERE lessonName = :name")
    suspend fun getLessonByName(name: String): Lesson?
}
