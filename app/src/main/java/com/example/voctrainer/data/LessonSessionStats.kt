package com.example.voctrainer.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "lesson_session_stats",
    foreignKeys = [
        ForeignKey(
            entity = Lesson::class,
            parentColumns = ["lessonId"],
            childColumns = ["lessonId"],
            onDelete = ForeignKey.CASCADE // If a lesson is deleted, its session stats are also deleted
        )
    ],
    indices = [Index(value = ["lessonId"])]
)
data class LessonSessionStats(
    @PrimaryKey(autoGenerate = true)
    val sessionId: Long = 0,
    val lessonId: Long,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val durationMillis: Long,
    val sessionTotalWords: Int,     // Words presented in this session
    val sessionCorrectAnswers: Int,
    val sessionIncorrectAnswers: Int
)
