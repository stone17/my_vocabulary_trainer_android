package com.example.voctrainer.ui.lesson_creator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.voctrainer.data.LessonDao
import com.example.voctrainer.data.WordEntryDao

class LessonCreatorViewModelFactory(
    private val lessonDao: LessonDao,
    private val wordEntryDao: WordEntryDao
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LessonCreatorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LessonCreatorViewModel(lessonDao, wordEntryDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
