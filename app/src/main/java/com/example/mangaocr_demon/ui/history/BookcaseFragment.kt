package com.example.mangaocr_demon.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.mangaocr_demon.R
import com.example.mangaocr_demon.data.AppDatabase
import com.example.mangaocr_demon.databinding.FragmentBookcaseBinding
import com.example.mangaocr_demon.ui.history.AlbumAdapter
import kotlinx.coroutines.launch
import com.example.mangaocr_demon.data.AlbumEntity
import com.example.mangaocr_demon.ui.history.AlbumDetailFragment
import com.example.mangaocr_demon.ui.history.CreateAlbumDialogFragment

class BookcaseFragment : Fragment() {

    private var _binding: FragmentBookcaseBinding? = null
    private val binding get() = _binding!!

    private lateinit var albumAdapter: AlbumAdapter
    private lateinit var db: AppDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookcaseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = AppDatabase.getDatabase(requireContext())

        setupRecyclerView()
        observeAlbums()

        binding.fabAddAlbum.setOnClickListener {
            showCreateAlbumDialog()
        }
    }

    private fun setupRecyclerView() {
        albumAdapter = AlbumAdapter(
            onAlbumClick = { album ->
                // Mở chi tiết album
                val fragment = AlbumDetailFragment.newInstance(album.id)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack("album_detail")
                    .commit()
            },
            onAlbumLongClick = { album ->
                // Show options menu (edit, delete)
                showAlbumOptionsDialog(album)
            }
        )

        binding.rvAlbums.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = albumAdapter
        }
    }

    private fun observeAlbums() {
        lifecycleScope.launch {
            db.albumDao().getAllAlbums().collect { albums ->
                albumAdapter.submitList(albums)

                if (albums.isEmpty()) {
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.rvAlbums.visibility = View.GONE
                } else {
                    binding.tvEmptyState.visibility = View.GONE
                    binding.rvAlbums.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun showCreateAlbumDialog() {
        val dialog = CreateAlbumDialogFragment.newInstance()
        dialog.show(childFragmentManager, "create_album")
    }

    private fun showAlbumOptionsDialog(album: AlbumEntity) {
        // TODO: Show bottom sheet with edit/delete options
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}