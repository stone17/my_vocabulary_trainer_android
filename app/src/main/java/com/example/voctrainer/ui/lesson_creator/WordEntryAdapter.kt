package com.example.voctrainer.ui.lesson_creator

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.voctrainer.data.WordEntry
import com.example.voctrainer.databinding.ItemWordEntryEditBinding

class WordEntryAdapter(
    private val onDeleteClicked: (WordEntry) -> Unit,
    private val onToggleEnabledClicked: (WordEntry) -> Unit
    // private val onEditClicked: (WordEntry) -> Unit // For future text editing
) : ListAdapter<WordEntry, WordEntryAdapter.WordEntryViewHolder>(WordEntryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordEntryViewHolder {
        val binding = ItemWordEntryEditBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WordEntryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WordEntryViewHolder, position: Int) {
        val currentWordEntry = getItem(position)
        holder.bind(currentWordEntry)
    }

    inner class WordEntryViewHolder(private val binding: ItemWordEntryEditBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.buttonDeleteWord.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeleteClicked(getItem(position))
                }
            }
            binding.switchWordEnabled.setOnCheckedChangeListener { compoundButton, _ -> // isChecked param can be ignored as VM is source of truth
                // Only trigger the toggle if the change was due to user interaction
                if (compoundButton.isPressed) {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onToggleEnabledClicked(getItem(position))
                        // The ViewModel will handle the state flip and data update.
                        // The list will re-submit, and the switch will reflect the new state from the DB.
                    }
                }
            }
            // itemView.setOnClickListener { // For future editing of the word text
            //     val position = adapterPosition
            //     if (position != RecyclerView.NO_POSITION) {
            //         onEditClicked(getItem(position))
            //     }
            // }
        }

        fun bind(wordEntry: WordEntry) {
            binding.textViewSwedishWordEdit.text = wordEntry.swedishWord
            binding.textViewEnglishWordEdit.text = wordEntry.englishWord
            // Set checked state without triggering listener if possible by managing listener attachment,
            // but isPressed check in the listener is a more common and robust approach.
            binding.switchWordEnabled.isChecked = wordEntry.isEnabled
        }
    }

    private class WordEntryDiffCallback : DiffUtil.ItemCallback<WordEntry>() {
        override fun areItemsTheSame(oldItem: WordEntry, newItem: WordEntry): Boolean {
            return oldItem.wordId == newItem.wordId
        }

        override fun areContentsTheSame(oldItem: WordEntry, newItem: WordEntry): Boolean {
            return oldItem == newItem
        }
    }
}
