package com.example.basekotlin.ui.files.music.fragment

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseFragment
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.hideNavigation
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
import kotlin.getValue

class AllFragment : BaseFragment<FragmentAllBinding>() {


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
            Log.d("DEBUG_TRACK", track.toString())

            val currentList = viewModel.allTracks.value

//            Timf vị trí bài hát được click
            var startIndex = 0
            for(i in currentList.indices){
                if (currentList[i].id == track.id) {
                    startIndex = i
                }
            }

            // Phát danh sách, bắt đầu từ bài được chọn
            MusicPlayerConnection.playTracks(currentList, startIndex)
            // Mở màn hình đang phát
            startNextActivity(SongPlayActivity::class.java, null)
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

        binding.layoutSelectAll.tap {
            val allIds = viewModel.allTracks.value.map { it.id }
            val selectedIds = viewModel.selectedTrackIds.value
            val isAllSelected = allIds.isNotEmpty() && selectedIds.size == allIds.size
            if (isAllSelected) {
                viewModel.clearTrackSelection()
            } else {
                viewModel.selectAllTracks(allIds)
            }
        }

//        binding.swipeRefresh.setOnRefreshListener {
//            viewModel.refreshAllTracks()
//        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.allTracks.collect { tracks ->
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

                // Cập nhật checkbox từng item + icon Select All theo danh sách đang chọn
                launch {
                    viewModel.selectedTrackIds.collect { selectedIds ->
                        trackAdapter.selectedIds = selectedIds
                        trackAdapter.notifyDataSetChanged()

                        val total = viewModel.allTracks.value.size
                        val isAllSelected = total > 0 && selectedIds.size == total
//                        if (isAllSelected) {
//                            binding.imgSelectAll.setImageResource(R.drawable.ic_checkbox_checked)
//                        } else {
//                            binding.imgSelectAll.setImageResource(R.drawable.ic_checkbox_unchecked)
//                        }
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

    private fun hideNavigationBar() {
        val activity = requireActivity()
        val window = activity.window
        val decorView = window.decorView
        window.hideNavigation()
        val controller = WindowCompat.getInsetsController(window, decorView)
        if (controller != null) {
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.navigationBars())
        }
    }

    private fun showMenuMore(track: MusicTrack, anchor: View) {
        PopupMenuUtils.showAnchoredMenu(
            anchor = anchor,
            inflateBinding = { inflater -> PopupMoreBinding.inflate(inflater) },
        ) { popupBinding, popupWindow ->
            popupBinding.tvAddToPlaylist.tap {
                popupWindow.dismiss()
                showSelectPlaylistDialog(track)
            }
            popupBinding.tvPlay.tap {
                popupWindow.dismiss()
                val currentList = viewModel.allTracks.value

//            Timf vị trí bài hát được click
                var startIndex = 0
                for(i in currentList.indices){
                    if (currentList[i].id == track.id) {
                        startIndex = i
                    }
                }

                // Phát danh sách, bắt đầu từ bài được chọn
                MusicPlayerConnection.playTracks(currentList, startIndex)
            }

            popupBinding.tvAddToFavorite.tap{
                popupWindow.dismiss()
                var isAlreadyFavorite = false
                val currentList = viewModel.allTracks.value
                for (item in currentList) {
                    if (item.id == track.id) {
                        isAlreadyFavorite = item.isFavorite
                        break
                    }
                }
                if (isAlreadyFavorite) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.song_already_in_favorite),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    viewModel.toggleFavorite(track.id)
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.song_added_to_favorite),
                        Toast.LENGTH_SHORT
                    ).show()
                }
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
                val bundle = Bundle().apply {
                    putLong("EXTRA_TRACK_ID", track.id)
                }
                startNextActivity(RingtoneActivity::class.java, bundle)
            }

            popupBinding.tvSelect.tap {
                popupWindow.dismiss()
                viewModel.enterSelectionMode(initialTrackId = track.id)
            }
            // Các item khác (Play, Share, Delete, Add to favorite...) gắn tương tự tại đây
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
                    // Không cần gọi refresh thủ công — MediaStoreObserver sẽ tự bắn lại danh sách mới
                }
                is DeleteResult.NeedsUserConsent -> {
                    (requireActivity() as MusicActivity).requestMediaConsent(result.intentSender) { granted ->
                        if (granted) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                // R+: hệ thống đã xóa xong khi user đồng ý -> chỉ cần dọn Room + thông báo
                                viewLifecycleOwner.lifecycleScope.launch {
                                    for (deletedTrack in tracks) {
                                        viewModel.removeFromRecentlyPlayed(deletedTrack.id)
                                    }
                                }
                                Toast.makeText(requireContext(), getString(R.string.delete_song_success), Toast.LENGTH_SHORT).show()
                            } else {
                                // API 29: chưa xóa thật, phải gọi lại đúng thao tác ban đầu
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
            Log.d("DEBUG_RENAME", result.toString())
            when (result) {
                is RenameResult.Success -> {
                    Toast.makeText(requireContext(), getString(R.string.rename_song_success), Toast.LENGTH_SHORT).show()
                }
                is RenameResult.NeedsUserConsent -> {
                    (requireActivity() as MusicActivity).requestMediaConsent(result.intentSender) { granted ->
                        if (granted) {
                            // createWriteRequest CHỈ cấp quyền -> luôn phải gọi lại renameTrack,
                            // khác với xóa trên Android 11+ (hệ thống tự hoàn tất).
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