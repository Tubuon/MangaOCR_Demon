package com.example.mangaocr_demon

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.activity.result.contract.ActivityResultContracts
import com.example.mangaocr_demon.data.MangaEntity
import com.example.mangaocr_demon.databinding.FragmentHomeBinding
import com.example.mangaocr_demon.ui.manga.MangaAdapter
import com.example.mangaocr_demon.ui.viewmodel.HomeViewModel

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HomeViewModel
    private lateinit var adapter: MangaAdapter

    private var isFabMenuOpen = false

    // Launcher cho PDF
    private val pdfPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { pdfUri ->
                try {
                    // Lấy quyền truy cập lâu dài
                    requireContext().contentResolver.takePersistableUriPermission(
                        pdfUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )

                    val manga = MangaEntity(
                        title = "Manga mới (PDF)",
                        description = "Nhập từ file PDF"
                    )
                    viewModel.addMangaFromPdf(manga, pdfUri.toString())
                    Toast.makeText(context, "Đã thêm PDF thành công!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Launcher cho Images - ĐÃ THÊM GIỚI HẠN
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { intent ->
                try {
                    val uris = mutableListOf<String>()

                    // Xử lý nhiều ảnh
                    val clipData = intent.clipData
                    if (clipData != null) {
                        for (i in 0 until clipData.itemCount) {
                            val uri = clipData.getItemAt(i).uri
                            // Lấy quyền cho từng URI
                            requireContext().contentResolver.takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                            uris.add(uri.toString())
                        }
                    } else {
                        // Chỉ 1 ảnh
                        intent.data?.let { uri ->
                            requireContext().contentResolver.takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                            uris.add(uri.toString())
                        }
                    }

                    // ✅ VALIDATE SỐ LƯỢNG ẢNH
                    when {
                        uris.isEmpty() -> {
                            Toast.makeText(
                                context,
                                "Bạn chưa chọn ảnh nào",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        uris.size < MIN_IMAGES -> {
                            Toast.makeText(
                                context,
                                "Vui lòng chọn ít nhất $MIN_IMAGES ảnh",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        uris.size > MAX_IMAGES -> {
                            Toast.makeText(
                                context,
                                "Chỉ được chọn tối đa $MAX_IMAGES ảnh. Bạn đã chọn ${uris.size} ảnh.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        else -> {
                            // ✅ OK - Số lượng hợp lệ
                            val manga = MangaEntity(
                                title = "Manga mới (ảnh)",
                                description = "Nhập từ ${uris.size} ảnh"
                            )
                            viewModel.addMangaWithImages(manga, uris)
                            Toast.makeText(
                                context,
                                "Đã thêm ${uris.size} ảnh thành công!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(requireActivity())[HomeViewModel::class.java]

        // Setup RecyclerView
        adapter = MangaAdapter { manga ->
            val bundle = Bundle().apply {
                putLong("mangaId", manga.id)
                putString("mangaTitle", manga.title)
            }
            val fragment = MangaDetailFragment().apply {
                arguments = bundle
            }
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }

        binding.recyclerViewManga.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewManga.adapter = adapter

        // Quan sát danh sách manga
        viewModel.mangaList.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            updateEmptyState(list.isEmpty())
            binding.tvMangaCount.text = "${list.size} truyện"
        }

        setupFabMenu()

        return binding.root
    }

    private fun setupFabMenu() {
        // Click nút FAB chính để mở/đóng menu
        binding.fabAdd.setOnClickListener {
            if (isFabMenuOpen) {
                closeFabMenu()
            } else {
                openFabMenu()
            }
        }

        // Click overlay để đóng menu
        binding.scrimOverlay.setOnClickListener {
            closeFabMenu()
        }

        // Click nút thêm từ PDF
        binding.btnAddPdf.setOnClickListener {
            closeFabMenu()
            openPdfPicker()
        }

        // Click nút thêm từ ảnh
        binding.btnAddImages.setOnClickListener {
            closeFabMenu()
            // ✅ HIỂN THỊ THÔNG BÁO TRƯỚC KHI CHỌN
            showImagePickerInfo()
        }
    }

    // ✅ THÊM HÀM HIỂN THỊ THÔNG BÁO
    private fun showImagePickerInfo() {
        Toast.makeText(
            context,
            "Vui lòng chọn từ $MIN_IMAGES đến $MAX_IMAGES ảnh",
            Toast.LENGTH_SHORT
        ).show()
        openImagePicker()
    }

    private fun openFabMenu() {
        isFabMenuOpen = true
        binding.fabMenuContainer.visibility = View.VISIBLE
        binding.scrimOverlay.visibility = View.VISIBLE

        // Animation xoay nút FAB
        binding.fabAdd.animate()
            .rotation(45f)
            .setDuration(200)
            .start()
    }

    private fun closeFabMenu() {
        isFabMenuOpen = false
        binding.fabMenuContainer.visibility = View.GONE
        binding.scrimOverlay.visibility = View.GONE

        // Animation xoay về nút FAB
        binding.fabAdd.animate()
            .rotation(0f)
            .setDuration(200)
            .start()
    }

    private fun openPdfPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        pdfPickerLauncher.launch(intent)
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        imagePickerLauncher.launch(intent)
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.emptyState.visibility = View.VISIBLE
            binding.recyclerViewManga.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.recyclerViewManga.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val MIN_IMAGES = 1
        private const val MAX_IMAGES = 20
    }
}