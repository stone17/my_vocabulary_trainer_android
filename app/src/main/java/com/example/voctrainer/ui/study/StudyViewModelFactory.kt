package com.example.voctrainer.ui.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.voctrainer.data.LessonSessionStatsDao
import com.example.voctrainer.data.WordEntryDao

class StudyViewModelFactory(
    private val lessonId: Long,
    private val wordEntryDao: WordEntryDao,
    private val lessonSessionStatsDao: LessonSessionStatsDao,
    private val isPracticeWorstWords: Boolean // Added
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StudyViewModel::class.java)) {
            return StudyViewModel(lessonId, wordEntryDao, lessonSessionStatsDao, isPracticeWorstWords) as T // Pass it here
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
