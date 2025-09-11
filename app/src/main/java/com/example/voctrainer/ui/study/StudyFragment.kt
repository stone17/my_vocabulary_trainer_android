package com.example.voctrainer.ui.study

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.voctrainer.R // Added import for R class
import com.example.voctrainer.data.AppDatabase
import com.example.voctrainer.databinding.FragmentStudyBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class StudyFragment : Fragment() {

    private var _binding: FragmentStudyBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: StudyViewModel
    private val args: StudyFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStudyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val lessonId = args.lessonId
        val isPracticeWorstWords = args.isPracticeWorstWords // Get the new arg

        val application = requireNotNull(this.activity).application
        val db = AppDatabase.getDatabase(application)
        val wordEntryDao = db.wordEntryDao()
        val lessonSessionStatsDao = db.lessonSessionStatsDao()

        val viewModelFactory = StudyViewModelFactory(
            lessonId,
            wordEntryDao,
            lessonSessionStatsDao,
            isPracticeWorstWords // Pass it here
        )
        viewModel = ViewModelProvider(this, viewModelFactory).get(StudyViewModel::class.java)

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.buttonCheckAnswer.setOnClickListener {
            val userAnswer = binding.editTextEnglishAnswer.text.toString()
            viewModel.checkAnswer(userAnswer)
        }

        binding.buttonNextWord.setOnClickListener {
            viewModel.proceedToNextWord()
            binding.editTextEnglishAnswer.text?.clear()
            binding.editTextEnglishAnswer.requestFocus()
        }

        binding.buttonFinishLesson.setOnClickListener {
            // Stats will be saved by onDestroyView if not already saved by finishing the lesson
            findNavController().popBackStack()
        }

        binding.buttonRestartLesson.setOnClickListener {
            viewModel.restartLesson()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.sessionTitle.collectLatest { title ->
                (activity as? AppCompatActivity)?.supportActionBar?.title = title
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentSwedishWord.collectLatest { swedishWord ->
                val isLessonOngoing = !viewModel.isLessonFinished.value
                val hasWord = !swedishWord.isNullOrEmpty()
                binding.textViewSwedishWordStudyLabel.isVisible = hasWord && isLessonOngoing
                binding.textViewSwedishWordStudy.isVisible = hasWord && isLessonOngoing
                binding.textViewSwedishWordStudy.text = swedishWord ?: ""
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.feedbackMessage.collectLatest {
                binding.textViewFeedback.text = it
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.userAnswerFeedbackDisplay.collectLatest { feedbackSpannable ->
                binding.textViewUserAnswerFeedback.text = feedbackSpannable
                binding.textViewUserAnswerFeedback.isVisible = feedbackSpannable != null
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.sessionSummary.collectLatest { // Renamed from lessonSummary
                binding.textViewLessonSummary.text = it
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.showCorrectAnswer.collectLatest { show ->
                // Visibility of the correct answer area is also contingent on the user answer feedback NOT being shown, if that is preferred.
                // For now, they can both show.
                val shouldShowCorrectAnswerText = show && !viewModel.isLessonFinished.value
                binding.textViewCorrectAnswerDisplayLabel.isVisible = shouldShowCorrectAnswerText
                binding.textViewCorrectAnswerDisplay.isVisible = shouldShowCorrectAnswerText
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.correctAnswerText.collectLatest {
                binding.textViewCorrectAnswerDisplay.text = it
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isAnswerChecked.collectLatest { isChecked ->
                if (!viewModel.isLessonFinished.value) {
                    binding.buttonCheckAnswer.isEnabled = !isChecked
                    binding.buttonNextWord.isVisible = isChecked
                    binding.editTextEnglishAnswer.isEnabled = !isChecked
                    if (isChecked && binding.buttonNextWord.isVisible) {
                        binding.buttonNextWord.requestFocus()
                    } else if (!isChecked && binding.inputLayoutEnglishAnswer.isVisible) {
                        binding.editTextEnglishAnswer.requestFocus()
                    }
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLessonFinished.collectLatest { isFinished ->
                binding.layoutLessonEndOptions.isVisible = isFinished

                if (isFinished) {
                    binding.textViewSwedishWordStudyLabel.isVisible = false
                    binding.textViewSwedishWordStudy.isVisible = false
                    binding.inputLayoutEnglishAnswer.isVisible = false
                    binding.buttonCheckAnswer.isVisible = false
                    binding.buttonNextWord.isVisible = false
                    // Keep correct answer display and user feedback display visible if the lesson just ended
                    // and an answer was checked.
                     if (!_binding?.textViewUserAnswerFeedback?.isVisible!! && !_binding?.textViewCorrectAnswerDisplayLabel?.isVisible!!) {
                         // If neither feedback is up, hide them (e.g. empty lesson finished)
                        binding.textViewCorrectAnswerDisplayLabel.isVisible = false
                        binding.textViewCorrectAnswerDisplay.isVisible = false
                        binding.textViewUserAnswerFeedback.isVisible = false
                    }

                } else {
                    binding.textViewSwedishWordStudyLabel.isVisible = true 
                    binding.textViewSwedishWordStudy.isVisible = true    
                    binding.inputLayoutEnglishAnswer.isVisible = true
                    binding.editTextEnglishAnswer.isEnabled = true 
                    binding.buttonCheckAnswer.isVisible = true
                    binding.buttonCheckAnswer.isEnabled = !viewModel.isAnswerChecked.value 
                    binding.buttonNextWord.isVisible = viewModel.isAnswerChecked.value      
                    binding.textViewCorrectAnswerDisplayLabel.isVisible = false 
                    binding.textViewCorrectAnswerDisplay.isVisible = false
                    binding.textViewUserAnswerFeedback.isVisible = false // Hide user answer feedback on new word
                    
                    binding.editTextEnglishAnswer.text?.clear()
                    if (binding.inputLayoutEnglishAnswer.isVisible) { 
                        binding.editTextEnglishAnswer.requestFocus()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        // Attempt to save stats if the session wasn't fully completed or if already completed and not saved.
        // The ViewModel's saveSessionStats() has logic to prevent duplicate saves.
        if (::viewModel.isInitialized) { // Ensure viewModel is initialized before calling
            viewModel.saveSessionStats()
        }
        super.onDestroyView()
        (activity as? AppCompatActivity)?.supportActionBar?.title = getString(R.string.app_name) // Reset title or to default
        _binding = null
    }
}
