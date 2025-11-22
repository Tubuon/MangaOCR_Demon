// File: ui/reader/TextBlockDetailDialog.kt
package com.example.mangaocr_demon.ui.reader

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.mangaocr_demon.R
import com.example.mangaocr_demon.data.model.TextBlock
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class TextBlockDetailDialog : DialogFragment() {

    private var pageId: Long = -1
    private lateinit var textBlock: TextBlock

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            pageId = it.getLong(ARG_PAGE_ID)
            val textBlockJson = it.getString(ARG_TEXT_BLOCK)
            textBlock = Json.decodeFromString(textBlockJson ?: "")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_text_block_detail, null)

        setupViews(view)

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Text Block Details")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                saveChanges(view)
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Copy") { _, _ ->
                copyToClipboard()
            }
            .create()
    }

    private fun setupViews(view: View) {
        val tvOriginalText = view.findViewById<TextView>(R.id.tvOriginalText)
        val etEditText = view.findViewById<EditText>(R.id.etEditText)
        val tvLanguage = view.findViewById<TextView>(R.id.tvLanguage)
        val tvConfidence = view.findViewById<TextView>(R.id.tvConfidence)

        tvOriginalText.text = textBlock.originalText
        etEditText.setText(textBlock.originalText)
        tvLanguage.text = "Language: ${getLanguageName(textBlock.language)}"
        tvConfidence.text = "Confidence: ${(textBlock.confidence * 100).toInt()}%"
    }

    private fun saveChanges(view: View) {
        val etEditText = view.findViewById<EditText>(R.id.etEditText)
        val newText = etEditText.text.toString()

        // TODO: Update the text block in database
        // This will require updating PageEntity's ocrDataJson

        // For now, just show a message
        android.widget.Toast.makeText(
            requireContext(),
            "Text updated (save functionality coming soon)",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    private fun copyToClipboard() {
        val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("OCR Text", textBlock.originalText)
        clipboard.setPrimaryClip(clip)

        android.widget.Toast.makeText(
            requireContext(),
            "Text copied to clipboard",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    private fun getLanguageName(code: String): String {
        return when (code) {
            "zh" -> "Chinese (中文)"
            "en" -> "English"
            "ja" -> "Japanese (日本語)"
            else -> code
        }
    }

    companion object {
        private const val ARG_PAGE_ID = "page_id"
        private const val ARG_TEXT_BLOCK = "text_block"

        fun newInstance(pageId: Long, textBlock: TextBlock): TextBlockDetailDialog {
            return TextBlockDetailDialog().apply {
                arguments = Bundle().apply {
                    putLong(ARG_PAGE_ID, pageId)
                    putString(ARG_TEXT_BLOCK, Json.encodeToString(textBlock))
                }
            }
        }
    }
}