package com.example.basekotlin.dialog.common

import android.content.Context
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Toast
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseDialog
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.PopupSelectionMoreBinding
import com.example.basekotlin.model.MusicTrack

class SelectMoreDialog(
    context: Context,
    private val selectedTracks: List<MusicTrack>,
    private val onRename: (MusicTrack) -> Unit,
    private val onRingtoneCutter: (MusicTrack) -> Unit,
    private val onAddToPlaylist: (List<MusicTrack>) -> Unit,
    private val onInformation: (MusicTrack) -> Unit,
    private val showRingtoneCutter: Boolean = true,
    private val showInformation: Boolean = true,
    private val renameUnitCount: Int = selectedTracks.size,
    private val onRenameOverride: (() -> Unit)? = null,
    private val renameBlockedMessageRes: Int = R.string.select_only_one_song_to_rename,
) : BaseDialog<PopupSelectionMoreBinding>(context, true) {
    override fun setBinding(): PopupSelectionMoreBinding {
        return PopupSelectionMoreBinding.inflate(layoutInflater)
    }

    override fun initView() {
        val win = window
        if (win != null) {
            win.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            win.setGravity(Gravity.BOTTOM)
            win.setBackgroundDrawableResource(android.R.color.transparent)
        }

        if (showRingtoneCutter == false) {
            binding.tvRingtoneCutter.gone()
            binding.dividerRingtone.gone()
        }
        if (showInformation == false) {
            binding.tvInformation.gone()
            binding.dividerInformation.gone()
        }
    }

    override fun bindView() {
        binding.tvRename.tap {
            if (renameUnitCount == 1) {
                val overrideCallback = onRenameOverride
                if (overrideCallback != null) {
                    overrideCallback()
                } else {
                    if (selectedTracks.isNotEmpty()) {
                        onRename(selectedTracks[0])
                    }
                }
                dismiss()
            } else {
                Toast.makeText(
                    context,
                    context.getString(renameBlockedMessageRes),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.tvRingtoneCutter.tap {
            if (selectedTracks.size == 1) {
                onRingtoneCutter(selectedTracks[0])
                dismiss()
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.select_only_one_song_for_ringtone),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.tvAddToPlaylist.tap {
            onAddToPlaylist(selectedTracks)
            dismiss()
        }

        binding.tvInformation.tap {
            if (selectedTracks.size == 1) {
                onInformation(selectedTracks[0])
                dismiss()
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.select_only_one_song_for_info),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
