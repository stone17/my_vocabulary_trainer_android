package com.example.voctrainer.ui.user_statistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.voctrainer.data.AppDatabase
import com.example.voctrainer.databinding.FragmentUserStatisticsBinding
import com.google.android.material.divider.MaterialDividerItemDecoration
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class UserStatisticsFragment : Fragment() {

    private var _binding: FragmentUserStatisticsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: UserStatisticsViewModel
    private lateinit var lessonStatsAdapter: LessonStatsAdapter
    private lateinit var worstWordsAdapter: WorstWordsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val application = requireNotNull(activity).application
        val db = AppDatabase.getDatabase(application)
        val factory = UserStatisticsViewModelFactory(db.lessonDao(), db.wordEntryDao(), db.lessonSessionStatsDao())
        viewModel = ViewModelProvider(this, factory).get(UserStatisticsViewModel::class.java)

        setupRecyclerViews()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerViews() {
        lessonStatsAdapter = LessonStatsAdapter()
        binding.recyclerViewLessonStats.apply {
            adapter = lessonStatsAdapter
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(MaterialDividerItemDecoration(context, LinearLayoutManager.VERTICAL))
        }

        worstWordsAdapter = WorstWordsAdapter()
        binding.recyclerViewWorstWords.apply {
            adapter = worstWordsAdapter
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(MaterialDividerItemDecoration(context, LinearLayoutManager.VERTICAL))
        }
    }

    private fun setupClickListeners() {
        binding.buttonClearStatistics.setOnClickListener {
            viewModel.requestClearAllStatistics()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.progressBarStats.isVisible = isLoading
                binding.contentLayoutStats.isVisible = !isLoading
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.totalStudyTimeFormatted.collectLatest {
                binding.textViewTotalStudyTime.text = it
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.lessonCompletionStats.collectLatest { stats ->
                lessonStatsAdapter.submitList(stats)
                binding.textViewNoLessonStats.isVisible = stats.isEmpty()
                binding.recyclerViewLessonStats.isVisible = stats.isNotEmpty()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.worstWords.collectLatest { words ->
                worstWordsAdapter.submitList(words)
                binding.textViewNoWorstWords.isVisible = words.isEmpty()
                binding.recyclerViewWorstWords.isVisible = words.isNotEmpty()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.showClearConfirmationDialog.collectLatest { showDialog ->
                if (showDialog) {
                    showClearStatsConfirmationDialog()
                }
            }
        }
    }

    private fun showClearStatsConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Clear Statistics")
            .setMessage("Are you sure you want to clear all learning statistics? This action cannot be undone.")
            .setPositiveButton("Clear All") { _, _ ->
                viewModel.confirmClearAllStatistics()
            }
            .setNegativeButton("Cancel") { _, _ ->
                viewModel.cancelClearAllStatistics()
            }
            .setOnDismissListener { // Ensure ViewModel state is updated if dialog is dismissed e.g. by back button
                viewModel.cancelClearAllStatistics() 
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerViewLessonStats.adapter = null // Clear adapter to avoid leaks
        binding.recyclerViewWorstWords.adapter = null
        _binding = null
    }
}
