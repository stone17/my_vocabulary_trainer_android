package com.example.voctrainer.ui.lessons

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.voctrainer.data.Lesson
import com.example.voctrainer.databinding.ItemLessonSelectableBinding

class LessonsAdapter(
    private val onLessonSelected: (Long) -> Unit
) : ListAdapter<Lesson, LessonsAdapter.LessonViewHolder>(LessonDiffCallback()) {

    private var selectedLessonIds: Set<Long> = emptySet()

    fun setSelectedLessonIds(ids: Set<Long>) {
        selectedLessonIds = ids
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LessonViewHolder {
        val binding = ItemLessonSelectableBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LessonViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LessonViewHolder, position: Int) {
        val lesson = getItem(position)
        holder.bind(lesson, selectedLessonIds.contains(lesson.lessonId))
    }

    inner class LessonViewHolder(private val binding: ItemLessonSelectableBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            val clickListener = {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    val lesson = getItem(adapterPosition)
                    onLessonSelected(lesson.lessonId)
                }
            }
            binding.root.setOnClickListener { clickListener() }
            binding.checkboxLessonSelected.setOnClickListener { clickListener() }
        }

        fun bind(lesson: Lesson, isSelected: Boolean) {
            binding.textViewLessonName.text = lesson.lessonName
            binding.checkboxLessonSelected.isChecked = isSelected
        }
    }
}

class LessonDiffCallback : DiffUtil.ItemCallback<Lesson>() {
    override fun areItemsTheSame(oldItem: Lesson, newItem: Lesson): Boolean {
        return oldItem.lessonId == newItem.lessonId
    }

    override fun areContentsTheSame(oldItem: Lesson, newItem: Lesson): Boolean {
        return oldItem == newItem
    }
}
