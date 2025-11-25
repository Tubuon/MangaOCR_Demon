package com.example.mangaocr_demon

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.mangaocr_demon.databinding.FragmentAccountDetailsBinding
import com.example.mangaocr_demon.data.AppDatabase
import com.example.mangaocr_demon.data.MangaEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AccountDetailsFragment : Fragment() {

    private var _binding: FragmentAccountDetailsBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: AppDatabase

    override fun onAttach(context: Context) {
        super.onAttach(context)
        db = AppDatabase.getDatabase(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        loadUserInfo()
        loadStatistics()
    }

    private fun setupViews() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun loadUserInfo() {
        val prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val username = prefs.getString("username", "Guest")
        val email = prefs.getString("email", "Chưa đăng nhập")
        binding.tvUsername.text = username
        binding.tvEmail.text = email
    }

    private fun loadStatistics() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val mangaCount = db.mangaDao().getMangaCount()
                var totalChapters = 0
                var totalPages = 0

                val allManga: List<MangaEntity> = db.mangaDao().getAllMangaSync()
                for (manga in allManga) {
                    val chapters = db.chapterDao().getChaptersByAlbumIdSync(manga.id)
                    totalChapters += chapters.size
                    for (chapter in chapters) {
                        totalPages += db.pageDao().getPageCountByChapterId(chapter.id)
                    }
                }

                val historyCount = db.historyDao().getHistoryCount()
                val albumCount = try { db.albumDao().getAlbumCount() } catch (e: Exception) { 0 }

                withContext(Dispatchers.Main) {
                    updateStatisticsUI(mangaCount, totalChapters, totalPages, historyCount, albumCount)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    updateStatisticsUI(0, 0, 0, 0, 0)
                }
            }
        }
    }

    private fun updateStatisticsUI(
        mangaCount: Int,
        chapterCount: Int,
        pageCount: Int,
        historyCount: Int,
        albumCount: Int
    ) {
        binding.tvMangaCount.text = mangaCount.toString()
        binding.tvChapterCount.text = chapterCount.toString()
        binding.tvPageCount.text = pageCount.toString()
        binding.tvHistoryCount.text = historyCount.toString()
        binding.tvBackupCount.text = albumCount.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance(): AccountDetailsFragment = AccountDetailsFragment()
    }
}
