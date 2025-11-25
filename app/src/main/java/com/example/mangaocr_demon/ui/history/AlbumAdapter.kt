package com.example.mangaocr_demon.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mangaocr_demon.data.AlbumEntity
import com.example.mangaocr_demon.databinding.ItemAlbumBinding
import android.graphics.Color

class AlbumAdapter(
    private val onAlbumClick: (AlbumEntity) -> Unit,
    private val onAlbumLongClick: (AlbumEntity) -> Unit
) : ListAdapter<AlbumEntity, AlbumAdapter.AlbumViewHolder>(DiffCallback) {

    companion object DiffCallback : DiffUtil.ItemCallback<AlbumEntity>() {
        override fun areItemsTheSame(oldItem: AlbumEntity, newItem: AlbumEntity) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: AlbumEntity, newItem: AlbumEntity) = oldItem == newItem
    }

    inner class AlbumViewHolder(private val binding: ItemAlbumBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(album: AlbumEntity) {
            binding.tvAlbumName.text = album.title
            binding.tvAlbumDescription.text = album.description ?: "Không có mô tả"
            binding.cardAlbum.setCardBackgroundColor(Color.parseColor("#4CAF50"))

            binding.root.setOnClickListener { onAlbumClick(album) }
            binding.root.setOnLongClickListener { onAlbumLongClick(album); true }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        AlbumViewHolder(ItemAlbumBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) = holder.bind(getItem(position))
}
