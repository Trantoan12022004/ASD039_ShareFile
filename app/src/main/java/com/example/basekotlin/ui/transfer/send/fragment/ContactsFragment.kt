package com.example.basekotlin.ui.transfer.send.fragment

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.basekotlin.base.BaseFragment
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.FragmentSendBinding
import com.example.basekotlin.ui.transfer.send.SendFilesViewModel
import com.example.basekotlin.ui.transfer.send.adapter.FileGroupAdapter
import com.example.basekotlin.util.PermissionManager
import kotlinx.coroutines.launch
import kotlin.getValue

class ContactsFragment : BaseFragment<FragmentSendBinding>(){

    private val viewModel: SendFilesViewModel by activityViewModels()
    private val adapter = FileGroupAdapter()
    // Launcher xin quyền danh bạ runtime
    private val requestContactsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                viewModel.loadContacts()
            } else {
                // Nếu bị từ chối vĩnh viễn (Don't ask again), mở Cài đặt ứng dụng
                if (!shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {
                    openAppSettings()
                }
            }
        }
    override fun setBinding(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        saveInstanceState: Bundle?
    ): FragmentSendBinding {
        return FragmentSendBinding.inflate(inflater!!, container, false)
    }

    override fun getData() {
        checkAndLoadContacts()
    }
    override fun onResume() {
        super.onResume()
        // Khi người dùng từ Settings quay lại, tự động nạp danh bạ nếu quyền đã được cấp
        if (hasPermission()) {
            viewModel.loadContacts()
        }
    }

    override fun initView() {
        binding.rvSends.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSends.adapter = adapter
        binding.layoutHeader.gone()
        // Bấm item: Chỉ gọi 1 lần duy nhất để ViewModel xử lý
        adapter.onItemClick  = { item ->
            viewModel.toggleFileSelection(item)
        }

    }

    override fun bindView() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                // 1. Lắng nghe danh sách Photo theo nhóm
                launch {
                    viewModel.displayContactGroups.collect { photos ->
                        adapter.addListData(photos.toMutableList())
                        updateEmptyState(viewModel.isLoadingContacts.value)
                    }
                }
                // 2. Lắng nghe trạng thái loading
                launch {
                    viewModel.isLoadingContacts .collect { isLoading ->
                        handleLoading(isLoading)
                    }
                }
                // 3. Đồng bộ Checkbox: BaseApdaterSelected tự diff O(1) chỉ cập nhật riêng checkbox của video vừa click
                launch {
                    viewModel.selectedFiles.collect { files ->
                        val selectedIds = files.map { it.id }.toSet()
                        adapter.selectedKeys = selectedIds
                    }
                }
            }
        }

    }
    private fun checkAndLoadContacts() {
        if (hasPermission()) {
            viewModel.loadContacts()
        } else {
            requestContactsLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    private fun hasPermission(): Boolean {
        return PermissionManager.checkContactsPermission(requireContext())
    }
    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", requireContext().packageName, null)
        }
        startActivity(intent)
    }

    private fun handleLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.progressLoading.visible()
            binding.rvSends.gone()
            binding.allEmpty.gone()
        } else {
            binding.progressLoading.gone()
            updateEmptyState(false)
        }
    }
    private fun updateEmptyState(isLoading: Boolean) {
        if (isLoading) {
            binding.allEmpty.gone()
            binding.rvSends.gone()
            return
        }
        val isEmpty = adapter.listData.isEmpty()
        if (isEmpty) {
            binding.allEmpty.visible()
            binding.rvSends.gone()
        } else {
            binding.allEmpty.gone()
            binding.rvSends.visible()
        }
    }
}