package com.example.basekotlin.ui.permission

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.View
import android.widget.CompoundButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.example.basekotlin.base.BaseActivity
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.ActivityPermissionBinding
import com.example.basekotlin.ui.main.MainActivity
import com.example.basekotlin.util.PermissionManager
import com.example.basekotlin.util.SharedPreUtils

class PermissionActivity :
    BaseActivity<ActivityPermissionBinding>(ActivityPermissionBinding::inflate) {

    companion object {
        private const val MAX_PERMISSION_REQUEST_CLICKS = 2
    }

    private val sharedPreUtils = SharedPreUtils.getInstance()

    private var notificationClickCount = 0
    private var memoryClickCount = 0
    private var writeClickCount = 0

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            updateSwitchStates()
        }

    override fun initView() {
        notificationClickCount = sharedPreUtils.getNotificationPermissionClickCount(this)
        memoryClickCount = sharedPreUtils.getMemoryPermissionClickCount(this)
        updateSwitchStates()
    }

    override fun bindView() {
        binding.tvContinue.tap {
            goToMain()
        }

        bindPermissionRow(
            switch = binding.swNotification,
            rowLayout = binding.llNotification,
            onRequest = { handleNotificationPermission() }
        )
        bindPermissionRow(
            switch = binding.swMemory,
            rowLayout = binding.llMemory,
            onRequest = { handleMemoryPermission() }
        )
        bindPermissionRow(
            switch = binding.swWrite,
            rowLayout = binding.llWrite,
            onRequest = { handleWritePermission() }
        )
    }

    private fun bindPermissionRow(
        switch: CompoundButton,
        rowLayout: View,
        onRequest: () -> Unit
    ) {
        switch.setOnClickListener {
            onRequest()
        }
        rowLayout.setOnClickListener {
            if (switch.isEnabled) {
                onRequest()
            }
        }
    }

    private fun getReadPermission(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return Manifest.permission.READ_MEDIA_AUDIO
        } else {
            return Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    private fun handleNotificationPermission() {
        val isGranted = PermissionManager.checkNotificationPermission(this)
        if (!isGranted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (notificationClickCount < MAX_PERMISSION_REQUEST_CLICKS) {
                    notificationClickCount++
                    sharedPreUtils.setNotificationPermissionClickCount(this, notificationClickCount)
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    openNotificationSettings()
                }
            } else {
                openNotificationSettings()
            }
        }
    }

    private fun handleMemoryPermission() {
        val isGranted = PermissionManager.checkReadPermission(this)
        if (!isGranted) {
            if (memoryClickCount < MAX_PERMISSION_REQUEST_CLICKS) {
                memoryClickCount++
                sharedPreUtils.setMemoryPermissionClickCount(this, memoryClickCount)
                val permission = getReadPermission()
                requestPermissionLauncher.launch(permission)
            } else {
                openAppSettings()
            }
        }
    }

    private fun handleWritePermission() {
        val isGranted = PermissionManager.checkWritePermission(this)
        if (!isGranted) {
//            RingtoneHelper.openWriteSettings(this)
        }
    }

    override fun onResume() {
        super.onResume()
        updateSwitchStates()
    }

    private fun updateSwitchStates() {
        updateSwitchState(
            switch = binding.swNotification,
            isGranted = PermissionManager.checkNotificationPermission(this)
        )
        updateSwitchState(
            switch = binding.swMemory,
            isGranted = PermissionManager.checkReadPermission(this)
        )
        updateSwitchState(
            switch = binding.swWrite,
            isGranted = PermissionManager.checkWritePermission(this)
        )
    }

    private fun updateSwitchState(switch: CompoundButton, isGranted: Boolean) {
        switch.isChecked = isGranted
        switch.isEnabled = !isGranted
    }

    private fun openNotificationSettings() {
        val intent = Intent()
        intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        startActivity(intent)
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    @OptIn(UnstableApi::class)
    private fun goToMain() {
        startNextActivity(MainActivity::class.java, null)
        finishAffinity()
    }

    override fun onBack() {
        finishAffinity()
    }
}
