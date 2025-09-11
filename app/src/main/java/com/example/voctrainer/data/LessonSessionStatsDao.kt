package com.example.voctrainer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LessonSessionStatsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(sessionStats: LessonSessionStats): Long

    @Query("SELECT * FROM lesson_session_stats ORDER BY startTimeMillis DESC")
    fun getAllSessions(): Flow<List<LessonSessionStats>>

    @Query("SELECT * FROM lesson_session_stats WHERE lessonId = :lessonId ORDER BY startTimeMillis DESC")
    fun getSessionsForLesson(lessonId: Long): Flow<List<LessonSessionStats>>

    @Query("SELECT COUNT(sessionId) FROM lesson_session_stats WHERE lessonId = :lessonId")
    fun getCompletionCountForLesson(lessonId: Long): Flow<Int>

    @Query("SELECT SUM(durationMillis) FROM lesson_session_stats")
    fun getTotalStudyTimeMillis(): Flow<Long?> // Nullable if no sessions
    
    @Query("DELETE FROM lesson_session_stats")
    suspend fun clearAllSessionStats()
}
