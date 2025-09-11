package com.example.voctrainer.ui.user_statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.voctrainer.data.LessonDao
import com.example.voctrainer.data.LessonSessionStatsDao
import com.example.voctrainer.data.WordEntryDao

class UserStatisticsViewModelFactory(
    private val lessonDao: LessonDao,
    private val wordEntryDao: WordEntryDao,
    private val lessonSessionStatsDao: LessonSessionStatsDao
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserStatisticsViewModel::class.java)) {
            return UserStatisticsViewModel(lessonDao, wordEntryDao, lessonSessionStatsDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
