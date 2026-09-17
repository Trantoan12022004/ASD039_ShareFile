package com.example.basekotlin.ui.group_share.dialog

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseDialog
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.DialogEnterIpAddressBinding
import com.example.basekotlin.util.transfer.NetworkUtils

class EnterIpAddressDialog(
    context: Context,
    private val onConnectClick: (ip: String, port: Int) -> Unit
) : BaseDialog<DialogEnterIpAddressBinding>(context, true) {

    override fun setBinding(): DialogEnterIpAddressBinding {
        return DialogEnterIpAddressBinding.inflate(layoutInflater)
    }

    override fun initView() {
        // Tự động gợi ý Gateway IP nếu máy đã kết nối vào Hotspot của Host
        val suggestedIp = NetworkUtils.getWifiGatewayIp(context)
        if (!suggestedIp.isNullOrEmpty() && suggestedIp != "0.0.0.0") {
            binding.etIpAddress.setText(suggestedIp)
            binding.etIpAddress.setSelection(suggestedIp.length)
        }
    }

    override fun bindView() {
        binding.btnCancel.tap {
            dismiss()
        }

        binding.btnConnect.tap {
            val input = binding.etIpAddress.text.toString().trim()
            val (ip, port) = parseIpAndPort(input)

            if (!NetworkUtils.isWifiConnected(context)) {
                Toast.makeText(context, context.getString(R.string.prompt_turn_on_wifi_to_connect), Toast.LENGTH_LONG).show()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try { context.startActivity(Intent(Settings.Panel.ACTION_WIFI)) } catch (_: Exception) {}
                }
                return@tap
            }

            if (isValidIp(ip)) {
                dismiss()
                onConnectClick(ip, port)
            } else {
                Toast.makeText(context, context.getString(R.string.error_invalid_ip), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun parseIpAndPort(input: String): Pair<String, Int> {
        if (input.contains(":")) {
            val parts = input.split(":")
            val ip = parts[0].trim()
            val port = parts.getOrNull(1)?.toIntOrNull() ?: 8888
            return Pair(ip, port)
        }
        return Pair(input, 8888)
    }

    private fun isValidIp(ip: String): Boolean {
        if (ip.isEmpty()) return false
        val parts = ip.split(".")
        if (parts.size != 4) return false
        return parts.all { part ->
            val num = part.toIntOrNull() ?: return false
            num in 0..255
        }
    }
}