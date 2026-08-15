package com.example.basekotlin.ui.files.music.fragment

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseFragment
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.data.local.mediastore.MusicPlayerConnection
import com.example.basekotlin.databinding.FragmentAllBinding
import com.example.basekotlin.databinding.PopupMoreBinding
import com.example.basekotlin.dialog.common.ConfirmActionDialog
import com.example.basekotlin.dialog.common.SelectPlaylistDialog
import com.example.basekotlin.dialog.common.TextInputDialog
import com.example.basekotlin.model.DeleteResult
import com.example.basekotlin.model.MusicFolder
import com.example.basekotlin.model.MusicPlaylist
import com.example.basekotlin.model.MusicSelectionTarget
import com.example.basekotlin.model.MusicTrack
import com.example.basekotlin.ui.files.music.MusicActivity
import com.example.basekotlin.ui.files.music.MusicViewModel
import com.example.basekotlin.ui.files.music.adapter.MusicFolderAdapter
import com.example.basekotlin.ui.files.music.folder.FolderActivity
import com.example.basekotlin.util.PopupMenuUtils
import kotlinx.coroutines.launch

class FoldersFragment : BaseFragment<FragmentAllBinding>() {
    private val viewModel: MusicViewModel by activityViewModels()
    private val folderAdapter = MusicFolderAdapter()

    override fun setBinding(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        saveInstanceState: Bundle?
    ): FragmentAllBinding {
        return FragmentAllBinding.inflate(inflater!!, container, false)
    }

    override fun initView() {
        binding.rvMusic.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMusic.adapter = folderAdapter
        binding.viewTop.gone()
        MusicPlayerConnection.connect(requireContext())
    }

