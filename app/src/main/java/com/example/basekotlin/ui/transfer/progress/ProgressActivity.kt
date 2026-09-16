package com.example.basekotlin.ui.transfer.progress

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import com.example.basekotlin.databinding.ActivityProgressBinding
import com.example.basekotlin.service.transfer.TransferService
import com.example.basekotlin.ui.transfer.model.ProgressItem
import com.example.basekotlin.ui.transfer.model.TransferFile
import com.example.basekotlin.ui.transfer.send.SendFileActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

class ProgressActivity : BaseActivity<ActivityProgressBinding>(ActivityProgressBinding::inflate) {

    private val viewModel: ProgressViewModel by viewModels()
    private lateinit var progressAdapter: ProgressAdapter

    // Launcher mở SendFileActivity để chọn thêm file (btn_add)
    private val pickMoreFilesLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedFiles = result.data?.getParcelableArrayListExtra<TransferFile>(SendFileActivity.EXTRA_SELECTED_FILES)
            if (!selectedFiles.isNullOrEmpty()) {
                viewModel.addFiles(selectedFiles)
            }
        }
    }

    override fun initView() {
        super.initView()
        setupRecyclerView()
        viewModel.bindService(this)
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
        // Nút thêm file (+) -> Mở SendFileActivity ở Pick Mode
        binding.btnAdd.tap {
            val intent = Intent(this, SendFileActivity::class.java).apply {
                putExtra(SendFileActivity.EXTRA_IS_PICK_MODE, true)
            }
            pickMoreFilesLauncher.launch(intent)
        }

        // Nút gửi text message
        binding.btnSend.tap {
            sendTextMessage()
        }

        // Phím Enter trên bàn phím để gửi
        binding.etSend.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendTextMessage()
                true
            } else false
        }

        // Nút Back trên toolbar
        binding.viewTop.btnBack.tap {
            onBack()
        }

        // Observe danh sách item chat & file
        var lastItemCount = 0
        lifecycleScope.launch {
            viewModel.items.collectLatest { list ->
                progressAdapter.submitList(list)
                if (list.size > lastItemCount) {
                    binding.rvChatProgress.scrollToPosition(list.size - 1)
                    lastItemCount = list.size
                }
            }
        }

        // Observe trạng thái Online / Offline
        lifecycleScope.launch {
            viewModel.isOnline.collectLatest { online ->
                val device = viewModel.deviceName.value
                if (online) {
                    binding.tvConnectionStatus.visible()
                    binding.tvConnectionStatus.text = getString(R.string.device_status_online_fmt, device)
                    binding.tvConnectionStatus.postDelayed({ binding.tvConnectionStatus.gone() }, 3000)
                } else {
                    binding.tvConnectionStatus.visible()
                    binding.tvConnectionStatus.text = getString(R.string.device_status_offline_fmt, device)
                }
            }
        }

        // Observe tên thiết bị
        lifecycleScope.launch {
            viewModel.deviceName.collectLatest { name ->
                if (name.isNotEmpty()) {
                    binding.viewTop.tvDeviceName.text = name
                }
            }
        }

        // Observe thống kê tổng (Đếm xuôi từ 0)
        lifecycleScope.launch {
            viewModel.stats.collectLatest { s ->
                val mb = s.totalTransferredBytes / (1024f * 1024f)
                binding.viewTop.tvTotalSize.text = String.format(java.util.Locale.US, "%.1f", mb)
                if (s.elapsedSeconds >= 3600) {
                    val hours = s.elapsedSeconds / 3600
                    binding.viewTop.tvTotalTime.text = String.format(java.util.Locale.US, "%02d", hours)
                } else if (s.elapsedSeconds >= 60) {
                    val minutes = s.elapsedSeconds / 60
                    binding.viewTop.tvTotalTime.text = String.format(java.util.Locale.US, "%02d", minutes)
                } else {
                    binding.viewTop.tvTotalTime.text = String.format(java.util.Locale.US, "%02d", s.elapsedSeconds)
                }
            }
        }
    }

    private fun sendTextMessage() {
        val text = binding.etSend.text.toString().trim()
        if (text.isNotEmpty()) {
            viewModel.sendText(text)
            binding.etSend.text?.clear()
        }
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

        dialog.findViewById<TextView>(R.id.tvTitleConfirm)?.text = getString(R.string.dialog_exit_transfer_title)
        dialog.findViewById<TextView>(R.id.tvMessageConfirm)?.text = getString(R.string.dialog_exit_transfer_msg)
        dialog.findViewById<TextView>(R.id.tvCancelConfirm)?.text = getString(R.string.btn_stay)
        dialog.findViewById<TextView>(R.id.tvPositiveConfirm)?.text = getString(R.string.btn_exit)

        dialog.findViewById<View>(R.id.btnCancelConfirm)?.setOnClickListener { dialog.dismiss() }
        dialog.findViewById<View>(R.id.btnPositiveConfirm)?.setOnClickListener {
            dialog.dismiss()
            TransferService.disconnect(this)
            com.example.basekotlin.util.transfer.WifiHelper.disconnectActiveWifi(this)
            finishThisActivity()
        }
        dialog.show()
    }

    override fun onBack() {
        showExitConfirmDialog()
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
                1 -> { // Open
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, item.file.mimeType)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(intent, getString(R.string.action_open_file)))
                }
                2 -> { // Share
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

    override fun onDestroy() {
        viewModel.unbindService(this)
        super.onDestroy()
    }
}
