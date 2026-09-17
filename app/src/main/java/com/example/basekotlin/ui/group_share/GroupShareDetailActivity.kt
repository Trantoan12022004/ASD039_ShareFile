package com.example.basekotlin.ui.group_share

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.PopupMenu
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseActivity
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.ActivityGroupShareDetailBinding
import com.example.basekotlin.service.group.GroupShareService
import com.example.basekotlin.ui.group_share.dialog.GroupMemberDialog
import com.example.basekotlin.ui.group_share.dialog.GroupQrGuidelineDialog
import com.example.basekotlin.util.transfer.NetworkUtils
import com.example.basekotlin.util.transfer.WifiHelper
import com.example.basekotlin.ui.transfer.model.ProgressItem
import com.example.basekotlin.ui.transfer.model.TransferFile
import com.example.basekotlin.ui.transfer.progress.ProgressAdapter
import com.example.basekotlin.ui.transfer.send.SendFileActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

class GroupShareDetailActivity : BaseActivity<ActivityGroupShareDetailBinding>(ActivityGroupShareDetailBinding::inflate) {

    companion object {
        const val EXTRA_IS_HOST = "EXTRA_IS_HOST"
        const val EXTRA_HOST_IP = "EXTRA_HOST_IP"
        const val EXTRA_HOST_PORT = "EXTRA_HOST_PORT"
    }

    private val viewModel: GroupShareViewModel by viewModels()
    private lateinit var progressAdapter: ProgressAdapter
    private var hasShownInitialQrDialog = false

