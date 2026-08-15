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
import com.example.basekotlin.model.MusicTrack
import com.example.basekotlin.model.MusicSelectionTarget
import com.example.basekotlin.model.RenameResult
import com.example.basekotlin.ui.files.music.MusicActivity
import com.example.basekotlin.ui.files.music.MusicViewModel
import com.example.basekotlin.ui.files.music.adapter.MusicTrackAdapter
import com.example.basekotlin.ui.files.music.playing.SongPlayActivity
import com.example.basekotlin.ui.files.music.ringtone.RingtoneActivity
import com.example.basekotlin.util.PopupMenuUtils
import kotlinx.coroutines.launch

class FavoriteFragment : BaseFragment<FragmentAllBinding>() {

    private val viewModel: MusicViewModel by activityViewModels()

    private val trackAdapter = MusicTrackAdapter()

    override fun setBinding(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        saveInstanceState: Bundle?,
    ): FragmentAllBinding {
        return FragmentAllBinding.inflate(inflater!!, container, false)
    }

    override fun getData() {
    }

    override fun initView() {
        binding.rvMusic.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMusic.adapter = trackAdapter
        // Kết nối service sớm để controller sẵn sàng khi user click
        MusicPlayerConnection.connect(requireContext())
    }

    override fun bindView() {
        trackAdapter.onMoreClick = { track, anchor ->
            showMenuMore(track, anchor)
        }

        trackAdapter.onTrackClick = { track ->
            if (viewModel.isSelectionMode.value) {
                viewModel.toggleTrackSelection(track.id)
            } else {
                // Phát trong phạm vi danh sách yêu thích (không phải toàn bộ thư viện)
                val currentList = viewModel.favoriteTracks.value
                var startIndex = 0
                for (i in currentList.indices) {
                    if (currentList[i].id == track.id) {
                        startIndex = i
                    }
                }
                MusicPlayerConnection.playTracks(currentList, startIndex)
                startNextActivity(SongPlayActivity::class.java, null)
            }
        }

        // Bấm vào 1 item khi đang ở chế độ chọn -> tick/bỏ tick bài đó
        trackAdapter.onSelectToggle = { track ->
            if (viewModel.isSelectionMode.value) {
                viewModel.toggleTrackSelection(track.id)
            } else {
                // Long-press khi chưa ở chế độ chọn -> vào chế độ chọn kèm bài vừa nhấn giữ
                viewModel.enterSelectionMode(initialTrackId = track.id)
            }
        }

        binding.layoutShuffle.tap {
            val tracks = viewModel.favoriteTracks.value
            if (tracks.isEmpty()) {
                // Không có bài để phát
            } else {
                MusicPlayerConnection.playTracks(tracks.shuffled(), 0)
                startNextActivity(SongPlayActivity::class.java, null)
            }
        }

        binding.layoutSelectAll.tap {
            val allIds = mutableListOf<Long>()
            val currentTracks = viewModel.favoriteTracks.value
            for (track in currentTracks) {
                allIds.add(track.id)
            }
            val selectedIds = viewModel.selectedTrackIds.value
            val isAllSelected = allIds.isNotEmpty() && selectedIds.size == allIds.size
            if (isAllSelected) {
                viewModel.clearTrackSelection()
            } else {
                viewModel.selectAllTracks(allIds)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.favoriteTracks.collect { tracks ->
                        binding.swipeRefresh.isRefreshing = false
                        trackAdapter.addListData(tracks.toMutableList())
                        binding.tvCount.text = getString(R.string.song_count, tracks.size)
                        binding.tvCountSelect.text = tracks.size.toString()
                        if (tracks.isEmpty()) {
                            binding.allEmpty.visible()
                            binding.swipeRefresh.gone()
                        } else {
                            binding.allEmpty.gone()
                            binding.swipeRefresh.visible()
                        }
                    }
                }

                // Đổi header Shuffle <-> Select All và bật/tắt checkbox trên adapter
                launch {
                    viewModel.isSelectionMode.collect {
                        applyTrackSelectionUi()
                    }
                }

                launch {
                    viewModel.selectionTarget.collect {
                        applyTrackSelectionUi()
                    }
                }

                // Cập nhật checkbox từng item theo danh sách đang chọn
                launch {
                    viewModel.selectedTrackIds.collect { selectedIds ->
                        trackAdapter.selectedIds = selectedIds
                        trackAdapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    private fun applyTrackSelectionUi() {
        val isSelecting = viewModel.isSelectionMode.value
        val target = viewModel.selectionTarget.value
        val isTrackSelecting = isSelecting && target == MusicSelectionTarget.TRACK
        trackAdapter.isSelectionMode = isTrackSelecting
        trackAdapter.notifyDataSetChanged()
        if (isTrackSelecting) {
            binding.layoutShuffle.gone()
            binding.layoutSelectHeader.visible()
        } else {
            binding.layoutShuffle.visible()
            binding.layoutSelectHeader.gone()
        }
    }

    private fun showMenuMore(track: MusicTrack, anchor: View) {
        PopupMenuUtils.showAnchoredMenu(
            anchor = anchor,
            inflateBinding = { inflater -> PopupMoreBinding.inflate(inflater) },
        ) { popupBinding, popupWindow ->
            popupBinding.tvAddToFavorite.text = getString(R.string.remove_from_favorite)

            popupBinding.tvAddToPlaylist.tap {
                popupWindow.dismiss()
                showSelectPlaylistDialog(track)
            }

            popupBinding.tvPlay.tap {
                popupWindow.dismiss()
                val currentList = viewModel.favoriteTracks.value
                var startIndex = 0
                for (i in currentList.indices) {
                    if (currentList[i].id == track.id) {
                        startIndex = i
                    }
                }
                MusicPlayerConnection.playTracks(currentList, startIndex)
            }

            popupBinding.tvAddToFavorite.tap {
                popupWindow.dismiss()
                // Tab Favorite: bài đã nằm trong yêu thích -> gỡ khỏi danh sách
                viewModel.toggleFavorite(track.id)
                Toast.makeText(
                    requireContext(),
                    getString(R.string.song_removed_from_favorite),
                    Toast.LENGTH_SHORT
                ).show()
            }

            popupBinding.tvDelete.tap {
                popupWindow.dismiss()
                showDeleteConfirmDialog(track)
            }

            popupBinding.tvRename.tap {
                popupWindow.dismiss()
                showRenameDialog(track)
            }

            popupBinding.tvRingtoneCutter.tap {
                popupWindow.dismiss()
                val bundle = Bundle()
                bundle.putLong(RingtoneActivity.EXTRA_TRACK_ID, track.id)
                startNextActivity(RingtoneActivity::class.java, bundle)
            }

            popupBinding.tvSelect.tap {
                popupWindow.dismiss()
                viewModel.enterSelectionMode(initialTrackId = track.id)
            }
        }
    }

    private fun showSelectPlaylistDialog(track: MusicTrack) {
        SelectPlaylistDialog(
            context = requireContext(),
            playlists = viewModel.playlists.value,
            onCreateNewPlaylist = {
                showCreatePlaylistDialog(track)
            },
            onPlaylistSelected = { playlist ->
                viewModel.addTrackToPlaylist(playlist.id, track.id) { wasAdded ->
                    if (wasAdded) {
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
        ).show()
    }

    private fun showCreatePlaylistDialog(track: MusicTrack) {
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
                viewModel.addTrackToPlaylist(newPlaylistId, track.id)
            }
        }.show()
    }

    private fun showDeleteConfirmDialog(track: MusicTrack) {
        ConfirmActionDialog(
            context = requireContext(),
            title = getString(R.string.delete_song),
            message = getString(R.string.delete_song_desc, track.title),
            positiveText = getString(R.string.delete)
        ) {
            performDelete(listOf(track))
        }.show()
    }

    private fun performDelete(tracks: List<MusicTrack>) {
        viewModel.deleteTracks(tracks) { result ->
            when (result) {
                is DeleteResult.Success -> {
                    Toast.makeText(requireContext(), getString(R.string.delete_song_success), Toast.LENGTH_SHORT).show()
                }
                is DeleteResult.NeedsUserConsent -> {
                    (requireActivity() as MusicActivity).requestMediaConsent(result.intentSender) { granted ->
                        if (granted) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                viewLifecycleOwner.lifecycleScope.launch {
                                    for (deletedTrack in tracks) {
                                        viewModel.removeFromRecentlyPlayed(deletedTrack.id)
                                    }
                                }
                                Toast.makeText(requireContext(), getString(R.string.delete_song_success), Toast.LENGTH_SHORT).show()
                            } else {
                                performDelete(tracks)
                            }
                        } else {
                            Toast.makeText(requireContext(), getString(R.string.delete_song_failed), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                is DeleteResult.Failure -> {
                    Toast.makeText(requireContext(), getString(R.string.delete_song_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showRenameDialog(track: MusicTrack) {
        TextInputDialog(
            context = requireContext(),
            title = getString(R.string.rename),
            hint = getString(R.string.rename),
            initialText = track.title,
            positiveText = getString(R.string.rename)
        ) { newTitle ->
            performRename(track, newTitle)
        }.show()
    }

    private fun performRename(track: MusicTrack, newTitle: String) {
        viewModel.renameTrack(track, newTitle) { result ->
            when (result) {
                is RenameResult.Success -> {
                    Toast.makeText(requireContext(), getString(R.string.rename_song_success), Toast.LENGTH_SHORT).show()
                }
                is RenameResult.NeedsUserConsent -> {
                    (requireActivity() as MusicActivity).requestMediaConsent(result.intentSender) { granted ->
                        if (granted) {
                            performRename(track, newTitle)
                        } else {
                            Toast.makeText(requireContext(), getString(R.string.rename_song_failed), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                is RenameResult.Failure -> {
                    Toast.makeText(requireContext(), getString(R.string.rename_song_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
