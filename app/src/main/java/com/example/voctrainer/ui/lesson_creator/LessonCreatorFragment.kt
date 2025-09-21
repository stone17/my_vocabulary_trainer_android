package com.example.voctrainer.ui.lesson_creator

import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.voctrainer.data.AppDatabase
import com.example.voctrainer.data.Lesson
import com.example.voctrainer.databinding.FragmentLessonCreatorBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LessonCreatorFragment : Fragment() {

    private var _binding: FragmentLessonCreatorBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: LessonCreatorViewModel
    private lateinit var lessonsSpinnerAdapter: ArrayAdapter<String>
    private lateinit var wordEntryAdapter: WordEntryAdapter
    private var allLessonsList: List<Lesson> = emptyList()
    private val noLessonSelectedPrompt = "Select lesson to edit..."
    private var isSpinnerUserAction = true

    private lateinit var filePickerLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                val lessonName = getFileName(uri)?.substringBeforeLast(".") ?: "Imported Lesson"
                viewModel.importLessonFromFile(lessonName, it, requireContext().contentResolver)
            } ?: run {
                Toast.makeText(context, "No file selected.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLessonCreatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val application = requireNotNull(this.activity).application
        val lessonDao = AppDatabase.getDatabase(application).lessonDao()
        val wordEntryDao = AppDatabase.getDatabase(application).wordEntryDao()
        val viewModelFactory = LessonCreatorViewModelFactory(lessonDao, wordEntryDao)
        viewModel = ViewModelProvider(this, viewModelFactory).get(LessonCreatorViewModel::class.java)

        setupSpinner()
        setupWordsRecyclerView()
        setupClickListeners()

        observeLessonsForSpinner()
        observeSelectedLesson()
        observeSelectedLessonWords()
        observeStatusMessages()
    }

    private fun setupClickListeners() {
        binding.buttonCreateNewLesson.setOnClickListener {
            viewModel.prepareForNewLesson()
            binding.editTextSwedishWord.text.clear()
            binding.editTextEnglishWord.text.clear()
            Toast.makeText(context, "Ready to create a new lesson.", Toast.LENGTH_SHORT).show()
        }

        binding.buttonAddWord.setOnClickListener {
            val swedishWord = binding.editTextSwedishWord.text.toString()
            val englishWord = binding.editTextEnglishWord.text.toString()
            if (swedishWord.isNotBlank() && englishWord.isNotBlank()) {
                viewModel.addWordPair(swedishWord, englishWord)
                binding.editTextSwedishWord.text.clear()
                binding.editTextEnglishWord.text.clear()
                // Consider using the status message from ViewModel for consistency
                Toast.makeText(context, "Word pair added to buffer. Save lesson to persist.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Both Swedish and English words are required.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.buttonSaveLesson.setOnClickListener {
            val lessonName = binding.editTextLessonName.text.toString()
            viewModel.saveLesson(lessonName)
        }

        binding.buttonImportFile.setOnClickListener {
            filePickerLauncher.launch("text/plain")
        }

        binding.buttonDeleteLesson.setOnClickListener {
            showDeleteLessonConfirmationDialog()
        }
    }

    private fun showDeleteLessonConfirmationDialog() {
        val lessonToDelete = viewModel.selectedLesson.value
        if (lessonToDelete == null) {
            Toast.makeText(context, "No lesson selected to delete.", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Lesson")
            .setMessage("Are you sure you want to delete the lesson '${lessonToDelete.lessonName}'? This action cannot be undone and all its words will be removed.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteSelectedLesson()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupSpinner() {
        lessonsSpinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mutableListOf(noLessonSelectedPrompt))
        lessonsSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSelectLesson.adapter = lessonsSpinnerAdapter

        binding.spinnerSelectLesson.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!isSpinnerUserAction) {
                    isSpinnerUserAction = true
                    return
                }
                if (position == 0) {
                    viewModel.selectLesson(null)
                } else {
                    val selectedLesson = allLessonsList[position - 1]
                    viewModel.selectLesson(selectedLesson)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                 isSpinnerUserAction = true
                 viewModel.selectLesson(null)
            }
        }
    }

    private fun setupWordsRecyclerView() {
        wordEntryAdapter = WordEntryAdapter(
            onDeleteClicked = { wordEntry ->
                viewModel.deleteWord(wordEntry)
                Toast.makeText(context, "'${wordEntry.swedishWord}' deleted from buffer or lesson.", Toast.LENGTH_SHORT).show()
            },
            onToggleEnabledClicked = { wordEntry ->
                viewModel.toggleWordEnabled(wordEntry)
            }
        )
        binding.recyclerViewLessonWords.apply {
            adapter = wordEntryAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun observeLessonsForSpinner() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allLessons.collectLatest { lessons ->
                allLessonsList = lessons
                val lessonNames = mutableListOf(noLessonSelectedPrompt)
                lessonNames.addAll(lessons.map { it.lessonName })
                lessonsSpinnerAdapter.clear()
                lessonsSpinnerAdapter.addAll(lessonNames)
                lessonsSpinnerAdapter.notifyDataSetChanged()
                updateSpinnerSelection(viewModel.selectedLesson.value)
            }
        }
    }

    private fun observeSelectedLesson() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedLesson.collectLatest { lesson ->
                binding.editTextLessonName.setText(lesson?.lessonName ?: "")
                binding.buttonDeleteLesson.isVisible = lesson != null // Control visibility
                updateSpinnerSelection(lesson)
                if (lesson != null) {
                    binding.editTextSwedishWord.hint = "Add new Swedish word to '${lesson.lessonName}'"
                    binding.editTextEnglishWord.hint = "Add new English word to '${lesson.lessonName}'"
                } else {
                    binding.editTextSwedishWord.hint = "Swedish Word/Phrase (for new lesson)"
                    binding.editTextEnglishWord.hint = "English Word/Phrase (for new lesson)"
                }
            }
        }
    }

    private fun observeSelectedLessonWords() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedLessonWords.collectLatest { words ->
                wordEntryAdapter.submitList(words)
                val hasWords = words.isNotEmpty()
                binding.labelCurrentWords.isVisible = hasWords
                binding.recyclerViewLessonWords.isVisible = hasWords
                if (hasWords) {
                    binding.labelCurrentWords.text = "Current Words in Lesson (${words.size})"
                }
            }
        }
    }

    private fun observeStatusMessages() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.saveStatusMessage.collectLatest { message ->
                if (message.isNotBlank()) {
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    viewModel.clearSaveStatusMessage()
                }
            }
        }
    }

    private fun updateSpinnerSelection(selectedLesson: Lesson?) {
        isSpinnerUserAction = false
        if (selectedLesson == null) {
            binding.spinnerSelectLesson.setSelection(0, false)
        } else {
            val positionInAdapter = allLessonsList.indexOfFirst { it.lessonId == selectedLesson.lessonId }
            if (positionInAdapter != -1) {
                binding.spinnerSelectLesson.setSelection(positionInAdapter + 1, false)
            }
        }
        view?.post { isSpinnerUserAction = true } 
    }

    private fun getFileName(uri: Uri): String? {
        var fileName: String? = null
        val cursor: Cursor? = context?.contentResolver?.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val displayNameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1) {
                    fileName = it.getString(displayNameIndex)
                }
            }
        }
        return fileName
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerViewLessonWords.adapter = null 
        _binding = null
    }
}
