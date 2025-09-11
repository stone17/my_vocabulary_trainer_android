package com.example.voctrainer.ui.user_statistics

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.voctrainer.databinding.ItemLessonCompletionStatBinding

class LessonStatsAdapter : ListAdapter<LessonCompletionStat, LessonStatsAdapter.ViewHolder>(LessonStatDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLessonCompletionStatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class ViewHolder(private val binding: ItemLessonCompletionStatBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: LessonCompletionStat) {
            binding.textViewLessonNameStat.text = item.lessonName
            binding.textViewLessonCompletionCount.text = "Completed: ${item.completionCount} time${if (item.completionCount != 1) "s" else ""}"
        }
    }
}

class LessonStatDiffCallback : DiffUtil.ItemCallback<LessonCompletionStat>() {
    override fun areItemsTheSame(oldItem: LessonCompletionStat, newItem: LessonCompletionStat): Boolean {
        return oldItem.lessonId == newItem.lessonId
    }

    override fun areContentsTheSame(oldItem: LessonCompletionStat, newItem: LessonCompletionStat): Boolean {
        return oldItem == newItem
    }
}
