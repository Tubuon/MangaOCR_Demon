package com.example.mangaocr_demon

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.mangaocr_demon.databinding.FragmentSettingsBinding
import com.example.mangaocr_demon.ui.GoogleAuthManager
import com.example.mangaocr_demon.ui.GoogleDriveSyncManager
import com.example.mangaocr_demon.ui.SettingsManager
import com.example.mangaocr_demon.ui.dialogs.BackupSelectionDialog

class SettingsFragment : Fragment(), ThemeAware {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var settingsManager: SettingsManager
    private lateinit var googleAuthManager: GoogleAuthManager
    private lateinit var syncManager: GoogleDriveSyncManager

    private val loginLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            updateAccountUI()
            showToast("Đăng nhập thành công!")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        settingsManager = SettingsManager(requireContext())
        googleAuthManager = GoogleAuthManager(requireContext())
        syncManager = GoogleDriveSyncManager(requireContext())

        applyTheme()

        setupThemeSettings()
        setupReadingModeSettings()
        setupOtherSettings()
        setupGoogleDriveSync()

        updateAccountUI()
    }

    private fun setupThemeSettings() {
        when (settingsManager.theme) {
            SettingsManager.THEME_LIGHT -> binding.rbThemeLight.isChecked = true
            SettingsManager.THEME_DARK -> binding.rbThemeDark.isChecked = true
            SettingsManager.THEME_SEPIA -> binding.rbThemeSepia.isChecked = true
        }

        binding.rgTheme.setOnCheckedChangeListener { _, checkedId ->
            val theme = when (checkedId) {
                R.id.rbThemeLight -> SettingsManager.THEME_LIGHT
                R.id.rbThemeSepia -> SettingsManager.THEME_SEPIA
                else -> SettingsManager.THEME_DARK
            }
            settingsManager.theme = theme

            applyTheme()
            (activity as? MainActivity)?.refreshTheme()

            showToast("Đã thay đổi theme")
        }
    }

    private fun setupReadingModeSettings() {
        when (settingsManager.readingMode) {
            SettingsManager.MODE_HORIZONTAL -> binding.rbModeHorizontal.isChecked = true
            SettingsManager.MODE_VERTICAL -> binding.rbModeVertical.isChecked = true
            SettingsManager.MODE_WEBTOON -> binding.rbModeWebtoon.isChecked = true
        }

        binding.rgReadingMode.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.rbModeVertical -> SettingsManager.MODE_VERTICAL
                R.id.rbModeWebtoon -> SettingsManager.MODE_WEBTOON
                else -> SettingsManager.MODE_HORIZONTAL
            }
            settingsManager.readingMode = mode
            showToast("Đã thay đổi kiểu đọc")
        }
    }

    private fun setupOtherSettings() {
        binding.switchKeepScreenOn.isChecked = settingsManager.keepScreenOn
        binding.switchKeepScreenOn.setOnCheckedChangeListener { _, isChecked ->
            settingsManager.keepScreenOn = isChecked
        }

        val currentBrightness = settingsManager.brightness
        if (currentBrightness >= 0) {
            binding.sliderBrightness.value = currentBrightness.toFloat()
        } else {
            binding.sliderBrightness.value = 50f
        }

        binding.sliderBrightness.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                settingsManager.brightness = value.toInt()
            }
        }

        binding.btnResetBrightness.setOnClickListener {
            settingsManager.brightness = -1
            binding.sliderBrightness.value = 50f
            showToast("Đã đặt lại độ sáng")
        }
    }

    private fun setupGoogleDriveSync() {
        binding.btnSignIn.setOnClickListener {
            val intent = Intent(requireContext(), LoginActivity::class.java)
            loginLauncher.launch(intent)
        }

        binding.btnSignOut.setOnClickListener {
            googleAuthManager.signOut {
                if (isAdded && !isDetached && !isRemoving) {
                    clearUserPreferences()
                    updateAccountUI()
                    showToast("Đã đăng xuất")
                }
            }
        }

        binding.btnViewAccount.setOnClickListener {
            if (isAdded) {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, AccountDetailsFragment.newInstance())
                    .addToBackStack(null)
                    .commit()
            }
        }

        // ⭐ CẬP NHẬT - Mở BackupSelectionDialog
        binding.btnBackupToDrive.setOnClickListener {
            val dialog = BackupSelectionDialog.newBackupDialog()
            dialog.show(childFragmentManager, "backup_dialog")
        }

        binding.btnRestoreFromDrive.setOnClickListener {
            val dialog = BackupSelectionDialog.newRestoreDialog()
            dialog.show(childFragmentManager, "restore_dialog")
        }
    }

    override fun onThemeChanged() {
        applyTheme()
    }

    private fun applyTheme() {
        if (_binding == null) return

        val bgColor = settingsManager.getThemeBackgroundColor()
        val textColor = settingsManager.getThemeTextColor()

        binding.root.setBackgroundColor(bgColor)

        binding.tvTitle.setTextColor(textColor)
        binding.tvUserName.setTextColor(textColor)
        binding.tvUserEmail.setTextColor(textColor)
        binding.tvLastSync.setTextColor(textColor)

        binding.rbThemeLight.setTextColor(textColor)
        binding.rbThemeDark.setTextColor(textColor)
        binding.rbThemeSepia.setTextColor(textColor)
        binding.rbModeHorizontal.setTextColor(textColor)
        binding.rbModeVertical.setTextColor(textColor)
        binding.rbModeWebtoon.setTextColor(textColor)

        binding.switchKeepScreenOn.setTextColor(textColor)
    }

    private fun updateAccountUI() {
        if (_binding == null) return

        val prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean("is_logged_in", false)

        if (isLoggedIn) {
            binding.btnSignIn.visibility = View.GONE
            binding.layoutUserInfo.visibility = View.VISIBLE
            binding.layoutSyncButtons.visibility = View.VISIBLE
            binding.btnViewAccount.visibility = View.VISIBLE
            binding.tvLastSync.visibility = View.VISIBLE

            val userName = prefs.getString("user_name", "User") ?: "User"
            val userEmail = prefs.getString("user_email", "") ?: ""
            val photoUrl = prefs.getString("user_photo", null)

            binding.tvUserName.text = userName
            binding.tvUserEmail.text = userEmail

            // ⭐ CẬP NHẬT - Hiển thị thời gian sync cuối
            binding.tvLastSync.text = "Lần cuối: ${syncManager.getLastSyncTime()}"

            photoUrl?.let {
                if (isAdded) {
                    Glide.with(this)
                        .load(it)
                        .circleCrop()
                        .into(binding.ivUserAvatar)
                }
            }
        } else {
            binding.btnSignIn.visibility = View.VISIBLE
            binding.layoutUserInfo.visibility = View.GONE
            binding.layoutSyncButtons.visibility = View.GONE
            binding.btnViewAccount.visibility = View.GONE
            binding.tvLastSync.visibility = View.GONE
        }
    }

    // ⭐ THÊM MỚI - Refresh UI khi quay lại
    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            binding.tvLastSync.text = "Lần cuối: ${syncManager.getLastSyncTime()}"
        }
    }

    private fun clearUserPreferences() {
        val prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
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