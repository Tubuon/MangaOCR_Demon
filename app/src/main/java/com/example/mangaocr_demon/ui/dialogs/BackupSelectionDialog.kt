package com.example.mangaocr_demon.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.mangaocr_demon.R
import com.example.mangaocr_demon.data.AppDatabase
import com.example.mangaocr_demon.data.BackupSelection
import com.example.mangaocr_demon.databinding.DialogBackupSelectionBinding
import com.example.mangaocr_demon.ui.GoogleDriveSyncManager
import com.example.mangaocr_demon.data.SyncProgressListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BackupSelectionDialog : DialogFragment() {

    private var _binding: DialogBackupSelectionBinding? = null
    private val binding get() = _binding!!

    private lateinit var syncManager: GoogleDriveSyncManager
    private lateinit var db: AppDatabase

    private var mode: Mode = Mode.BACKUP

    enum class Mode {
        BACKUP, RESTORE
    }

    companion object {
        private const val ARG_MODE = "mode"

        fun newBackupDialog(): BackupSelectionDialog {
            return BackupSelectionDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_MODE, Mode.BACKUP.name)
                }
            }
        }

        fun newRestoreDialog(): BackupSelectionDialog {
            return BackupSelectionDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_MODE, Mode.RESTORE.name)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_MangaOCR_Demon)

        mode = Mode.valueOf(arguments?.getString(ARG_MODE) ?: Mode.BACKUP.name)
        syncManager = GoogleDriveSyncManager(requireContext())
        db = AppDatabase.getDatabase(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogBackupSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        loadStatistics()
        setupListeners()
    }

    private fun setupUI() {
        binding.tvTitle.text = when (mode) {
            Mode.BACKUP -> "Sao lưu lên Drive"
            Mode.RESTORE -> "Khôi phục từ Drive"
        }

        binding.btnAction.text = when (mode) {
            Mode.BACKUP -> "Sao lưu"
            Mode.RESTORE -> "Khôi phục"
        }

        binding.cbManga.isChecked = true
        binding.cbChapters.isChecked = true
        binding.cbPages.isChecked = true
        binding.cbHistory.isChecked = true
        binding.cbAlbums.isChecked = true
    }

    private fun loadStatistics() {
        lifecycleScope.launch {
            try {
                val mangaCount = db.mangaDao().getAllManga().first().size
                val historyCount = db.historyDao().getHistoryCount()
                val albumCount = db.albumDao().getAlbumCount()

                binding.tvMangaCount.text = "$mangaCount truyện"
                binding.tvChaptersCount.text = "Tất cả chapters"
                binding.tvPagesCount.text = "Tất cả pages"
                binding.tvHistoryCount.text = "$historyCount lịch sử"
                binding.tvAlbumsCount.text = "$albumCount albums"

                val estimatedSize = calculateEstimatedSize(mangaCount, historyCount)
                binding.tvEstimatedSize.text = "Ước tính: ${formatSize(estimatedSize)}"
            } catch (e: Exception) {
                android.util.Log.e("BackupDialog", "Error loading stats", e)
            }
        }
    }

    private fun setupListeners() {
        binding.btnClose.setOnClickListener {
            dismiss()
        }

        binding.btnSelectAll.setOnClickListener {
            selectAll(true)
        }

        binding.btnDeselectAll.setOnClickListener {
            selectAll(false)
        }

        binding.btnAction.setOnClickListener {
            val selection = getSelection()

            if (!selection.hasAnySelection()) {
                Toast.makeText(
                    requireContext(),
                    "Vui lòng chọn ít nhất 1 loại dữ liệu",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            when (mode) {
                Mode.BACKUP -> startBackup(selection)
                Mode.RESTORE -> showRestoreFileList(selection)
            }
        }
    }

    private fun selectAll(select: Boolean) {
        binding.cbManga.isChecked = select
        binding.cbChapters.isChecked = select
        binding.cbPages.isChecked = select
        binding.cbHistory.isChecked = select
        binding.cbAlbums.isChecked = select
    }

    private fun getSelection(): BackupSelection {
        return BackupSelection(
            includeManga = binding.cbManga.isChecked,
            includeChapters = binding.cbChapters.isChecked,
            includePages = binding.cbPages.isChecked,
            includeHistory = binding.cbHistory.isChecked,
            includeAlbums = binding.cbAlbums.isChecked
        )
    }

    private fun startBackup(selection: BackupSelection) {
        setUIEnabled(false)
        binding.progressBar.visibility = View.VISIBLE
        binding.tvProgress.visibility = View.VISIBLE

        lifecycleScope.launch {
            syncManager.backupToGoogleDrive(
                selection,
                object : SyncProgressListener {
                    override fun onProgress(progress: Int, message: String) {
                        binding.progressBar.progress = progress
                        binding.tvProgress.text = message
                    }

                    override fun onSuccess(message: String) {
                        if (isAdded) {
                            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                            dismiss()
                        }
                    }

                    override fun onError(error: String) {
                        if (isAdded) {
                            binding.tvProgress.text = error
                            setUIEnabled(true)
                            Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )
        }
    }

    private fun showRestoreFileList(selection: BackupSelection) {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvProgress.visibility = View.VISIBLE
        binding.tvProgress.text = "Đang tải danh sách backup..."

        lifecycleScope.launch {
            val backupFiles = syncManager.getBackupFiles()

            if (backupFiles.isEmpty()) {
                binding.tvProgress.text = "Không tìm thấy file backup nào"
                Toast.makeText(
                    requireContext(),
                    "Không có file backup trên Drive",
                    Toast.LENGTH_SHORT
                ).show()
                binding.progressBar.visibility = View.GONE
                return@launch
            }

            binding.progressBar.visibility = View.GONE
            binding.tvProgress.visibility = View.GONE

            val fileNames = backupFiles.map {
                "${it.fileName} (${formatSize(it.size)})"
            }.toTypedArray()

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Chọn file khôi phục")
                .setItems(fileNames) { _, which ->
                    val selectedFile = backupFiles[which]
                    startRestore(selectedFile.fileId, selection)
                }
                .setNegativeButton("Hủy", null)
                .show()
        }
    }

    private fun startRestore(fileId: String, selection: BackupSelection) {
        setUIEnabled(false)
        binding.progressBar.visibility = View.VISIBLE
        binding.tvProgress.visibility = View.VISIBLE

        lifecycleScope.launch {
            syncManager.restoreFromGoogleDrive(
                fileId,
                selection,
                object : SyncProgressListener {
                    override fun onProgress(progress: Int, message: String) {
                        binding.progressBar.progress = progress
                        binding.tvProgress.text = message
                    }

                    override fun onSuccess(message: String) {
                        if (isAdded) {
                            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                            dismiss()
                        }
                    }

                    override fun onError(error: String) {
                        if (isAdded) {
                            binding.tvProgress.text = error
                            setUIEnabled(true)
                            Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )
        }
    }

    private fun setUIEnabled(enabled: Boolean) {
        binding.cbManga.isEnabled = enabled
        binding.cbChapters.isEnabled = enabled
        binding.cbPages.isEnabled = enabled
        binding.cbHistory.isEnabled = enabled
        binding.cbAlbums.isEnabled = enabled
        binding.btnSelectAll.isEnabled = enabled
        binding.btnDeselectAll.isEnabled = enabled
        binding.btnAction.isEnabled = enabled
    }

    private fun calculateEstimatedSize(mangaCount: Int, historyCount: Int): Long {
        return (mangaCount * 1024L) + (historyCount * 512L)
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}