package com.example.voctrainer.ui.lessons

import androidx.lifecycle.ViewModel
import com.example.voctrainer.data.Lesson
import com.example.voctrainer.data.LessonDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LessonsViewModel(private val lessonDao: LessonDao) : ViewModel() {

    // Expose a Flow of all lessons from the DAO
    val allLessons: Flow<List<Lesson>> = lessonDao.getAllLessons()

    private val _selectedLesson = MutableStateFlow<Lesson?>(null)
    val selectedLesson: StateFlow<Lesson?> = _selectedLesson.asStateFlow()

    fun selectLesson(lesson: Lesson?) {
        _selectedLesson.value = lesson
    }
}
