package com.example.mangaocr_demon.ui.bookcase

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mangaocr_demon.data.ChapterEntity
import com.example.mangaocr_demon.databinding.ItemChapterBinding
import java.text.SimpleDateFormat
import java.util.*

class AlbumChapterAdapter(
    private val onChapterClick: (ChapterEntity) -> Unit,
    private val onChapterLongClick: (ChapterEntity) -> Unit
) : ListAdapter<ChapterEntity, AlbumChapterAdapter.ChapterViewHolder>(DiffCallback) {

    companion object DiffCallback : DiffUtil.ItemCallback<ChapterEntity>() {
        override fun areItemsTheSame(oldItem: ChapterEntity, newItem: ChapterEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChapterEntity, newItem: ChapterEntity): Boolean {
            return oldItem == newItem
        }
    }

    inner class ChapterViewHolder(
        private val binding: ItemChapterBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentChapter: ChapterEntity? = null

        init {
            binding.root.setOnClickListener {
                currentChapter?.let { onChapterClick(it) }
            }

            binding.root.setOnLongClickListener {
                currentChapter?.let {
                    onChapterLongClick(it)
                    true
                } ?: false
            }
        }

        fun bind(chapter: ChapterEntity) {
            currentChapter = chapter

            binding.tvChapterNumber.text = "Chapter ${chapter.number}"
            binding.tvChapterTitle.text = chapter.title ?: "No title"

            // ✅ FIXED: Dùng createdAt thay vì created_at
            try {
                binding.tvChapterDate.text = formatDate(chapter.createdAt)
            } catch (e: Exception) {
                // View doesn't exist in layout, skip
            }
        }

        private fun formatDate(timestamp: Long): String {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapterViewHolder {
        val binding = ItemChapterBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChapterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChapterViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}