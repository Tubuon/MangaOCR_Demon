package com.example.mangaocr_demon.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.mangaocr_demon.data.AlbumEntity
import com.example.mangaocr_demon.data.AppDatabase
import com.example.mangaocr_demon.databinding.DialogCreateAlbumBinding
import kotlinx.coroutines.launch

class CreateAlbumDialogFragment : DialogFragment() {

    private var _binding: DialogCreateAlbumBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: AppDatabase

    companion object {
        fun newInstance(): CreateAlbumDialogFragment {
            return CreateAlbumDialogFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCreateAlbumBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = AppDatabase.getDatabase(requireContext())

        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnCreate.setOnClickListener { createAlbum() }
    }

    private fun createAlbum() {
        val name = binding.etAlbumName.text.toString().trim()
        val description = binding.etAlbumDescription.text.toString().trim()

        if (name.isEmpty()) {
            binding.etAlbumName.error = "Vui lòng nhập tên album"
            return
        }

        // ✅ FIXED: Dùng constructor mới với title
        val album = AlbumEntity(
            title = name, // ✅ CHANGED from name to title
            description = description.ifEmpty { null },
            color = generateRandomColor(),
            created_at = System.currentTimeMillis()
        )

        lifecycleScope.launch {
            try {
                db.albumDao().insert(album)

                parentFragmentManager.setFragmentResult(
                    "album_created",
                    bundleOf("success" to true)
                )

                if (isAdded) {
                    Toast.makeText(requireContext(), "Đã tạo album: $name", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (isAdded) {
                    Toast.makeText(requireContext(), "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun generateRandomColor(): String {
        val colors = listOf(
            "#4CAF50", "#2196F3", "#FF9800", "#E91E63",
            "#9C27B0", "#00BCD4", "#CDDC39", "#FF5722"
        )
        return colors.random()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}