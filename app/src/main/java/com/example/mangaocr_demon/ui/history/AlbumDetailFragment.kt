package com.example.mangaocr_demon.ui.bookcase

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mangaocr_demon.data.AppDatabase
import com.example.mangaocr_demon.data.ChapterEntity
import com.example.mangaocr_demon.databinding.FragmentAlbumDetailBinding
import kotlinx.coroutines.launch

class AlbumDetailFragment : Fragment() {

    private var _binding: FragmentAlbumDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var chapterAdapter: AlbumChapterAdapter  // ĐỔI TYPE
    private lateinit var db: AppDatabase
    private var albumId: Long = -1

    companion object {
        private const val ARG_ALBUM_ID = "album_id"

        fun newInstance(albumId: Long): AlbumDetailFragment {
            val fragment = AlbumDetailFragment()
            val args = Bundle()
            args.putLong(ARG_ALBUM_ID, albumId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        albumId = arguments?.getLong(ARG_ALBUM_ID) ?: -1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlbumDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = AppDatabase.getDatabase(requireContext())

        setupRecyclerView()
        loadAlbumData()
        observeChapters()
    }

    private fun setupRecyclerView() {
        chapterAdapter = AlbumChapterAdapter(
            onChapterClick = { chapter ->
                openChapterReader(chapter)
            },
            onChapterLongClick = { chapter ->
                showChapterOptions(chapter)
            }
        )

        binding.rvChapters.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = chapterAdapter
        }
    }

    private fun loadAlbumData() {
        lifecycleScope.launch {
            db.albumDao().getAlbumById(albumId).collect { album ->
                album?.let {
                    binding.tvAlbumName.text = it.name
                    binding.tvAlbumDescription.text = it.description ?: "Không có mô tả"
                }
            }
        }
    }

    private fun observeChapters() {
        lifecycleScope.launch {
            db.albumDao().getChaptersInAlbum(albumId).collect { chapters ->
                chapterAdapter.submitList(chapters)

                if (chapters.isEmpty()) {
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.rvChapters.visibility = View.GONE
                } else {
                    binding.tvEmptyState.visibility = View.GONE
                    binding.rvChapters.visibility = View.VISIBLE
                }

                binding.tvChapterCount.text = "${chapters.size} chapters"
            }
        }
    }

    private fun openChapterReader(chapter: ChapterEntity) {
        // TODO: Navigate to ChapterReaderActivity
    }

    private fun showChapterOptions(chapter: ChapterEntity) {
        lifecycleScope.launch {
            db.albumChapterDao().removeChapterFromAlbumById(albumId, chapter.id)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}