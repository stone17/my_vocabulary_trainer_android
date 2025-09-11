package com.example.voctrainer.ui.study

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voctrainer.data.LessonSessionStats
import com.example.voctrainer.data.LessonSessionStatsDao
import com.example.voctrainer.data.WordEntry
import com.example.voctrainer.data.WordEntryDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StudyViewModel(
    private val lessonId: Long,
    private val wordEntryDao: WordEntryDao,
    private val lessonSessionStatsDao: LessonSessionStatsDao,
    private val isPracticeWorstWords: Boolean
) : ViewModel() {

    private var wordsInSession: List<WordEntry> = emptyList()
    private var currentWordIndex = -1
    private var sessionStartTimeMillis: Long = 0L
    private var statsSavedForCurrentSession = false

    private val _currentSwedishWord = MutableStateFlow<String?>(null)
    val currentSwedishWord: StateFlow<String?> = _currentSwedishWord.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>("")
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private val _showCorrectAnswer = MutableStateFlow(false)
    val showCorrectAnswer: StateFlow<Boolean> = _showCorrectAnswer.asStateFlow()

    private val _correctAnswerText = MutableStateFlow<String?>("")
    val correctAnswerText: StateFlow<String?> = _correctAnswerText.asStateFlow()

    private val _isAnswerChecked = MutableStateFlow(false)
    val isAnswerChecked: StateFlow<Boolean> = _isAnswerChecked.asStateFlow()

    private val _isLessonFinished = MutableStateFlow(false)
    val isLessonFinished: StateFlow<Boolean> = _isLessonFinished.asStateFlow()

    private val _correctAnswersCount = MutableStateFlow(0)
    val correctAnswersCount: StateFlow<Int> = _correctAnswersCount.asStateFlow()

    private val _incorrectAnswersCount = MutableStateFlow(0)
    val incorrectAnswersCount: StateFlow<Int> = _incorrectAnswersCount.asStateFlow()

    private val _totalWordsInSession = MutableStateFlow(0)
    val totalWordsInSession: StateFlow<Int> = _totalWordsInSession.asStateFlow()

    private val _sessionSummary = MutableStateFlow("")
    val sessionSummary: StateFlow<String> = _sessionSummary.asStateFlow()

    private val _sessionTitle = MutableStateFlow("Studying Lesson")
    val sessionTitle: StateFlow<String> = _sessionTitle.asStateFlow()

    private val _userAnswerFeedbackDisplay = MutableStateFlow<Spannable?>(null)
    val userAnswerFeedbackDisplay: StateFlow<Spannable?> = _userAnswerFeedbackDisplay.asStateFlow()

    init {
        loadWords()
    }

    private fun resetStatesForNewSession() {
        currentWordIndex = -1
        _correctAnswersCount.value = 0
        _incorrectAnswersCount.value = 0
        _currentSwedishWord.value = null
        _feedbackMessage.value = ""
        _showCorrectAnswer.value = false
        _correctAnswerText.value = ""
        _isAnswerChecked.value = false
        _isLessonFinished.value = false
        _sessionSummary.value = ""
        _userAnswerFeedbackDisplay.value = null // Clear feedback
        sessionStartTimeMillis = System.currentTimeMillis()
        statsSavedForCurrentSession = false
    }

    private fun loadWords() {
        resetStatesForNewSession()
        viewModelScope.launch {
            if (isPracticeWorstWords) {
                _sessionTitle.value = "Practicing Worst Words"
                wordsInSession = wordEntryDao.getWorstWords(10).firstOrNull() ?: emptyList()
                if (wordsInSession.isEmpty()) {
                    _feedbackMessage.value = "No words to practice yet, or all words are well known!"
                }
            } else {
                 _sessionTitle.value = "Studying Lesson"
                wordsInSession = wordEntryDao.getEnabledWordsForLessonOrderedRandomly(lessonId).firstOrNull() ?: emptyList()
                if (wordsInSession.isEmpty()) {
                    _feedbackMessage.value = "This lesson has no enabled words to study."
                }
            }
            _totalWordsInSession.value = wordsInSession.size

            if (wordsInSession.isEmpty()) {
                _isAnswerChecked.value = true
                _isLessonFinished.value = true
                updateLessonSummary()
                if (!isPracticeWorstWords) {
                    saveSessionStats()
                }
            } else {
                proceedToNextWord()
            }
        }
    }

    private fun updateLessonSummary() {
        val correct = _correctAnswersCount.value
        val incorrect = _incorrectAnswersCount.value
        val presented = correct + incorrect
        val title = if (isPracticeWorstWords) "Total words in practice:" else "Total words in lesson:"

        _sessionSummary.value = """
            $title ${wordsInSession.size}
            Answered: $presented
            Correct: $correct
            Incorrect: $incorrect
        """.trimIndent()
    }

    fun checkAnswer(userAnswer: String) {
        if (currentWordIndex < 0 || currentWordIndex >= wordsInSession.size || _isAnswerChecked.value) {
            return
        }
        val currentWord = wordsInSession[currentWordIndex]
        val correctAnswer = currentWord.englishWord

        val isMatch = when {
            userAnswer.isEmpty() && correctAnswer.isEmpty() -> true
            userAnswer.isEmpty() || correctAnswer.isEmpty() -> false
            // Length check is implicitly handled by character-wise comparison now for feedback
            else -> {
                if (userAnswer.length != correctAnswer.length) false // Still a mismatch if lengths differ
                else {
                    val firstCharUser = userAnswer.first()
                    val firstCharCorrect = correctAnswer.first()
                    val firstCharMatch = firstCharUser.equals(firstCharCorrect, ignoreCase = true)
                    if (userAnswer.length == 1) {
                        firstCharMatch
                    } else {
                        firstCharMatch && userAnswer.substring(1) == correctAnswer.substring(1)
                    }
                }
            }
        }

        if (isMatch) {
            _feedbackMessage.value = "Correct!"
            _correctAnswersCount.update { it + 1 }
            _showCorrectAnswer.value = false
            _userAnswerFeedbackDisplay.value = null
            viewModelScope.launch { wordEntryDao.incrementTimesCorrect(currentWord.wordId) }
        } else {
            _feedbackMessage.value = "Incorrect."
            _incorrectAnswersCount.update { it + 1 }
            _correctAnswerText.value = correctAnswer
            _showCorrectAnswer.value = true
            generateUserAnswerFeedback(userAnswer, correctAnswer)
            viewModelScope.launch { wordEntryDao.incrementTimesIncorrect(currentWord.wordId) }
        }
        _isAnswerChecked.value = true
        updateLessonSummary()
    }

    private fun generateUserAnswerFeedback(userAnswer: String, correctAnswer: String) {
        val feedbackSpan = SpannableStringBuilder(userAnswer)
        val lenUser = userAnswer.length
        val lenCorrect = correctAnswer.length

        for (i in 0 until lenUser) {
            val userChar = userAnswer[i]
            val isCharCorrect = if (i < lenCorrect) {
                val correctChar = correctAnswer[i]
                if (i == 0) {
                    userChar.equals(correctChar, ignoreCase = true)
                } else {
                    userChar == correctChar
                }
            } else {
                false // User answer is longer, so extra chars are incorrect
            }

            val color = if (isCharCorrect) Color.GREEN else Color.RED
            feedbackSpan.setSpan(
                ForegroundColorSpan(color),
                i,
                i + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        _userAnswerFeedbackDisplay.value = feedbackSpan
    }

    fun proceedToNextWord() {
        currentWordIndex++
        if (currentWordIndex < wordsInSession.size) {
            val nextWord = wordsInSession[currentWordIndex]
            _currentSwedishWord.value = nextWord.swedishWord
            _feedbackMessage.value = ""
            _showCorrectAnswer.value = false
            _correctAnswerText.value = ""
            _userAnswerFeedbackDisplay.value = null // Clear feedback
            _isAnswerChecked.value = false
            _isLessonFinished.value = false
            viewModelScope.launch { 
                wordEntryDao.incrementTimesPresented(nextWord.wordId)
            }
        } else {
            _currentSwedishWord.value = null
            _feedbackMessage.value = if (isPracticeWorstWords) "Practice finished! Great job!" else "Lesson finished! Great job!"
            _showCorrectAnswer.value = false
            _correctAnswerText.value = ""
            // _userAnswerFeedbackDisplay is already null or will be if lesson is restarted
            _isAnswerChecked.value = true 
            _isLessonFinished.value = true
            updateLessonSummary()
            if (!isPracticeWorstWords) {
                 saveSessionStats()
            }
        }
    }

    fun saveSessionStats() {
        if (statsSavedForCurrentSession || isPracticeWorstWords) {
            return
        }
        if (lessonId == 0L && !isPracticeWorstWords) { 
            println("StudyViewModel: Attempted to save stats for a regular lesson with lessonId 0L. Skipping.")
            return
        }
        if (sessionStartTimeMillis == 0L) {
             println("StudyViewModel: Session start time is 0. Skipping save.")
            return
        }

        val endTimeMillis = System.currentTimeMillis()
        val durationMillis = endTimeMillis - sessionStartTimeMillis
        
        if (durationMillis <= 0 && wordsInSession.isNotEmpty()) { 
            println("StudyViewModel: Session duration is 0 or negative for a non-empty lesson. Skipping save.")
            return
        }

        val sessionStats = LessonSessionStats(
            lessonId = lessonId,
            startTimeMillis = sessionStartTimeMillis,
            endTimeMillis = endTimeMillis,
            durationMillis = durationMillis,
            sessionTotalWords = wordsInSession.size, 
            sessionCorrectAnswers = _correctAnswersCount.value,
            sessionIncorrectAnswers = _incorrectAnswersCount.value
        )
        
        statsSavedForCurrentSession = true 

        viewModelScope.launch { 
            lessonSessionStatsDao.insertSession(sessionStats)
            println("StudyViewModel: Session stats saved for lessonId $lessonId. Duration: $durationMillis ms")
        }
    }

    fun restartLesson() {
        loadWords() 
    }
}
