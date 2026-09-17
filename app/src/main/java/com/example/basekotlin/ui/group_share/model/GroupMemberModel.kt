package com.example.basekotlin.ui.group_share.model

data class GroupMemberModel(
    val deviceName: String,
    val ipAddress: String,
    val isHost: Boolean,
    val subInfo: String = ipAddress
)
