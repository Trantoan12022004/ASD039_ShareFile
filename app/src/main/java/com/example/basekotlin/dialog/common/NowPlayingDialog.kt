package com.example.basekotlin.dialog.common

import android.R
import android.content.Context
import android.view.Gravity
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.basekotlin.base.BaseDialog
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.DialogNowPlayingBinding
import com.example.basekotlin.model.MusicPlaylist
import com.example.basekotlin.model.MusicTrack
import com.example.basekotlin.ui.files.music.adapter.MusicNowPlayingAdapter
import com.example.basekotlin.ui.files.music.adapter.MusicPlaylistAdapter1

class NowPlayingDialog(
    context: Context,
    private val tracks: List<MusicTrack>,
    private val onTrackClick: (MusicTrack) -> Unit
//    private val onCreateNewPlaylist: () -> Unit,
//    private val onPlaylistSelected: (MusicPlaylist) -> Unit
) : BaseDialog<DialogNowPlayingBinding>(context, true) {

    private val pickAdapter = MusicNowPlayingAdapter()
    override fun setBinding(): DialogNowPlayingBinding {
        return DialogNowPlayingBinding.inflate(layoutInflater)
    }

    override fun initView() {
        window?.let { win ->
            win.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            win.setGravity(Gravity.BOTTOM)
            win.setBackgroundDrawableResource(R.color.transparent)
        }
        binding.rvNowPlaying.layoutManager = LinearLayoutManager(context)
        binding.rvNowPlaying.adapter = pickAdapter
        pickAdapter.addListData(tracks.toMutableList())
        binding.tvCount.text = tracks.size.toString()

    }
    override fun bindView() {
        binding.ivClose.tap {
            dismiss()
        }

        pickAdapter.onTrackClick = { track ->
            dismiss()
            onTrackClick(track)
        }

    }
}