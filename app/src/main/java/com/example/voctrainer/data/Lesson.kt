package com.example.voctrainer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lessons")
data class Lesson(
    @PrimaryKey(autoGenerate = true)
    val lessonId: Long = 0,
    val lessonName: String
)