    override fun bindView() {
        folderAdapter.onMoreClick = { folder, anchor ->
            showMenuMore(folder, anchor)
        }

        folderAdapter.onFoledrClick = { folder ->
            val isFolderSelecting = viewModel.isSelectionMode.value &&
                    viewModel.selectionTarget.value == MusicSelectionTarget.FOLDER
            if (isFolderSelecting) {
                viewModel.toggleFolderSelection(folder.folderPath)
            } else {
                val bundle = Bundle()
                bundle.putString("EXTRA_FOLDER_PATH", folder.folderPath)
                bundle.putString("EXTRA_FOLDER_NAME", folder.folderName)
                startNextActivity(FolderActivity::class.java, bundle)
            }
        }

        folderAdapter.onSelectToggle = { folder ->
            val isFolderSelecting = viewModel.isSelectionMode.value &&
                    viewModel.selectionTarget.value == MusicSelectionTarget.FOLDER
            if (isFolderSelecting) {
                viewModel.toggleFolderSelection(folder.folderPath)
            } else {
                viewModel.enterFolderSelectionMode(initialFolderPath = folder.folderPath)
            }
        }

        binding.layoutSelectAll.tap {
            val allPaths = mutableListOf<String>()
            val currentFolders = viewModel.folders.value
            for (folder in currentFolders) {
                allPaths.add(folder.folderPath)
            }
            val selectedPaths = viewModel.selectedFolderPaths.value
            val isAllSelected = allPaths.isNotEmpty() && selectedPaths.size == allPaths.size
            if (isAllSelected) {
                viewModel.clearFolderSelection()
            } else {
                viewModel.selectAllFolders(allPaths)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.folders.collect { folders ->
                        folderAdapter.addListData(folders.toMutableList())
                        binding.tvCountSelect.text = folders.size.toString()
                        if (folders.isEmpty()) {
                            binding.allEmpty.visible()
                            binding.swipeRefresh.gone()
                        } else {
                            binding.allEmpty.gone()
                            binding.swipeRefresh.visible()
                        }
                    }
                }

                launch {
                    viewModel.isSelectionMode.collect {
                        applyFolderSelectionUi()
                    }
                }

                launch {
                    viewModel.selectionTarget.collect {
                        applyFolderSelectionUi()
                    }
                }

                launch {
                    viewModel.selectedFolderPaths.collect { selectedPaths ->
                        folderAdapter.selectedPaths = selectedPaths
                        folderAdapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    private fun applyFolderSelectionUi() {
        val isSelecting = viewModel.isSelectionMode.value
        val target = viewModel.selectionTarget.value
        val isFolderSelecting = isSelecting && target == MusicSelectionTarget.FOLDER
        folderAdapter.isSelectionMode = isFolderSelecting
        folderAdapter.notifyDataSetChanged()
        if (isFolderSelecting) {
            binding.viewTop.visible()
            binding.layoutShuffle.gone()
            binding.layoutSelectHeader.visible()
        } else {
            binding.viewTop.gone()
        }
    }

    private fun showMenuMore(folder: MusicFolder, anchor: View) {
        PopupMenuUtils.showAnchoredMenu(
            anchor = anchor,
            inflateBinding = { inflater -> PopupMoreBinding.inflate(inflater) },
        ) { popupBinding, popupWindow ->
            popupBinding.tvRingtoneCutter.gone()
            popupBinding.viewDivider2.gone()
            popupBinding.tvRename.gone()
            popupBinding.viewDivider7.gone()

            popupBinding.tvPlay.tap {
                popupWindow.dismiss()
                val tracks = viewModel.getTracksInFolder(folder.folderPath)
                if (tracks.isEmpty()) {
                    // Không có bài để phát
                } else {
                    MusicPlayerConnection.playTracks(tracks, 0)
                }
            }

            popupBinding.tvSelect.tap {
                popupWindow.dismiss()
                viewModel.enterFolderSelectionMode(initialFolderPath = folder.folderPath)
            }

            popupBinding.tvShare.tap {
                popupWindow.dismiss()
                val tracks = viewModel.getTracksInFolder(folder.folderPath)
                shareFolderTracks(tracks)
            }

            popupBinding.tvAddToFavorite.tap {
                popupWindow.dismiss()
                addFolderTracksToFavorite(folder)
            }

            popupBinding.tvAddToPlaylist.tap {
                popupWindow.dismiss()
                val tracks = viewModel.getTracksInFolder(folder.folderPath)
                showSelectPlaylistDialogMultiple(tracks)
            }

            popupBinding.tvMoveToSafebox.tap {
                popupWindow.dismiss()
                // Move to SafeBox sẽ xử lý sau
            }

            popupBinding.tvDelete.tap {
                popupWindow.dismiss()
                showDeleteFolderConfirmDialog(folder)
            }
        }
    }

    private fun shareFolderTracks(tracks: List<MusicTrack>) {
        if (tracks.isEmpty()) {
            Toast.makeText(
                requireContext(),
                getString(R.string.please_select_at_least_one_song),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            val activity = requireActivity()
            if (activity is MusicActivity) {
                activity.shareTracks(tracks)
            }
        }
    }

    private fun addFolderTracksToFavorite(folder: MusicFolder) {
        val tracks = viewModel.getTracksInFolder(folder.folderPath)
        if (tracks.isEmpty()) {
            Toast.makeText(
                requireContext(),
                getString(R.string.please_select_at_least_one_song),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            val trackIds = mutableListOf<Long>()
            for (track in tracks) {
                trackIds.add(track.id)
            }
            viewModel.addTracksToFavorite(trackIds) { addedCount ->
                if (addedCount > 0) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.song_added_to_favorite),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.song_already_in_favorite),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showSelectPlaylistDialogMultiple(tracks: List<MusicTrack>) {
        if (tracks.isEmpty()) {
            Toast.makeText(
                requireContext(),
                getString(R.string.please_select_at_least_one_song),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        SelectPlaylistDialog(
            context = requireContext(),
            playlists = viewModel.playlists.value,
            onCreateNewPlaylist = {
                showCreatePlaylistDialogMultiple(tracks)
            },
            onPlaylistSelected = { playlist ->
                addTracksToPlaylist(playlist, tracks)
            }
        ).show()
    }

    private fun showCreatePlaylistDialogMultiple(tracks: List<MusicTrack>) {
        TextInputDialog(
            context = requireContext(),
            title = getString(R.string.create_new_playlist),
            hint = getString(R.string.create_new_playlist),
            positiveText = getString(R.string.create),
            validate = { enteredName ->
                val isNameTaken = viewModel.isPlaylistNameTaken(enteredName)
                if (isNameTaken) {
                    getString(R.string.text_input_failed1)
                } else {
                    null
                }
            }
        ) { enteredName ->
            viewModel.createPlaylist(enteredName) { newPlaylistId ->
                val newPlaylist = MusicPlaylist(
                    id = newPlaylistId,
                    name = enteredName,
                    trackCount = 0,
                    createdAtMillis = System.currentTimeMillis()
                )
                addTracksToPlaylist(newPlaylist, tracks)
            }
        }.show()
    }

    private fun addTracksToPlaylist(playlist: MusicPlaylist, tracks: List<MusicTrack>) {
        if (tracks.isEmpty()) {
            return
        }

        var processedCount = 0
        var addedCount = 0
        for (track in tracks) {
            viewModel.addTrackToPlaylist(playlist.id, track.id) { wasAdded ->
                processedCount++
                if (wasAdded) {
                    addedCount++
                }
                if (processedCount == tracks.size) {
                    if (addedCount > 0) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.song_added_to_playlist),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.song_already_in_playlist),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun showDeleteFolderConfirmDialog(folder: MusicFolder) {
        ConfirmActionDialog(
            context = requireContext(),
            title = getString(R.string.delete_folder),
            message = getString(R.string.delete_folder_desc, folder.folderName),
            positiveText = getString(R.string.delete)
        ) {
            val tracks = viewModel.getTracksInFolder(folder.folderPath)
            performDelete(tracks)
        }.show()
    }

    private fun performDelete(tracks: List<MusicTrack>) {
        if (tracks.isEmpty()) {
            return
        }
        viewModel.deleteTracks(tracks) { result ->
            when (result) {
                is DeleteResult.Success -> {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.delete_song_success),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is DeleteResult.NeedsUserConsent -> {
                    val activity = requireActivity()
                    if (activity is MusicActivity) {
                        activity.requestMediaConsent(result.intentSender) { granted ->
                            if (granted) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    viewLifecycleOwner.lifecycleScope.launch {
                                        for (deletedTrack in tracks) {
                                            viewModel.removeFromRecentlyPlayed(deletedTrack.id)
                                        }
                                    }
                                    Toast.makeText(
                                        requireContext(),
                                        getString(R.string.delete_song_success),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    performDelete(tracks)
                                }
                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    getString(R.string.delete_song_failed),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
                is DeleteResult.Failure -> {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.delete_song_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
