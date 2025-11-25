package com.example.mangaocr_demon

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mangaocr_demon.data.AppDatabase
import com.example.mangaocr_demon.data.MangaRepository
import com.example.mangaocr_demon.data.PageEntity
import com.example.mangaocr_demon.databinding.FragmentChapterReaderBinding
import com.example.mangaocr_demon.ml.GeminiTranslator
import com.example.mangaocr_demon.ml.OcrEngine
import com.example.mangaocr_demon.ui.SettingsManager
import com.example.mangaocr_demon.ui.manga.PageAdapter
import com.example.mangaocr_demon.ui.manga.PageVerticalAdapter
import com.example.mangaocr_demon.ui.viewmodel.ChapterReaderViewModel
import com.example.mangaocr_demon.ui.viewmodel.ChapterReaderViewModelFactory
import kotlinx.coroutines.launch

class ChapterReaderFragment : Fragment() {

    private var _binding: FragmentChapterReaderBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ChapterReaderViewModel
    private lateinit var database: AppDatabase
    private lateinit var settings: SettingsManager

    private var chapterId: Long = -1
    private var pageAdapter: PageAdapter? = null
    private var verticalAdapter: PageVerticalAdapter? = null

    companion object {
        private const val ARG_CHAPTER_ID = "chapterId"

        fun newInstance(id: Long): ChapterReaderFragment =
            ChapterReaderFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_CHAPTER_ID, id)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        chapterId = arguments?.getLong(ARG_CHAPTER_ID) ?: -1
        database = AppDatabase.getDatabase(requireContext())
        settings = SettingsManager(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChapterReaderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        setupReadingMode()
        observePages()
        observeProcessingState()

        binding.tvChapterTitle?.text = "Chapter $chapterId"
    }

    private fun setupViewModel() {
        val repo = MangaRepository(
            database.mangaDao(),
            database.chapterDao(),
            database.pageDao()
        )

        val ocr = OcrEngine(requireContext())
        val translator = GeminiTranslator(requireContext())

        viewModel = ViewModelProvider(
            this,
            ChapterReaderViewModelFactory(
                repo,
                database.historyDao(),
                ocr,
                translator,
                chapterId
            )
        )[ChapterReaderViewModel::class.java]
    }

    // =================== UI =====================
    private fun setupReadingMode() {
        when (settings.readingMode) {
            SettingsManager.MODE_HORIZONTAL -> setupHorizontal()
            else -> setupVertical()
        }
    }

    private fun setupHorizontal() {
        binding.recyclerViewPages?.visibility = View.GONE
        binding.viewPagerPages?.visibility = View.VISIBLE

        pageAdapter = PageAdapter(
            onPageClick = { toggleHeader() },
            onPageLongClick = { page -> showOcrDialog(page) }
        )

        binding.viewPagerPages?.adapter = pageAdapter
    }

    private fun setupVertical() {
        binding.recyclerViewPages?.visibility = View.VISIBLE
        binding.viewPagerPages?.visibility = View.GONE

        verticalAdapter = PageVerticalAdapter(
            onPageClick = { toggleHeader() },
            onPageLongClick = { page -> showOcrDialog(page) }
        )

        binding.recyclerViewPages?.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewPages?.adapter = verticalAdapter
    }

    // =================== DATA OBSERVER =====================
    private fun observePages() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { pageState ->
                val pages = pageState.pages

                if (settings.readingMode == SettingsManager.MODE_HORIZONTAL) {
                    pageAdapter?.submitList(pages)
                } else {
                    verticalAdapter?.submitList(pages)
                }
            }
        }
    }

    // ✅ FIXED: Dùng cardOcrProgress thay vì progressBar
    private fun observeProcessingState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                // ✅ Hiển thị card OCR progress
                _binding?.cardOcrProgress?.visibility = if (state.isProcessing) View.VISIBLE else View.GONE

                // ✅ Cập nhật text progress nếu có
                if (state.isProcessing) {
                    _binding?.tvOcrProgress?.text = "Đang xử lý OCR & Dịch..."
                }

                // Hiển thị error nếu có
                state.errorMessage?.let { error ->
                    toast(error)
                }
            }
        }
    }

    // =================== OCR + DỊCH =====================
    private fun showOcrDialog(page: PageEntity) {
        // Check state thay vì local flag
        if (viewModel.state.value.isProcessing) {
            toast("Đang xử lý…")
            return
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("OCR & Dịch")
            .setMessage("Bạn muốn xử lý trang này?")
            .setPositiveButton("Xử lý") { _, _ ->
                processPage(page)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun processPage(page: PageEntity) {
        toast("Đang OCR/Dịch…")

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.processPageOcrAndTranslate(page)

            // Kiểm tra kết quả
            val state = viewModel.state.value
            if (state.errorMessage == null) {
                toast("✅ Xong!")
            } else {
                toast("❌ ${state.errorMessage}")
            }
        }
    }

    private fun toggleHeader() {
        binding.header?.visibility =
            if (binding.header?.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    private fun toast(msg: String) {
        if (isAdded && context != null) {
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}