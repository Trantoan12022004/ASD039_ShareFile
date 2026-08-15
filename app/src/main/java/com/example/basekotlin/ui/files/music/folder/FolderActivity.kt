package com.example.basekotlin.ui.files.music.folder

import android.content.Intent
import android.content.IntentSender
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseActivity
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.data.local.mediastore.MusicPlayerConnection
import com.example.basekotlin.databinding.ActivityFolderBinding
import com.example.basekotlin.databinding.ActivityMusicBinding
import com.example.basekotlin.databinding.ActivityMusicBinding.inflate
import com.example.basekotlin.databinding.PopupMoreBinding
import com.example.basekotlin.dialog.common.ConfirmActionDialog
import com.example.basekotlin.dialog.common.DetailInformationDialog
import com.example.basekotlin.dialog.common.SelectMoreDialog
import com.example.basekotlin.dialog.common.SelectPlaylistDialog
import com.example.basekotlin.dialog.common.TextInputDialog
import com.example.basekotlin.model.DeleteResult
import com.example.basekotlin.model.MusicPlaylist
import com.example.basekotlin.model.MusicTrack
import com.example.basekotlin.model.RenameResult
import com.example.basekotlin.ui.files.music.MusicActivity
import com.example.basekotlin.ui.files.music.MusicViewModel
import com.example.basekotlin.ui.files.music.adapter.MusicTrackAdapter
import com.example.basekotlin.ui.files.music.playing.SongPlayActivity
import com.example.basekotlin.ui.files.music.ringtone.RingtoneActivity
import com.example.basekotlin.util.AlbumArtUtils
import com.example.basekotlin.util.PopupMenuUtils
import kotlinx.coroutines.launch

class FolderActivity : BaseActivity<ActivityFolderBinding>(ActivityFolderBinding::inflate) {
    private val viewModel: FolderViewModel by viewModels()
    private val trackAdapter = MusicTrackAdapter()
    private var pendingConsentCallback: ((Boolean) -> Unit)? = null

