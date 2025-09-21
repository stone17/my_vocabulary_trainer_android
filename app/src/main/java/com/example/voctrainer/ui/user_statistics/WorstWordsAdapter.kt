package com.example.voctrainer.ui.user_statistics

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.voctrainer.data.WordEntry
import com.example.voctrainer.databinding.ItemWorstWordBinding
import kotlin.math.roundToInt

class WorstWordsAdapter : ListAdapter<WordEntry, WorstWordsAdapter.ViewHolder>(WordEntryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemWorstWordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class ViewHolder(private val binding: ItemWorstWordBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: WordEntry) {
            binding.textViewSwedishWordWorst.text = item.swedishWord
            binding.textViewEnglishWordWorst.text = "(${item.englishWord})"
            binding.textViewIncorrectCountWorst.text =
                "Incorrect: ${item.timesIncorrectInStudy} time${if (item.timesIncorrectInStudy != 1) "s" else ""} " +
                "| Presented: ${item.timesPresentedInStudy} time${if (item.timesPresentedInStudy != 1) "s" else ""}"

            if (item.timesPresentedInStudy > 0) {
                val correctnessPercentage = (item.timesPresentedInStudy - item.timesIncorrectInStudy).toFloat() / item.timesPresentedInStudy * 100
                binding.correctnessProgressBar.progress = correctnessPercentage.roundToInt()
            } else {
                binding.correctnessProgressBar.progress = 0
            }
        }
    }
}

class WordEntryDiffCallback : DiffUtil.ItemCallback<WordEntry>() {
    override fun areItemsTheSame(oldItem: WordEntry, newItem: WordEntry): Boolean {
        return oldItem.wordId == newItem.wordId
    }

    override fun areContentsTheSame(oldItem: WordEntry, newItem: WordEntry): Boolean {
        return oldItem == newItem
    }
}
