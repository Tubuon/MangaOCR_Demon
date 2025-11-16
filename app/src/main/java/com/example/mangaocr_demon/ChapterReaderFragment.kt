package com.example.mangaocr_demon

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.example.mangaocr_demon.data.AppDatabase
import com.example.mangaocr_demon.databinding.FragmentChapterReaderBinding
import com.example.mangaocr_demon.ui.manga.PageAdapter
import com.example.mangaocr_demon.ui.manga.PageVerticalAdapter
import com.example.mangaocr_demon.ui.viewmodel.ChapterReaderViewModel
import com.example.mangaocr_demon.ui.viewmodel.ChapterReaderViewModelFactory
import com.example.mangaocr_demon.ui.SettingsManager
class ChapterReaderFragment : Fragment() {

    private var _binding: FragmentChapterReaderBinding? = null
    private val binding get() = _binding!!

    private lateinit var pageAdapter: PageAdapter
    private lateinit var pageVerticalAdapter: PageVerticalAdapter
    private lateinit var viewModel: ChapterReaderViewModel
    private lateinit var settingsManager: SettingsManager

    private var chapterId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            chapterId = it.getLong("chapterId", -1L)
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

        // Apply settings
        applyTheme()
        applyReadingMode()
        applyScreenSettings()

        setupViewModel()
        setupBackButton()
    }

    private fun applyTheme() {
        val bgColor = settingsManager.getThemeBackgroundColor()
        val textColor = settingsManager.getThemeTextColor()

        binding.root.setBackgroundColor(bgColor)
        binding.tvChapterTitle.setTextColor(textColor)
        binding.tvPageIndicator.setTextColor(textColor)

        // Update header background based on theme
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

        pageAdapter = PageAdapter { page ->
            // TODO: Handle OCR/translate on long click
        }

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
        // Webtoon mode is similar to vertical but with continuous scrolling
        setupVerticalMode()
        // You can add specific webtoon features here like:
        // - Seamless scrolling between chapters
        // - Auto-load next chapter
        // - Different spacing between images
    }

    private fun applyScreenSettings() {
        // Keep screen on
        if (settingsManager.keepScreenOn) {
            requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        // Apply brightness
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
        }

        viewModel.pages.observe(viewLifecycleOwner) { pages ->
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
            // Restore brightness to default
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

    override fun onDestroyView() {
        super.onDestroyView()

        // Clean up screen settings
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
                }
            }
        }
    }
}