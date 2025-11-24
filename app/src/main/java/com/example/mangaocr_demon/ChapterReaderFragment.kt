package com.example.mangaocr_demon

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.example.mangaocr_demon.data.AppDatabase
import com.example.mangaocr_demon.data.PageEntity
import com.example.mangaocr_demon.data.model.TextBlock
import com.example.mangaocr_demon.databinding.FragmentChapterReaderBinding
import com.example.mangaocr_demon.ui.manga.PageAdapter
import com.example.mangaocr_demon.ui.manga.PageVerticalAdapter
import com.example.mangaocr_demon.ui.viewmodel.ChapterReaderViewModel
import com.example.mangaocr_demon.ui.viewmodel.ChapterReaderViewModelFactory
import com.example.mangaocr_demon.ui.viewmodel.OcrViewModel
import com.example.mangaocr_demon.ui.SettingsManager
import com.example.mangaocr_demon.ui.reader.TextBlockDetailDialog
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class ChapterReaderFragment : Fragment() {

    private var _binding: FragmentChapterReaderBinding? = null
    private val binding get() = _binding!!
    private lateinit var ocrViewModel: OcrViewModel
    private var currentPages: List<PageEntity> = emptyList()
    private var isFabMenuExpanded = false
    private lateinit var pageAdapter: PageAdapter
    private lateinit var pageVerticalAdapter: PageVerticalAdapter
    private lateinit var viewModel: ChapterReaderViewModel
    private lateinit var settingsManager: SettingsManager
    private var chapterId: Long = -1L
    private var mangaTitle: String? = null
    private var chapterNumber: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            chapterId = it.getLong("chapterId", -1L)
            mangaTitle = it.getString("mangaTitle") // ⭐ NEW
            chapterNumber = it.getString("chapterNumber") // ⭐ NEW
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChapterReaderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        settingsManager = SettingsManager(requireContext())

        applyTheme()
        applyReadingMode()
        applyScreenSettings()
        setupViewModel()
        setupOcrViewModel()
        setupBackButton()
        setupOcrControls()
    }

    private fun applyTheme() {
        val bgColor = settingsManager.getThemeBackgroundColor()
        val textColor = settingsManager.getThemeTextColor()

        binding.root.setBackgroundColor(bgColor)
        binding.tvChapterTitle.setTextColor(textColor)
        binding.tvPageIndicator.setTextColor(textColor)

        when (settingsManager.theme) {
            SettingsManager.THEME_LIGHT -> {
                binding.header.setBackgroundColor(0xCCFFFFFF.toInt())
            }
            SettingsManager.THEME_SEPIA -> {
                binding.header.setBackgroundColor(0xCCF4ECD8.toInt())
            }
            else -> {
                binding.header.setBackgroundColor(0x80000000.toInt())
            }
        }
    }

    private fun applyReadingMode() {
        when (settingsManager.readingMode) {
            SettingsManager.MODE_VERTICAL -> setupVerticalMode()
            SettingsManager.MODE_WEBTOON -> setupWebtoonMode()
            else -> setupHorizontalMode()
        }
    }

    private fun setupHorizontalMode() {
        binding.viewPagerPages.visibility = View.VISIBLE
        binding.recyclerViewPages.visibility = View.GONE

        pageAdapter = PageAdapter (
            onPageLongClick = { page ->
            processOcrForPage(page)
            },
            onTextBlockClick = { page, textBlock ->
                showTextBlockDetails(page, textBlock)
            }
        )
        binding.viewPagerPages.apply {
            adapter = pageAdapter
            orientation = ViewPager2.ORIENTATION_HORIZONTAL
        }

        binding.viewPagerPages.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updatePageIndicator(position, pageAdapter.itemCount)
            }
        })
    }

    private fun setupVerticalMode() {
        binding.viewPagerPages.visibility = View.GONE
        binding.recyclerViewPages.visibility = View.VISIBLE

        pageVerticalAdapter = PageVerticalAdapter { page ->
            // TODO: Handle OCR/translate on long click
        }

        binding.recyclerViewPages.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = pageVerticalAdapter
        }
    }

    private fun setupWebtoonMode() {
        setupVerticalMode()
    }

    private fun applyScreenSettings() {
        if (settingsManager.keepScreenOn) {
            requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        val brightness = settingsManager.brightness
        if (brightness >= 0) {
            val layoutParams = requireActivity().window.attributes
            layoutParams.screenBrightness = brightness / 100f
            requireActivity().window.attributes = layoutParams
        }
    }

    private fun setupViewModel() {
        val db = AppDatabase.getDatabase(requireContext())
        val factory = ChapterReaderViewModelFactory(
            db.pageDao(),
            db.chapterDao(),
            chapterId
        )
        viewModel = ViewModelProvider(this, factory)[ChapterReaderViewModel::class.java]

        viewModel.chapter.observe(viewLifecycleOwner) { chapter ->
            binding.tvChapterTitle.text = "Chapter ${chapter?.number ?: ""}"
            chapterNumber = chapter?.number?.toString()
        }

        viewModel.pages.observe(viewLifecycleOwner) { pages ->
            currentPages = pages // ⭐ Store current pages

            when (settingsManager.readingMode) {
                SettingsManager.MODE_VERTICAL, SettingsManager.MODE_WEBTOON -> {
                    pageVerticalAdapter.submitList(pages)
                }
                else -> {
                    pageAdapter.submitList(pages)
                }
            }

            if (pages.isNotEmpty()) {
                updatePageIndicator(0, pages.size)
            }
        }
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            if (settingsManager.brightness >= 0) {
                val layoutParams = requireActivity().window.attributes
                layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                requireActivity().window.attributes = layoutParams
            }
            parentFragmentManager.popBackStack()
        }
    }

    private fun updatePageIndicator(currentPage: Int, totalPages: Int) {
        binding.tvPageIndicator.text = "${currentPage + 1} / $totalPages"
    }
    private fun setupOcrViewModel() {
        ocrViewModel = ViewModelProvider(this)[OcrViewModel::class.java]

        // OCR progress
        ocrViewModel.ocrProgress.observe(viewLifecycleOwner) { progress ->
            handleOcrProgress(progress)
        }

        // ⭐ Translation progress
        ocrViewModel.translationProgress.observe(viewLifecycleOwner) { progress ->
            handleTranslationProgress(progress)
        }

        // Errors
        ocrViewModel.ocrError.observe(viewLifecycleOwner) { error ->
            error?.let {
                showError(it)
                ocrViewModel.clearError()
            }
        }
    }

    // ⭐ NEW: Handle translation progress
    private fun handleTranslationProgress(progress: OcrViewModel.TranslationProgress) {
        when (progress) {
            is OcrViewModel.TranslationProgress.Idle -> {
                binding.cardOcrProgress.visibility = View.GONE
            }

            is OcrViewModel.TranslationProgress.Processing -> {
                binding.cardOcrProgress.visibility = View.VISIBLE
                binding.tvOcrProgress.text = "Translating with Gemini..."
            }

            is OcrViewModel.TranslationProgress.Success -> {
                binding.cardOcrProgress.visibility = View.GONE

                Snackbar.make(
                    binding.root,
                    "Translated ${progress.translatedCount} text blocks",
                    Snackbar.LENGTH_LONG
                ).show()

                // Refresh page to show translation
                forceReloadPages()
            }

            is OcrViewModel.TranslationProgress.Error -> {
                binding.cardOcrProgress.visibility = View.GONE
                showError("Translation failed: ${progress.message}")
            }
        }
    }




    private fun setupOcrControls() {
        // FAB Menu toggle
        binding.fabOcrMenu.setOnClickListener {
            toggleFabMenu()
        }

        // Scan current page
        binding.fabScanPage.setOnClickListener {
            scanCurrentPage()
            toggleFabMenu()
        }

        binding.fabToggleOverlay.setOnClickListener {
            translateCurrentPage()
            toggleFabMenu()
        }

        binding.fabScanPage.setOnLongClickListener {
            clearOcrForCurrentPage()
            toggleFabMenu()
            true
        }

        // Debug mode toggle
        binding.fabDebugMode.setOnClickListener {
            toggleDebugMode()
            toggleFabMenu()
        }
    }

    // ⭐ NEW: Translate current page
    private fun translateCurrentPage() {
        val currentPosition = when (settingsManager.readingMode) {
            SettingsManager.MODE_VERTICAL, SettingsManager.MODE_WEBTOON -> {
                (binding.recyclerViewPages.layoutManager as? LinearLayoutManager)
                    ?.findFirstVisibleItemPosition() ?: 0
            }
            else -> binding.viewPagerPages.currentItem
        }

        if (currentPosition >= 0 && currentPosition < currentPages.size) {
            val page = currentPages[currentPosition]

            if (!page.isOcrProcessed) {
                showError("Please scan the page first")
                return
            }

            // ⭐ Pass manga info to ViewModel
            ocrViewModel.translatePage(page, mangaTitle, chapterNumber)
        } else {
            showError("No page selected")
        }
    }






    private fun clearOcrForCurrentPage() {
        val currentPosition = when (settingsManager.readingMode) {
            SettingsManager.MODE_VERTICAL, SettingsManager.MODE_WEBTOON -> {
                (binding.recyclerViewPages.layoutManager as? LinearLayoutManager)
                    ?.findFirstVisibleItemPosition() ?: 0
            }
            else -> binding.viewPagerPages.currentItem
        }

        if (currentPosition >= 0 && currentPosition < currentPages.size) {
            val page = currentPages[currentPosition]

            android.util.Log.d("ChapterReaderFragment", "🗑️ Clearing OCR data for page ${page.id}")

            lifecycleScope.launch {
                try {
                    val db = AppDatabase.getDatabase(requireContext())
                    db.pageDao().updateOcrData(
                        pageId = page.id,
                        ocrDataJson = "",
                        isProcessed = false,
                        language = "",
                        timestamp = 0L
                    )

                    android.util.Log.d("ChapterReaderFragment", "✅ OCR data cleared")

                    Snackbar.make(
                        binding.root,
                        "OCR data cleared. You can scan again.",
                        Snackbar.LENGTH_SHORT
                    ).show()

                    // Refresh page
                    refreshCurrentPage()

                } catch (e: Exception) {
                    android.util.Log.e("ChapterReaderFragment", "❌ Failed to clear OCR", e)
                    showError("Failed to clear OCR: ${e.message}")
                }
            }
        }
    }





    private fun toggleFabMenu() {
        isFabMenuExpanded = !isFabMenuExpanded

        if (isFabMenuExpanded) {
            binding.layoutOcrActions.visibility = View.VISIBLE
            binding.fabOcrMenu.animate().rotation(45f).setDuration(200).start()
        } else {
            binding.layoutOcrActions.visibility = View.GONE
            binding.fabOcrMenu.animate().rotation(0f).setDuration(200).start()
        }
    }

    private fun scanCurrentPage() {
        val currentPosition = when (settingsManager.readingMode) {
            SettingsManager.MODE_VERTICAL, SettingsManager.MODE_WEBTOON -> {
                (binding.recyclerViewPages.layoutManager as? LinearLayoutManager)
                    ?.findFirstVisibleItemPosition() ?: 0
            }
            else -> binding.viewPagerPages.currentItem
        }

        if (currentPosition >= 0 && currentPosition < currentPages.size) {
            val page = currentPages[currentPosition]

            // ⭐ ALLOW RE-SCAN
            if (page.isOcrProcessed) {
                android.util.Log.d("ChapterReaderFragment", "🔄 Re-scanning already processed page")
            }

            processOcrForPage(page)
        } else {
            showError("No page selected")
        }
    }

    private fun toggleOverlayForCurrentPage() {
        // This will be handled by the adapter/view
        // You can implement a callback to PageAdapter to toggle overlay
        Snackbar.make(binding.root, "Overlay toggled", Snackbar.LENGTH_SHORT).show()
    }

    // ⭐ NEW: Toggle debug mode
    private fun toggleDebugMode() {
        // This will show bounding boxes around text blocks
        Snackbar.make(binding.root, "Debug mode toggled", Snackbar.LENGTH_SHORT).show()
    }

    // ⭐ NEW: Process OCR for a page
    private fun processOcrForPage(page: PageEntity) {
        if (page.imageUri == null) {
            showError("No image found")
            return
        }


        android.util.Log.d("ChapterReaderFragment", "🔄 Processing OCR for page ${page.id}")
        ocrViewModel.processPage(page)
    }

    private fun handleOcrProgress(progress: OcrViewModel.OcrProgress) {
        when (progress) {
            is OcrViewModel.OcrProgress.Idle -> {
                binding.cardOcrProgress.visibility = View.GONE
            }

            is OcrViewModel.OcrProgress.Processing -> {
                binding.cardOcrProgress.visibility = View.VISIBLE
                binding.tvOcrProgress.text = "Processing OCR..."
            }

            is OcrViewModel.OcrProgress.NoTextFound -> {
                binding.cardOcrProgress.visibility = View.GONE
                Snackbar.make(
                    binding.root,
                    "No text found in this page",
                    Snackbar.LENGTH_SHORT
                ).show()
            }

            is OcrViewModel.OcrProgress.Success -> {
                binding.cardOcrProgress.visibility = View.GONE

                val message = "Found ${progress.textBlockCount} text blocks (${progress.language})"
                android.util.Log.d("ChapterReaderFragment", "✅ OCR Success: $message")

                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
                    .setAction("View") {
                        // Scroll to the page
                    }
                    .show()

                // ⭐ CRITICAL: Force reload from database
                android.util.Log.d("ChapterReaderFragment", "🔄 Triggering force refresh...")
                forceReloadPages()
            }

            is OcrViewModel.OcrProgress.Error -> {
                binding.cardOcrProgress.visibility = View.GONE
                showError("OCR Error: ${progress.message}")
            }
        }
    }

    private fun forceReloadPages() {
        android.util.Log.d("ChapterReaderFragment", "🔄 Force reloading pages from database")

        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(requireContext())

                // ⭐ Get fresh data from database
                val freshPages = db.pageDao().getPagesByChapterIdSync(chapterId)

                android.util.Log.d("ChapterReaderFragment", "📄 Loaded ${freshPages.size} fresh pages")

                // Log OCR status of each page
                freshPages.forEachIndexed { index, page ->
                    android.util.Log.d("ChapterReaderFragment",
                        "Page $index (id=${page.id}): isOcrProcessed=${page.isOcrProcessed}, " +
                                "ocrDataJson length=${page.ocrDataJson?.length ?: 0}")
                }

                // ⭐ Update adapter with fresh data
                currentPages = freshPages

                when (settingsManager.readingMode) {
                    SettingsManager.MODE_VERTICAL, SettingsManager.MODE_WEBTOON -> {
                        android.util.Log.d("ChapterReaderFragment", "📱 Updating vertical adapter")
                        pageVerticalAdapter.submitList(null) // Clear first
                        pageVerticalAdapter.submitList(freshPages)
                        pageVerticalAdapter.notifyDataSetChanged()
                    }
                    else -> {
                        android.util.Log.d("ChapterReaderFragment", "📱 Updating horizontal adapter")
                        pageAdapter.submitList(null) // Clear first
                        pageAdapter.submitList(freshPages)
                        pageAdapter.notifyDataSetChanged()
                    }
                }

                android.util.Log.d("ChapterReaderFragment", "✅ Adapter updated")

            } catch (e: Exception) {
                android.util.Log.e("ChapterReaderFragment", "❌ Error reloading pages", e)
                showError("Failed to reload: ${e.message}")
            }
        }
    }

    private fun refreshCurrentPage() {
        // Trigger a refresh of the LiveData
        viewModel.pages.value?.let { pages ->
            when (settingsManager.readingMode) {
                SettingsManager.MODE_VERTICAL, SettingsManager.MODE_WEBTOON -> {
                    pageVerticalAdapter.submitList(pages)
                    pageVerticalAdapter.notifyDataSetChanged()
                }
                else -> {
                    pageAdapter.submitList(pages)
                    pageAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun showTextBlockDetails(page: PageEntity, textBlock: TextBlock) {
        val dialog = TextBlockDetailDialog.newInstance(page.id, textBlock)

        dialog.onDismissListener = {
            android.util.Log.d("ChapterReaderFragment", "Dialog dismissed, refreshing view")
            forceReloadPages()
        }

        dialog.show(childFragmentManager, "TextBlockDetailDialog")
    }


    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(resources.getColor(android.R.color.holo_red_dark, null))
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val layoutParams = requireActivity().window.attributes
        layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        requireActivity().window.attributes = layoutParams

        _binding = null
    }

    companion object {
        fun newInstance(chapterId: Long): ChapterReaderFragment {
            return ChapterReaderFragment().apply {
                arguments = Bundle().apply {
                    putLong("chapterId", chapterId)
                    putString("mangaTitle", mangaTitle)
                }
            }
        }
    }
}