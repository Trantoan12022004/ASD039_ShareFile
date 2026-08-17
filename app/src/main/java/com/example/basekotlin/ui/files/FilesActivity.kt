package com.example.basekotlin.ui.files

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseActivity
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.ActivityFilesBinding
import com.example.basekotlin.dialog.exit.ExitAppDialog
import com.example.basekotlin.ui.files.apps.AppsActivity
import com.example.basekotlin.ui.files.documents.DocumentsActivity
import com.example.basekotlin.ui.files.music.MusicActivity
import com.example.basekotlin.ui.files.photos.PhotosActivity
import kotlinx.coroutines.launch
import kotlin.getValue

class FilesActivity : BaseActivity<ActivityFilesBinding>(ActivityFilesBinding::inflate) {
    private val viewModel: FilesViewModel by viewModels()
    override fun initView() {
    }
    override fun bindView() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.storageInfo.collect { info ->
                    binding.progressStorage.progress = info.usedPercentage
                    binding.tvStorageInfo.text = info.formattedDisplay
                }
            }
        }
        binding.layoutToolbar.btnBack.tap {
            onBack()
        }
        binding.layoutScan.tap {
            viewModel.loadStorageInfo()
            Toast.makeText(this, "Scanning local files...", Toast.LENGTH_SHORT).show()
        }
        binding.btnMusic.tap {
            startNextActivity(MusicActivity::class.java, null)
        }
        binding.btnApp.tap {
            startNextActivity(AppsActivity::class.java, null)
        }
        binding.btnDocument.tap {
            val bundle = Bundle().apply {
                putInt(com.example.basekotlin.ui.files.documents.DocumentsActivity.EXTRA_TAB_INDEX, 1) // Tab All
            }
            startNextActivity(com.example.basekotlin.ui.files.documents.DocumentsActivity::class.java, bundle)
        }
        binding.btnPdf.tap {
            val bundle = Bundle().apply {
                putInt(DocumentsActivity.EXTRA_TAB_INDEX, 2) // Tab PDF
            }
            startNextActivity(com.example.basekotlin.ui.files.documents.DocumentsActivity::class.java, bundle)
        }

        binding.btnPhoto.tap {
            startNextActivity(PhotosActivity::class.java, null)
        }

    }
}