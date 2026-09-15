package com.example.basekotlin.ui.transfer.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Thông tin thiết bị (hiển thị trong UI device discovery, history...)
 */
@Parcelize
data class DeviceInfo(
    val name: String,
    val ipAddress: String,
    val port: Int = 0
) : Parcelable
