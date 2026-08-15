package com.example.basekotlin.ui.files.music.fragment

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
import com.example.basekotlin.model.MusicPlaylist
import com.example.basekotlin.model.MusicSelectionTarget
import com.example.basekotlin.model.MusicTrack
import com.example.basekotlin.ui.files.music.MusicActivity
import com.example.basekotlin.ui.files.music.MusicViewModel
import com.example.basekotlin.ui.files.music.adapter.MusicPlaylistAdapter2
import com.example.basekotlin.ui.files.music.playlist.PlaylistActivity
import com.example.basekotlin.util.PopupMenuUtils
import kotlinx.coroutines.launch

class PlaylistFragment : BaseFragment<FragmentAllBinding>() {
    private val viewModel: MusicViewModel by activityViewModels()
    private val playlistAdapter = MusicPlaylistAdapter2()

    override fun setBinding(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        saveInstanceState: Bundle?
    ): FragmentAllBinding {
        return FragmentAllBinding.inflate(inflater!!, container, false)
    }

    override fun initView() {
        binding.rvMusic.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMusic.adapter = playlistAdapter
        binding.viewTop.gone()
        MusicPlayerConnection.connect(requireContext())
    }

    override fun bindView() {
        playlistAdapter.onMoreClick = { playlist, anchor ->
            showMenuMore(playlist, anchor)
        }

        playlistAdapter.onPlaylistClick = { playlist ->
            val isPlaylistSelecting = viewModel.isSelectionMode.value &&
                    viewModel.selectionTarget.value == MusicSelectionTarget.PLAYLIST
            if (isPlaylistSelecting) {
                viewModel.togglePlaylistSelection(playlist.id)
            } else {
                val bundle = Bundle()
                bundle.putLong(PlaylistActivity.EXTRA_PLAYLIST_ID, playlist.id)
                bundle.putString(PlaylistActivity.EXTRA_PLAYLIST_NAME, playlist.name)
                startNextActivity(PlaylistActivity::class.java, bundle)
            }
        }

        playlistAdapter.onSelectToggle = { playlist ->
            val isPlaylistSelecting = viewModel.isSelectionMode.value &&
                    viewModel.selectionTarget.value == MusicSelectionTarget.PLAYLIST
            if (isPlaylistSelecting) {
                viewModel.togglePlaylistSelection(playlist.id)
            } else {
                viewModel.enterPlaylistSelectionMode(initialPlaylistId = playlist.id)
            }
        }

        binding.layoutSelectAll.tap {
            val allIds = mutableListOf<Long>()
            val currentPlaylists = viewModel.playlists.value
            for (playlist in currentPlaylists) {
                allIds.add(playlist.id)
            }
            val selectedIds = viewModel.selectedPlaylistIds.value
            val isAllSelected = allIds.isNotEmpty() && selectedIds.size == allIds.size
            if (isAllSelected) {
                viewModel.clearPlaylistSelection()
            } else {
                viewModel.selectAllPlaylists(allIds)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.playlists.collect { playlists ->
                        playlistAdapter.addListData(playlists.toMutableList())
                        binding.tvCountSelect.text = playlists.size.toString()
                        if (playlists.isEmpty()) {
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
                        applyPlaylistSelectionUi()
                    }
                }

                launch {
                    viewModel.selectionTarget.collect {
                        applyPlaylistSelectionUi()
                    }
                }

                launch {
                    viewModel.selectedPlaylistIds.collect { selectedIds ->
                        playlistAdapter.selectedIds = selectedIds
                        playlistAdapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    private fun applyPlaylistSelectionUi() {
        val isSelecting = viewModel.isSelectionMode.value
        val target = viewModel.selectionTarget.value
        val isPlaylistSelecting = isSelecting && target == MusicSelectionTarget.PLAYLIST
        playlistAdapter.isSelectionMode = isPlaylistSelecting
        playlistAdapter.notifyDataSetChanged()
        if (isPlaylistSelecting) {
            binding.viewTop.visible()
            binding.layoutShuffle.gone()
            binding.layoutSelectHeader.visible()
        } else {
            binding.viewTop.gone()
        }
    }

    private fun showMenuMore(playlist: MusicPlaylist, anchor: View) {
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
                viewModel.getPlaylistTracks(playlist.id) { tracks ->
                    if (tracks.isEmpty()) {
                        // Không có bài để phát
                    } else {
                        MusicPlayerConnection.playTracks(tracks, 0)
                    }
                }
            }

            popupBinding.tvSelect.tap {
                popupWindow.dismiss()
                viewModel.enterPlaylistSelectionMode(initialPlaylistId = playlist.id)
            }

            popupBinding.tvShare.tap {
                popupWindow.dismiss()
                viewModel.getPlaylistTracks(playlist.id) { tracks ->
                    sharePlaylistTracks(tracks)
                }
            }

            popupBinding.tvAddToFavorite.tap {
                popupWindow.dismiss()
                addPlaylistTracksToFavorite(playlist)
            }

            popupBinding.tvAddToPlaylist.tap {
                popupWindow.dismiss()
                viewModel.getPlaylistTracks(playlist.id) { tracks ->
                    showSelectPlaylistDialogMultiple(tracks)
                }
            }

            popupBinding.tvMoveToSafebox.tap {
                popupWindow.dismiss()
                // Move to SafeBox sẽ xử lý sau
            }

            popupBinding.tvDelete.tap {
                popupWindow.dismiss()
                showDeletePlaylistConfirmDialog(playlist)
            }
        }
    }

    private fun sharePlaylistTracks(tracks: List<MusicTrack>) {
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

    private fun addPlaylistTracksToFavorite(playlist: MusicPlaylist) {
        viewModel.getPlaylistTracks(playlist.id) { tracks ->
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
            onPlaylistSelected = { targetPlaylist ->
                addTracksToPlaylist(targetPlaylist, tracks)
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

    private fun showDeletePlaylistConfirmDialog(playlist: MusicPlaylist) {
        ConfirmActionDialog(
            context = requireContext(),
            title = getString(R.string.delete_playlist),
            message = getString(R.string.delete_playlist_desc, playlist.name),
            positiveText = getString(R.string.delete)
        ) {
            viewModel.deletePlaylist(playlist.id) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.delete_playlist_success),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }.show()
    }
}
