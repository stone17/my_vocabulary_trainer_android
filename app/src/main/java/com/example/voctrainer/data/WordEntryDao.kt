package com.example.voctrainer.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WordEntryDao {
    @Insert
    suspend fun insertWordEntry(wordEntry: WordEntry)

    @Insert
    suspend fun insertAllWordEntries(wordEntries: List<WordEntry>)

    @Update
    suspend fun updateWordEntry(wordEntry: WordEntry)

    @Delete
    suspend fun deleteWordEntry(wordEntry: WordEntry)

    @Query("SELECT * FROM word_entries WHERE lessonOwnerId = :lessonId ORDER BY swedishWord ASC")
    fun getWordsForLesson(lessonId: Long): Flow<List<WordEntry>> // Gets all words for editing

    @Query("SELECT * FROM word_entries WHERE lessonOwnerId = :lessonId AND isEnabled = 1 ORDER BY RANDOM()")
    fun getEnabledWordsForLessonOrderedRandomly(lessonId: Long): Flow<List<WordEntry>> // Gets enabled words for studying

    // Methods for updating word-specific study statistics
    @Query("UPDATE word_entries SET timesPresentedInStudy = timesPresentedInStudy + 1 WHERE wordId = :wordId")
    suspend fun incrementTimesPresented(wordId: Long)

    @Query("UPDATE word_entries SET timesCorrectInStudy = timesCorrectInStudy + 1 WHERE wordId = :wordId")
    suspend fun incrementTimesCorrect(wordId: Long)

    @Query("UPDATE word_entries SET timesIncorrectInStudy = timesIncorrectInStudy + 1 WHERE wordId = :wordId")
    suspend fun incrementTimesIncorrect(wordId: Long)

    // Method to get "worst" words (most incorrect answers, must have been presented at least once)
    // For words with same incorrect count, prioritize those presented more often, then by swedishWord
    @Query("SELECT * FROM word_entries WHERE timesPresentedInStudy > 0 ORDER BY timesIncorrectInStudy DESC, timesPresentedInStudy DESC, swedishWord ASC LIMIT :limit")
    fun getWorstWords(limit: Int): Flow<List<WordEntry>>

    // Method to reset study statistics for all words
    @Query("UPDATE word_entries SET timesPresentedInStudy = 0, timesCorrectInStudy = 0, timesIncorrectInStudy = 0")
    suspend fun resetAllWordStats()
}
