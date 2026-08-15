// app/src/main/java/com/example/basekotlin/dialog/common/AddToPlaylistDialog.kt
package com.example.basekotlin.dialog.common

import android.content.Context
import android.view.Gravity
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.basekotlin.base.BaseDialog
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.DialogMyPlaylistBinding
import com.example.basekotlin.databinding.DialogSelectPlaylistBinding
import com.example.basekotlin.model.MusicPlaylist
import com.example.basekotlin.ui.files.music.adapter.MusicPlaylistAdapter
import com.example.basekotlin.ui.files.music.adapter.MusicPlaylistAdapter1

class MyPlaylistDialog(
    context: Context,
    private val playlists: List<MusicPlaylist>,
    private val onCreateNewPlaylist: () -> Unit,
    private val onPlaylistSelected: (MusicPlaylist) -> Unit
) : BaseDialog<DialogMyPlaylistBinding>(context, true) {

    private val pickAdapter = MusicPlaylistAdapter1()
    override fun setBinding(): DialogMyPlaylistBinding {
        return DialogMyPlaylistBinding.inflate(layoutInflater)
    }

    override fun initView() {
        window?.let { win ->
            win.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            win.setGravity(Gravity.BOTTOM)
            win.setBackgroundDrawableResource(android.R.color.transparent)
        }
        binding.rvPlaylists.layoutManager = LinearLayoutManager(context)
        binding.rvPlaylists.adapter = pickAdapter
        pickAdapter.addListData(playlists.toMutableList())
    }
    override fun bindView() {
        binding.btnAddPlaylist.tap {
            dismiss()
            onCreateNewPlaylist()
        }

        binding.ivClose.tap {
            dismiss()
        }

        pickAdapter.onPlaylistClick = { playlist ->
            dismiss()
            onPlaylistSelected(playlist)
        }
    }
}