    private val pickFilesLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedFiles = result.data?.getParcelableArrayListExtra<TransferFile>(SendFileActivity.EXTRA_SELECTED_FILES)
            if (!selectedFiles.isNullOrEmpty()) {
                viewModel.sendFiles(selectedFiles)
            }
        }
    }

    override fun initView() {
        super.initView()
        setupRecyclerView()

        val isHost = intent.getBooleanExtra(EXTRA_IS_HOST, false)
        val hostIp = intent.getStringExtra(EXTRA_HOST_IP)
        val hostPort = intent.getIntExtra(EXTRA_HOST_PORT, 8888)

        viewModel.initSession(this, isHost, hostIp, hostPort)
    }

    private fun setupRecyclerView() {
        progressAdapter = ProgressAdapter(
            onCancelClick = { item -> showCancelConfirmDialog(item) },
            onRefreshClick = { item -> viewModel.retryFile(item.id) },
            onMoreClick = { item, anchorView -> showFilePopupMenu(item, anchorView) }
        )
        binding.rvChatProgress.adapter = progressAdapter
    }

    override fun bindView() {
        // Toolbar: Nút Back -> Xác nhận rời nhóm
        binding.viewTop.btnBack.tap {
            onBack()
        }

        // Toolbar: Click vào tvDeviceName -> Mở lại Dialog 1 (QR Guideline)
        binding.viewTop.tvDeviceName.tap {
            val qr = viewModel.qrBitmap.value
            val ip = viewModel.hostIp.value
            if (qr != null && ip.isNotEmpty()) {
                showQrGuidelineDialog(qr, ip)
            }
        }

        // Toolbar: Click vào tv_count_user -> Mở Dialog 4 (Group Member)
        binding.viewTop.tvCountUser.tap {
            showGroupMemberDialog()
        }

        // Bottom: Nút thêm file
        binding.btnAdd.tap {
            val intent = Intent(this, SendFileActivity::class.java).apply {
                putExtra(SendFileActivity.EXTRA_IS_PICK_MODE, true)
            }
            pickFilesLauncher.launch(intent)
        }

        // Bottom: Nút gửi text message
        binding.btnSend.tap {
            sendTextMessage()
        }

        // Phím Enter gửi text
        binding.etSend.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendTextMessage()
                true
            } else false
        }

        observeData()
    }

    private fun observeData() {
        val isHost = intent.getBooleanExtra(EXTRA_IS_HOST, false)

        // Lần đầu vào màn nếu là Host -> tự động hiện Dialog QR (Ảnh 1)
        if (isHost) {
            lifecycleScope.launch {
                viewModel.qrBitmap.collectLatest { bitmap ->
                    val ip = viewModel.hostIp.value
                    if (bitmap != null && ip.isNotEmpty() && !hasShownInitialQrDialog) {
                        hasShownInitialQrDialog = true
                        showQrGuidelineDialog(bitmap, ip)
                    }
                }
            }
        }

        // Observe số lượng thành viên
        lifecycleScope.launch {
            viewModel.memberCount.collectLatest { count ->
                binding.viewTop.tvCountUser.text = count.toString()
            }
        }

        // Observe tên nhóm / tên Host
        lifecycleScope.launch {
            viewModel.groupName.collectLatest { name ->
                if (name.isNotEmpty()) {
                    binding.viewTop.tvDeviceName.text = name
                }
            }
        }

        // Observe danh sách tin nhắn & tiến trình file
        var lastCount = 0
        lifecycleScope.launch {
            viewModel.items.collectLatest { list ->
                progressAdapter.submitList(list)
                if (list.size > lastCount) {
                    binding.rvChatProgress.scrollToPosition(list.size - 1)
                    lastCount = list.size
                }
            }
        }

        // Observe thống kê dung lượng và thời gian
        lifecycleScope.launch {
            viewModel.stats.collectLatest { stats ->
                val mb = stats.totalTransferredBytes / (1024f * 1024f)
                binding.viewTop.tvTotalSize.text = String.format(Locale.US, "%.1f", mb)
                if (stats.elapsedSeconds >= 3600) {
                    val hours = stats.elapsedSeconds / 3600
                    binding.viewTop.tvTotalTime.text = String.format(Locale.US, "%02d", hours)
                } else if (stats.elapsedSeconds >= 60) {
                    val minutes = stats.elapsedSeconds / 60
                    binding.viewTop.tvTotalTime.text = String.format(Locale.US, "%02d", minutes)
                } else {
                    binding.viewTop.tvTotalTime.text = String.format(Locale.US, "%02d", stats.elapsedSeconds)
                }
            }
        }

        // Observe trạng thái online
        lifecycleScope.launch {
            viewModel.isOnline.collectLatest { online ->
                if (online) {
                    binding.tvConnectionStatus.visible()
                    binding.tvConnectionStatus.text = getString(R.string.status_online)
                    binding.tvConnectionStatus.postDelayed({ binding.tvConnectionStatus.gone() }, 3000)
                } else {
                    binding.tvConnectionStatus.visible()
                    binding.tvConnectionStatus.text = getString(R.string.status_offline)
                }
            }
        }
    }

    private fun showQrGuidelineDialog(bitmap: Bitmap, ip: String) {
        val info = viewModel.connectionInfo.value
        val isHotspot = info?.password?.isNotEmpty() == true || !NetworkUtils.isWifiConnected(this)
        GroupQrGuidelineDialog(this, bitmap, ip, isHotspotMode = isHotspot).show()
    }

    private fun showGroupMemberDialog() {
        val list = viewModel.members.value
        GroupMemberDialog(this, list).show()
    }

    private fun sendTextMessage() {
        val text = binding.etSend.text.toString().trim()
        if (text.isNotEmpty()) {
            viewModel.sendTextMessage(text)
            binding.etSend.text?.clear()
        }
    }

    private fun showFilePopupMenu(item: ProgressItem.FileTransfer, anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, getString(R.string.action_open_file))
        popup.menu.add(0, 2, 1, getString(R.string.action_share_file))

        popup.setOnMenuItemClickListener { menuItem ->
            val path = item.savedPath ?: return@setOnMenuItemClickListener false
            val file = File(path)
            val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)

            when (menuItem.itemId) {
                1 -> {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, item.file.mimeType)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(intent, getString(R.string.action_open_file)))
                }
                2 -> {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = item.file.mimeType
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(intent, getString(R.string.action_share_file)))
                }
            }
            true
        }
        popup.show()
    }

    private fun showCancelConfirmDialog(item: ProgressItem.FileTransfer) {
        val dialog = Dialog(this, R.style.BaseDialog)
        dialog.setContentView(R.layout.dialog_confirm_action)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.findViewById<TextView>(R.id.tvTitleConfirm)?.text = getString(R.string.cancel)
        dialog.findViewById<TextView>(R.id.tvMessageConfirm)?.text =
            getString(R.string.cancel_transfer_confirm_message)
        dialog.findViewById<TextView>(R.id.tvCancelConfirm)?.text = getString(R.string.no)
        dialog.findViewById<TextView>(R.id.tvPositiveConfirm)?.text = getString(R.string.cancel)

        dialog.findViewById<View>(R.id.btnCancelConfirm)?.setOnClickListener { dialog.dismiss() }
        dialog.findViewById<View>(R.id.btnPositiveConfirm)?.setOnClickListener {
            viewModel.cancelFile(item.id)
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showExitConfirmDialog() {
        val dialog = Dialog(this, R.style.BaseDialog)
        dialog.setContentView(R.layout.dialog_confirm_action)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.findViewById<TextView>(R.id.tvTitleConfirm)?.text = getString(R.string.dialog_exit_group_title)
        dialog.findViewById<TextView>(R.id.tvMessageConfirm)?.text = getString(R.string.dialog_exit_group_msg)
        dialog.findViewById<TextView>(R.id.tvCancelConfirm)?.text = getString(R.string.btn_stay)
        dialog.findViewById<TextView>(R.id.tvPositiveConfirm)?.text = getString(R.string.btn_exit)

        dialog.findViewById<View>(R.id.btnCancelConfirm)?.setOnClickListener { dialog.dismiss() }
        dialog.findViewById<View>(R.id.btnPositiveConfirm)?.setOnClickListener {
            dialog.dismiss()
            GroupShareService.disconnect(this)
            WifiHelper.disconnectActiveWifi(this)
            finishThisActivity()
        }
        dialog.show()
    }

    override fun onBack() {
        showExitConfirmDialog()
    }

    override fun onDestroy() {
        viewModel.unbindService(this)
        WifiHelper.disconnectActiveWifi(this)
        super.onDestroy()
    }
}