    private val consentLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        val granted = activityResult.resultCode == RESULT_OK
        val callback = pendingConsentCallback
        pendingConsentCallback = null
        if (callback != null) {
            callback(granted)
        }
    }

    fun requestMediaConsent(intentSender: IntentSender, onResult: (granted: Boolean) -> Unit) {
        pendingConsentCallback = onResult
        consentLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
    }

    override fun getData() {
        val folderPath = intent.getStringExtra("EXTRA_FOLDER_PATH")
        val folderName = intent.getStringExtra("EXTRA_FOLDER_NAME")
        if (folderPath == null || folderPath.isEmpty()) {
            finishThisActivity()
            return
        }
        var nameToShow = folderName
        if (nameToShow == null) {
            nameToShow = ""
        }
        viewModel.setFolder(folderPath, nameToShow)
    }
    override fun initView() {
        binding.rvMusic.layoutManager = LinearLayoutManager(this)
        binding.rvMusic.adapter = trackAdapter
        MusicPlayerConnection.connect(this)
    }

    override fun bindView() {
        bindSelectionActions()
        binding.layoutToolbar.btnBack.tap {
            onBack()
        }
        binding.layoutToolbar.btnSelect.tap {
            viewModel.enterSelectionMode()
        }

        binding.layoutShuffle.tap {
            val tracks = viewModel.tracks.value
            if (tracks.isEmpty()) {
                return@tap
            }
            MusicPlayerConnection.playTracks(tracks.shuffled(), 0)
            startNextActivity(SongPlayActivity::class.java, null)
        }

        val miniPlayer = binding.layoutMiniPlayer
        miniPlayer.btnPlayPause.tap { MusicPlayerConnection.togglePlayPause() }
        miniPlayer.btnNext.tap { MusicPlayerConnection.skipToNext() }
        miniPlayer.btnPrev.tap { MusicPlayerConnection.skipToPrevious() }
        miniPlayer.root.tap {
            val trackId = MusicPlayerConnection.currentTrackId.value
            if (trackId != null) {
                startNextActivity(SongPlayActivity::class.java, null)
            }
        }
        trackAdapter.onTrackClick = { track ->
            if (viewModel.isSelectionMode.value) {
                viewModel.toggleTrackSelection(track.id)
            } else {
                val currentList = viewModel.tracks.value
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

        trackAdapter.onSelectToggle = { track ->
            if (viewModel.isSelectionMode.value) {
                viewModel.toggleTrackSelection(track.id)
            } else {
                viewModel.enterSelectionMode(initialTrackId = track.id)
            }
        }
        trackAdapter.onMoreClick = { track, anchor ->
            showMenuMore(track, anchor)
        }

        binding.layoutSelectAll.tap {
            val allIds = mutableListOf<Long>()
            val currentTracks = viewModel.tracks.value
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
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.folderName.collect { name ->
                        binding.layoutToolbar.tvTitle.text = name
                    }
                }
                launch {
                    viewModel.tracks.collect { tracks ->
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
                launch {
                    viewModel.isSelectionMode.collect { isSelecting ->
                        trackAdapter.isSelectionMode = isSelecting
                        trackAdapter.notifyDataSetChanged()
                        if (isSelecting) {
                            binding.layoutToolbar.tvTitle.gone()
                            binding.layoutToolbar.tvTitle1.visible()
                            binding.layoutMiniPlayer.root.gone()
                            binding.layoutSelectionActions.root.visible()
                            binding.layoutShuffle.gone()
                            binding.layoutSelectHeader.visible()
                        } else {
                            binding.layoutToolbar.tvTitle.visible()
                            binding.layoutToolbar.tvTitle1.gone()
                            binding.layoutMiniPlayer.root.visible()
                            binding.layoutSelectionActions.root.gone()
                            binding.layoutShuffle.visible()
                            binding.layoutSelectHeader.gone()
                        }
                    }
                }
                launch {
                    viewModel.selectedTrackIds.collect { selectedIds ->
                        trackAdapter.selectedIds = selectedIds
                        trackAdapter.notifyDataSetChanged()
                        binding.layoutToolbar.tvCountSong.text = selectedIds.size.toString()
                    }
                }

                launch {
                    MusicPlayerConnection.isPlaying.collect { isPlaying ->
                        if (isPlaying) {
                            miniPlayer.btnPlayPause.setImageResource(R.drawable.ic_pause)
                        } else {
                            miniPlayer.btnPlayPause.setImageResource(R.drawable.ic_play)
                        }
                    }
                }
                launch {
                    MusicPlayerConnection.currentTrack.collect { matchedTrack ->
                        if (matchedTrack != null) {
                            miniPlayer.tvFileName.text = matchedTrack.title
                            miniPlayer.tvFileInfo.text = matchedTrack.artist
                            val bitmap = AlbumArtUtils.loadAlbumArt(
                                context = binding.root.context,
                                contentUri = matchedTrack.contentUri,
                                albumId = matchedTrack.albumId
                            )
                            if (bitmap != null) {
                                miniPlayer.imgThumbnail.setImageBitmap(bitmap)
                            } else {
                                miniPlayer.imgThumbnail.setImageResource(R.drawable.ic_audio)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getSelectedTracks(): List<MusicTrack> {
        val selectedIds = viewModel.selectedTrackIds.value
        val allTracks = viewModel.tracks.value
        val result = mutableListOf<MusicTrack>()
        for (track in allTracks) {
            if (selectedIds.contains(track.id)) {
                result.add(track)
            }
        }
        return result
    }

    private fun bindSelectionActions() {
        val actions = binding.layoutSelectionActions

        actions.btnDeleteSelected.tap {
            val tracks = getSelectedTracks()
            if (tracks.isEmpty()) {
                Toast.makeText(
                    this,
                    getString(R.string.please_select_at_least_one_song),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                showDeleteConfirmDialog1(tracks)
            }
        }

        actions.btnShare.tap {
            val tracks = getSelectedTracks()
            if (tracks.isEmpty()) {
                Toast.makeText(
                    this,
                    getString(R.string.please_select_at_least_one_song),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                shareTracks(tracks)
            }
        }

        actions.btnSend.tap {
            val tracks = getSelectedTracks()
            if (tracks.isEmpty()) {
                Toast.makeText(
                    this,
                    getString(R.string.please_select_at_least_one_song),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                shareTracks(tracks)
            }
        }

        actions.btnMoreSelected.tap {
            showSelectionMoreMenu()
        }
    }

    private fun showSelectionMoreMenu() {
        val selectedTracks = getSelectedTracks()
        if (selectedTracks.isEmpty()) {
            Toast.makeText(
                this,
                getString(R.string.please_select_at_least_one_song),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        SelectMoreDialog(
            context = this,
            selectedTracks = selectedTracks,
            onRename = { track ->
                showRenameDialog(track)
            },
            onRingtoneCutter = { track ->
                val bundle = android.os.Bundle()
                bundle.putLong(RingtoneActivity.EXTRA_TRACK_ID, track.id)
                startNextActivity(RingtoneActivity::class.java, bundle)
            },
            onAddToPlaylist = { tracks ->
                showSelectPlaylistDialogMultiple(tracks)
            },
            onInformation = { track ->
                showDetailInformationDialog(track)
            }
        ).show()
    }
    private fun showDetailInformationDialog(track: MusicTrack) {
        DetailInformationDialog(this, track).show()
    }

    private fun showSelectPlaylistDialogMultiple(tracks: List<MusicTrack>) {
        SelectPlaylistDialog(
            context = this,
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
            context = this,
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
                            this,
                            getString(R.string.song_added_to_playlist),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this,
                            getString(R.string.song_already_in_playlist),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    viewModel.exitSelectionMode()
                }
            }
        }
    }

    private fun shareTracks(tracks: List<MusicTrack>) {
        val uris = ArrayList<android.net.Uri>()
        for (track in tracks) {
            uris.add(track.contentUri)
        }

        val intent: Intent
        if (uris.size == 1) {
            intent = Intent(Intent.ACTION_SEND)
            intent.type = "audio/*"
            intent.putExtra(Intent.EXTRA_STREAM, uris[0])
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            intent = Intent(Intent.ACTION_SEND_MULTIPLE)
            intent.type = "audio/*"
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            startActivity(Intent.createChooser(intent, getString(R.string.share)))
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.share), Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSelectPlaylistDialog(track: MusicTrack) {
        SelectPlaylistDialog(
            context = this,
            playlists = viewModel.playlists.value,
            onCreateNewPlaylist = {
                showCreatePlaylistDialog(track)
            },
            onPlaylistSelected = { playlist ->
                viewModel.addTrackToPlaylist(playlist.id, track.id) { wasAdded ->
                    if (wasAdded) {
                        Toast.makeText(
                            this,
                            getString(R.string.song_added_to_playlist),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this,
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
            context = this,
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
            context = this,
            title = getString(R.string.delete_song),
            message = getString(R.string.delete_song_desc, track.title),
            positiveText = getString(R.string.delete)
        ) {
            performDelete(listOf(track))
        }.show()
    }

    private fun showDeleteConfirmDialog1(tracks: List<MusicTrack>) {
        val message: String
        if (tracks.size == 1) {
            message = getString(R.string.delete_song_desc, tracks[0].title)
        } else {
            message = getString(R.string.delete_songs_desc, tracks.size)
        }
        ConfirmActionDialog(
            context = this,
            title = getString(R.string.delete_song),
            message = message,
            positiveText = getString(R.string.delete)
        ) {
            performDelete(tracks)
            viewModel.exitSelectionMode()
        }.show()
    }

    private fun performDelete(tracks: List<MusicTrack>) {
        viewModel.deleteTracks(tracks) { result ->
            when (result) {
                is DeleteResult.Success -> {
                    Toast.makeText(this, getString(R.string.delete_song_success), Toast.LENGTH_SHORT).show()
                }
                is DeleteResult.NeedsUserConsent -> {
                    requestMediaConsent(result.intentSender) { granted ->
                        if (granted) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                lifecycleScope.launch {
                                    for (deletedTrack in tracks) {
                                        viewModel.removeFromRecentlyPlayed(deletedTrack.id)
                                    }
                                }
                                Toast.makeText(this, getString(R.string.delete_song_success), Toast.LENGTH_SHORT).show()
                            } else {
                                performDelete(tracks)
                            }
                        } else {
                            Toast.makeText(this, getString(R.string.delete_song_failed), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                is DeleteResult.Failure -> {
                    Toast.makeText(this, getString(R.string.delete_song_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showRenameDialog(track: MusicTrack) {
        TextInputDialog(
            context = this,
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
                    Toast.makeText(this, getString(R.string.rename_song_success), Toast.LENGTH_SHORT).show()
                }
                is RenameResult.NeedsUserConsent -> {
                    requestMediaConsent(result.intentSender) { granted ->
                        if (granted) {
                            performRename(track, newTitle)
                        } else {
                            Toast.makeText(this, getString(R.string.rename_song_failed), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                is RenameResult.Failure -> {
                    Toast.makeText(this, getString(R.string.rename_song_failed), Toast.LENGTH_SHORT).show()
                }
            }
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
                val currentList = viewModel.tracks.value

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
                val currentList = viewModel.tracks.value
                for (item in currentList) {
                    if (item.id == track.id) {
                        isAlreadyFavorite = item.isFavorite
                        break
                    }
                }
                if (isAlreadyFavorite) {
                    Toast.makeText(
                        this,
                        getString(R.string.song_already_in_favorite),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    viewModel.toggleFavorite(track.id)
                    Toast.makeText(
                        this,
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
    override fun onBack() {
        if (viewModel.isSelectionMode.value) {
            viewModel.exitSelectionMode()
        } else {
            super.onBack()
        }
    }

}