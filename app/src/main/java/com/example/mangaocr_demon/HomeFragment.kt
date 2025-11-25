package com.example.mangaocr_demon

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.activity.result.contract.ActivityResultContracts
import com.example.mangaocr_demon.data.MangaEntity
import com.example.mangaocr_demon.databinding.FragmentHomeBinding
import com.example.mangaocr_demon.ui.manga.MangaAdapter
import com.example.mangaocr_demon.ui.viewmodel.HomeViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by lazy {
        ViewModelProvider(requireActivity())[HomeViewModel::class.java]
    }

    private lateinit var adapter: MangaAdapter
    private var isFabMenuOpen = false
    private var isProcessingImages = false
    private var isPdfProcessing = false

    companion object {
        private const val MIN_IMAGES = 1
        private const val MAX_IMAGES = 20
        private const val KEY_FAB_MENU_OPEN = "fab_menu_open"
        private const val KEY_PROCESSING = "processing_images"
        private const val TAG = "HomeFragment"
    }

    // =================== LAUNCHERS ===================

    private val pdfPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        android.util.Log.d(TAG, "PDF picker result: ${result.resultCode}")

        if (isPdfProcessing || !isAdded || _binding == null) {
            android.util.Log.w(TAG, "Skipping PDF - already processing or fragment detached")
            return@registerForActivityResult
        }

        if (result.resultCode != Activity.RESULT_OK) {
            isPdfProcessing = false
            return@registerForActivityResult
        }

        result.data?.data?.let { pdfUri ->
            isPdfProcessing = true
            android.util.Log.d(TAG, "Processing PDF: $pdfUri")

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    if (!isAdded || _binding == null) return@launch

                    showProgressBar(true, "Đang sao chép PDF...")

                    withContext(Dispatchers.IO) {
                        try {
                            requireContext().contentResolver.takePersistableUriPermission(
                                pdfUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        } catch (e: Exception) {
                            android.util.Log.e(TAG, "Failed to take PDF permission", e)
                        }
                    }

                    val manga = MangaEntity(
                        title = "Manga mới (PDF) - ${System.currentTimeMillis()}",
                        description = "Nhập từ file PDF",
                        createdAt = System.currentTimeMillis()
                    )

                    withContext(Dispatchers.IO) {
                        viewModel.addMangaFromPdf(manga, pdfUri.toString())
                    }

                    if (isAdded && context != null) {
                        showProgressBar(false)
                        showToast("✅ Đã thêm truyện từ PDF!")
                    }

                } catch (e: Exception) {
                    android.util.Log.e(TAG, "PDF processing error", e)
                    if (isAdded && context != null) {
                        showProgressBar(false)
                        showToast("❌ Lỗi: ${e.message}")
                    }
                } finally {
                    isPdfProcessing = false
                }
            }
        } ?: run {
            isPdfProcessing = false
        }
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        android.util.Log.d(TAG, "Image picker result: ${result.resultCode}")

        if (isProcessingImages || !isAdded || _binding == null) {
            android.util.Log.w(TAG, "Skipping images - already processing or fragment detached")
            return@registerForActivityResult
        }

        if (result.resultCode != Activity.RESULT_OK) {
            isProcessingImages = false
            return@registerForActivityResult
        }

        result.data?.let { intent ->
            isProcessingImages = true
            android.util.Log.d(TAG, "Processing images")

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    if (!isAdded || _binding == null) return@launch

                    showProgressBar(true, "Đang sao chép ảnh...")

                    val uris = mutableListOf<String>()

                    withContext(Dispatchers.IO) {
                        val clipData = intent.clipData
                        if (clipData != null) {
                            for (i in 0 until clipData.itemCount) {
                                val uri = clipData.getItemAt(i).uri
                                try {
                                    requireContext().contentResolver.takePersistableUriPermission(
                                        uri,
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    )
                                    uris.add(uri.toString())
                                } catch (e: SecurityException) {
                                    android.util.Log.e(TAG, "Cannot take permission for URI: $uri", e)
                                }
                            }
                        } else {
                            intent.data?.let { uri ->
                                try {
                                    requireContext().contentResolver.takePersistableUriPermission(
                                        uri,
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    )
                                    uris.add(uri.toString())
                                } catch (e: SecurityException) {
                                    android.util.Log.e(TAG, "Cannot take permission for URI: $uri", e)
                                }
                            }
                        }
                    }

                    when {
                        uris.isEmpty() -> {
                            showProgressBar(false)
                            showToast("⚠️ Bạn chưa chọn ảnh nào")
                        }
                        uris.size < MIN_IMAGES -> {
                            showProgressBar(false)
                            showToast("⚠️ Vui lòng chọn ít nhất $MIN_IMAGES ảnh")
                        }
                        uris.size > MAX_IMAGES -> {
                            showProgressBar(false)
                            showToast("⚠️ Chỉ được chọn tối đa $MAX_IMAGES ảnh. Bạn đã chọn ${uris.size} ảnh.")
                        }
                        else -> {
                            val manga = MangaEntity(
                                title = "Manga mới (ảnh) - ${System.currentTimeMillis()}",
                                description = "Nhập từ ${uris.size} ảnh",
                                createdAt = System.currentTimeMillis()
                            )

                            withContext(Dispatchers.IO) {
                                viewModel.addMangaWithImages(manga, uris)
                            }

                            if (isAdded && context != null) {
                                showProgressBar(false)
                                showToast("✅ Đã thêm ${uris.size} ảnh thành công!")
                            }
                        }
                    }

                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Image processing error", e)
                    if (isAdded && context != null) {
                        showProgressBar(false)
                        showToast("❌ Lỗi: ${e.message}")
                    }
                } finally {
                    isProcessingImages = false
                }
            }
        } ?: run {
            isProcessingImages = false
        }
    }

    // =================== LIFECYCLE ===================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedInstanceState?.let {
            isFabMenuOpen = it.getBoolean(KEY_FAB_MENU_OPEN, false)
            isProcessingImages = it.getBoolean(KEY_PROCESSING, false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        setupRecyclerView()
        setupFabMenu()
        observeViewModel()

        if (isFabMenuOpen) {
            openFabMenu()
        }

        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_FAB_MENU_OPEN, isFabMenuOpen)
        outState.putBoolean(KEY_PROCESSING, isProcessingImages)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isPdfProcessing = false
        isProcessingImages = false
        _binding = null
    }

    override fun onStop() {
        super.onStop()
        if (isFabMenuOpen) {
            closeFabMenu()
        }
    }

    // =================== UI SETUP ===================

    private fun setupRecyclerView() {
        adapter = MangaAdapter(
            onItemClick = { manga ->
                if (!isAdded) return@MangaAdapter

                val bundle = Bundle().apply {
                    putLong("mangaId", manga.id)
                    putString("mangaTitle", manga.title)
                }
                val fragment = MangaDetailFragment().apply {
                    arguments = bundle
                }

                try {
                    parentFragmentManager.beginTransaction()
                        .replace(android.R.id.content, fragment)
                        .addToBackStack(null)
                        .commit()
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Error navigating to detail", e)
                }
            },
            onDeleteClick = { manga ->
                showDeleteConfirmDialog(manga)
            }
        )

        binding.recyclerViewManga.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@HomeFragment.adapter
        }
    }

    private fun setupFabMenu() {
        binding.fabAdd.setOnClickListener {
            if (isFabMenuOpen) {
                closeFabMenu()
            } else {
                openFabMenu()
            }
        }

        binding.scrimOverlay.setOnClickListener {
            closeFabMenu()
        }

        binding.btnAddPdf.setOnClickListener {
            closeFabMenu()
            openPdfPicker()
        }

        binding.btnAddImages.setOnClickListener {
            closeFabMenu()
            showImagePickerInfo()
        }
    }

    private fun observeViewModel() {
        viewModel.mangaList.observe(viewLifecycleOwner) { list ->
            if (!isAdded || _binding == null) return@observe

            adapter.submitList(list)
            updateEmptyState(list.isEmpty())
            binding.tvMangaCount.text = "${list.size} truyện"
        }
    }

    // =================== DELETE ===================

    private fun showDeleteConfirmDialog(manga: MangaEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Xóa truyện?")
            .setMessage("Bạn có chắc muốn xóa \"${manga.title}\"?\n\nToàn bộ chapters, pages và files ảnh/PDF sẽ bị xóa vĩnh viễn.")
            .setPositiveButton("Xóa") { _, _ ->
                deleteManga(manga)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun deleteManga(manga: MangaEntity) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                showProgressBar(true, "Đang xóa...")

                withContext(Dispatchers.IO) {
                    // Xóa files trước
                    viewModel.deleteMangaFiles(manga.id)

                    // Xóa manga (cascade sẽ xóa chapters và pages)
                    val db = com.example.mangaocr_demon.data.AppDatabase.getDatabase(requireContext())
                    db.mangaDao().delete(manga)
                }

                if (isAdded && context != null) {
                    showProgressBar(false)
                    showToast("✅ Đã xóa \"${manga.title}\"")
                }

            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error deleting manga", e)
                if (isAdded && context != null) {
                    showProgressBar(false)
                    showToast("❌ Lỗi xóa: ${e.message}")
                }
            }
        }
    }

    // =================== FAB MENU ===================

    private fun openFabMenu() {
        isFabMenuOpen = true
        binding.fabMenuContainer.visibility = View.VISIBLE
        binding.scrimOverlay.visibility = View.VISIBLE
        binding.fabAdd.animate().rotation(45f).setDuration(200).start()
    }

    private fun closeFabMenu() {
        isFabMenuOpen = false
        binding.fabMenuContainer.visibility = View.GONE
        binding.scrimOverlay.visibility = View.GONE
        binding.fabAdd.animate().rotation(0f).setDuration(200).start()
    }

    // =================== PICKERS ===================

    private fun showImagePickerInfo() {
        showToast("Vui lòng chọn từ $MIN_IMAGES đến $MAX_IMAGES ảnh")
        openImagePicker()
    }

    private fun openPdfPicker() {
        if (isPdfProcessing) {
            showToast("⏳ Đang xử lý PDF trước đó, vui lòng đợi...")
            return
        }

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        pdfPickerLauncher.launch(intent)
    }

    private fun openImagePicker() {
        if (isProcessingImages) {
            showToast("⏳ Đang xử lý ảnh trước đó, vui lòng đợi...")
            return
        }

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        imagePickerLauncher.launch(intent)
    }

    // =================== HELPERS ===================

    private fun updateEmptyState(isEmpty: Boolean) {
        if (_binding == null) return

        if (isEmpty) {
            binding.emptyState.visibility = View.VISIBLE
            binding.recyclerViewManga.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.recyclerViewManga.visibility = View.VISIBLE
        }
    }

    private fun showProgressBar(show: Boolean, message: String = "") {
        if (_binding == null) return

        try {
            binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE

            binding.tvProgressMessage?.apply {
                if (show && message.isNotEmpty()) {
                    visibility = View.VISIBLE
                    text = message
                } else {
                    visibility = View.GONE
                }
            }

            binding.fabAdd.isEnabled = !show
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error showing progress", e)
        }
    }

    private fun showToast(message: String) {
        if (isAdded && context != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }
}