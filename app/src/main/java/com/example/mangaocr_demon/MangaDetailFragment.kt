package com.example.mangaocr_demon

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mangaocr_demon.data.AppDatabase
import com.example.mangaocr_demon.data.ChapterEntity
import com.example.mangaocr_demon.databinding.FragmentMangaDetailBinding
import com.example.mangaocr_demon.ui.dialogs.AddChapterToAlbumDialog
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MangaDetailFragment : Fragment() {

    private var _binding: FragmentMangaDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: AppDatabase
    private lateinit var chapterAdapter: ChapterAdapter

    private var mangaId: Long = -1
    private var mangaTitle: String = ""

    companion object {
        private const val TAG = "MangaDetailFragment"

        fun newInstance(mangaId: Long, mangaTitle: String): MangaDetailFragment {
            return MangaDetailFragment().apply {
                arguments = Bundle().apply {
                    putLong("mangaId", mangaId)
                    putString("mangaTitle", mangaTitle)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            mangaId = it.getLong("mangaId", -1)
            mangaTitle = it.getString("mangaTitle", "")
        }

        android.util.Log.d(TAG, "📖 MangaDetailFragment created for mangaId=$mangaId")
        db = AppDatabase.getDatabase(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMangaDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        loadChapters()
    }

    private fun setupToolbar() {
        binding.toolbar.title = mangaTitle
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        chapterAdapter = ChapterAdapter(
            onChapterClick = { chapter ->
                android.util.Log.d(TAG, "📖 Opening chapter ${chapter.id}")
                openChapterReader(chapter.id)
            },
            onChapterLongClick = { chapter ->
                showChapterContextMenu(chapter)
            }
        )

        binding.rvChapters.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = chapterAdapter
        }
    }

    private fun loadChapters() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                android.util.Log.d(TAG, "🔄 Loading chapters for mangaId=$mangaId")

                // ✅ Dùng getChaptersByAlbumIdFlow (albumId thực chất là mangaId)
                db.chapterDao().getChaptersByAlbumIdFlow(mangaId).collectLatest { chapters ->
                    if (!isAdded || _binding == null) return@collectLatest

                    android.util.Log.d(TAG, "✅ Loaded ${chapters.size} chapters")

                    chapterAdapter.submitList(chapters)

                    // Cập nhật UI
                    if (chapters.isEmpty()) {
                        binding.emptyState?.visibility = View.VISIBLE
                        binding.rvChapters.visibility = View.GONE
                        binding.tvChapterCount?.text = "0 chapters"
                    } else {
                        binding.emptyState?.visibility = View.GONE
                        binding.rvChapters.visibility = View.VISIBLE
                        binding.tvChapterCount?.text = "${chapters.size} chapters"
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Error loading chapters", e)
                if (isAdded) showToast("Lỗi: ${e.message}")
            }
        }
    }

    private fun showChapterContextMenu(chapter: ChapterEntity) {
        val options = arrayOf(
            "📖 Đọc chapter",
            "📚 Thêm vào album",
            "🗑️ Xóa chapter"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Chapter ${chapter.number}")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> openChapterReader(chapter.id)
                    1 -> showAddToAlbumDialog(chapter)
                    2 -> confirmDeleteChapter(chapter)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showAddToAlbumDialog(chapter: ChapterEntity) {
        val dialog = AddChapterToAlbumDialog.newInstance(chapter.id)
        dialog.show(childFragmentManager, "add_to_album")
    }

    private fun confirmDeleteChapter(chapter: ChapterEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("Xóa chapter")
            .setMessage("Bạn có chắc muốn xóa Chapter ${chapter.number}?\n\nTất cả pages của chapter này cũng sẽ bị xóa.")
            .setPositiveButton("Xóa") { _, _ -> deleteChapter(chapter) }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun deleteChapter(chapter: ChapterEntity) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                db.chapterDao().delete(chapter)
                if (isAdded) showToast("✅ Đã xóa Chapter ${chapter.number}")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error deleting chapter", e)
                if (isAdded) showToast("❌ Lỗi xóa: ${e.message}")
            }
        }
    }

    private fun openChapterReader(chapterId: Long) {
        try {
            val fragment = ChapterReaderFragment.newInstance(chapterId)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error opening chapter reader", e)
            showToast("Lỗi mở chapter: ${e.message}")
        }
    }

    private fun showToast(message: String) {
        if (isAdded && context != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}