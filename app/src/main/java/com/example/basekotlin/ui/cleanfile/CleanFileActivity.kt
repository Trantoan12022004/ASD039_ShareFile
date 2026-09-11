package com.example.basekotlin.ui.cleanfile

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseActivity
import com.example.basekotlin.base.tap
import com.example.basekotlin.data.local.cleanfile.MessengerMediaScanner
import com.example.basekotlin.databinding.ActivityCleanFileBinding
import com.example.basekotlin.ui.cleanfile.apps.AppsCleanupActivity
import com.example.basekotlin.ui.cleanfile.apps.AppsCleanupViewModel
import com.example.basekotlin.ui.cleanfile.audiocleanup.AudioCleanupActivity
import com.example.basekotlin.ui.cleanfile.bigfiles.BigFilesActivity
import com.example.basekotlin.ui.cleanfile.duplicate.DuplicateFilesActivity
import com.example.basekotlin.ui.cleanfile.messenger.MessengerCleanerActivity
import com.example.basekotlin.ui.cleanfile.photocleanup.PhotoCleanupActivity
import com.example.basekotlin.ui.cleanfile.videocleanup.VideoCleanupActivity
import kotlinx.coroutines.launch

class CleanFileActivity : BaseActivity<ActivityCleanFileBinding>(ActivityCleanFileBinding::inflate) {

    private val viewModel: CleanFileViewModel by viewModels()

    override fun initView() {
        // Khởi tạo ban đầu
        Glide.with(this)
            .load(R.drawable.trash)
            .placeholder(R.drawable.junk)
            .error(R.drawable.junk)
            .into(binding.imgJunkIcon)
    }

    override fun onResume() {
        super.onResume()
        // Tải lại dữ liệu khi quay lại màn hình
        viewModel.loadData()
    }

    override fun bindView() {
        // Nút Back
        binding.btnBack.tap {
            finishThisActivity()
        }

        // Quan sát dữ liệu junk
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.junkSizeBytes.collect { size ->
                    binding.tvJunkSize.text = CleanFileUtils.formatFileSize(size)
                }
            }
        }

// 1. Apps Cleanup
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.appsCleanupInfo.collect { info ->
                    binding.tvAppsDesc.text = info
                }
            }
        }
        // 2. Big Files
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.bigFilesInfo.collect { info ->
                    binding.tvBigFilesDesc.text = info
                }
            }
        }
        // 3. Video Clean
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.videoCleanInfo.collect { info ->
                    binding.tvVideoDesc.text = info
                }
            }
        }
        // 4. Photo Clean
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.photoCleanInfo.collect { info ->
                    binding.tvPhotoDesc.text = info
                }
            }
        }
        // 5. Audio Clean
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.audioCleanInfo.collect { info ->
                    binding.tvAudioDesc.text = info
                }
            }
        }
        // 6. Duplicate
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.duplicateInfo.collect { info ->
                    binding.tvDuplicateDesc.text = info
                }
            }
        }

        // Nút Clean Up More
        binding.btnCleanUpMore.tap {
            Toast.makeText(this, getString(R.string.cleaning_junk), Toast.LENGTH_SHORT).show()
            viewModel.cleanJunk { success ->
                val msg = if (success) getString(R.string.junk_clean_success) else getString(R.string.junk_clean_success)
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        }

        // Quan sát Telegram
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.telegramInstalled.collect { installed ->
                    if (!installed) {
                        binding.tvTelegramStatus.text = getString(R.string.not_installed)
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.telegramSizeBytes.collect { size ->
                    if (viewModel.telegramInstalled.value) {
                        binding.tvTelegramStatus.text = CleanFileUtils.formatFileSize(size)
                    }
                }
            }
        }

        // Quan sát WhatsApp
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.whatsappInstalled.collect { installed ->
                    if (!installed) {
                        binding.tvWhatsappStatus.text = getString(R.string.not_installed)
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.whatsappSizeBytes.collect { size ->
                    if (viewModel.whatsappInstalled.value) {
                        binding.tvWhatsappStatus.text = CleanFileUtils.formatFileSize(size)
                    }
                }
            }
        }

        // CÁC THẺ SYSTEM CLEANERS
        binding.cardAppsCleanup.tap {
            startNextActivity(AppsCleanupActivity::class.java, null)
        }

        binding.cardBigFiles.tap {
            startNextActivity(BigFilesActivity::class.java, null)
        }

        binding.cardVideoCleanup.tap {
            startNextActivity(VideoCleanupActivity::class.java, null)
        }

        binding.cardPhotoCleanup.tap {
            startNextActivity(PhotoCleanupActivity::class.java, null)
        }

        binding.cardAudioCleanup.tap {
            startNextActivity(AudioCleanupActivity::class.java, null)
        }

        binding.cardDuplicateFiles.tap {
            startNextActivity(DuplicateFilesActivity::class.java, null)
        }

        // CÁC THẺ MESSENGER CLEANERS
        binding.cardTelegramCleaner.tap {
            if (viewModel.telegramInstalled.value) {
                val bundle = Bundle().apply {
                    putString(MessengerCleanerActivity.EXTRA_MESSENGER_TYPE, MessengerMediaScanner.MESSENGER_TELEGRAM)
                }
                startNextActivity(MessengerCleanerActivity::class.java, bundle)
            } else {
                Toast.makeText(this, getString(R.string.app_not_installed_msg), Toast.LENGTH_SHORT).show()
            }
        }

        binding.cardWhatsappCleaner.tap {
            if (viewModel.whatsappInstalled.value) {
                val bundle = Bundle().apply {
                    putString(MessengerCleanerActivity.EXTRA_MESSENGER_TYPE, MessengerMediaScanner.MESSENGER_WHATSAPP)
                }
                startNextActivity(MessengerCleanerActivity::class.java, bundle)
            } else {
                Toast.makeText(this, getString(R.string.app_not_installed_msg), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
