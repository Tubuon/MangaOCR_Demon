package com.example.mangaocr_demon

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.mangaocr_demon.databinding.ActivityMainBinding
import com.example.mangaocr_demon.ui.SettingsManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Khởi tạo SettingsManager
        settingsManager = SettingsManager(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Áp dụng theme ngay khi khởi động
        applyTheme()

        // Fragment mặc định là Home
        replaceFragment(HomeFragment())
        binding.bottomNav.selectedItemId = R.id.nav_home

        // Lắng nghe chọn item BottomNavigationView
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> replaceFragment(HomeFragment())
                R.id.nav_bookcase -> replaceFragment(BookcaseFragment())
                R.id.nav_history -> replaceFragment(HistoryFragment())
                R.id.nav_settings -> replaceFragment(SettingsFragment())
            }
            true
        }
    }

    // ✅ HÀM MỚI: Áp dụng theme
    private fun applyTheme() {
        val bgColor = settingsManager.getThemeBackgroundColor()
        val textColor = settingsManager.getThemeTextColor()

        // Đổi màu nền toàn app
        binding.root.setBackgroundColor(bgColor)
        binding.fragmentContainer.setBackgroundColor(bgColor)

        // Đổi màu bottom navigation
        when (settingsManager.theme) {
            SettingsManager.THEME_DARK -> {
                binding.bottomNav.setBackgroundColor(0xFF1E1E1E.toInt())
                binding.bottomNav.itemIconTintList = android.content.res.ColorStateList.valueOf(0xFFFFFFFF.toInt())
                binding.bottomNav.itemTextColor = android.content.res.ColorStateList.valueOf(0xFFFFFFFF.toInt())
            }
            SettingsManager.THEME_SEPIA -> {
                binding.bottomNav.setBackgroundColor(0xFFF4ECD8.toInt())
                binding.bottomNav.itemIconTintList = android.content.res.ColorStateList.valueOf(0xFF5C4033.toInt())
                binding.bottomNav.itemTextColor = android.content.res.ColorStateList.valueOf(0xFF5C4033.toInt())
            }
            else -> {
                binding.bottomNav.setBackgroundColor(0xFFFFFFFF.toInt())
                binding.bottomNav.itemIconTintList = android.content.res.ColorStateList.valueOf(0xFF000000.toInt())
                binding.bottomNav.itemTextColor = android.content.res.ColorStateList.valueOf(0xFF000000.toInt())
            }
        }
    }

    // ✅ HÀM MỚI: Gọi từ SettingsFragment khi đổi theme
    fun refreshTheme() {
        applyTheme()
        // Refresh fragment hiện tại
        supportFragmentManager.fragments.lastOrNull()?.let { fragment ->
            if (fragment is ThemeAware) {
                fragment.onThemeChanged()
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}

// ✅ Interface để các Fragment tự động cập nhật theme
interface ThemeAware {
    fun onThemeChanged()
}