package com.example.voctrainer.ui.lessons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voctrainer.data.Lesson
import com.example.voctrainer.data.LessonDao
import com.example.voctrainer.data.UserPreferencesRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LessonsViewModel(
    private val lessonDao: LessonDao,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val allLessons: Flow<List<Lesson>> = lessonDao.getAllLessons()

    private val _selectedLessonIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedLessonIds: StateFlow<Set<Long>> = _selectedLessonIds.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferencesRepository.selectedLessonIds.collect { ids ->
                _selectedLessonIds.value = ids
            }
        }
    }

    fun toggleLessonSelection(lessonId: Long) {
        val currentSelection = _selectedLessonIds.value.toMutableSet()
        if (currentSelection.contains(lessonId)) {
            currentSelection.remove(lessonId)
        } else {
            currentSelection.add(lessonId)
        }
        _selectedLessonIds.value = currentSelection
        viewModelScope.launch {
            userPreferencesRepository.setSelectedLessonIds(currentSelection)
        }
    }

    fun clearSelection() {
        _selectedLessonIds.value = emptySet()
        viewModelScope.launch {
            userPreferencesRepository.setSelectedLessonIds(emptySet())
        }
    }
}
