package com.example.basekotlin.ui.transfer.test

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseActivity
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.ActivityTransferTestBinding
import com.example.basekotlin.service.transfer.HotspotManager
import com.example.basekotlin.service.transfer.TransferService
import com.example.basekotlin.ui.transfer.model.ConnectionInfo
import com.example.basekotlin.ui.transfer.model.TransferFile
import com.example.basekotlin.ui.transfer.model.TransferState
import com.example.basekotlin.util.transfer.NetworkUtils
import com.example.basekotlin.util.transfer.QrCodeHelper
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransferTestActivity : BaseActivity<ActivityTransferTestBinding>(ActivityTransferTestBinding::inflate) {
    override fun bindView() {
        super.bindView()
    }
}
