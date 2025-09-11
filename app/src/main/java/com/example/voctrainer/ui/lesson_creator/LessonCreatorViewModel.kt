package com.example.voctrainer.ui.lesson_creator

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voctrainer.data.Lesson
import com.example.voctrainer.data.LessonDao
import com.example.voctrainer.data.WordEntry
import com.example.voctrainer.data.WordEntryDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalCoroutinesApi::class)
class LessonCreatorViewModel(private val lessonDao: LessonDao, private val wordEntryDao: WordEntryDao) : ViewModel() {

    private val _newlyAddedWordPairs = mutableListOf<Pair<String, String>>() // Buffer for NEW words in this session
    val newlyAddedWordPairs: List<Pair<String, String>> get() = _newlyAddedWordPairs

    val allLessons: Flow<List<Lesson>> = lessonDao.getAllLessons()

    private val _selectedLesson = MutableStateFlow<Lesson?>(null)
    val selectedLesson: StateFlow<Lesson?> = _selectedLesson.asStateFlow()

    val selectedLessonWords: StateFlow<List<WordEntry>> = _selectedLesson.flatMapLatest { lesson ->
        lesson?.let { wordEntryDao.getWordsForLesson(it.lessonId) } ?: emptyFlow()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    private val _importStatusMessage = MutableStateFlow("")
    val importStatusMessage: StateFlow<String> = _importStatusMessage.asStateFlow()

    private val _saveStatusMessage = MutableStateFlow("")
    val saveStatusMessage: StateFlow<String> = _saveStatusMessage.asStateFlow()

    fun addWordPair(swedishWord: String, englishWord: String) {
        if (swedishWord.isNotBlank() && englishWord.isNotBlank()) {
            _newlyAddedWordPairs.add(Pair(swedishWord, englishWord))
        }
    }

    fun selectLesson(lesson: Lesson?) {
        _selectedLesson.value = lesson
        _newlyAddedWordPairs.clear()
    }

    fun saveLesson(lessonName: String) {
        if (lessonName.isBlank()) {
            _saveStatusMessage.value = "Error: Lesson name cannot be blank."
            return
        }

        viewModelScope.launch {
            val lessonToSave: Lesson
            val lessonIdToUse: Long
            var wasNewLesson = false

            if (_selectedLesson.value != null) {
                val existingLesson = _selectedLesson.value!!
                if (existingLesson.lessonName != lessonName) {
                    lessonToSave = existingLesson.copy(lessonName = lessonName)
                    lessonDao.updateLesson(lessonToSave)
                } else {
                    lessonToSave = existingLesson
                }
                lessonIdToUse = existingLesson.lessonId
            } else {
                val newLesson = Lesson(lessonName = lessonName)
                lessonIdToUse = lessonDao.insertLesson(newLesson)
                lessonToSave = newLesson.copy(lessonId = lessonIdToUse)
                wasNewLesson = true
            }

            var wordsActuallyAdded = 0
            if (_newlyAddedWordPairs.isNotEmpty()) {
                val wordsToInsert = _newlyAddedWordPairs.map {
                    WordEntry(swedishWord = it.first, englishWord = it.second, lessonOwnerId = lessonIdToUse, isEnabled = true)
                }
                wordEntryDao.insertAllWordEntries(wordsToInsert)
                wordsActuallyAdded = _newlyAddedWordPairs.size
                _newlyAddedWordPairs.clear()
            }

            if (wasNewLesson) {
                _selectedLesson.value = lessonToSave // Select the new lesson
                _saveStatusMessage.value = "Lesson '${lessonName}' saved with $wordsActuallyAdded words."
            } else {
                // For existing lesson, check if anything changed
                if (lessonToSave.lessonName != _selectedLesson.value?.lessonName || wordsActuallyAdded > 0) {
                    _saveStatusMessage.value = "Lesson '${lessonName}' updated."
                } else {
                    _saveStatusMessage.value = "No changes to lesson '${lessonName}'."
                }
                 // If name changed, update the selected lesson value to reflect the new name immediately.
                if(lessonToSave.lessonName != _selectedLesson.value?.lessonName) {
                    _selectedLesson.value = lessonToSave
                }
            }
        }
    }

    fun prepareForNewLesson() {
        selectLesson(null) 
    }

    fun deleteWord(wordEntry: WordEntry) {
        viewModelScope.launch {
            wordEntryDao.deleteWordEntry(wordEntry)
            // Consider adding a status message here too if desired
        }
    }

    fun deleteSelectedLesson() {
        val lessonToDelete = _selectedLesson.value
        if (lessonToDelete == null) {
            _saveStatusMessage.value = "No lesson selected to delete."
            return
        }

        viewModelScope.launch {
            lessonDao.deleteLesson(lessonToDelete)
            _saveStatusMessage.value = "Lesson '${lessonToDelete.lessonName}' deleted."
            prepareForNewLesson() // Reset UI to a state for creating a new lesson
        }
    }

    fun toggleWordEnabled(wordEntry: WordEntry) {
        viewModelScope.launch {
            val updatedWordEntry = wordEntry.copy(isEnabled = !wordEntry.isEnabled)
            wordEntryDao.updateWordEntry(updatedWordEntry)
        }
    }

    fun importWordsFromFile(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            var importedCount = 0
            var failedCount = 0
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).useLines { lines ->
                        lines.forEach { line ->
                            val parts = line.split(',', limit = 2)
                            if (parts.size == 2) {
                                val swedish = parts[0].trim()
                                val english = parts[1].trim()
                                if (swedish.isNotBlank() && english.isNotBlank()) {
                                    _newlyAddedWordPairs.add(Pair(swedish, english))
                                    importedCount++
                                } else {
                                    failedCount++
                                }
                            } else {
                                failedCount++
                            }
                        }
                    }
                }
                if (importedCount > 0 || failedCount > 0) {
                    _importStatusMessage.value = "Imported $importedCount words. Failed lines: $failedCount."
                } else {
                    _importStatusMessage.value = "File was empty or no valid lines found."
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _importStatusMessage.value = "Error reading or parsing file: ${e.message}"
            }
        }
    }

    fun clearImportStatusMessage() {
        _importStatusMessage.value = ""
    }
    fun clearSaveStatusMessage() {
        _saveStatusMessage.value = ""
    }
}
