package com.example.mangaocr_demon.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mangaocr_demon.R
import com.example.mangaocr_demon.data.HistoryEntity
import com.example.mangaocr_demon.databinding.ItemHistoryBinding

class HistoryAdapter :
    ListAdapter<HistoryEntity, HistoryAdapter.HistoryViewHolder>(DiffCallback) {

    companion object DiffCallback : DiffUtil.ItemCallback<HistoryEntity>() {
        override fun areItemsTheSame(oldItem: HistoryEntity, newItem: HistoryEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: HistoryEntity, newItem: HistoryEntity): Boolean {
            return oldItem == newItem
        }
    }

    inner class HistoryViewHolder(
        private val binding: ItemHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HistoryEntity) {
            binding.tvOCR.text = item.ocrText
            binding.tvTranslated.text = item.translatedText
            binding.tvTime.text = item.timestamp

            Glide.with(binding.root.context)
                .load(item.imageUri)
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .centerCrop()
                .into(binding.imageView)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}