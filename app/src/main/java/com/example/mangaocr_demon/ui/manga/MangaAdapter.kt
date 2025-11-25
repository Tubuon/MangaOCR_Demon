package com.example.mangaocr_demon.ui.manga

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mangaocr_demon.data.MangaEntity
import com.example.mangaocr_demon.databinding.ItemMangaBinding

class MangaAdapter(
    private val onItemClick: (MangaEntity) -> Unit,
    private val onDeleteClick: (MangaEntity) -> Unit // 1. THÊM THAM SỐ NÀY
) : ListAdapter<MangaEntity, MangaAdapter.MangaViewHolder>(MangaDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MangaViewHolder {
        val binding = ItemMangaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MangaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MangaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MangaViewHolder(
        private val binding: ItemMangaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(manga: MangaEntity) {
            binding.apply {
                // Bind data
                tvMangaTitle.text = manga.title
                tvMangaDesc.text = manga.description ?: "Chưa có mô tả"
                tvChapterCount.text = "0 ch"
                tvPageCount.text = "0 trang"

                // 2. GÁN SỰ KIỆN CLICK CHO VÙNG NỘI DUNG CHÍNH
                clickableArea.setOnClickListener {
                    onItemClick(manga)
                }

                ivDeleteIcon.setOnClickListener {
                    onDeleteClick(manga)
                }
            }
        }
    }

    class MangaDiffCallback : DiffUtil.ItemCallback<MangaEntity>() {
        override fun areItemsTheSame(oldItem: MangaEntity, newItem: MangaEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MangaEntity, newItem: MangaEntity): Boolean {
            return oldItem == newItem
        }
    }
}