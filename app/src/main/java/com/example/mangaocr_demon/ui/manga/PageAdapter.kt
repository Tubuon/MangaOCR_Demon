package com.example.mangaocr_demon.ui.manga

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mangaocr_demon.R
import com.example.mangaocr_demon.data.PageEntity
import com.example.mangaocr_demon.databinding.ItemPageBinding
import com.ymg.pdf.viewer.PDFView

class PageAdapter(
    private val onPageLongClick: (PageEntity) -> Unit
) : ListAdapter<PageEntity, PageAdapter.PageViewHolder>(DiffCallback) {

    companion object DiffCallback : DiffUtil.ItemCallback<PageEntity>() {
        override fun areItemsTheSame(oldItem: PageEntity, newItem: PageEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PageEntity, newItem: PageEntity): Boolean {
            return oldItem == newItem
        }
    }

    class PageViewHolder(
        private val binding: ItemPageBinding,
        private val onPageLongClick: (PageEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        private var currentPage: PageEntity? = null

        init {
            binding.root.setOnLongClickListener {
                currentPage?.let {
                    onPageLongClick(it)
                    true
                } ?: false
            }
        }

        fun bind(page: PageEntity) {
            currentPage = page

            // Reset all views
            binding.imageView.visibility = View.GONE
            binding.pdfView.visibility = View.GONE
            binding.loadingIndicator.visibility = View.GONE
            binding.errorText.visibility = View.GONE

            when (page.pageType) {
                "PDF" -> bindPdfPage(page)
                "IMAGE" -> bindImagePage(page)
                else -> bindImagePage(page) // fallback to image
            }

            // Show indicators
            binding.ocrIndicator.visibility = if (!page.ocrText.isNullOrEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }

            binding.translationIndicator.visibility = if (!page.translatedText.isNullOrEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        private fun bindImagePage(page: PageEntity) {
            binding.imageView.visibility = View.VISIBLE

            try {
                val uri = page.imageUri?.let { Uri.parse(it) }

                Glide.with(binding.root.context)
                    .load(uri)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_error)
                    .into(binding.imageView)

            } catch (e: Exception) {
                android.util.Log.e("PageAdapter", "Error loading image", e)
                binding.imageView.setImageResource(R.drawable.ic_image_error)
            }
        }

        private fun bindPdfPage(page: PageEntity) {
            binding.pdfView.visibility = View.VISIBLE
            binding.loadingIndicator.visibility = View.VISIBLE

            try {
                val pdfUri = page.pdfUri
                val pageNumber = page.pdfPageNumber ?: 0

                if (pdfUri.isNullOrEmpty()) {
                    showError("PDF URI is empty")
                    return
                }

                val uri = Uri.parse(pdfUri)
                android.util.Log.d("PageAdapter", "Loading PDF page $pageNumber from $pdfUri")

                // Use the correct API for this library
                binding.pdfView.fromUri(uri)
                    .defaultPage(pageNumber)
                    .enableSwipe(true)
                    .swipeHorizontal(false)
                    .enableDoubletap(true)
                    .spacing(10)
                    .onLoad { numPages ->
                        binding.loadingIndicator.visibility = View.GONE
                        android.util.Log.d("PageAdapter", "PDF loaded with $numPages pages")
                    }
                    .onPageChange { page, pageCount ->
                        android.util.Log.d("PageAdapter", "Page changed to $page of $pageCount")
                    }
                    .onError { error ->
                        binding.loadingIndicator.visibility = View.GONE
                        showError("PDF Error: ${error.message}")
                        android.util.Log.e("PageAdapter", "PDF load error", error)
                    }
                    .load()

            } catch (e: Exception) {
                binding.loadingIndicator.visibility = View.GONE
                showError("Exception: ${e.message}")
                android.util.Log.e("PageAdapter", "Error binding PDF page", e)
            }
        }

        private fun showError(message: String) {
            binding.pdfView.visibility = View.GONE
            binding.errorText.visibility = View.VISIBLE
            binding.errorText.text = message
            android.util.Log.e("PageAdapter", "PDF Error: $message")
        }
        fun cleanup() {
            try {
                binding.pdfView.recycle()
            } catch (e: Exception) {
                android.util.Log.e("PageAdapter", "Error cleaning up PDF viewer", e)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val binding = ItemPageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PageViewHolder(binding, onPageLongClick)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    override fun onViewRecycled(holder: PageViewHolder) {
        super.onViewRecycled(holder)
        holder.cleanup()
    }
}