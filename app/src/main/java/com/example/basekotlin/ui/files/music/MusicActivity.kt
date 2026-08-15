package com.example.basekotlin.ui.files.music

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseActivity
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.data.local.mediastore.MusicPlayerConnection
import com.example.basekotlin.databinding.ActivityMusicBinding
import com.example.basekotlin.dialog.common.ConfirmActionDialog
import com.example.basekotlin.dialog.common.DetailInformationDialog
import com.example.basekotlin.dialog.common.SelectMoreDialog
import com.example.basekotlin.dialog.common.SelectPlaylistDialog
import com.example.basekotlin.dialog.common.TextInputDialog
import com.example.basekotlin.model.DeleteResult
import com.example.basekotlin.model.MusicFolder
import com.example.basekotlin.model.MusicPlaylist
import com.example.basekotlin.model.MusicSelectionTarget
import com.example.basekotlin.model.MusicTrack
import com.example.basekotlin.model.RenameResult
import com.example.basekotlin.ui.files.music.fragment.SearchFragment
import com.example.basekotlin.ui.files.music.playing.SongPlayActivity
import com.example.basekotlin.ui.files.music.ringtone.RingtoneActivity
import com.example.basekotlin.util.AlbumArtUtils
import com.example.basekotlin.util.Utils
import com.google.android.material.tabs.TabLayoutMediator
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.launch

class MusicActivity : BaseActivity<ActivityMusicBinding>(ActivityMusicBinding::inflate) {
    private lateinit var pagerAdapter: MusicPagerAdapter
    private val viewModel: MusicViewModel by viewModels()
    private var isSearchMode = false

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

    override fun initView() {
        pagerAdapter = MusicPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.offscreenPageLimit = 1

        val tabTitles = arrayOf(
            getString(R.string.all),
            getString(R.string.receive),
            getString(R.string.folders),
            getString(R.string.favorite),
            getString(R.string.recently_played),
            getString(R.string.playlist)
        )

        TabLayoutMediator(
            binding.tabLayout,
            binding.viewPager,
            true,
            false
        ) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()

        MusicPlayerConnection.connect(this)
    }

