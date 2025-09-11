package com.example.voctrainer.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "word_entries",
    foreignKeys = [
        ForeignKey(
            entity = Lesson::class,
            parentColumns = ["lessonId"],
            childColumns = ["lessonOwnerId"],
            onDelete = ForeignKey.CASCADE // If a lesson is deleted, its words are also deleted
        )
    ],
    indices = [Index(value = ["lessonOwnerId"])]
)
data class WordEntry(
    @PrimaryKey(autoGenerate = true)
    val wordId: Long = 0,
    val swedishWord: String,
    val englishWord: String,
    val lessonOwnerId: Long, // Foreign key to Lesson table
    val isEnabled: Boolean = true, // New field to enable/disable word entry

    // Fields for tracking study statistics
    val timesPresentedInStudy: Int = 0,
    val timesCorrectInStudy: Int = 0,
    val timesIncorrectInStudy: Int = 0
)
