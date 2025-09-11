package com.example.voctrainer.ui.lessons

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.voctrainer.data.AppDatabase
import com.example.voctrainer.data.Lesson
import com.example.voctrainer.databinding.FragmentLessonsBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LessonsFragment : Fragment() {

    private var _binding: FragmentLessonsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: LessonsViewModel
    private lateinit var lessonsSpinnerAdapter: ArrayAdapter<String>
    private var allLessonsList: List<Lesson> = emptyList()
    private val noLessonSelectedPrompt = "Select a lesson..."
    private var isSpinnerUserAction = true // To prevent onItemSelected from firing due to programmatic changes


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLessonsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val application = requireNotNull(this.activity).application
        val lessonDao = AppDatabase.getDatabase(application).lessonDao()
        val viewModelFactory = LessonsViewModelFactory(lessonDao)
        viewModel = ViewModelProvider(this, viewModelFactory).get(LessonsViewModel::class.java)

        setupSpinner()
        observeLessonsForSpinner()
        observeSelectedLesson()
        setupClickListeners() // Call new method to setup all click listeners
    }

    private fun setupClickListeners() {
        binding.buttonStartLesson.setOnClickListener {
            viewModel.selectedLesson.value?.let { lesson ->
                val action = LessonsFragmentDirections.actionNavLessonsToNavStudySession(lessonId = lesson.lessonId, isPracticeWorstWords = false)
                findNavController().navigate(action)
            } ?: run {
                Toast.makeText(context, "Please select a lesson first.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.buttonPracticeWorstWords.setOnClickListener {
            // For worst words, lessonId can be a placeholder like 0L or -1L as it won't be used by StudyViewModel in this mode
            val action = LessonsFragmentDirections.actionNavLessonsToNavStudySession(lessonId = 0L, isPracticeWorstWords = true)
            findNavController().navigate(action)
        }
    }

    private fun setupSpinner() {
        lessonsSpinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mutableListOf(noLessonSelectedPrompt))
        lessonsSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerLessonsSelection.adapter = lessonsSpinnerAdapter

        binding.spinnerLessonsSelection.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!isSpinnerUserAction) {
                    isSpinnerUserAction = true 
                    return
                }
                if (position == 0) { // Prompt selected
                    viewModel.selectLesson(null)
                } else {
                    if (allLessonsList.isNotEmpty() && position -1 < allLessonsList.size) {
                        val selected = allLessonsList[position - 1] // Adjust for prompt
                        viewModel.selectLesson(selected)
                    } else {
                        Log.w("LessonsFragment", "Spinner selection out of sync with allLessonsList.")
                        viewModel.selectLesson(null) // Fallback to no selection
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                isSpinnerUserAction = true
                viewModel.selectLesson(null)
            }
        }
    }

    private fun observeLessonsForSpinner() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allLessons.collectLatest { lessons ->
                allLessonsList = lessons
                val lessonNames = mutableListOf(noLessonSelectedPrompt)
                lessonNames.addAll(lessons.map { it.lessonName })
                val previousSelectedLessonId = viewModel.selectedLesson.value?.lessonId
                
                lessonsSpinnerAdapter.clear()
                lessonsSpinnerAdapter.addAll(lessonNames)
                lessonsSpinnerAdapter.notifyDataSetChanged() 

                val lessonToRestore = previousSelectedLessonId?.let { id -> lessons.find { it.lessonId == id } }
                updateSpinnerSelection(lessonToRestore ?: viewModel.selectedLesson.value)
            }
        }
    }

    private fun observeSelectedLesson() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedLesson.collectLatest { lesson ->
                binding.buttonStartLesson.isEnabled = lesson != null
                updateSpinnerSelection(lesson)
            }
        }
    }

    private fun updateSpinnerSelection(selectedLesson: Lesson?) {
        isSpinnerUserAction = false
        if (selectedLesson == null) {
            if (binding.spinnerLessonsSelection.selectedItemPosition != 0) {
                 binding.spinnerLessonsSelection.setSelection(0, false)
            }
        } else {
            val positionInAllLessonsList = allLessonsList.indexOfFirst { it.lessonId == selectedLesson.lessonId }
            if (positionInAllLessonsList != -1) {
                val targetSpinnerIndex = positionInAllLessonsList + 1
                if (targetSpinnerIndex < lessonsSpinnerAdapter.count) {
                    if (binding.spinnerLessonsSelection.selectedItemPosition != targetSpinnerIndex) {
                        binding.spinnerLessonsSelection.setSelection(targetSpinnerIndex, false)
                    }
                } else {
                    Log.w("LessonsFragment", "updateSpinnerSelection: Target index $targetSpinnerIndex out of bounds for adapter size ${lessonsSpinnerAdapter.count}. Selecting prompt.")
                    if (binding.spinnerLessonsSelection.selectedItemPosition != 0) {
                        binding.spinnerLessonsSelection.setSelection(0, false)
                    }
                }
            } else {
                Log.w("LessonsFragment", "updateSpinnerSelection: Selected lesson (ID: ${selectedLesson.lessonId}) not found in allLessonsList. Selecting prompt.")
                if (binding.spinnerLessonsSelection.selectedItemPosition != 0) {
                     binding.spinnerLessonsSelection.setSelection(0, false)
                }
            }
        }
        view?.post { isSpinnerUserAction = true }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
