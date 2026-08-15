// app/src/main/java/com/example/basekotlin/dialog/common/AddToPlaylistDialog.kt
package com.example.basekotlin.dialog.common

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.basekotlin.base.BaseDialog
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.DialogSelectPlaylistBinding
import com.example.basekotlin.model.MusicPlaylist
import com.example.basekotlin.ui.files.music.adapter.MusicPlaylistAdapter

class SelectPlaylistDialog(
    context: Context,
    private val playlists: List<MusicPlaylist>,
    private val onCreateNewPlaylist: () -> Unit,
    private val onPlaylistSelected: (MusicPlaylist) -> Unit
) : BaseDialog<DialogSelectPlaylistBinding>(context, true) {

    private val pickAdapter = MusicPlaylistAdapter()
    override fun setBinding(): DialogSelectPlaylistBinding {
        return DialogSelectPlaylistBinding.inflate(layoutInflater)
    }

    override fun initView() {
        binding.rvPlaylists.layoutManager = LinearLayoutManager(context)
        binding.rvPlaylists.adapter = pickAdapter
        pickAdapter.addListData(playlists.toMutableList())
    }
    override fun bindView() {
        binding.btnCreatePlaylist.tap {
            dismiss()
            onCreateNewPlaylist()
        }

        pickAdapter.onPlaylistClick = { playlist ->
            dismiss()
            onPlaylistSelected(playlist)
        }
    }
}