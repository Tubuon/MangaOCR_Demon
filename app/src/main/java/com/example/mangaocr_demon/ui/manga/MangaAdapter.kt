package com.example.mangaocr_demon.ui.manga

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mangaocr_demon.R
import com.example.mangaocr_demon.data.MangaEntity

class MangaAdapter(
    private val onClick: (MangaEntity) -> Unit
) : ListAdapter<MangaEntity, MangaAdapter.MangaViewHolder>(DiffCallback) {

    companion object DiffCallback : DiffUtil.ItemCallback<MangaEntity>() {
        override fun areItemsTheSame(oldItem: MangaEntity, newItem: MangaEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MangaEntity, newItem: MangaEntity): Boolean {
            return oldItem == newItem
        }
    }

    class MangaViewHolder(itemView: View, val onClick: (MangaEntity) -> Unit) :
        RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.tvMangaTitle)
        private val descText: TextView = itemView.findViewById(R.id.tvMangaDesc)
        private var currentManga: MangaEntity? = null

        init {
            itemView.setOnClickListener {
                currentManga?.let { onClick(it) }
            }
        }

        fun bind(manga: MangaEntity) {
            currentManga = manga
            titleText.text = manga.title
            descText.text = manga.description
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MangaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_manga, parent, false)
        return MangaViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: MangaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
