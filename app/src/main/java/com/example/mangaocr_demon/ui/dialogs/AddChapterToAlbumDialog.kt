package com.example.mangaocr_demon.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mangaocr_demon.R
import com.example.mangaocr_demon.data.*
import com.example.mangaocr_demon.databinding.DialogAddToAlbumBinding
import com.example.mangaocr_demon.ui.dialogs.adapter.AlbumSelectionAdapter
import com.example.mangaocr_demon.ui.bookcase.CreateAlbumDialogFragment
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AddChapterToAlbumDialog : DialogFragment() {

    private var _binding: DialogAddToAlbumBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: AppDatabase
    private lateinit var adapter: AlbumSelectionAdapter
    private var chapterId: Long = -1
    private val selectedAlbums = mutableSetOf<Long>()

    companion object {
        private const val ARG_CHAPTER_ID = "chapter_id"

        fun newInstance(chapterId: Long): AddChapterToAlbumDialog {
            return AddChapterToAlbumDialog().apply {
                arguments = bundleOf(ARG_CHAPTER_ID to chapterId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_MangaOCR_Demon)
        chapterId = arguments?.getLong(ARG_CHAPTER_ID) ?: -1
        db = AppDatabase.getDatabase(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddToAlbumBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadAlbums()
        setupButtons()
    }

    private fun setupRecyclerView() {
        adapter = AlbumSelectionAdapter(
            onAlbumToggle = { albumId, isSelected ->
                if (isSelected) {
                    selectedAlbums.add(albumId)
                } else {
                    selectedAlbums.remove(albumId)
                }
            }
        )

        binding.rvAlbums.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@AddChapterToAlbumDialog.adapter
        }
    }

    private fun loadAlbums() {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val allAlbums = db.albumDao().getAllAlbums().first()

                if (allAlbums.isEmpty()) {
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.rvAlbums.visibility = View.GONE
                    binding.progressBar.visibility = View.GONE
                    return@launch
                }

                val existingAlbumIds = mutableSetOf<Long>()
                for (album in allAlbums) {
                    val exists = db.albumChapterDao().isChapterInAlbum(album.id, chapterId)
                    if (exists != null) {
                        existingAlbumIds.add(album.id)
                        selectedAlbums.add(album.id)
                    }
                }

                adapter.submitList(allAlbums, existingAlbumIds)

                binding.tvEmptyState.visibility = View.GONE
                binding.rvAlbums.visibility = View.VISIBLE
                binding.progressBar.visibility = View.GONE

            } catch (e: Exception) {
                e.printStackTrace()
                binding.progressBar.visibility = View.GONE
                if (isAdded) {
                    Toast.makeText(requireContext(), "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupButtons() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnCreateAlbum.setOnClickListener {
            val createDialog = CreateAlbumDialogFragment.newInstance()
            createDialog.show(childFragmentManager, "create_album")

            childFragmentManager.setFragmentResultListener(
                "album_created",
                this
            ) { _, _ ->
                loadAlbums()
            }
        }

        binding.btnDone.setOnClickListener {
            saveChanges()
        }
    }

    private fun saveChanges() {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val allAlbums = db.albumDao().getAllAlbums().first()
                val currentAlbumIds = mutableSetOf<Long>()

                for (album in allAlbums) {
                    val exists = db.albumChapterDao().isChapterInAlbum(album.id, chapterId)
                    if (exists != null) {
                        currentAlbumIds.add(album.id)
                    }
                }

                val toAdd = selectedAlbums - currentAlbumIds
                val toRemove = currentAlbumIds - selectedAlbums

                for (albumId in toAdd) {
                    val albumChapter = AlbumChapterEntity(
                        albumId = albumId,
                        chapterId = chapterId,
                        addedAt = System.currentTimeMillis()
                    )
                    db.albumChapterDao().addChapterToAlbum(albumChapter)
                }

                for (albumId in toRemove) {
                    db.albumChapterDao().removeChapterFromAlbumById(albumId, chapterId)
                }

                if (isAdded) {
                    val message = when {
                        toAdd.isNotEmpty() && toRemove.isNotEmpty() ->
                            "Đã cập nhật ${toAdd.size + toRemove.size} albums"
                        toAdd.isNotEmpty() ->
                            "Đã thêm vào ${toAdd.size} albums"
                        toRemove.isNotEmpty() ->
                            "Đã xóa khỏi ${toRemove.size} albums"
                        else ->
                            "Không có thay đổi"
                    }

                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    dismiss()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                binding.progressBar.visibility = View.GONE
                if (isAdded) {
                    Toast.makeText(requireContext(), "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}