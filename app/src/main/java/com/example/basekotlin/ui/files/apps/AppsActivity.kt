package com.example.basekotlin.ui.files.apps

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseActivity
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.ActivityAppsBinding
import com.example.basekotlin.dialog.common.ConfirmActionDialog
import com.example.basekotlin.model.AppInfo
import com.example.basekotlin.util.Utils
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import java.io.File
import kotlin.getValue

class AppsActivity : BaseActivity<ActivityAppsBinding>(ActivityAppsBinding::inflate) {

    private lateinit var pagerAdapter: AppsPagerAdapter

    private val viewModel: AppsViewModel by viewModels()
    private var isSearchMode = false


    override fun initView() {
        binding.layoutToolbar.tvTitle.text = getString(R.string.apps)
        pagerAdapter = AppsPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.offscreenPageLimit = 1

        val tabTitles = arrayOf(
            getString(R.string.receive),
            getString(R.string.apk),
            getString(R.string.installed),
        )

        TabLayoutMediator(
            binding.tabLayout,
            binding.viewPager,
        ) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val isInstalledTab = position == 2
                if (isInstalledTab) {
                    binding.layoutToolbar.btnSelect.gone()
                } else {
                    binding.layoutToolbar.btnSelect.visible()
                }
            }
        })
    }

    override fun bindView() {
        binding.layoutToolbar.btnBack.tap {
            onBack()
        }
        binding.layoutToolbar.btnSelect.tap {
           viewModel.enterApkSelectionMode()
        }
        binding.layoutToolbar.btnSearch.tap {
            openSearch()
        }

        binding.edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString() ?: ""
                viewModel.updateSearchQuery(query)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        setupSelectionActions()
        observeSelectMode()
    }

    private fun setupSelectionActions() {
        val actions = binding.layoutSelectionApps

        actions.btnDeselectAll.tap {
            if (viewModel.selectedApkFilePaths.value.isNotEmpty()) {
                viewModel.clearApkSelection()
            } else {
                val currentApks = getCurrentTabApks()
                viewModel.selectAllApks(currentApks.map { it.apkFilePath })
            }
        }
        actions.btnDelete.tap {
            val selectedApks = getSelectedApks()
            if (selectedApks.isEmpty()) {
                Toast.makeText(
                    this,
                    getString(R.string.please_select_at_least_one_item),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                showDeleteSelectedApksDialog(selectedApks)
            }
        }

        actions.btnSend.tap {
            val selectedApks = getSelectedApks()
            if (selectedApks.isEmpty()) {
                Toast.makeText(
                    this,
                    getString(R.string.please_select_at_least_one_item),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                shareSelectedApks(selectedApks)
            }
        }
    }

    private fun getCurrentTabApks(): List<AppInfo> {
        return when (binding.viewPager.currentItem) {
            0 -> viewModel.receivedAppsSearch.value
            1 -> viewModel.notInstalledApkFiles.value + viewModel.deletableApkFiles.value
            else -> emptyList()
        }
    }

    private fun observeSelectMode() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isSelectionMode.collect { isSelecting ->
                        if (isSelecting) {
                            binding.layoutToolbar.tvTitle.gone()
                            binding.layoutToolbar.tvTitle1.visible()
                            binding.layoutSelectionApps.root.visible()
                        } else {
                            binding.layoutToolbar.tvTitle.visible()
                            binding.layoutToolbar.tvTitle1.gone()
                            binding.layoutSelectionApps.root.gone()
                        }
                    }
                }
                launch {
                    viewModel.selectedApkFilePaths.collect { selectedPaths ->
                        updateSelectionCount()
                        if (selectedPaths.isNotEmpty()) {
                            binding.layoutSelectionApps.tvDeselectAll.text = getString(R.string.deselect_all)
                        } else {
                            binding.layoutSelectionApps.tvDeselectAll.text = getString(R.string.select_all)
                        }
                    }
                }
            }
        }
    }

    private fun showDeleteSelectedApksDialog(apkList: List<AppInfo>) {
        val message: String
        if (apkList.size == 1) {
            message = getString(R.string.delete_apk_desc, apkList[0].appName)
        } else {
            message = getString(R.string.delete_apk_desc, apkList.size.toString())
        }

        ConfirmActionDialog(
            context = this,
            title = getString(R.string.delete_apk),
            message = message,
            positiveText = getString(R.string.delete)
        ) {
            performDeleteSelectedApks(apkList)
        }.show()
    }

    private fun performDeleteSelectedApks(apkList: List<AppInfo>) {
        var successCount = 0
        var failCount = 0

        for (apk in apkList) {
            val apkFile = File(apk.apkFilePath)
            val deleted = apkFile.delete()
            if (deleted) {
                successCount = successCount + 1
            } else {
                failCount = failCount + 1
            }
        }

        if (failCount == 0) {
            Toast.makeText(this, getString(R.string.delete_apk_success), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, getString(R.string.delete_apk_failed), Toast.LENGTH_SHORT).show()
        }

        viewModel.exitSelectionMode()
        viewModel.refreshApkFiles()
    }

    private fun shareSelectedApks(apkList: List<AppInfo>) {
        val uris = ArrayList<Uri>()
        for (apk in apkList) {
            val apkFile = File(apk.apkFilePath)
            if (apkFile.exists() == false) {
                continue
            }
            val apkUri = FileProvider.getUriForFile(
                this,
                packageName + ".provider",
                apkFile
            )
            uris.add(apkUri)
        }

        if (uris.isEmpty()) {
            Toast.makeText(
                this,
                getString(R.string.please_select_at_least_one_item),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val intent: Intent
        if (uris.size == 1) {
            intent = Intent(Intent.ACTION_SEND)
            intent.type = "application/vnd.android.package-archive"
            intent.putExtra(Intent.EXTRA_STREAM, uris[0])
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            intent = Intent(Intent.ACTION_SEND_MULTIPLE)
            intent.type = "application/vnd.android.package-archive"
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            startActivity(Intent.createChooser(intent, getString(R.string.send)))
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.send), Toast.LENGTH_SHORT).show()
        }
    }

    private fun getSelectedApks(): List<AppInfo> {
        val selectedPaths = viewModel.selectedApkFilePaths.value
        val result = mutableListOf<AppInfo>()

        val notInstalledList = viewModel.notInstalledApkFiles.value
        for (apk in notInstalledList) {
            if (selectedPaths.contains(apk.apkFilePath)) {
                result.add(apk)
            }
        }

        val deletableList = viewModel.deletableApkFiles.value
        for (apk in deletableList) {
            if (selectedPaths.contains(apk.apkFilePath)) {
                result.add(apk)
            }
        }

        return result
    }
    private fun updateSelectionCount() {
        val count = viewModel.selectedApkFilePaths.value.size
        binding.layoutToolbar.tvCountSong.text = count.toString()
    }

    private fun openSearch() {
        if (isSearchMode) {
            return
        }
        isSearchMode = true

        if (viewModel.isSelectionMode.value) {
            viewModel.exitSelectionMode()
        }

        binding.tabLayout.gone()
        binding.layoutSearch.visible()
        binding.edtSearch.requestFocus()
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(binding.edtSearch, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun closeSearch() {
        if (isSearchMode == false) {
            return
        }
        isSearchMode = false

        viewModel.updateSearchQuery("")
        binding.edtSearch.setText("")
        Utils.hideKeyboard(this)

        binding.layoutSearch.gone()
        binding.tabLayout.visible()
    }

    override fun onBack() {
        if (viewModel.isSelectionMode.value) {
            viewModel.exitSelectionMode()
        } else if (isSearchMode) {
            closeSearch()
        } else {
            finish()
        }
    }

    companion object {
        private const val SEARCH_APP_FRAGMENT_TAG = "SearchAppFragment"
    }

}