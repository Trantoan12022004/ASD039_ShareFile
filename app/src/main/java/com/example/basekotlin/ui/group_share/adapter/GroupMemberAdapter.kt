package com.example.basekotlin.ui.group_share.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.basekotlin.R
import com.example.basekotlin.databinding.ItemGroupMemberBinding
import com.example.basekotlin.ui.group_share.model.GroupMemberModel

class GroupMemberAdapter(
    private val members: List<GroupMemberModel>
) : RecyclerView.Adapter<GroupMemberAdapter.MemberViewHolder>() {

    inner class MemberViewHolder(val binding: ItemGroupMemberBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val binding = ItemGroupMemberBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MemberViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val member = members[position]
        with(holder.binding) {
            tvDeviceName.text = member.deviceName
            tvSubInfo.text = member.subInfo
            if (member.isHost) {
                tvRole.text = root.context.getString(R.string.role_host)
                tvRole.setTextColor(ContextCompat.getColor(root.context, R.color.primary_40))
            } else {
                tvRole.text = root.context.getString(R.string.role_member)
                tvRole.setTextColor(ContextCompat.getColor(root.context, R.color.black))
            }
        }
    }

    override fun getItemCount(): Int = members.size
}
