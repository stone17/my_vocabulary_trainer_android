package com.example.voctrainer.ui.lessons

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.voctrainer.data.AppDatabase
import com.example.voctrainer.data.UserPreferencesRepository
import com.example.voctrainer.databinding.FragmentLessonsBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LessonsFragment : Fragment() {

    private var _binding: FragmentLessonsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: LessonsViewModel
    private lateinit var lessonsAdapter: LessonsAdapter

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
        val userPreferencesRepository = UserPreferencesRepository(application)
        val viewModelFactory = LessonsViewModelFactory(lessonDao, userPreferencesRepository)
        viewModel = ViewModelProvider(this, viewModelFactory).get(LessonsViewModel::class.java)

        setupRecyclerView()
        observeLessons()
        observeSelectedLessons()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        lessonsAdapter = LessonsAdapter { lessonId ->
            viewModel.toggleLessonSelection(lessonId)
        }
        binding.recyclerViewLessons.apply {
            adapter = lessonsAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun observeLessons() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allLessons.collectLatest { lessons ->
                lessonsAdapter.submitList(lessons)
            }
        }
    }

    private fun observeSelectedLessons() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedLessonIds.collectLatest { selectedIds ->
                lessonsAdapter.setSelectedLessonIds(selectedIds)
                binding.buttonStartLesson.isEnabled = selectedIds.isNotEmpty()
                binding.buttonClearSelection.visibility = if (selectedIds.isNotEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun setupClickListeners() {
        binding.buttonStartLesson.setOnClickListener {
            val selectedIds = viewModel.selectedLessonIds.value
            if (selectedIds.isNotEmpty()) {
                // If there is only one id, just use that.
                val lessonIdToUse = if (selectedIds.size == 1) {
                    selectedIds.first()
                } else {
                    // Otherwise, create a temporary lesson.
                    // This logic will need to be implemented in the ViewModel.
                    // For now, let's just use the first selected ID.
                    selectedIds.first()
                }
                val action = LessonsFragmentDirections.actionNavLessonsToNavStudySession(
                    lessonId = lessonIdToUse,
                    isPracticeWorstWords = false
                )
                findNavController().navigate(action)
            } else {
                Toast.makeText(context, "Please select at least one lesson.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.buttonPracticeWorstWords.setOnClickListener {
            val action = LessonsFragmentDirections.actionNavLessonsToNavStudySession(
                lessonId = -1L, // Using a special ID for practicing worst words
                isPracticeWorstWords = true
            )
            findNavController().navigate(action)
        }

        binding.buttonClearSelection.setOnClickListener {
            viewModel.clearSelection()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
