package com.example.mangaocr_demon.ui.dialogs.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mangaocr_demon.data.AlbumEntity
import com.example.mangaocr_demon.databinding.ItemAlbumSelectionBinding

class AlbumSelectionAdapter(
    private val onAlbumToggle: (albumId: Long, isSelected: Boolean) -> Unit
) : ListAdapter<AlbumEntity, AlbumSelectionAdapter.ViewHolder>(DiffCallback) {

    private val selectedAlbumIds = mutableSetOf<Long>()

    companion object DiffCallback : DiffUtil.ItemCallback<AlbumEntity>() {
        override fun areItemsTheSame(oldItem: AlbumEntity, newItem: AlbumEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AlbumEntity, newItem: AlbumEntity): Boolean {
            return oldItem == newItem
        }
    }

    inner class ViewHolder(
        private val binding: ItemAlbumSelectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(album: AlbumEntity) {
            // Sử dụng title thay vì name
            binding.tvAlbumName.text = album.title
            binding.tvAlbumDescription.text = album.description ?: "Không có mô tả"
            binding.checkbox.isChecked = selectedAlbumIds.contains(album.id)

            // Màu nền album
            try {
                val color = Color.parseColor(album.color)
                binding.colorIndicator.setBackgroundColor(color)
            } catch (e: Exception) {
                binding.colorIndicator.setBackgroundColor(Color.parseColor("#4CAF50"))
            }

            // Click toàn bộ item
            binding.root.setOnClickListener {
                toggleSelection(album.id)
            }

            // Click checkbox
            binding.checkbox.setOnCheckedChangeListener(null)
            binding.checkbox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked != selectedAlbumIds.contains(album.id)) {
                    toggleSelection(album.id)
                }
            }
        }

        private fun toggleSelection(albumId: Long) {
            val isSelected = if (selectedAlbumIds.contains(albumId)) {
                selectedAlbumIds.remove(albumId)
                false
            } else {
                selectedAlbumIds.add(albumId)
                true
            }

            binding.checkbox.isChecked = isSelected
            onAlbumToggle(albumId, isSelected)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAlbumSelectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // Hàm để set initial selected albums
    fun submitList(list: List<AlbumEntity>?, preSelectedIds: Set<Long>) {
        selectedAlbumIds.clear()
        selectedAlbumIds.addAll(preSelectedIds)
        super.submitList(list) // Gọi ListAdapter submitList đúng
    }
}
