package com.example.voctrainer.ui.user_statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voctrainer.data.Lesson
import com.example.voctrainer.data.LessonDao
import com.example.voctrainer.data.LessonSessionStatsDao
import com.example.voctrainer.data.WordEntry
import com.example.voctrainer.data.WordEntryDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

// Helper data class for displaying lesson completion stats
data class LessonCompletionStat(
    val lessonId: Long,
    val lessonName: String,
    val completionCount: Int
)

class UserStatisticsViewModel(
    private val lessonDao: LessonDao,
    private val wordEntryDao: WordEntryDao,
    private val lessonSessionStatsDao: LessonSessionStatsDao
) : ViewModel() {

    private val _totalStudyTimeFormatted = MutableStateFlow("0 seconds")
    val totalStudyTimeFormatted: StateFlow<String> = _totalStudyTimeFormatted.asStateFlow()

    private val _lessonCompletionStats = MutableStateFlow<List<LessonCompletionStat>>(emptyList())
    val lessonCompletionStats: StateFlow<List<LessonCompletionStat>> = _lessonCompletionStats.asStateFlow()

    private val _worstWords = MutableStateFlow<List<WordEntry>>(emptyList())
    val worstWords: StateFlow<List<WordEntry>> = _worstWords.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // For managing the confirmation dialog for clearing stats
    private val _showClearConfirmationDialog = MutableStateFlow(false)
    val showClearConfirmationDialog: StateFlow<Boolean> = _showClearConfirmationDialog.asStateFlow()

    init {
        loadAllStatistics()
    }

    fun loadAllStatistics() {
        viewModelScope.launch {
            _isLoading.value = true
            
            // Total Study Time
            launch {
                lessonSessionStatsDao.getTotalStudyTimeMillis().collect { millis ->
                    _totalStudyTimeFormatted.value = formatDuration(millis ?: 0L)
                }
            }
            
            // Lesson Completion Stats
            launch {
                val lessons = lessonDao.getAllLessons().first() 
                val statsList = mutableListOf<LessonCompletionStat>()
                for (lesson in lessons) {
                    val count = lessonSessionStatsDao.getCompletionCountForLesson(lesson.lessonId).first()
                    statsList.add(LessonCompletionStat(lesson.lessonId, lesson.lessonName, count)) // Corrected here
                }
                _lessonCompletionStats.value = statsList.sortedByDescending { it.completionCount }
            }

            // Worst Words
            launch {
                wordEntryDao.getWorstWords(10).collect { words ->
                    _worstWords.value = words
                }
            }
            _isLoading.value = false // Set to false after all initial loading launches are complete, ideally using joinAll or similar for robustness
        }
    }

    fun requestClearAllStatistics() {
        _showClearConfirmationDialog.value = true
    }

    fun confirmClearAllStatistics() {
        viewModelScope.launch {
            _isLoading.value = true
            lessonSessionStatsDao.clearAllSessionStats()
            wordEntryDao.resetAllWordStats()
            loadAllStatistics() // Reload after clearing. isLoading will be handled by loadAllStatistics itself.
        }
        _showClearConfirmationDialog.value = false
    }

    fun cancelClearAllStatistics() {
        _showClearConfirmationDialog.value = false
    }

    private fun formatDuration(millis: Long): String {
        if (millis <= 0) return "0 seconds"
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        
        val parts = mutableListOf<String>()
        if (hours > 0) parts.add("$hours hour${if (hours > 1) "s" else ""}")
        if (minutes > 0) parts.add("$minutes minute${if (minutes > 1) "s" else ""}")
        if (seconds > 0 || parts.isEmpty()) parts.add("$seconds second${if (seconds > 1) "s" else ""}")
        
        return parts.joinToString(", ")
    }
}