    override fun bindView() {
        binding.layoutToolbar.btnBack.tap {
            if (viewModel.isSelectionMode.value) {
                viewModel.exitSelectionMode()
            } else if (isSearchMode) {
                closeSearch()
            } else {
                finish()
            }
        }
        binding.layoutToolbar.btnSearch.tap {
            openSearch()
        }
        bindSearchInput()
        binding.layoutToolbar.btnSelect.tap {
            // Ở chế độ search chỉ chọn bài hát, không theo tab đang ẩn
            if (isSearchMode) {
                viewModel.enterSelectionMode()
            } else {
                val currentTab = binding.viewPager.currentItem
                if (currentTab == 2) {
                    viewModel.enterFolderSelectionMode()
                } else if (currentTab == 5) {
                    viewModel.enterPlaylistSelectionMode()
                } else {
                    viewModel.enterSelectionMode()
                }
            }
        }

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (viewModel.isSelectionMode.value) {
                    viewModel.exitSelectionMode()
                }
            }
        })

        bindSelectionActions()

        val miniPlayer = binding.layoutMiniPlayer
        miniPlayer.btnPlayPause.tap {
            MusicPlayerConnection.togglePlayPause()
        }
        miniPlayer.btnNext.tap {
            MusicPlayerConnection.skipToNext()
        }
        miniPlayer.btnPrev.tap {
            MusicPlayerConnection.skipToPrevious()
        }
        miniPlayer.root.tap {
            val trackId = MusicPlayerConnection.currentTrackId.value
            if (trackId != null) {
                startNextActivity(SongPlayActivity::class.java, null)
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
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
                    MusicPlayerConnection.currentTrackId.collect { trackId ->
                        val allTracks = viewModel.allTracks.value
                        var matchedTrack: MusicTrack? = null
                        for (track in allTracks) {
                            if (track.id == trackId) {
                                matchedTrack = track
                            }
                        }
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

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isSelectionMode.collect { isSelecting ->
                        if (isSelecting) {
                            binding.layoutToolbar.tvTitle.gone()
                            binding.layoutToolbar.tvTitle1.visible()
                            binding.layoutMiniPlayer.root.gone()
                            binding.layoutSelectionActions.root.visible()
                        } else {
                            binding.layoutToolbar.tvTitle.visible()
                            binding.layoutToolbar.tvTitle1.gone()
                            binding.layoutMiniPlayer.root.visible()
                            binding.layoutSelectionActions.root.gone()
                        }
                    }
                }
                launch {
                    viewModel.selectedTrackIds.collect {
                        updateSelectionCount()
                    }
                }
                launch {
                    viewModel.selectedFolderPaths.collect {
                        updateSelectionCount()
                    }
                }
                launch {
                    viewModel.selectedPlaylistIds.collect {
                        updateSelectionCount()
                    }
                }
                launch {
                    viewModel.selectionTarget.collect {
                        updateSelectionCount()
                    }
                }
            }
        }
    }

    private fun updateSelectionCount() {
        val target = viewModel.selectionTarget.value
        val count: Int
        if (target == MusicSelectionTarget.FOLDER) {
            count = viewModel.selectedFolderPaths.value.size
        } else if (target == MusicSelectionTarget.PLAYLIST) {
            count = viewModel.selectedPlaylistIds.value.size
        } else {
            count = viewModel.selectedTrackIds.value.size
        }
        binding.layoutToolbar.tvCountSong.text = count.toString()
    }

    private fun getSelectedTracks(): List<MusicTrack> {
        val selectedIds = viewModel.selectedTrackIds.value
        val allTracks = viewModel.allTracks.value
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
            val target = viewModel.selectionTarget.value
            if (target == MusicSelectionTarget.FOLDER) {
                val folders = getSelectedFolders()
                if (folders.isEmpty()) {
                    Toast.makeText(
                        this,
                        getString(R.string.please_select_at_least_one_item),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    showDeleteFoldersConfirmDialog(folders)
                }
            } else if (target == MusicSelectionTarget.PLAYLIST) {
                val playlists = getSelectedPlaylists()
                if (playlists.isEmpty()) {
                    Toast.makeText(
                        this,
                        getString(R.string.please_select_at_least_one_item),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    showDeletePlaylistsConfirmDialog(playlists)
                }
            } else {
                val tracks = getSelectedTracks()
                if (tracks.isEmpty()) {
                    Toast.makeText(
                        this,
                        getString(R.string.please_select_at_least_one_song),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    showDeleteConfirmDialog(tracks)
                }
            }
        }

        actions.btnShare.tap {
            shareCurrentSelection()
        }

        actions.btnSend.tap {
            shareCurrentSelection()
        }

        actions.btnMoreSelected.tap {
            showSelectionMoreMenu()
        }
    }

    private fun shareCurrentSelection() {
        val target = viewModel.selectionTarget.value
        if (target == MusicSelectionTarget.FOLDER) {
            val tracks = viewModel.getTracksInSelectedFolders()
            if (tracks.isEmpty()) {
                Toast.makeText(
                    this,
                    getString(R.string.please_select_at_least_one_song),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                shareTracks(tracks)
            }
        } else if (target == MusicSelectionTarget.PLAYLIST) {
            viewModel.getTracksOfSelectedPlaylists { tracks ->
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
        } else {
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
    }

    fun shareTracks(tracks: List<MusicTrack>) {
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

    private fun showDeleteConfirmDialog(tracks: List<MusicTrack>) {
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
                    Toast.makeText(
                        this,
                        getString(R.string.delete_song_success),
                        Toast.LENGTH_SHORT
                    ).show()
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
                                Toast.makeText(
                                    this,
                                    getString(R.string.delete_song_success),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                performDelete(tracks)
                            }
                        } else {
                            Toast.makeText(
                                this,
                                getString(R.string.delete_song_failed),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                is DeleteResult.Failure -> {
                    Toast.makeText(
                        this,
                        getString(R.string.delete_song_failed),
                        Toast.LENGTH_SHORT
                    ).show()
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
                    Toast.makeText(
                        this,
                        getString(R.string.rename_song_success),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                is RenameResult.NeedsUserConsent -> {
                    requestMediaConsent(result.intentSender) { granted ->
                        if (granted) {
                            performRename(track, newTitle)
                        } else {
                            Toast.makeText(
                                this,
                                getString(R.string.rename_song_failed),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                is RenameResult.Failure -> {
                    Toast.makeText(
                        this,
                        getString(R.string.rename_song_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
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

    private fun showDetailInformationDialog(track: MusicTrack) {
        DetailInformationDialog(this, track).show()
    }

    private fun showSelectionMoreMenu() {
        val target = viewModel.selectionTarget.value
        if (target == MusicSelectionTarget.FOLDER) {
            showFolderSelectionMoreMenu()
        } else if (target == MusicSelectionTarget.PLAYLIST) {
            showPlaylistSelectionMoreMenu()
        } else {
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
    }

    private fun getSelectedFolders(): List<MusicFolder> {
        val selectedPaths = viewModel.selectedFolderPaths.value
        val result = mutableListOf<MusicFolder>()
        val allFolders = viewModel.folders.value
        for (folder in allFolders) {
            if (selectedPaths.contains(folder.folderPath)) {
                result.add(folder)
            }
        }
        return result
    }

    private fun getSelectedPlaylists(): List<MusicPlaylist> {
        val selectedIds = viewModel.selectedPlaylistIds.value
        val result = mutableListOf<MusicPlaylist>()
        val allPlaylists = viewModel.playlists.value
        for (playlist in allPlaylists) {
            if (selectedIds.contains(playlist.id)) {
                result.add(playlist)
            }
        }
        return result
    }

    private fun showFolderSelectionMoreMenu() {
        val folders = getSelectedFolders()
        if (folders.isEmpty()) {
            Toast.makeText(
                this,
                getString(R.string.please_select_at_least_one_item),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val tracks = viewModel.getTracksInSelectedFolders()
        SelectMoreDialog(
            context = this,
            selectedTracks = tracks,
            onRename = { },
            onRingtoneCutter = { },
            onAddToPlaylist = { selectedTracks ->
                showSelectPlaylistDialogMultiple(selectedTracks)
            },
            onInformation = { },
            showRingtoneCutter = false,
            showInformation = false,
            renameUnitCount = folders.size,
            onRenameOverride = {
                showRenameFolderDialog(folders[0])
            },
            renameBlockedMessageRes = R.string.select_only_one_item_to_rename
        ).show()
    }

    private fun showPlaylistSelectionMoreMenu() {
        val playlists = getSelectedPlaylists()
        if (playlists.isEmpty()) {
            Toast.makeText(
                this,
                getString(R.string.please_select_at_least_one_item),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        viewModel.getTracksOfSelectedPlaylists { tracks ->
            SelectMoreDialog(
                context = this,
                selectedTracks = tracks,
                onRename = { },
                onRingtoneCutter = { },
                onAddToPlaylist = { selectedTracks ->
                    showSelectPlaylistDialogMultiple(selectedTracks)
                },
                onInformation = { },
                showRingtoneCutter = false,
                showInformation = false,
                renameUnitCount = playlists.size,
                onRenameOverride = {
                    showRenamePlaylistDialog(playlists[0])
                },
                renameBlockedMessageRes = R.string.select_only_one_item_to_rename
            ).show()
        }
    }

    private fun showDeleteFoldersConfirmDialog(folders: List<MusicFolder>) {
        val message: String
        if (folders.size == 1) {
            message = getString(R.string.delete_folder_desc, folders[0].folderName)
        } else {
            message = getString(R.string.delete_folders_desc, folders.size)
        }
        ConfirmActionDialog(
            context = this,
            title = getString(R.string.delete_folder),
            message = message,
            positiveText = getString(R.string.delete)
        ) {
            val tracks = viewModel.getTracksInSelectedFolders()
            performDelete(tracks)
            viewModel.exitSelectionMode()
        }.show()
    }

    private fun showDeletePlaylistsConfirmDialog(playlists: List<MusicPlaylist>) {
        val message: String
        if (playlists.size == 1) {
            message = getString(R.string.delete_playlist_desc, playlists[0].name)
        } else {
            message = getString(R.string.delete_playlists_desc, playlists.size)
        }
        ConfirmActionDialog(
            context = this,
            title = getString(R.string.delete_playlist),
            message = message,
            positiveText = getString(R.string.delete)
        ) {
            val playlistIds = mutableListOf<Long>()
            for (playlist in playlists) {
                playlistIds.add(playlist.id)
            }
            viewModel.deletePlaylists(playlistIds) {
                Toast.makeText(
                    this,
                    getString(R.string.delete_playlist_success),
                    Toast.LENGTH_SHORT
                ).show()
            }
            viewModel.exitSelectionMode()
        }.show()
    }

    private fun showRenameFolderDialog(folder: MusicFolder) {
        TextInputDialog(
            context = this,
            title = getString(R.string.rename),
            hint = getString(R.string.rename),
            initialText = folder.folderName,
            positiveText = getString(R.string.rename),
            validate = { enteredName ->
                val isNameTaken = viewModel.isFolderNameTaken(folder.folderPath, enteredName)
                if (isNameTaken) {
                    getString(R.string.text_input_failed1)
                } else {
                    null
                }
            }
        ) { newName ->
            performRenameFolder(folder, newName)
        }.show()
    }

    private fun performRenameFolder(folder: MusicFolder, newName: String) {
        viewModel.renameFolder(folder.folderPath, newName) { result ->
            when (result) {
                is RenameResult.Success -> {
                    Toast.makeText(
                        this,
                        getString(R.string.rename_folder_success),
                        Toast.LENGTH_SHORT
                    ).show()
                    viewModel.exitSelectionMode()
                }
                is RenameResult.NeedsUserConsent -> {
                    requestMediaConsent(result.intentSender) { granted ->
                        if (granted) {
                            performRenameFolder(folder, newName)
                        } else {
                            Toast.makeText(
                                this,
                                getString(R.string.rename_folder_failed),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                is RenameResult.Failure -> {
                    Toast.makeText(
                        this,
                        getString(R.string.rename_folder_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showRenamePlaylistDialog(playlist: MusicPlaylist) {
        TextInputDialog(
            context = this,
            title = getString(R.string.rename),
            hint = getString(R.string.rename),
            initialText = playlist.name,
            positiveText = getString(R.string.rename),
            validate = { enteredName ->
                val isNameTaken = viewModel.isPlaylistNameTaken(enteredName, playlist.id)
                if (isNameTaken) {
                    getString(R.string.text_input_failed1)
                } else {
                    null
                }
            }
        ) { newName ->
            viewModel.renamePlaylist(playlist.id, newName) {
                Toast.makeText(
                    this,
                    getString(R.string.rename_playlist_success),
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.exitSelectionMode()
            }
        }.show()
    }

    override fun onBack() {
        if (viewModel.isSelectionMode.value) {
            viewModel.exitSelectionMode()
        } else if (isSearchMode) {
            closeSearch()
        } else {
            super.onBack()
        }
    }

    private fun bindSearchInput() {
        binding.edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query: String
                if (s == null) {
                    query = ""
                } else {
                    query = s.toString()
                }
                viewModel.updateSearchQuery(query)
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        binding.edtSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                Utils.hideKeyboard(this)
                true
            } else {
                false
            }
        }
    }

    private fun openSearch() {
        if (isSearchMode) {
            return
        }
        isSearchMode = true

        // Thoát chọn nhiều nếu đang bật, rồi hiện search thay TabLayout
        if (viewModel.isSelectionMode.value) {
            viewModel.exitSelectionMode()
        }

        binding.tabLayout.gone()
        binding.layoutSearch.visible()
        binding.viewPager.gone()
        binding.fragmentSearchContainer.visible()

        val existingFragment = supportFragmentManager.findFragmentByTag(SEARCH_FRAGMENT_TAG)
        if (existingFragment == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentSearchContainer, SearchFragment(), SEARCH_FRAGMENT_TAG)
                .commit()
        }

        binding.edtSearch.requestFocus()
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(binding.edtSearch, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun closeSearch() {
        if (isSearchMode == false) {
            return
        }
        isSearchMode = false

        viewModel.updateSearchQuery("")
        binding.edtSearch.setText("")
        Utils.hideKeyboard(this)

        binding.layoutSearch.gone()
        binding.tabLayout.visible()
        binding.fragmentSearchContainer.gone()
        binding.viewPager.visible()

        val existingFragment = supportFragmentManager.findFragmentByTag(SEARCH_FRAGMENT_TAG)
        if (existingFragment != null) {
            supportFragmentManager.beginTransaction()
                .remove(existingFragment)
                .commit()
        }
    }

    companion object {
        private const val SEARCH_FRAGMENT_TAG = "SearchFragment"
    }
}
