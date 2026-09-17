package com.example.basekotlin.ui.group_share.dialog

import android.content.Context
import com.example.basekotlin.base.BaseDialog
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.DialogGroupMemberBinding
import com.example.basekotlin.ui.group_share.adapter.GroupMemberAdapter
import com.example.basekotlin.ui.group_share.model.GroupMemberModel

class GroupMemberDialog(
    context: Context,
    private val members: List<GroupMemberModel>
) : BaseDialog<DialogGroupMemberBinding>(context, true) {

    override fun setBinding(): DialogGroupMemberBinding {
        return DialogGroupMemberBinding.inflate(layoutInflater)
    }

    override fun initView() {
        val adapter = GroupMemberAdapter(members)
        binding.rvMembers.adapter = adapter
    }

    override fun bindView() {
        binding.btnClose.tap {
            dismiss()
        }
    }
}