package com.example.mangaocr_demon.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mangaocr_demon.ChapterReaderFragment
import com.example.mangaocr_demon.data.AppDatabase
import com.example.mangaocr_demon.data.ChapterEntity
import com.example.mangaocr_demon.databinding.FragmentAlbumDetailBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlbumDetailFragment : Fragment() {

    private var _binding: FragmentAlbumDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var chapterAdapter: AlbumChapterAdapter
    private lateinit var db: AppDatabase
    private var albumId: Long = -1

    companion object {
        private const val ARG_ALBUM_ID = "album_id"

        fun newInstance(albumId: Long): AlbumDetailFragment {
            val fragment = AlbumDetailFragment()
            fragment.arguments = Bundle().apply { putLong(ARG_ALBUM_ID, albumId) }
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        albumId = arguments?.getLong(ARG_ALBUM_ID) ?: -1
        db = AppDatabase.getDatabase(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlbumDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        loadAlbumDetails()
        loadChapters()
    }

    private fun setupUI() {
        chapterAdapter = AlbumChapterAdapter(
            onChapterClick = { chapter -> openChapterReader(chapter) },
            onChapterLongClick = { chapter -> showChapterOptions(chapter) }
        )

        binding.rvChapters.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = chapterAdapter
        }
    }

    private fun loadAlbumDetails() {
        lifecycleScope.launch {
            val album = db.albumDao().getAlbumById(albumId)
            if (album != null) {
                binding.tvAlbumName.text = album.title
                binding.tvAlbumDescription.text = album.description ?: "Không có mô tả"
                binding.tvChapterCount.text = "" // count sẽ set ở loadChapters
            }
        }
    }

    private fun loadChapters() {
        // Flow<List<ChapterEntity>> → LiveData → observe
        db.albumChapterDao().getChaptersByAlbumId(albumId)
            .asLiveData()
            .observe(viewLifecycleOwner) { chapters ->
                chapterAdapter.submitList(chapters)
                binding.tvEmptyState.visibility = if (chapters.isEmpty()) View.VISIBLE else View.GONE
                binding.rvChapters.visibility = if (chapters.isEmpty()) View.GONE else View.VISIBLE
                binding.tvChapterCount.text = "${chapters.size} chapters"
            }
    }

    private fun openChapterReader(chapter: ChapterEntity) {
        parentFragmentManager.beginTransaction()
            .replace(
                com.example.mangaocr_demon.R.id.fragment_container,
                ChapterReaderFragment.newInstance(chapter.id)
            )
            .addToBackStack(null)
            .commit()
    }

    private fun showChapterOptions(chapter: ChapterEntity) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Xóa khỏi Album?")
            .setMessage("Chapter ${chapter.number}: ${chapter.title ?: ""}")
            .setPositiveButton("Xóa") { _, _ ->
                lifecycleScope.launch {
                    db.albumChapterDao().removeChapterFromAlbumById(albumId, chapter.id)
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Nếu cần format ngày, có thể dùng
    private fun formatTimestamp(timestamp: Long): String {
        return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
}
